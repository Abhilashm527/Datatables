package com.dataflow.DataTable.service;

import com.dataflow.DataTable.config.GeminiClient;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final GeminiClient geminiClient;

    public AiService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public String ask(String input) {
        return geminiClient.generateText(input);
    }
}
