package com.dataflow.DataTable.service;

import com.dataflow.DataTable.model.AiTrainingData;
import com.dataflow.DataTable.repository.AiTrainingDataRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiActionCatalogService {

    private final AiTrainingDataRepository trainingDataRepository;

    public AiActionCatalogService(AiTrainingDataRepository trainingDataRepository) {
        this.trainingDataRepository = trainingDataRepository;
    }

    /**
     * Generates and stores the complete AI Action Catalog in MongoDB.
     * This acts as the "source of truth" for the AI model to know every capability.
     */
    public List<AiTrainingData> generateFullActionCatalog() {
        List<AiTrainingData> catalog = new ArrayList<>();

        // 1. Table Management Actions
        catalog.add(createActionEntry("LIST_TABLES", "list_tables",
                "Retrieve all dynamic table schemas in the system.",
                Map.of("parameters", Map.of(), "returns", "List of DataTableSchema objects")));

        catalog.add(createActionEntry("CREATE_TABLE", "create_table",
                "Create a new table definition with columns and types.",
                Map.of("parameters", Map.of(
                        "tableName", "String (Required)",
                        "description", "String (Optional)",
                        "columns", "List of ColumnDefinition (name, dataType, required, unique, indexed)"))));

        // 2. Data Ingestion Actions (Single and bulk)
        catalog.add(createActionEntry("INSERT_RECORD", "insert_single",
                "Insert a single JSON record into a table.",
                Map.of("parameters", Map.of(
                        "tableName", "String (Name or ID)",
                        "records", "Map/Object containing the record fields"), "note",
                        "Uses 'records' key for historical matching purposes")));

        catalog.add(createActionEntry("INSERT_MULTI_RECORDS", "insert_bulk",
                "Insert a list of multiple JSON records efficiently.",
                Map.of("parameters", Map.of(
                        "tableName", "String (Name or ID)",
                        "records", "List of Maps containing record data"))));

        // 3. Search and Query Actions
        catalog.add(createActionEntry("SEARCH_RECORDS", "fuzzy_search",
                "Fuzzy full-text search across all fields in a specific table.",
                Map.of("parameters", Map.of(
                        "tableName", "String",
                        "query", "The search string"))));

        catalog.add(createActionEntry("SEARCH_BY_FIELD", "exact_search",
                "Search for exact matches on a specific column with automatic type casting.",
                Map.of("parameters", Map.of(
                        "tableName", "String",
                        "field", "Column name",
                        "value", "The exact value to match"))));

        // 4. Performance Actions
        catalog.add(createActionEntry("CREATE_INDEX", "indexing",
                "Creates a MongoDB index on a specific column to speed up queries.",
                Map.of("parameters", Map.of(
                        "tableName", "String",
                        "columnName", "Column to index",
                        "unique", "Boolean (If true, prevents duplicates)"))));

        // Clean old catalog items and save new ones
        trainingDataRepository.deleteAll(); // Start fresh for catalog sync
        return trainingDataRepository.saveAll(catalog);
    }

    private AiTrainingData createActionEntry(String actionName, String type, String desc, Map<String, Object> data) {
        AiTrainingData entry = new AiTrainingData();
        entry.setCategory("action_capability");
        entry.setType(type);
        entry.setTitle("AI Action: " + actionName);
        entry.setDescription(desc);
        entry.setData(data);
        entry.setPriority(10);
        entry.setTags(List.of("capability", "api", actionName));
        return entry;
    }
}
