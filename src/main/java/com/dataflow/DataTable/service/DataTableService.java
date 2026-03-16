package com.dataflow.DataTable.service;

import com.dataflow.DataTable.model.DataTableSchema;
import com.dataflow.DataTable.repository.DataTableSchemaRepository;
import com.dataflow.DataTable.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import com.mongodb.MongoNamespace;

import java.util.*;

@Service
public class DataTableService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private DataTableSchemaRepository schemaRepository;

    @Autowired
    private ApplicationValidationService validationService;

    private String getBearerTokenFromContext() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader("Authorization");
            }
        } catch (Exception e) {
            // Not in a request context
        }
        return null;
    }

    // Schema operations
    public DataTableSchema createTable(DataTableSchema schema) {
        return createTable(schema, getBearerTokenFromContext());
    }

    public DataTableSchema createTable(DataTableSchema schema, String bearerToken) {
        // Validate application exists if token is present
        if (bearerToken != null && !validationService.validateApplication(schema.getApplicationId(), bearerToken)) {
            throw new RuntimeException("Invalid applicationId or unauthorized: " + schema.getApplicationId());
        }

        if (schemaRepository.existsByTableName(schema.getTableName())) {
            throw new RuntimeException("Table with name '" + schema.getTableName() + "' already exists");
        }

        // Validate schema
        validateSchema(schema);

        // (audit metadata handled by AuditMetaData if present)

        DataTableSchema savedSchema = schemaRepository.save(schema);
        createIndexes(savedSchema);
        return savedSchema;
    }

    public List<DataTableSchema> getAllTables() {
        return schemaRepository.findAll();
    }

    public List<DataTableSchema> getTablesByApplicationId(String applicationId) {
        return schemaRepository.findByApplicationId(applicationId);
    }

    public Optional<DataTableSchema> getTableByName(String tableName) {
        return schemaRepository.findByTableName(tableName);
    }

    public Optional<DataTableSchema> getTableByName(String tableName, String applicationId) {
        if (applicationId == null) {
            return getTableByName(tableName);
        }
        return schemaRepository.findByTableNameAndApplicationId(tableName, applicationId);
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
        String oldTableName = schema.getTableName();
        String newTableName = updatedSchema.getTableName();

        if (newTableName != null && !newTableName.equals(oldTableName)) {
            if (schemaRepository.existsByTableName(newTableName)) {
                throw new RuntimeException("Table with name '" + newTableName + "' already exists");
            }
            String oldCollectionName = RecordService.toCollectionName(oldTableName);
            String newCollectionName = RecordService.toCollectionName(newTableName);
            if (mongoTemplate.collectionExists(oldCollectionName)) {
                mongoTemplate.getDb().getCollection(oldCollectionName)
                        .renameCollection(new MongoNamespace(mongoTemplate.getDb().getName(), newCollectionName));
            }
            schema.setTableName(newTableName);
        }

        schema.setDescription(updatedSchema.getDescription());
        schema.setColumns(updatedSchema.getColumns());
        schema.setMetadata(updatedSchema.getMetadata());

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

        // Drop the dedicated data collection for this table
        String collectionName = RecordService.toCollectionName(schema.get().getTableName());
        if (mongoTemplate.collectionExists(collectionName)) {
            mongoTemplate.dropCollection(collectionName);
        }

        // Delete the schema
        schemaRepository.deleteById(id);
    }

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

            // Check if data type is supported
            String dataType = column.getDataType().toLowerCase();
            List<String> supportedTypes = Arrays.asList(
                "string", "boolean", "number", "integer", "int", "long", "double", "date", "array", "object", "json"
            );
            
            if (!supportedTypes.contains(dataType)) {
                throw new RuntimeException("Unsupported data type '" + column.getDataType() + "' for column '" + column.getName() + "'. " +
                        "Supported types are: " + String.join(", ", supportedTypes));
            }
        }
    }

    public String createIndex(String tableId, String columnName, boolean unique) {
        Optional<DataTableSchema> schemaOpt = schemaRepository.findById(tableId);
        if (schemaOpt.isEmpty()) {
            throw new RuntimeException("Table not found with id: " + tableId);
        }

        DataTableSchema schema = schemaOpt.get();
        String fieldPath = columnName;
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

        String collectionName = RecordService.toCollectionName(schema.getTableName());
        mongoTemplate.indexOps(collectionName).ensureIndex(index);
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
            Optional<DataTableSchema> schemaForDrop = schemaRepository.findById(tableId);
            if (schemaForDrop.isPresent()) {
                String collectionName = RecordService.toCollectionName(schemaForDrop.get().getTableName());
                mongoTemplate.indexOps(collectionName).dropIndex(indexName);
            }
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

        String collectionName = RecordService.toCollectionName(schema.getTableName());

        for (DataTableSchema.ColumnDefinition column : schema.getColumns()) {
            if (column.isIndexed() || column.isUnique()) {
                String fieldPath = column.getName();
                String indexName = "idx_" + schema.getId() + "_" + column.getName();

                Index index = new Index().on(fieldPath, Sort.Direction.ASC).named(indexName);

                if (column.isUnique()) {
                    index.unique();
                }

                // No partial filter needed — collection is already table-specific
                try {
                    mongoTemplate.indexOps(collectionName).ensureIndex(index);
                } catch (Exception e) {
                    System.err.println("Error creating index " + indexName + ": " + e.getMessage());
                }
            }
        }
    }
}