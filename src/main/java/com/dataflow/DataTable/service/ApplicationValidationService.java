package com.dataflow.DataTable.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class ApplicationValidationService {

    private final WebClient webClient;

    @Value("${application.manager.url}")
    private String applicationManagerUrl;

    public ApplicationValidationService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public boolean validateApplication(String applicationId, String bearerToken) {
        if (applicationId == null || applicationId.isEmpty()) {
            return false;
        }

        try {
            String url = applicationManagerUrl + "/" + applicationId;
            
            // If the token already starts with "Bearer ", use it, otherwise add it.
            String authHeader = bearerToken.startsWith("Bearer ") ? bearerToken : "Bearer " + bearerToken;

            Mono<Object> response = webClient.get()
                    .uri(url)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .bodyToMono(Object.class);

            // Execute synchronously for validation in this context
            response.block();
            return true;
        } catch (WebClientResponseException.NotFound e) {
            return false;
        } catch (Exception e) {
            // Log the error and assume invalid for safety, or handle as needed
            System.err.println("Error validating application: " + e.getMessage());
            return false;
        }
    }
}
