package com.dataflow.DataTable.model;

import com.dataflow.DataTable.util.DateUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditMetaData {
    @JsonIgnore
    private Long createdAt;
    private String createdBy;
    @JsonIgnore
    private Long updatedAt;
    private String updatedBy;

    @JsonProperty("createdAtDisplay")
    public String getCreatedAtDisplay() {
        return (createdAt != null) ? DateUtils.getFormattedDate(createdAt) : null;
    }

    @JsonProperty("updatedAtDisplay")
    public String getUpdatedAtDisplay() {
        return (updatedAt != null) ? DateUtils.getFormattedDate(updatedAt) : null;
    }
}
