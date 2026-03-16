package com.dataflow.DataTable.service;

import com.dataflow.DataTable.model.AiTrainingData;
import com.dataflow.DataTable.model.DataTableSchema;
import com.dataflow.DataTable.repository.AiTrainingDataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiTrainingDataService {

    private final DataTableService dataTableService;
    private final RecordService recordService;
    private final AiTrainingDataRepository trainingDataRepository;
    private final ObjectMapper objectMapper;

    public AiTrainingDataService(DataTableService dataTableService,
            RecordService recordService,
            AiTrainingDataRepository trainingDataRepository,
            ObjectMapper objectMapper) {
        this.dataTableService = dataTableService;
        this.recordService = recordService;
        this.trainingDataRepository = trainingDataRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Build comprehensive training data about the current database state
     */
    public String buildDatabaseContext(String applicationId) {
        StringBuilder context = new StringBuilder();

        context.append("=== DYNAMIC DATABASE STATE ===\n\n");

        // Get all tables for the application
        List<DataTableSchema> tables = applicationId != null ? 
            dataTableService.getTablesByApplicationId(applicationId) : 
            dataTableService.getAllTables();

        if (tables.isEmpty()) {
            context.append("No tables exist in the database yet.\n");
        } else {
            context.append("Total Tables: ").append(tables.size()).append("\n\n");

            // Detailed information about each table
            for (DataTableSchema table : tables) {
                context.append("TABLE: ").append(table.getTableName()).append("\n");
                context.append("  ID: ").append(table.getId()).append("\n");
                context.append("  Description: ")
                        .append(table.getDescription() != null ? table.getDescription() : "N/A")
                        .append("\n");

                try {
                    long count = recordService.getRecordCount(table.getId());
                    context.append("  Record Count: ").append(count).append("\n");
                } catch (Exception e) {
                    context.append("  Record Count: Unable to fetch\n");
                }

                context.append("  Columns:\n");
                for (DataTableSchema.ColumnDefinition column : table.getColumns()) {
                    context.append("    - ").append(column.getName())
                            .append(" (").append(column.getDataType()).append(")");

                    List<String> attributes = new ArrayList<>();
                    if (column.isRequired())
                        attributes.add("required");
                    if (column.isUnique())
                        attributes.add("unique");
                    if (column.isIndexed())
                        attributes.add("indexed");
                    if (column.getDefaultValue() != null) {
                        attributes.add("default: " + column.getDefaultValue());
                    }

                    if (!attributes.isEmpty()) {
                        context.append(" [").append(String.join(", ", attributes)).append("]");
                    }
                    context.append("\n");
                }
                context.append("\n");
            }
        }

        // Include stored training data
        context.append(buildStoredTrainingContext());

        return context.toString();
    }

    /**
     * Build context from stored training data in MongoDB
     */
    public String buildStoredTrainingContext() {
        List<AiTrainingData> storedData = trainingDataRepository.findByActiveTrueOrderByPriorityDesc();
        if (storedData.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("\n=== STORED TRAINING KNOWLEDGE ===\n\n");

        // Group by category for better organization
        Map<String, List<AiTrainingData>> groupedData = storedData.stream()
                .collect(Collectors.groupingBy(AiTrainingData::getCategory));

        for (Map.Entry<String, List<AiTrainingData>> entry : groupedData.entrySet()) {
            context.append("CATEGORY: ").append(entry.getKey().toUpperCase()).append("\n");
            for (AiTrainingData data : entry.getValue()) {
                context.append("- ").append(data.getTitle()).append(": ");
                context.append(data.getDescription()).append("\n");
                if (data.getData() != null && !data.getData().isEmpty()) {
                    try {
                        context.append("  Data: ").append(objectMapper.writeValueAsString(data.getData())).append("\n");
                    } catch (Exception e) {
                        context.append("  Data: ").append(data.getData().toString()).append("\n");
                    }
                }
            }
            context.append("\n");
        }

        return context.toString();
    }

    /**
     * Save new training data
     */
    public AiTrainingData saveTrainingData(AiTrainingData trainingData) {
        trainingData.setUpdatedAt(java.time.LocalDateTime.now());
        return trainingDataRepository.save(trainingData);
    }

    /**
     * Initialize default training data if none exists
     */
    @PostConstruct
    public void initDefaultTrainingData() {
        if (trainingDataRepository.count() == 0) {
            // Add API Info
            AiTrainingData apiInfo = new AiTrainingData();
            apiInfo.setCategory("api");
            apiInfo.setTitle("DataTable API Endpoints");
            apiInfo.setDescription("Information about available REST endpoints for table and record management.");
            apiInfo.setData(Map.of(
                    "tables", "/api/datatables/tables",
                    "records", "/api/datatables/tables/{tableId}/records",
                    "search", "/api/datatables/tables/{tableId}/search"));
            trainingDataRepository.save(apiInfo);

            // Add Schema Info
            AiTrainingData schemaInfo = new AiTrainingData();
            schemaInfo.setCategory("schema");
            schemaInfo.setTitle("DataTable Schema Definition");
            schemaInfo.setDescription("The core model for defining tables and columns.");
            schemaInfo.setData(Map.of(
                    "model", "DataTableSchema",
                    "fields", List.of("tableName", "description", "columns"),
                    "columnFields", List.of("name", "dataType", "required", "unique", "indexed", "defaultValue")));
            trainingDataRepository.save(schemaInfo);

            System.out.println("Default AI training data initialized.");
        }
    }

    /**
     * Synchronizes all current system knowledge (Tables, Records, APIs) into the
     * persistent Training Data collection.
     */
    public Map<String, Object> syncAllSystemKnowledge() {
        trainingDataRepository.deleteAll(); // Optional: Start fresh

        int tableCount = syncDynamicTables();
        int codebaseCount = syncCodebaseKnowledge();

        return Map.of(
                "status", "success",
                "syncedTables", tableCount,
                "codebaseKnowledgeEntries", codebaseCount,
                "timestamp", java.time.LocalDateTime.now());
    }

    private int syncDynamicTables() {
        int count = 0;
        List<DataTableSchema> tables = dataTableService.getAllTables();
        for (DataTableSchema table : tables) {
            AiTrainingData schemaData = new AiTrainingData();
            schemaData.setCategory("schema");
            schemaData.setType("dynamic_table");
            schemaData.setTitle("Dynamic Table: " + table.getTableName());
            schemaData.setDescription("Current runtime definition of the " + table.getTableName() + " table.");
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = objectMapper.convertValue(table, Map.class);
            schemaData.setData(dataMap);
            schemaData.setPriority(10);
            trainingDataRepository.save(schemaData);
            count++;
        }
        return count;
    }

    /**
     * Generates deep training data specifically from the codebase analysis.
     * Maps every AI Action to its internal purpose and parameter requirements.
     */
    public int syncCodebaseKnowledge() {
        int count = 0;

        // 1. Core Schema Management
        count += syncActionDocumentation("CREATE_TABLE", "Blueprint for creating new dynamic Mongo collections.",
                Map.of("params", List.of("tableName (String)", "columns (List<ColDef>)"), "logic",
                        "Validates schema -> Saves to Mongo -> Creates multi-indexes"));

        count += syncActionDocumentation("LIST_TABLES", "Lists all current business schemas.",
                Map.of("logic", "Queries the 'data_table_schemas' collection."));

        // 2. Data Persistence (Single & Multi)
        count += syncActionDocumentation("INSERT_RECORD", "Persists a single data document.",
                Map.of("params", List.of("tableName (String)", "records (Map)"), "note",
                        "Uses 'records' key for backward compatibility."));

        count += syncActionDocumentation("INSERT_MULTI_RECORDS", "High-performance batch insertion.",
                Map.of("params", List.of("tableName (String)", "records (List<Map>)"), "logic",
                        "Transactional batch insert with schema validation per item."));

        // 3. Search & Intelligence
        count += syncActionDocumentation("SEARCH_RECORDS", "Full-text cross-field query.",
                Map.of("params", List.of("tableName (String)", "query (String)"), "logic",
                        "Uses the 'searchText' field which aggregates all record data."));

        count += syncActionDocumentation("SEARCH_BY_FIELD", "Targeted column querying.",
                Map.of("params", List.of("tableName (String)", "field (String)", "value (Object)"), "logic",
                        "Handles dynamic casting (e.g. text 'true' becomes Boolean true) before query."));

        // 4. Utility & Metadata
        count += syncActionDocumentation("GET_RECORD_COUNT", "Real-time record statistics.",
                Map.of("params", "tableName (String)", "logic", "Fast count query via record repository."));

        count += syncActionDocumentation("UPDATE_RECORD", "Modifies an existing document content.",
                Map.of("params", List.of("recordId (String)", "data (Map)"), "logic",
                        "Fetches existing -> replaces data -> rebuilds search text -> saves."));

        return count;
    }

    private int syncActionDocumentation(String actionName, String desc, Map<String, Object> techDetails) {
        AiTrainingData data = new AiTrainingData();
        data.setCategory("api_logic");
        data.setType("action_documentation");
        data.setTitle("Logic: " + actionName);
        data.setDescription(desc);
        data.setData(techDetails);
        data.setPriority(10);
        trainingDataRepository.save(data);
        return 1;
    }

    // Previous helper methods maintained or adapted...

    public String buildTableContext(String tableNameOrId, String applicationId) {
        StringBuilder context = new StringBuilder();
        try {
            Optional<DataTableSchema> tableOpt = dataTableService.getTableById(tableNameOrId);
            if (tableOpt.isEmpty()) {
                tableOpt = dataTableService.getTableByName(tableNameOrId, applicationId);
            }
            if (tableOpt.isEmpty()) {
                return "Table '" + tableNameOrId + "' not found.";
            }
            DataTableSchema table = tableOpt.get();
            
            // Validate application context if provided
            if (applicationId != null && !applicationId.equals(table.getApplicationId())) {
                return "Table '" + tableNameOrId + "' exists but does not belong to application '" + applicationId + "'.";
            }

            context.append("TABLE DETAILS: ").append(table.getTableName()).append("\n");
            context.append("ID: ").append(table.getId()).append("\n");
            context.append("SCHEMA:\n");
            for (DataTableSchema.ColumnDefinition column : table.getColumns()) {
                context.append("  ").append(column.getName()).append(": ").append(column.getDataType()).append("\n");
            }
        } catch (Exception e) {
            context.append("Error: ").append(e.getMessage());
        }
        return context.toString();
    }

    public Map<String, Object> buildDataStatistics() {
        Map<String, Object> stats = new HashMap<>();
        List<DataTableSchema> tables = dataTableService.getAllTables();
        stats.put("totalTables", tables.size());
        return stats;
    }
}
