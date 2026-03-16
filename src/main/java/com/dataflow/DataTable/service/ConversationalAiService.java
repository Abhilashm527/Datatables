package com.dataflow.DataTable.service;

import com.dataflow.DataTable.config.GeminiClient;
import org.springframework.data.domain.Pageable;
import com.dataflow.DataTable.model.DataTableSchema;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ConversationalAiService {

    private final GeminiClient geminiClient;
    private final DataTableService dataTableService;
    private final RecordService recordService;
    private final ObjectMapper objectMapper;
    private static final int MAX_ITERATIONS = 5; // Prevent infinite loops

    public ConversationalAiService(GeminiClient geminiClient,
            DataTableService dataTableService,
            RecordService recordService,
            ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.dataTableService = dataTableService;
        this.recordService = recordService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> processConversation(String userPrompt, String applicationId) {
        List<Map<String, Object>> conversationHistory = new ArrayList<>();
        Map<String, Object> context = new HashMap<>();
        if (applicationId != null) {
            context.put("applicationId", applicationId);
        }

        try {
            // Start the conversation
            Map<String, Object> finalResult = executeConversationalLoop(userPrompt, conversationHistory, context, 0, applicationId);

            return Map.of(
                    "success", true,
                    "result", finalResult,
                    "conversationHistory", conversationHistory,
                    "totalSteps", conversationHistory.size());
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "conversationHistory", conversationHistory);
        }
    }

    private Map<String, Object> executeConversationalLoop(
            String currentPrompt,
            List<Map<String, Object>> history,
            Map<String, Object> context,
            int iteration,
            String applicationId) throws Exception {

        if (iteration >= MAX_ITERATIONS) {
            return Map.of(
                    "status", "max_iterations_reached",
                    "message", "Reached maximum conversation depth",
                    "context", context);
        }

        System.out.println("\n=== Iteration " + (iteration + 1) + " ===");
        System.out.println("Prompt: " + currentPrompt);

        // Build the system prompt with context
        String systemPrompt = buildConversationalSystemPrompt(context, history);
        String fullPrompt = systemPrompt + "\n\nUser Request: " + currentPrompt;

        // Get AI response
        String aiResponse = geminiClient.generateText(fullPrompt);
        System.out.println("AI Response: " + aiResponse);

        // Parse the AI response
        Map<String, Object> parsedResponse = parseAiResponse(aiResponse);

        // Inject applicationId into parameters if present
        if (applicationId != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters_ = (Map<String, Object>) parsedResponse.get("parameters");
            if (parameters_ != null) {
                parameters_.put("applicationId", applicationId);
            }
        }

        // Record this step in history
        Map<String, Object> step = new HashMap<>();
        step.put("iteration", iteration + 1);
        step.put("prompt", currentPrompt);
        step.put("aiResponse", parsedResponse);

        String action = (String) parsedResponse.get("action");

        // Check if AI needs more information or wants to make another call
        if ("NEED_MORE_INFO".equals(action)) {
            // AI is asking for more information
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) parsedResponse.get("parameters");
            String nextAction = (String) params.get("nextAction");

            // Execute the information gathering action
            Object result = executeAction(nextAction, params, applicationId);
            step.put("result", result);
            history.add(step);

            // Update context with the result
            context.put(nextAction, result);

            // Recursively call with updated context
            String nextPrompt = (String) params.getOrDefault("nextPrompt", currentPrompt);
            return executeConversationalLoop(nextPrompt, history, context, iteration + 1, applicationId);

        } else if ("FINAL_ANSWER".equals(action)) {
            // AI has the final answer
            step.put("result", parsedResponse.get("parameters"));
            step.put("isFinal", true);
            history.add(step);
            return (Map<String, Object>) parsedResponse.get("parameters");

        } else {
            // Execute the action directly
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) parsedResponse.get("parameters");
            Object result = executeAction(action, params, applicationId);
            step.put("result", result);
            step.put("action", action);
            history.add(step);

            // Update context
            context.put(action, result);

            // Check if we should continue or this is the final result
            if (shouldContinueConversation(result, parsedResponse)) {
                String nextPrompt = buildNextPrompt(currentPrompt, result, parsedResponse);
                return executeConversationalLoop(nextPrompt, history, context, iteration + 1, applicationId);
            } else {
                return Map.of(
                        "action", action,
                        "result", result,
                        "context", context);
            }
        }
    }

    private String buildConversationalSystemPrompt(Map<String, Object> context, List<Map<String, Object>> history) {
        String applicationId = (String) context.get("applicationId");
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                You are an intelligent AI assistant for a DataTable management system with conversational capabilities.
                You can make multiple sequential calls to gather information and complete complex tasks.

                """ + (applicationId != null ? "CURRENT APPLICATION ID: " + applicationId + "\n\n" : "") + """
                Available Actions:
                1. LIST_TABLES - List all tables
                2. GET_TABLE - Get table schema
                3. CREATE_TABLE - Create a new table
                4. DELETE_TABLE - Delete a table
                5. INSERT_RECORD - Insert a record
                6. GET_RECORD - Get a specific record
                7. UPDATE_RECORD - Update a record
                8. DELETE_RECORD - Delete a record
                9. DELETE_RECORDS - Batch delete
                10. SEARCH_RECORDS - Full-text search
                11. SEARCH_BY_FIELD - Search by field
                12. CREATE_INDEX - Create index
                13. DROP_INDEX - Drop index
                14. GET_RECORD_COUNT - Count records
                15. NEED_MORE_INFO - Request more information (triggers another AI call)
                16. FINAL_ANSWER - Provide final answer to user

                CONVERSATIONAL FLOW:
                - If you need to gather information first (like finding a table name), use NEED_MORE_INFO
                - After gathering info, you'll be called again with the results in context
                - When you have everything needed, use the appropriate action or FINAL_ANSWER

                For NEED_MORE_INFO action:
                {
                  "action": "NEED_MORE_INFO",
                  "parameters": {
                    "nextAction": "LIST_TABLES|GET_TABLE|etc",
                    "reason": "Why you need this info",
                    "nextPrompt": "What to do after getting the info"
                  },
                  "explanation": "Explaining what info is needed"
                }

                For FINAL_ANSWER action:
                {
                  "action": "FINAL_ANSWER",
                  "parameters": {
                    "answer": "Your response to the user",
                    "data": {} // Any relevant data
                  },
                  "explanation": "Summary"
                }

                """);

        // Add context from previous calls
        if (!context.isEmpty()) {
            prompt.append("\nCONTEXT FROM PREVIOUS CALLS:\n");
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                prompt.append("- ").append(entry.getKey()).append(": ");
                try {
                    prompt.append(objectMapper.writeValueAsString(entry.getValue()));
                } catch (Exception e) {
                    prompt.append(entry.getValue().toString());
                }
                prompt.append("\n");
            }
        }

        // Add conversation history summary
        if (!history.isEmpty()) {
            prompt.append("\nPREVIOUS STEPS:\n");
            for (int i = 0; i < history.size(); i++) {
                Map<String, Object> step = history.get(i);
                prompt.append(i + 1).append(". ").append(step.get("action")).append("\n");
            }
        }

        prompt.append("\nIMPORTANT: Respond ONLY with valid JSON. No markdown, no code blocks.\n");

        return prompt.toString();
    }

    private Map<String, Object> parseAiResponse(String aiResponse) throws JsonProcessingException {
        String cleanedResponse = aiResponse.trim();
        if (cleanedResponse.startsWith("```json")) {
            cleanedResponse = cleanedResponse.substring(7);
        }
        if (cleanedResponse.startsWith("```")) {
            cleanedResponse = cleanedResponse.substring(3);
        }
        if (cleanedResponse.endsWith("```")) {
            cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
        }
        cleanedResponse = cleanedResponse.trim();

        try {
            JsonNode jsonNode = objectMapper.readTree(cleanedResponse);

            if (!jsonNode.has("action") || jsonNode.get("action") == null) {
                throw new RuntimeException("AI response missing 'action' field. Response: " + cleanedResponse);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("action", jsonNode.get("action").asText());

            if (jsonNode.has("parameters") && jsonNode.get("parameters").isObject()) {
                result.put("parameters", objectMapper.convertValue(jsonNode.get("parameters"), Map.class));
            } else {
                result.put("parameters", new HashMap<String, Object>());
            }

            result.put("explanation", jsonNode.has("explanation") && jsonNode.get("explanation") != null
                    ? jsonNode.get("explanation").asText()
                    : "");

            return result;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse AI response as JSON. Response was: " + cleanedResponse, e);
        }
    }

    private Object executeAction(String action, Map<String, Object> parameters, String applicationId) {
        try {
            return switch (action) {
                case "LIST_TABLES" -> listTables(applicationId);
                case "GET_TABLE" -> getTable(parameters, applicationId);
                case "CREATE_TABLE" -> createTable(parameters, applicationId);
                case "DELETE_TABLE" -> deleteTable(parameters, applicationId);
                case "INSERT_RECORD" -> insertRecord(parameters, applicationId);
                case "GET_RECORD" -> getRecord(parameters);
                case "UPDATE_RECORD" -> updateRecord(parameters);
                case "DELETE_RECORD" -> deleteRecord(parameters);
                case "DELETE_RECORDS" -> deleteRecords(parameters);
                case "SEARCH_RECORDS" -> searchRecords(parameters, applicationId);
                case "SEARCH_BY_FIELD" -> searchByField(parameters, applicationId);
                case "CREATE_INDEX" -> createIndex(parameters, applicationId);
                case "DROP_INDEX" -> dropIndex(parameters, applicationId);
                case "GET_RECORD_COUNT" -> getRecordCount(parameters, applicationId);
                default -> Map.of("error", "Unknown action: " + action);
            };
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private boolean shouldContinueConversation(Object result, Map<String, Object> parsedResponse) {
        // Check if the result suggests we need more steps
        if (result instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) result;

            // If there's a "continueWith" hint in the response
            if (parsedResponse.containsKey("continueWith")) {
                return true;
            }

            // If the result is empty or indicates we need more info
            if (resultMap.containsKey("success") && !((Boolean) resultMap.get("success"))) {
                return false; // Error, don't continue
            }
        }
        return false; // By default, don't continue
    }

    private String buildNextPrompt(String originalPrompt, Object result, Map<String, Object> parsedResponse) {
        // Use the explanation or build a prompt based on the result
        String explanation = (String) parsedResponse.getOrDefault("explanation", "");
        return "Based on the previous result, " + explanation + ". Original request: " + originalPrompt;
    }

    private String resolveTableNameFromParams(Map<String, Object> parameters) {
        String tableName = (String) parameters.get("tableName");
        if (tableName != null) return tableName;
        String tableId = (String) parameters.get("tableId");
        if (tableId != null) {
            return dataTableService.getTableById(tableId)
                    .map(t -> t.getTableName())
                    .orElseThrow(() -> new RuntimeException("Table not found with id: " + tableId));
        }
        throw new RuntimeException("tableName or tableId is required");
    }

    private String resolveTableId(String tableNameOrId, String applicationId) {
        var tableById = dataTableService.getTableById(tableNameOrId);
        if (tableById.isPresent()) return tableById.get().getId();
        var tableByName = dataTableService.getTableByName(tableNameOrId, applicationId);
        if (tableByName.isPresent()) return tableByName.get().getId();
        throw new RuntimeException("Table not found: " + tableNameOrId + (applicationId != null ? " in application " + applicationId : ""));
    }

    // All the action methods (same as before)
    private Object listTables(String applicationId) {
        List<DataTableSchema> tables;
        if (applicationId != null) {
            tables = dataTableService.getTablesByApplicationId(applicationId);
        } else {
            tables = dataTableService.getAllTables();
        }
        return Map.of("success", true, "count", tables.size(), "tables", tables);
    }

    private Object getTable(Map<String, Object> parameters, String applicationId) {
        String tableName = (String) parameters.get("tableName");
        String tableId = resolveTableId(tableName, applicationId);
        var table = dataTableService.getTableById(tableId);
        return table.map(t -> Map.of("success", true, "table", t))
                .orElse(Map.of("success", false, "error", "Table not found"));
    }

    @SuppressWarnings("unchecked")
    private Object createTable(Map<String, Object> parameters, String applicationId) {
        DataTableSchema schema = new DataTableSchema();
        schema.setApplicationId(applicationId);
        schema.setTableName((String) parameters.get("tableName"));
        schema.setDescription((String) parameters.get("description"));

        List<Map<String, Object>> columnsData = (List<Map<String, Object>>) parameters.get("columns");
        List<DataTableSchema.ColumnDefinition> columns = new ArrayList<>();

        for (Map<String, Object> colData : columnsData) {
            DataTableSchema.ColumnDefinition column = new DataTableSchema.ColumnDefinition();
            column.setName((String) colData.get("name"));
            column.setDataType((String) colData.get("dataType"));
            column.setRequired(colData.containsKey("required") ? (Boolean) colData.get("required") : false);
            column.setUnique(colData.containsKey("unique") ? (Boolean) colData.get("unique") : false);
            column.setIndexed(colData.containsKey("indexed") ? (Boolean) colData.get("indexed") : false);
            if (colData.containsKey("defaultValue")) {
                column.setDefaultValue(colData.get("defaultValue"));
            }
            columns.add(column);
        }

        schema.setColumns(columns);
        DataTableSchema createdSchema = dataTableService.createTable(schema);

        return Map.of("success", true, "tableId", createdSchema.getId(),
                "tableName", createdSchema.getTableName(), "message", "Table created successfully");
    }

    private Object deleteTable(Map<String, Object> parameters, String applicationId) {
        String tableName = (String) parameters.get("tableName");
        String tableId = resolveTableId(tableName, applicationId);
        dataTableService.deleteTable(tableId);
        return Map.of("success", true, "message", "Table deleted successfully");
    }

    @SuppressWarnings("unchecked")
    private Object insertRecord(Map<String, Object> parameters, String applicationId) {
        String tableName = resolveTableNameFromParams(parameters);
        Map<String, Object> data = (Map<String, Object>) parameters.get("data");
        var record = recordService.insertRecord(tableName, data);
        return Map.of("success", true, "recordId", record.get("_id"), "message", "Record inserted successfully");
    }

    private Object getRecord(Map<String, Object> parameters) {
        String tableName = resolveTableNameFromParams(parameters);
        String recordId = (String) parameters.get("recordId");
        var record = recordService.getRecord(tableName, recordId);
        return record.map(r -> Map.of("success", true, "record", r))
                .orElse(Map.of("success", false, "error", "Record not found"));
    }

    @SuppressWarnings("unchecked")
    private Object updateRecord(Map<String, Object> parameters) {
        String tableName = resolveTableNameFromParams(parameters);
        String recordId = (String) parameters.get("recordId");
        Map<String, Object> data = (Map<String, Object>) parameters.get("data");
        var updatedRecord = recordService.updateRecord(tableName, recordId, data);
        return Map.of("success", true, "recordId", updatedRecord.get("_id"), "message", "Record updated successfully");
    }

    private Object deleteRecord(Map<String, Object> parameters) {
        String tableName = resolveTableNameFromParams(parameters);
        String recordId = (String) parameters.get("recordId");
        recordService.deleteRecord(tableName, recordId);
        return Map.of("success", true, "message", "Record deleted successfully");
    }

    private Object searchRecords(Map<String, Object> parameters, String applicationId) {
        String tableName = resolveTableNameFromParams(parameters);
        String query = (String) parameters.get("query");
        var records = recordService.searchRecordsByText(tableName, query, Pageable.unpaged()).getContent();
        return Map.of("success", true, "count", records.size(), "records", records);
    }

    private Object searchByField(Map<String, Object> parameters, String applicationId) {
        String tableName = resolveTableNameFromParams(parameters);
        String field = (String) parameters.get("field");
        Object value = parameters.get("value");
        var records = recordService.searchRecords(tableName, field, value);
        return Map.of("success", true, "count", records.size(), "records", records);
    }

    private Object createIndex(Map<String, Object> parameters, String applicationId) {
        String tableName = (String) parameters.get("tableName");
        String tableId = resolveTableId(tableName, applicationId);
        String columnName = (String) parameters.get("columnName");
        boolean unique = parameters.containsKey("unique") ? (Boolean) parameters.get("unique") : false;
        String indexName = dataTableService.createIndex(tableId, columnName, unique);
        return Map.of("success", true, "indexName", indexName, "message", "Index created successfully");
    }

    private Object dropIndex(Map<String, Object> parameters, String applicationId) {
        String tableName = (String) parameters.get("tableName");
        String tableId = resolveTableId(tableName, applicationId);
        String columnName = (String) parameters.get("columnName");
        String indexName = dataTableService.dropIndex(tableId, columnName);
        return Map.of("success", true, "indexName", indexName, "message", "Index dropped successfully");
    }

    @SuppressWarnings("unchecked")
    private Object deleteRecords(Map<String, Object> parameters) {
        String tableName = (String) parameters.get("tableName");
        List<String> recordIds = (List<String>) parameters.get("recordIds");
        recordService.deleteRecords(tableName, recordIds);
        return Map.of("success", true, "deletedCount", recordIds.size(), "message", "Records deleted successfully");
    }

    private Object getRecordCount(Map<String, Object> parameters, String applicationId) {
        String tableName = (String) parameters.get("tableName");
        String tableId = resolveTableId(tableName, applicationId);
        long count = recordService.getRecordCount(tableId);
        return Map.of("success", true, "count", count);
    }
}
