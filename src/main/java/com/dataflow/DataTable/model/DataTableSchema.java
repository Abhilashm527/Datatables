package com.dataflow.DataTable.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import java.util.Map;

@Document(collection = "data_table_schemas")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataTableSchema extends AuditMetaData {

    @Id
    private String id;
    private String applicationId;
    private String tableName;
    private String description;
    private List<ColumnDefinition> columns;
    private Map<String, Object> metadata;

    public DataTableSchema() {
    }

    public DataTableSchema(String tableName, String description, List<ColumnDefinition> columns) {
        this();
        this.tableName = tableName;
        this.description = description;
        this.columns = columns;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ColumnDefinition> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnDefinition> columns) {
        this.columns = columns;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }


    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public static class ColumnDefinition {
        private String name;
        private String dataType;
        private boolean required;
        private Object defaultValue;
        private String description;
        private boolean isIndexed;
        private boolean isUnique;
        private Map<String, Object> constraints;

        public ColumnDefinition() {
        }

        public ColumnDefinition(String name, String dataType, boolean required) {
            this.name = name;
            this.dataType = dataType;
            this.required = required;
        }

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDataType() {
            return dataType;
        }

        public void setDataType(String dataType) {
            this.dataType = dataType;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isIndexed() {
            return isIndexed;
        }

        public void setIndexed(boolean indexed) {
            isIndexed = indexed;
        }

        public boolean isUnique() {
            return isUnique;
        }

        public void setUnique(boolean unique) {
            isUnique = unique;
        }

        public Map<String, Object> getConstraints() {
            return constraints;
        }

        public void setConstraints(Map<String, Object> constraints) {
            this.constraints = constraints;
        }
    }
}