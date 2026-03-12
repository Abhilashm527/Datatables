package com.dataflow.DataTable.util;

import com.dataflow.DataTable.exception.ErrorDefinition;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@JsonInclude(Include.NON_NULL)
public class Response {
    private Integer code;
    private String message;
    private Object data;
    private List<ErrorDefinition> causedBy;

    public Response() {
    }

    public Response(Integer code, String message, Object data, List<ErrorDefinition> causedBy) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.causedBy = causedBy;
    }

    public static ResponseEntity<Response> createResponse(Object data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(builder().code(201).message("Resource created successfully").data(data).build());
    }

    public static ResponseEntity<Response> updateResponse(Object data) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(builder().code(200).message("Resource updated successfully").data(data).build());
    }

    public static ResponseEntity<Response> getResponse(Object data) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(builder().code(200).message("Resource fetched successfully").data(data).build());
    }

    public static ResponseBuilder builder() {
        return new ResponseBuilder();
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public List<ErrorDefinition> getCausedBy() {
        return causedBy;
    }

    public void setCausedBy(List<ErrorDefinition> causedBy) {
        this.causedBy = causedBy;
    }

    public static class ResponseBuilder {
        private Integer code;
        private String message;
        private Object data;
        private List<ErrorDefinition> causedBy;

        public ResponseBuilder code(Integer code) {
            this.code = code;
            return this;
        }

        public ResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ResponseBuilder data(Object data) {
            this.data = data;
            return this;
        }

        public ResponseBuilder causedBy(List<ErrorDefinition> causedBy) {
            this.causedBy = causedBy;
            return this;
        }

        public Response build() {
            return new Response(code, message, data, causedBy);
        }
    }
}
