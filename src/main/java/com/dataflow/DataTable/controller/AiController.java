package com.dataflow.DataTable.controller;

import com.dataflow.DataTable.model.AiTrainingData;
import com.dataflow.DataTable.service.AiAssistantService;
import com.dataflow.DataTable.service.AiTrainingDataService;
import com.dataflow.DataTable.service.ConversationalAiService;
import com.dataflow.DataTable.service.MemoryAwareAiService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

import static com.dataflow.DataTable.config.APIConstants.AI_BASE_PATH;

@RestController
@RequestMapping(AI_BASE_PATH)
@CrossOrigin(origins = "*")
public class AiController {

    private final AiAssistantService aiAssistantService;
    private final ConversationalAiService conversationalAiService;
    private final MemoryAwareAiService memoryAwareAiService;
    private final AiTrainingDataService trainingDataService;

    public AiController(
            AiAssistantService aiAssistantService,
            ConversationalAiService conversationalAiService,
            MemoryAwareAiService memoryAwareAiService,
            AiTrainingDataService trainingDataService) {
        this.aiAssistantService = aiAssistantService;
        this.conversationalAiService = conversationalAiService;
        this.memoryAwareAiService = memoryAwareAiService;
        this.trainingDataService = trainingDataService;
    }

    @PostMapping("/training")
    public AiTrainingData addTrainingData(
            @RequestBody AiTrainingData data) {
        return trainingDataService.saveTrainingData(data);
    }

    @PostMapping("/sync-knowledge")
    public Map<String, Object> syncKnowledge() {
        return trainingDataService.syncAllSystemKnowledge();
    }

    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody Map<String, String> req) {
        String prompt = req.get("prompt");
        return aiAssistantService.processPrompt(prompt);
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> req) {
        String prompt = req.get("prompt");
        return conversationalAiService.processConversation(prompt);
    }

    /**
     * Memory-aware conversation with session support
     */
    @PostMapping("/conversation")
    public Map<String, Object> conversation(@RequestBody Map<String, String> req) {
        String prompt = req.get("prompt");
        String sessionId = req.getOrDefault("sessionId", UUID.randomUUID().toString());
        String userId = req.getOrDefault("userId", "default");

        return memoryAwareAiService.processWithMemory(prompt, sessionId, userId);
    }

    /**
     * Get conversation history
     */
    @GetMapping("/conversation/{sessionId}")
    public Map<String, Object> getConversationHistory(@PathVariable String sessionId) {
        return memoryAwareAiService.getConversationHistory(sessionId);
    }

    /**
     * End a conversation session
     */
    @DeleteMapping("/conversation/{sessionId}")
    public Map<String, Object> endConversation(@PathVariable String sessionId) {
        return memoryAwareAiService.endConversation(sessionId);
    }
}
