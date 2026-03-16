package com.dataflow.DataTable.service;

import com.dataflow.DataTable.config.GeminiClient;
import org.springframework.data.domain.Pageable;
import com.dataflow.DataTable.model.DataTableSchema;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiAssistantService {

    private final GeminiClient geminiClient;
    private final DataTableService dataTableService;
    private final RecordService recordService;
    private final ObjectMapper objectMapper;

    public AiAssistantService(GeminiClient geminiClient, DataTableService dataTableService,
            RecordService recordService) {
        this.geminiClient = geminiClient;
        this.dataTableService = dataTableService;
        this.recordService = recordService;
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> processPrompt(String userPrompt, String applicationId) {
        String aiResponse = null;
        try {
            // Build the system prompt with API documentation
            String systemPrompt = buildSystemPrompt()
                    + (applicationId != null ? "\nCURRENT APPLICATION ID: " + applicationId : "");
            String fullPrompt = systemPrompt + "\n\nUser Request: " + userPrompt;

            // Get AI response
            aiResponse = geminiClient.generateText(fullPrompt);

            System.out.println("=== AI Response ===");
            System.out.println(aiResponse);
            System.out.println("===================");

            // Parse the AI response to extract action and parameters
            Map<String, Object> parsedResponse = parseAiResponse(aiResponse);

            // Inject applicationId into parameters if present
            if (applicationId != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> parameters = (Map<String, Object>) parsedResponse.get("parameters");
                if (parameters != null) {
                    parameters.put("applicationId", applicationId);
                }
            }

            // Execute the action
            Object result = executeAction(parsedResponse);

            return Map.of(
                    "success", true,
                    "action", parsedResponse.getOrDefault("action", "unknown"),
                    "aiResponse", aiResponse,
                    "result", result);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("errorType", e.getClass().getSimpleName());
            if (aiResponse != null) {
                errorResponse.put("rawAiResponse", aiResponse);
            }
            e.printStackTrace(); // Log the full stack trace
            return errorResponse;
        }
    }

    private String buildSystemPrompt() {
        return """
                You are an AI assistant for a DataTable management system. You can help users create tables, insert records, search data, and manage indexes.

                Available Actions:
                1. CREATE_TABLE - Create a new data table with schema
                2. LIST_TABLES - List all tables in the system
                3. GET_TABLE - Get details of a specific table
                4. UPDATE_TABLE - Update table schema
                5. DELETE_TABLE - Delete a table
                6. INSERT_RECORD - Insert a record into a table
                7. GET_RECORD - Get a specific record by ID
                8. UPDATE_RECORD - Update an existing record
                9. DELETE_RECORD - Delete a single record
                10. DELETE_RECORDS - Delete multiple records
                11. SEARCH_RECORDS - Full-text search across all fields
                12. SEARCH_BY_FIELD - Search by specific field and value
                13. CREATE_INDEX - Create an index on a column
                14. DROP_INDEX - Remove an index from a column
                15. GET_RECORD_COUNT - Get total count of records in a table

                Data Types Supported: string, number, integer, boolean, date, array, object

                IMPORTANT NOTES:
                - When users mention a table name (like "users" or "products"), use the table name directly in tableId/tableName parameter
                - The system will automatically resolve table names to IDs
                - Users can provide either table ID or table name

                When responding, you MUST format your response as JSON with the following structure:
                {
                  "action": "ACTION_NAME",
                  "parameters": {
                    // action-specific parameters
                  },
                  "explanation": "Brief explanation of what you're doing"
                }

                For CREATE_TABLE action:
                {
                  "action": "CREATE_TABLE",
                  "parameters": {
                    "tableName": "table_name",
                    "description": "Table description",
                    "columns": [
                      {
                        "name": "column_name",
                        "dataType": "string|number|integer|boolean|date|array|object",
                        "required": true|false,
                        "unique": true|false,
                        "indexed": true|false,
                        "defaultValue": "optional_default_value"
                      }
                    ]
                  },
                  "explanation": "Creating a table for..."
                }

                For LIST_TABLES action:
                {
                  "action": "LIST_TABLES",
                  "parameters": {},
                  "explanation": "Listing all tables"
                }

                For GET_TABLE action:
                {
                  "action": "GET_TABLE",
                  "parameters": {
                    "tableName": "table_name_or_id"
                  },
                  "explanation": "Getting table details"
                }

                For DELETE_TABLE action:
                {
                  "action": "DELETE_TABLE",
                  "parameters": {
                    "tableName": "table_name_or_id"
                  },
                  "explanation": "Deleting table"
                }

                For INSERT_RECORD action:
                {
                  "action": "INSERT_RECORD",
                  "parameters": {
                    "tableName": "table_name_or_id",
                    "data": {
                      "field1": "value1",
                      "field2": "value2"
                    }
                  },
                  "explanation": "Inserting a record..."
                }

                For GET_RECORD action:
                {
                  "action": "GET_RECORD",
                  "parameters": {
                    "recordId": "record_id"
                  },
                  "explanation": "Getting record details"
                }

                For UPDATE_RECORD action:
                {
                  "action": "UPDATE_RECORD",
                  "parameters": {
                    "recordId": "record_id",
                    "data": {
                      "field1": "new_value1"
                    }
                  },
                  "explanation": "Updating record"
                }

                For DELETE_RECORD action:
                {
                  "action": "DELETE_RECORD",
                  "parameters": {
                    "recordId": "record_id"
                  },
                  "explanation": "Deleting record"
                }

                For SEARCH_RECORDS action (full-text search):
                {
                  "action": "SEARCH_RECORDS",
                  "parameters": {
                    "tableName": "table_name_or_id",
                    "query": "search_text"
                  },
                  "explanation": "Searching for records..."
                }

                For SEARCH_BY_FIELD action (search by specific field):
                {
                  "action": "SEARCH_BY_FIELD",
                  "parameters": {
                    "tableName": "table_name_or_id",
                    "field": "field_name",
                    "value": "search_value"
                  },
                  "explanation": "Searching by field..."
                }

                For CREATE_INDEX action:
                {
                  "action": "CREATE_INDEX",
                  "parameters": {
                    "tableName": "table_name_or_id",
                    "columnName": "column_name",
                    "unique": true|false
                  },
                  "explanation": "Creating index on..."
                }

                For DROP_INDEX action:
                {
                  "action": "DROP_INDEX",
                  "parameters": {
                    "tableName": "table_name_or_id",
                    "columnName": "column_name"
                  },
                  "explanation": "Dropping index from..."
                }

                For DELETE_RECORDS action (batch delete):
                {
                  "action": "DELETE_RECORDS",
                  "parameters": {
                    "recordIds": ["id1", "id2", "id3"]
                  },
                  "explanation": "Deleting records..."
                }

                For GET_RECORD_COUNT action:
                {
                  "action": "GET_RECORD_COUNT",
                  "parameters": {
                    "tableName": "table_name_or_id"
                  },
                  "explanation": "Getting record count"
                }

                IMPORTANT: Respond ONLY with valid JSON. Do not include any markdown formatting, code blocks, or additional text.
                """;
    }

    private Map<String, Object> parseAiResponse(String aiResponse) throws JsonProcessingException {
        // Clean up the response - remove markdown code blocks if present
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
            // Parse JSON response
            JsonNode jsonNode = objectMapper.readTree(cleanedResponse);

            // Validate required fields
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

    @SuppressWarnings("unchecked")
    private Object executeAction(Map<String, Object> parsedResponse) {
        String action = (String) parsedResponse.get("action");
        Map<String, Object> parameters = (Map<String, Object>) parsedResponse.get("parameters");
        String applicationId = (String) parameters.get("applicationId");

        return switch (action) {
            case "CREATE_TABLE" -> createTable(parameters);
            case "LIST_TABLES" -> listTables(applicationId);
            case "GET_TABLE" -> getTable(parameters);
            case "DELETE_TABLE" -> deleteTable(parameters);
            case "INSERT_RECORD" -> insertRecord(parameters);
            case "GET_RECORD" -> getRecord(parameters);
            case "UPDATE_RECORD" -> updateRecord(parameters);
            case "DELETE_RECORD" -> deleteRecord(parameters);
            case "DELETE_RECORDS" -> deleteRecords(parameters);
            case "SEARCH_RECORDS" -> searchRecords(parameters);
            case "SEARCH_BY_FIELD" -> searchByField(parameters);
            case "CREATE_INDEX" -> createIndex(parameters);
            case "DROP_INDEX" -> dropIndex(parameters);
            case "GET_RECORD_COUNT" -> getRecordCount(parameters);
            default -> Map.of("error", "Unknown action: " + action);
        };
    }

    // Resolves tableName from params (for record operations)
    private String resolveTableNameFromParams(Map<String, Object> parameters) {
        String tableName = (String) parameters.get("tableName");
        if (tableName != null)
            return tableName;
        String tableId = (String) parameters.get("tableId");
        if (tableId != null) {
            return dataTableService.getTableById(tableId)
                    .map(t -> t.getTableName())
                    .orElseThrow(() -> new RuntimeException("Table not found with id: " + tableId));
        }
        throw new RuntimeException("tableName or tableId is required");
    }

    // Resolves tableId from name (for schema operations like delete/update)
    private String resolveTableIdFromParams(Map<String, Object> parameters) {
        String tableId = (String) parameters.get("tableId");
        if (tableId != null)
            return tableId;
        String tableName = (String) parameters.get("tableName");
        String applicationId = (String) parameters.get("applicationId");
        if (tableName != null)
            return resolveTableId(tableName, applicationId);
        throw new RuntimeException("tableId or tableName is required");
    }

    private String resolveTableId(String tableNameOrId, String applicationId) {
        // First try to get by ID
        var tableById = dataTableService.getTableById(tableNameOrId);
        if (tableById.isPresent()) {
            return tableById.get().getId();
        }

        // Then try by name with application scope
        var tableByName = dataTableService.getTableByName(tableNameOrId, applicationId);
        if (tableByName.isPresent()) {
            return tableByName.get().getId();
        }

        throw new RuntimeException("Table not found: " + tableNameOrId
                + (applicationId != null ? " in application " + applicationId : ""));
    }

    @SuppressWarnings("unchecked")
    private Object createTable(Map<String, Object> parameters) {
        try {
            DataTableSchema schema = new DataTableSchema();
            schema.setApplicationId((String) parameters.get("applicationId"));
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

            return Map.of(
                    "success", true,
                    "tableId", createdSchema.getId(),
                    "tableName", createdSchema.getTableName(),
                    "message", "Table created successfully");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Object listTables(String applicationId) {
        try {
            List<DataTableSchema> tables;
            if (applicationId != null) {
                tables = dataTableService.getTablesByApplicationId(applicationId);
            } else {
                tables = dataTableService.getAllTables();
            }
            return Map.of(
                    "success", true,
                    "count", tables.size(),
                    "tables", tables);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Object getTable(Map<String, Object> parameters) {
        try {
            String tableName = (String) parameters.get("tableName");
            String applicationId = (String) parameters.get("applicationId");
            String tableId = resolveTableId(tableName, applicationId);
            var table = dataTableService.getTableById(tableId);

            if (table.isPresent()) {
                return Map.of(
                        "success", true,
                        "table", table.get());
            } else {
                return Map.of("success", false, "error", "Table not found");
            }
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Object deleteTable(Map<String, Object> parameters) {
        try {
            String tableName = (String) parameters.get("tableName");
            String applicationId = (String) parameters.get("applicationId");
            String tableId = resolveTableId(tableName, applicationId);
            dataTableService.deleteTable(tableId);
            return Map.of(
                    "success", true,
                    "message", "Table deleted successfully");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Object insertRecord(Map<String, Object> parameters) {
        try {
            String tableName = resolveTableNameFromParams(parameters);
            Map<String, Object> data = (Map<String, Object>) parameters.get("data");
            var record = recordService.insertRecord(tableName, data);
            return Map.of("success", true, "recordId", record.get("_id"), "message", "Record inserted successfully");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Object getRecord(Map<String, Object> parameters) {
        try {
            String tableName = resolveTableNameFromParams(parameters);
            String recordId = (String) parameters.get("recordId");
            var record = recordService.getRecord(tableName, recordId);
            return record.map(r -> Map.of("success", true, "record", r))
                    .orElse(Map.of("success", false, "error", "Record not found"));
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Object updateRecord(Map<String, Object> parameters) {
        try {
            String tableName = resolveTableNameFromParams(parameters);
            String recordId = (String) parameters.get("recordId");
            Map<String, Object> data = (Map<String, Object>) parameters.get("data");
            var updatedRecord = recordService.updateRecord(tableName, recordId, data);
            return Map.of("success", true, "recordId", updatedRecord.get("_id"), "message",
                    "Record updated successfully");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Object deleteRecord(Map<String, Object> parameters) {
        try {
            String tableName = resolveTableNameFromParams(parameters);
            String recordId = (String) parameters.get("recordId");
            recordService.deleteRecord(tableName, recordId);
            return Map.of("success", true, "message", "Record deleted successfully");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Object searchRecords(Map<String, Object> parameters) {
        try {
            String tableName = resolveTableNameFromParams(parameters);
            String query = (String) parameters.get("query");
            var records = recordService.searchRecordsByText(tableName, query, Pageable.unpaged()).getContent();
            return Map.of("success", true, "count", records.size(), "records", records);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Object searchByField(Map<String, Object> parameters) {
        try {
            String tableName = resolveTableNameFromParams(parameters);
            String field = (String) parameters.get("field");
            Object value = parameters.get("value");
            var records = recordService.searchRecords(tableName, field, value);
            return Map.of("success", true, "count", records.size(), "records", records);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Object createIndex(Map<String, Object> parameters) {
        try {
            String tableName = (String) parameters.get("tableName");
            String applicationId = (String) parameters.get("applicationId");
            String tableId = resolveTableId(tableName, applicationId);
            String columnName = (String) parameters.get("columnName");
            boolean unique = parameters.containsKey("unique") ? (Boolean) parameters.get("unique") : false;

            String indexName = dataTableService.createIndex(tableId, columnName, unique);
            return Map.of(
                    "success", true,
                    "indexName", indexName,
                    "message", "Index created successfully");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Object dropIndex(Map<String, Object> parameters) {
        try {
            String tableName = (String) parameters.get("tableName");
            String applicationId = (String) parameters.get("applicationId");
            String tableId = resolveTableId(tableName, applicationId);
            String columnName = (String) parameters.get("columnName");

            String indexName = dataTableService.dropIndex(tableId, columnName);
            return Map.of(
                    "success", true,
                    "indexName", indexName,
                    "message", "Index dropped successfully");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Object deleteRecords(Map<String, Object> parameters) {
        try {
            String tableId = resolveTableIdFromParams(parameters);
            List<String> recordIds = (List<String>) parameters.get("recordIds");
            recordService.deleteRecords(tableId, recordIds);
            return Map.of(
                    "success", true,
                    "deletedCount", recordIds.size(),
                    "message", "Records deleted successfully");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private Object getRecordCount(Map<String, Object> parameters) {
        try {
            String tableName = (String) parameters.get("tableName");
            String applicationId = (String) parameters.get("applicationId");
            String tableId = resolveTableId(tableName, applicationId);

            long count = recordService.getRecordCount(tableId);
            return Map.of(
                    "success", true,
                    "count", count);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
