package com.dataflow.DataTable.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Map;

@Document("data_table_records")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataTableRecord extends AuditMetaData {

    @Id
    private String id;
    private String tableId;
    private Map<String, Object> data;
    @JsonIgnore
    private String searchText;
    private Map<String, Object> metadata;

    public DataTableRecord() {
    }

    public DataTableRecord(String tableId, Map<String, Object> data) {
        this();
        this.tableId = tableId;
        this.data = data;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }


    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}