package com.dataflow.DataTable.service;

import com.dataflow.DataTable.model.DataTableSchema;
import com.dataflow.DataTable.repository.DataTableSchemaRepository;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RecordService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private DataTableSchemaRepository schemaRepository;

    /** Sanitizes a table name into a valid MongoDB collection name. */
    public static String toCollectionName(String tableName) {
        return tableName.toLowerCase().replaceAll("[^a-z0-9_]", "_");
    }

    private DataTableSchema getSchemaOrThrow(String tableName) {
        return schemaRepository.findByTableName(tableName)
                .orElseThrow(() -> new RuntimeException("Table not found: " + tableName));
    }

    // ── Insert ───────────────────────────────────────────────────────────────

    public Map<String, Object> insertRecord(String tableName, Map<String, Object> data) {
        DataTableSchema schema = getSchemaOrThrow(tableName);
        validateRecord(data, schema);
        Document doc = new Document(applyDefaults(data, schema));
        mongoTemplate.insert(doc, toCollectionName(tableName));
        return serializeDoc(doc);
    }

    public List<Map<String, Object>> insertRecords(String tableName, List<Map<String, Object>> recordsData) {
        DataTableSchema schema = getSchemaOrThrow(tableName);
        String collectionName = toCollectionName(tableName);

        List<Document> docs = new ArrayList<>();
        for (Map<String, Object> data : recordsData) {
            validateRecord(data, schema);
            docs.add(new Document(applyDefaults(data, schema)));
        }
        mongoTemplate.insert(docs, collectionName);
        return docs.stream().map(this::serializeDoc).collect(java.util.stream.Collectors.toList());
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    public List<Map<String, Object>> getRecords(String tableName) {
        return mongoTemplate.findAll(Document.class, toCollectionName(tableName))
                .stream().map(this::serializeDoc).toList();
    }

    public Page<Map<String, Object>> getRecords(String tableName, Pageable pageable) {
        String collectionName = toCollectionName(tableName);
        long total = mongoTemplate.count(new Query(), collectionName);
        List<Map<String, Object>> records = mongoTemplate
                .find(new Query().with(pageable), Document.class, collectionName)
                .stream().map(this::serializeDoc).toList();
        return new PageImpl<>(records, pageable, total);
    }

    public Optional<Map<String, Object>> getRecord(String tableName, String recordId) {
        Query query = new Query(Criteria.where("_id").is(toObjectId(recordId)));
        Document doc = mongoTemplate.findOne(query, Document.class, toCollectionName(tableName));
        return Optional.ofNullable(doc).map(this::serializeDoc);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public Map<String, Object> updateRecord(String tableName, String recordId, Map<String, Object> data) {
        DataTableSchema schema = getSchemaOrThrow(tableName);
        String collectionName = toCollectionName(tableName);

        Query query = new Query(Criteria.where("_id").is(toObjectId(recordId)));
        Document existing = mongoTemplate.findOne(query, Document.class, collectionName);
        if (existing == null) {
            throw new RuntimeException("Record not found with id: " + recordId);
        }

        validateRecord(data, schema);
        existing.putAll(data);
        mongoTemplate.save(existing, collectionName);
        return serializeDoc(existing);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void deleteRecord(String tableName, String recordId) {
        String collectionName = toCollectionName(tableName);
        Query query = new Query(Criteria.where("_id").is(toObjectId(recordId)));
        long deleted = mongoTemplate.remove(query, collectionName).getDeletedCount();
        if (deleted == 0) {
            throw new RuntimeException("Record not found with id: " + recordId);
        }
    }

    public void deleteRecords(String tableName, List<String> recordIds) {
        mongoTemplate.remove(new Query(Criteria.where("_id").in(recordIds)), toCollectionName(tableName));
    }

    public void deleteRecordsByTableName(String tableName) {
        String collectionName = toCollectionName(tableName);
        if (mongoTemplate.collectionExists(collectionName)) {
            mongoTemplate.dropCollection(collectionName);
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    public List<Map<String, Object>> searchRecords(String tableName, String fieldName, Object value) {
        Object typedValue = castToSchemaType(tableName, fieldName, value);
        Query query = new Query(Criteria.where(fieldName).is(typedValue));
        return mongoTemplate.find(query, Document.class, toCollectionName(tableName))
                .stream().map(this::serializeDoc).toList();
    }

    public Page<Map<String, Object>> searchRecords(String tableName, String fieldName, Object value,
            Pageable pageable) {
        Object typedValue = castToSchemaType(tableName, fieldName, value);
        String collectionName = toCollectionName(tableName);

        Query query = new Query(Criteria.where(fieldName).is(typedValue));
        long total = mongoTemplate.count(query, collectionName);
        List<Map<String, Object>> records = mongoTemplate.find(query.with(pageable), Document.class, collectionName)
                .stream().map(this::serializeDoc).toList();
        return new PageImpl<>(records, pageable, total);
    }

    /**
     * Full-text search across all string-typed columns using case-insensitive
     * regex.
     */
    public Page<Map<String, Object>> searchRecordsByText(String tableName, String searchQuery, Pageable pageable) {
        DataTableSchema schema = getSchemaOrThrow(tableName);
        String collectionName = toCollectionName(tableName);

        List<Criteria> criteriaList = schema.getColumns().stream()
                .filter(col -> col.getDataType().equalsIgnoreCase("string"))
                .map(col -> Criteria.where(col.getName()).regex(searchQuery, "i"))
                .toList();

        if (criteriaList.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        Query query = new Query(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])));
        long total = mongoTemplate.count(query, collectionName);
        List<Map<String, Object>> records = mongoTemplate.find(query.with(pageable), Document.class, collectionName)
                .stream().map(this::serializeDoc).toList();
        return new PageImpl<>(records, pageable, total);
    }

    // ── Count ─────────────────────────────────────────────────────────────────

    public long getRecordCount(String tableName) {
        return mongoTemplate.count(new Query(), toCollectionName(tableName));
    }

    // ── ID helpers ────────────────────────────────────────────────────────────

    /** Converts string to ObjectId if valid, otherwise returns the string as-is. */
    private Object toObjectId(String id) {
        try {
            return new ObjectId(id);
        } catch (Exception e) {
            return id;
        }
    }

    /** Converts the _id field in a document to its hex string representation. */
    private Map<String, Object> serializeDoc(Document doc) {
        Map<String, Object> map = new HashMap<>(doc);
        if (map.get("_id") instanceof ObjectId oid) {
            map.put("_id", oid.toHexString());
        }
        return map;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Object castToSchemaType(String tableName, String fieldName, Object value) {
        Optional<DataTableSchema> schemaOpt = schemaRepository.findByTableName(tableName);
        if (schemaOpt.isEmpty() || value == null)
            return value;

        String rootField = fieldName.contains(".") ? fieldName.split("\\.")[0] : fieldName;
        return schemaOpt.get().getColumns().stream()
                .filter(c -> c.getName().equals(rootField))
                .findFirst()
                .map(col -> {
                    String strVal = value.toString();
                    try {
                        return switch (col.getDataType().toLowerCase()) {
                            case "boolean" -> (Object) Boolean.parseBoolean(strVal);
                            case "number", "double" -> Double.parseDouble(strVal);
                            case "integer", "int" -> Integer.parseInt(strVal);
                            case "long" -> Long.parseLong(strVal);
                            default -> value;
                        };
                    } catch (Exception e) {
                        return value;
                    }
                })
                .orElse(value);
    }

    private void validateRecord(Map<String, Object> data, DataTableSchema schema) {
        for (DataTableSchema.ColumnDefinition col : schema.getColumns()) {
            if (col.isRequired() && (!data.containsKey(col.getName()) || data.get(col.getName()) == null)) {
                throw new RuntimeException("Required field missing: " + col.getName());
            }
        }

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            schema.getColumns().stream()
                    .filter(c -> c.getName().equals(entry.getKey()))
                    .findFirst()
                    .ifPresent(col -> {
                        if (entry.getValue() != null)
                            validateDataType(entry.getValue(), col.getDataType(), entry.getKey());
                    });
        }
    }

    private void validateDataType(Object value, String expectedType, String fieldName) {
        switch (expectedType.toLowerCase()) {
            case "string" -> {
                if (!(value instanceof String))
                    throw new RuntimeException("Field '" + fieldName + "' must be a string");
            }
            case "number", "double" -> {
                if (!(value instanceof Number))
                    throw new RuntimeException("Field '" + fieldName + "' must be a number");
            }
            case "integer", "int", "long" -> {
                if (!(value instanceof Number))
                    throw new RuntimeException("Field '" + fieldName + "' must be an integer/long");
            }
            case "boolean" -> {
                if (!(value instanceof Boolean))
                    throw new RuntimeException("Field '" + fieldName + "' must be a boolean");
            }
            case "date" -> {
                if (!(value instanceof String || value instanceof Date || value instanceof Long))
                    throw new RuntimeException("Field '" + fieldName + "' must be a date");
            }
            case "array" -> {
                if (!(value instanceof Collection || value instanceof Object[]))
                    throw new RuntimeException("Field '" + fieldName + "' must be an array");
            }
            case "object", "json" -> {
                if (!(value instanceof Map))
                    throw new RuntimeException("Field '" + fieldName + "' must be an object");
            }
        }
    }

    private Map<String, Object> applyDefaults(Map<String, Object> data, DataTableSchema schema) {
        Map<String, Object> processed = new HashMap<>(data);
        for (DataTableSchema.ColumnDefinition col : schema.getColumns()) {
            if (!processed.containsKey(col.getName()) && col.getDefaultValue() != null) {
                processed.put(col.getName(), col.getDefaultValue());
            }
        }
        return processed;
    }
}
