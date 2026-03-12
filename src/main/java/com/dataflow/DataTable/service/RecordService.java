package com.dataflow.DataTable.service;

import com.dataflow.DataTable.model.DataTableRecord;
import com.dataflow.DataTable.model.DataTableSchema;
import com.dataflow.DataTable.repository.DataTableRecordRepository;
import com.dataflow.DataTable.repository.DataTableSchemaRepository;
import com.dataflow.DataTable.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;

@Service
public class RecordService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private DataTableSchemaRepository schemaRepository;

    @Autowired
    private DataTableRecordRepository recordRepository;


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
        long now = DateUtils.getUnixTimestampInUTC();
        record.setCreatedAt(now);
        record.setCreatedBy("admin");
        record.setUpdatedAt(now);
        record.setUpdatedBy("admin");
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
            long now = DateUtils.getUnixTimestampInUTC();
            record.setCreatedAt(now);
            record.setCreatedBy("admin");
            record.setUpdatedAt(now);
            record.setUpdatedBy("admin");
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
        record.setUpdatedAt(DateUtils.getUnixTimestampInUTC());
        record.setUpdatedBy("admin");
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

    public void deleteRecordsByTableId(String tableId) {
        recordRepository.deleteByTableId(tableId);
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

        if (columnOpt.isPresent() && value != null) {
            String dataType = columnOpt.get().getDataType().toLowerCase();
            String strVal = value.toString();

            try {
                switch (dataType) {
                    case "boolean":
                        return Boolean.parseBoolean(strVal);
                    case "number":
                    case "double":
                        return Double.parseDouble(strVal);
                    case "integer":
                    case "int":
                        return Integer.parseInt(strVal);
                    case "long":
                        return Long.parseLong(strVal);
                    case "object":
                    case "json":
                        // If it's already a Map, return it
                        if (value instanceof Map) {
                            return value;
                        }
                        // Otherwise, return as is
                        return value;
                    default:
                        return value;
                }
            } catch (Exception e) {
                // Return original value if conversion fails
                return value;
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
            case "double":
                if (!(value instanceof Number)) {
                    throw new RuntimeException("Field '" + fieldName + "' must be a number/double");
                }
                break;
            case "integer":
            case "int":
            case "long":
                if (!(value instanceof Number)) {
                    throw new RuntimeException("Field '" + fieldName + "' must be an integer/long");
                }
                break;
            case "boolean":
                if (!(value instanceof Boolean)) {
                    throw new RuntimeException("Field '" + fieldName + "' must be a boolean");
                }
                break;
            case "date":
                // Accept various date formats
                if (!(value instanceof String || value instanceof Date || value instanceof Long)) {
                    throw new RuntimeException("Field '" + fieldName + "' must be a date (string, date object, or epoch)");
                }
                break;
            case "array":
                if (!(value instanceof Collection || value instanceof Object[])) {
                    throw new RuntimeException("Field '" + fieldName + "' must be an array");
                }
                break;
            case "object":
            case "json":
                if (!(value instanceof Map)) {
                    throw new RuntimeException("Field '" + fieldName + "' must be an object (Map)");
                }
                break;
            default:
                break;
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
}
