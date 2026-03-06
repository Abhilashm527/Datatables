package com.dataflow.DataTable.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

        private final String apiKey;
        private final String baseUrl;
        private final WebClient webClient;

        private final Map<String, List<Map<String, Object>>> conversationContexts = new java.util.concurrent.ConcurrentHashMap<>();

        public GeminiClient(
                        @Value("${gemini.api-key}") String apiKey,
                        @Value("${gemini.url}") String url) {
                this.apiKey = apiKey;
                this.baseUrl = url.contains("/models/")
                                ? url.substring(0, url.indexOf("/models/"))
                                : url;

                this.webClient = WebClient.builder()
                                .baseUrl(this.baseUrl)
                                .defaultHeader("Content-Type", "application/json")
                                .build();
        }

        /**
         * Simple one-off text generation
         */
        public String generateText(String prompt) {
                List<Map<String, Object>> contents = List.of(
                                Map.of("role", "user", "parts", List.of(Map.of("text", prompt))));
                return callGemini(contents, null).get("text").toString();
        }

        /**
         * Stateful chat generation that manages history internally
         */
        public Map<String, Object> chat(String conversationId, String systemInstruction, String userPrompt) {
                List<Map<String, Object>> history = conversationContexts.computeIfAbsent(conversationId,
                                k -> new java.util.ArrayList<>());

                // Add user message to history
                history.add(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt))));

                // Get response from Gemini
                Map<String, Object> response = callGemini(history, systemInstruction);

                // Add model response to history
                history.add(Map.of("role", "model", "parts", List.of(Map.of("text", response.get("text")))));

                // Keep history manageable (last 20 messages)
                if (history.size() > 20) {
                        conversationContexts.put(conversationId, new java.util.ArrayList<>(
                                        history.subList(history.size() - 20, history.size())));
                }

                return response;
        }

        private Map<String, Object> callGemini(List<Map<String, Object>> contents, String systemInstruction) {
                Map<String, Object> body = new java.util.HashMap<>();
                body.put("contents", contents);

                if (systemInstruction != null && !systemInstruction.isEmpty()) {
                        body.put("system_instruction", Map.of(
                                        "parts", List.of(Map.of("text", systemInstruction))));
                }

                try {
                        String rawResponse = webClient.post()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/models/gemini-2.0-flash:generateContent")
                                                        .queryParam("key", apiKey)
                                                        .build())
                                        .bodyValue(body)
                                        .retrieve()
                                        .bodyToMono(String.class)
                                        .block();

                        return parseDetailedResponse(rawResponse);
                } catch (WebClientResponseException e) {
                        throw new RuntimeException(
                                        "Gemini API Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(),
                                        e);
                } catch (Exception e) {
                        throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
                }
        }

        private Map<String, Object> parseDetailedResponse(String rawResponse) {
                try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode root = mapper.readTree(rawResponse);
                        Map<String, Object> result = new java.util.HashMap<>();

                        if (root.has("candidates") && root.get("candidates").size() > 0) {
                                JsonNode candidate = root.get("candidates").get(0);
                                if (candidate.has("content")) {
                                        JsonNode content = candidate.get("content");
                                        if (content.has("parts") && content.get("parts").size() > 0) {
                                                result.put("text", content.get("parts").get(0).get("text").asText());
                                        }
                                }
                        }

                        // Extract usage metadata if available
                        if (root.has("usageMetadata")) {
                                result.put("usage", mapper.convertValue(root.get("usageMetadata"), Map.class));
                        }

                        // Add the response ID if the response contains it (some variants do)
                        if (root.has("responseId")) {
                                result.put("responseId", root.get("responseId").asText());
                        }

                        if (!result.containsKey("text")) {
                                throw new RuntimeException(
                                                "Could not extract text from Gemini response: " + rawResponse);
                        }

                        return result;
                } catch (Exception e) {
                        throw new RuntimeException("Failed to parse Gemini response: " + e.getMessage(), e);
                }
        }

        public void clearConversation(String conversationId) {
                conversationContexts.remove(conversationId);
        }
}
