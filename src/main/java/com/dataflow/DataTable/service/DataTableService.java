package com.dataflow.DataTable.service;

import com.dataflow.DataTable.model.DataTableRecord;
import com.dataflow.DataTable.model.DataTableSchema;
import com.dataflow.DataTable.repository.DataTableRecordRepository;
import com.dataflow.DataTable.repository.DataTableSchemaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DataTableService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private DataTableSchemaRepository schemaRepository;

    @Autowired
    private DataTableRecordRepository recordRepository;

    // Schema operations
    public DataTableSchema createTable(DataTableSchema schema) {
        if (schemaRepository.existsByTableName(schema.getTableName())) {
            throw new RuntimeException("Table with name '" + schema.getTableName() + "' already exists");
        }

        // Validate schema
        validateSchema(schema);

        DataTableSchema savedSchema = schemaRepository.save(schema);
        createIndexes(savedSchema);
        return savedSchema;
    }

    public List<DataTableSchema> getAllTables() {
        return schemaRepository.findAll();
    }

    public Optional<DataTableSchema> getTableByName(String tableName) {
        return schemaRepository.findByTableName(tableName);
    }

    public Optional<DataTableSchema> getTableById(String id) {
        return schemaRepository.findById(id);
    }

    public DataTableSchema updateTable(String id, DataTableSchema updatedSchema) {
        Optional<DataTableSchema> existingSchema = schemaRepository.findById(id);
        if (existingSchema.isEmpty()) {
            throw new RuntimeException("Table not found with id: " + id);
        }

        DataTableSchema schema = existingSchema.get();
        schema.setDescription(updatedSchema.getDescription());
        schema.setColumns(updatedSchema.getColumns());
        schema.setMetadata(updatedSchema.getMetadata());
        schema.setUpdatedAt(LocalDateTime.now());

        validateSchema(schema);

        DataTableSchema savedSchema = schemaRepository.save(schema);
        createIndexes(savedSchema);
        return savedSchema;
    }

    @Transactional
    public void deleteTable(String id) {
        Optional<DataTableSchema> schema = schemaRepository.findById(id);
        if (schema.isEmpty()) {
            throw new RuntimeException("Table not found with id: " + id);
        }

        // Delete all records associated with this table
        recordRepository.deleteByTableId(id);

        // Delete the schema
        schemaRepository.deleteById(id);
    }

    // Record operations
    public DataTableRecord insertRecord(String tableId, Map<String, Object> data) {
        Optional<DataTableSchema> schemaOpt = schemaRepository.findById(tableId);
        if (schemaOpt.isEmpty()) {
            throw new RuntimeException("Table not found with id: " + tableId);
        }

        DataTableSchema schema = schemaOpt.get();

        // Validate data against schema
        validateRecord(data, schema);

        // Apply default values
        Map<String, Object> processedData = applyDefaults(data, schema);

        DataTableRecord record = new DataTableRecord(tableId, processedData);
        record.setSearchText(buildSearchText(processedData));
        return recordRepository.save(record);
    }

    @Transactional
    public List<DataTableRecord> insertRecords(String tableId, List<Map<String, Object>> recordsData) {
        Optional<DataTableSchema> schemaOpt = schemaRepository.findById(tableId);
        if (schemaOpt.isEmpty()) {
            throw new RuntimeException("Table not found with id: " + tableId);
        }

        DataTableSchema schema = schemaOpt.get();
        List<DataTableRecord> recordsToSave = new ArrayList<>();

        for (Map<String, Object> data : recordsData) {
            validateRecord(data, schema);
            Map<String, Object> processedData = applyDefaults(data, schema);
            DataTableRecord record = new DataTableRecord(tableId, processedData);
            record.setSearchText(buildSearchText(processedData));
            recordsToSave.add(record);
        }

        return recordRepository.saveAll(recordsToSave);
    }

    public List<DataTableRecord> getRecords(String tableId) {
        return recordRepository.findByTableId(tableId);
    }

    public Page<DataTableRecord> getRecords(String tableId, Pageable pageable) {
        return recordRepository.findByTableId(tableId, pageable);
    }

    public Optional<DataTableRecord> getRecord(String recordId) {
        return recordRepository.findById(recordId);
    }

    public DataTableRecord updateRecord(String recordId, Map<String, Object> data) {
        Optional<DataTableRecord> recordOpt = recordRepository.findById(recordId);
        if (recordOpt.isEmpty()) {
            throw new RuntimeException("Record not found with id: " + recordId);
        }

        DataTableRecord record = recordOpt.get();

        // Get schema for validation
        Optional<DataTableSchema> schemaOpt = schemaRepository.findById(record.getTableId());
        if (schemaOpt.isEmpty()) {
            throw new RuntimeException("Table schema not found");
        }

        // Validate updated data
        validateRecord(data, schemaOpt.get());

        record.setData(data);
        record.setSearchText(buildSearchText(data));
        return recordRepository.save(record);
    }

    public void deleteRecord(String recordId) {
        if (!recordRepository.existsById(recordId)) {
            throw new RuntimeException("Record not found with id: " + recordId);
        }
        recordRepository.deleteById(recordId);
    }

    public void deleteRecords(List<String> recordIds) {
        recordRepository.deleteAllById(recordIds);
    }

    public List<DataTableRecord> searchRecords(String tableId, String fieldName, Object value) {
        Object typedValue = castToSchemaType(tableId, fieldName, value);
        Query query = new Query();
        query.addCriteria(Criteria.where("tableId").is(tableId));
        query.addCriteria(Criteria.where("data." + fieldName).is(typedValue));
        return mongoTemplate.find(query, DataTableRecord.class);
    }

    public Page<DataTableRecord> searchRecords(String tableId, String fieldName, Object value, Pageable pageable) {
        Object typedValue = castToSchemaType(tableId, fieldName, value);

        Query query = new Query();
        query.addCriteria(Criteria.where("tableId").is(tableId));
        query.addCriteria(Criteria.where("data." + fieldName).is(typedValue));

        long total = mongoTemplate.count(query, DataTableRecord.class);
        List<DataTableRecord> records = mongoTemplate.find(query.with(pageable), DataTableRecord.class);

        return new PageImpl<>(records, pageable, total);
    }

    private Object castToSchemaType(String tableId, String fieldName, Object value) {
        Optional<DataTableSchema> schemaOpt = schemaRepository.findById(tableId);
        if (schemaOpt.isEmpty()) {
            return value;
        }

        // Handle nested fields (e.g., "address.city")
        String lookupName = fieldName.contains(".") ? fieldName.split("\\.")[0] : fieldName;

        Optional<DataTableSchema.ColumnDefinition> columnOpt = schemaOpt.get().getColumns().stream()
                .filter(c -> c.getName().equals(lookupName))
                .findFirst();

        if (columnOpt.isPresent()) {
            String dataType = columnOpt.get().getDataType();
            String strVal = value.toString();

            // We only apply type casting if it's the root field or if we can infer.
            // For nested fields, we usually treat as string unless we have more metadata.
            // But if it's explicitly defined as one of our types, we cast it.

            if ("boolean".equalsIgnoreCase(dataType)) {
                return Boolean.parseBoolean(strVal);
            }
            if ("number".equalsIgnoreCase(dataType) || "integer".equalsIgnoreCase(dataType)) {
                try {
                    if (strVal.contains(".")) {
                        return Double.parseDouble(strVal);
                    }
                    return Long.parseLong(strVal);
                } catch (NumberFormatException e) {
                    // Log or handle, for now return as is
                    return value;
                }
            }
        }
        return value;
    }

    public List<DataTableRecord> searchRecords(String tableId, String query) {
        return recordRepository.findByTableIdAndSearchText(tableId, query);
    }

    public Page<DataTableRecord> searchRecords(String tableId, String query, Pageable pageable) {
        return recordRepository.findByTableIdAndSearchText(tableId, query, pageable);
    }

    public long getRecordCount(String tableId) {
        return recordRepository.countByTableId(tableId);
    }

    // Validation methods
    private void validateSchema(DataTableSchema schema) {
        if (schema.getTableName() == null || schema.getTableName().trim().isEmpty()) {
            throw new RuntimeException("Table name is required");
        }

        if (schema.getColumns() == null || schema.getColumns().isEmpty()) {
            throw new RuntimeException("At least one column is required");
        }

        // Check for duplicate column names
        Set<String> columnNames = new HashSet<>();
        for (DataTableSchema.ColumnDefinition column : schema.getColumns()) {
            if (column.getName() == null || column.getName().trim().isEmpty()) {
                throw new RuntimeException("Column name is required");
            }

            if (columnNames.contains(column.getName())) {
                throw new RuntimeException("Duplicate column name: " + column.getName());
            }

            columnNames.add(column.getName());

            if (column.getDataType() == null || column.getDataType().trim().isEmpty()) {
                throw new RuntimeException("Data type is required for column: " + column.getName());
            }
        }
    }

    private void validateRecord(Map<String, Object> data, DataTableSchema schema) {
        // Check required fields
        for (DataTableSchema.ColumnDefinition column : schema.getColumns()) {
            if (column.isRequired() &&
                    (!data.containsKey(column.getName()) || data.get(column.getName()) == null)) {
                throw new RuntimeException("Required field missing: " + column.getName());
            }
        }

        // Validate data types (basic validation)
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();

            // Find column definition
            Optional<DataTableSchema.ColumnDefinition> columnDef = schema.getColumns().stream()
                    .filter(col -> col.getName().equals(fieldName))
                    .findFirst();

            if (columnDef.isPresent() && value != null) {
                validateDataType(value, columnDef.get().getDataType(), fieldName);
            }
        }
    }

    private void validateDataType(Object value, String expectedType, String fieldName) {
        switch (expectedType.toLowerCase()) {
            case "string":
                if (!(value instanceof String)) {
                    throw new RuntimeException("Field '" + fieldName + "' must be a string");
                }
                break;
            case "number":
            case "integer":
                if (!(value instanceof Number)) {
                    throw new RuntimeException("Field '" + fieldName + "' must be a number");
                }
                break;
            case "boolean":
                if (!(value instanceof Boolean)) {
                    throw new RuntimeException("Field '" + fieldName + "' must be a boolean");
                }
                break;
            case "date":
                // Accept various date formats
                if (!(value instanceof String || value instanceof Date)) {
                    throw new RuntimeException("Field '" + fieldName + "' must be a date");
                }
                break;
            case "array":
                if (!(value instanceof List || value instanceof Object[])) {
                    throw new RuntimeException("Field '" + fieldName + "' must be an array");
                }
                break;
            case "object":
                if (!(value instanceof Map)) {
                    throw new RuntimeException("Field '" + fieldName + "' must be an object");
                }
                break;
            // Add more data types as needed
        }
    }

    private Map<String, Object> applyDefaults(Map<String, Object> data, DataTableSchema schema) {
        Map<String, Object> processedData = new HashMap<>(data);

        for (DataTableSchema.ColumnDefinition column : schema.getColumns()) {
            if (!processedData.containsKey(column.getName()) && column.getDefaultValue() != null) {
                processedData.put(column.getName(), column.getDefaultValue());
            }
        }

        return processedData;
    }

    private String buildSearchText(Object value) {
        StringBuilder sb = new StringBuilder();
        collectValues(value, sb);
        return sb.toString().toLowerCase().trim();
    }

    private void collectValues(Object value, StringBuilder sb) {

        if (value == null) {
            return;
        }

        // String
        if (value instanceof String) {
            String str = ((String) value).trim();
            if (!str.isEmpty()) {
                sb.append(str).append(" ");
            }
            return;
        }

        // Number / Boolean
        if (value instanceof Number || value instanceof Boolean) {
            sb.append(value).append(" ");
            return;
        }

        // Collection (List, Set)
        if (value instanceof Collection<?>) {
            for (Object v : (Collection<?>) value) {
                collectValues(v, sb);
            }
            return;
        }

        // Map (nested objects)
        if (value instanceof Map<?, ?>) {
            for (Object v : ((Map<?, ?>) value).values()) {
                collectValues(v, sb);
            }
            return;
        }

        // Fallback
        sb.append(value.toString()).append(" ");
    }

    public String createIndex(String tableId, String columnName, boolean unique) {
        Optional<DataTableSchema> schemaOpt = schemaRepository.findById(tableId);
        if (schemaOpt.isEmpty()) {
            throw new RuntimeException("Table not found with id: " + tableId);
        }

        DataTableSchema schema = schemaOpt.get();
        String fieldPath = "data." + columnName;
        String indexName = "idx_" + schema.getId() + "_" + columnName;

        // Check for exact column match
        Optional<DataTableSchema.ColumnDefinition> colOpt = schema.getColumns().stream()
                .filter(c -> c.getName().equals(columnName))
                .findFirst();

        boolean isUniqueForIndex = unique;

        if (colOpt.isPresent()) {
            DataTableSchema.ColumnDefinition col = colOpt.get();
            // Throw error if already indexed
            if (col.isIndexed()) {
                throw new RuntimeException("Column '" + columnName + "' is already indexed");
            }
            col.setIndexed(true);
            col.setUnique(unique);
            schemaRepository.save(schema);
            isUniqueForIndex = unique;
        } else {
            // Check for nested path (e.g. "address.city")
            if (columnName.contains(".")) {
                String rootColumn = columnName.split("\\.")[0];
                boolean rootExists = schema.getColumns().stream()
                        .anyMatch(c -> c.getName().equals(rootColumn));

                if (!rootExists) {
                    throw new RuntimeException("Root column not found for nested path: " + columnName);
                }
            } else {
                throw new RuntimeException("Column not found: " + columnName);
            }
        }

        Index index = new Index().on(fieldPath, Sort.Direction.ASC).named(indexName);

        if (isUniqueForIndex) {
            index.unique();
        }

        index.partial(PartialIndexFilter.of(Criteria.where("tableId").is(schema.getId())));

        mongoTemplate.indexOps(DataTableRecord.class).ensureIndex(index);
        return indexName;
    }

    public String dropIndex(String tableId, String columnName) {
        Optional<DataTableSchema> schemaOpt = schemaRepository.findById(tableId);
        if (schemaOpt.isPresent()) {
            DataTableSchema schema = schemaOpt.get();
            schema.getColumns().stream()
                    .filter(c -> c.getName().equals(columnName))
                    .findFirst()
                    .ifPresent(col -> {
                        col.setIndexed(false);
                        schemaRepository.save(schema);
                    });
        }

        String indexName = "idx_" + tableId + "_" + columnName;
        try {
            mongoTemplate.indexOps(DataTableRecord.class).dropIndex(indexName);
            return indexName;
        } catch (Exception e) {
            return null;
        }
    }

    public void createTableIndexes(String tableId) {
        DataTableSchema schema = schemaRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found with id: " + tableId));
        createIndexes(schema);
    }

    public void createIndexes(DataTableSchema schema) {
        if (schema.getColumns() == null)
            return;

        for (DataTableSchema.ColumnDefinition column : schema.getColumns()) {
            if (column.isIndexed() || column.isUnique()) {
                // Index 'data.columnName'
                String fieldPath = "data." + column.getName();
                // Name convention: idx_TABLEID_COLUMN
                String indexName = "idx_" + schema.getId() + "_" + column.getName();

                Index index = new Index().on(fieldPath, Sort.Direction.ASC).named(indexName);

                if (column.isUnique()) {
                    index.unique();
                }

                // Partial Filter: { 'tableId': schemaId }
                // This ensures the index only applies to records belonging to THIS table
                index.partial(PartialIndexFilter.of(Criteria.where("tableId").is(schema.getId())));

                try {
                    mongoTemplate.indexOps(DataTableRecord.class).ensureIndex(index);
                } catch (Exception e) {
                    // In production, log this.
                    System.err.println("Error creating index " + indexName + ": " + e.getMessage());
                }
            }
        }
    }
}