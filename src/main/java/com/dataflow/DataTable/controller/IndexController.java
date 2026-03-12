package com.dataflow.DataTable.controller;

import com.dataflow.DataTable.service.DataTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import static com.dataflow.DataTable.config.APIConstants.INDEX_BASE_PATH;

@RestController
@RequestMapping(INDEX_BASE_PATH)
@Tag(name = "Index Management", description = "APIs for managing database indexes")
@CrossOrigin(origins = "*")
public class IndexController {

    @Autowired
    private DataTableService dataTableService;

    // @Operation(summary = "Create all indexes for a table based on schema")
    // @PostMapping("/{tableId}")
    // public ResponseEntity<Void> createTableIndexes(@PathVariable String tableId)
    // {
    // dataTableService.createTableIndexes(tableId);
    // return ResponseEntity.ok().build();
    // }

    @Operation(summary = "Create an index for a specific column")
    @PostMapping("/{tableId}/columns/{columnName}")
    public ResponseEntity<Map<String, String>> createIndex(
            @PathVariable String tableId,
            @PathVariable String columnName,
            @RequestParam(defaultValue = "false") boolean unique) {
        String indexName = dataTableService.createIndex(tableId, columnName, unique);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Index created successfully");
        response.put("indexName", indexName);
        response.put("column", columnName);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remove an index for a specific column")
    @DeleteMapping("/{tableId}/columns/{columnName}")
    public ResponseEntity<Map<String, String>> removeIndex(@PathVariable String tableId,
            @PathVariable String columnName) {
        String indexName = dataTableService.dropIndex(tableId, columnName);
        Map<String, String> response = new HashMap<>();
        if (indexName != null) {
            response.put("message", "Index removed successfully");
            response.put("indexName", indexName);
        } else {
            response.put("message", "Index not found or could not be removed");
        }
        response.put("column", columnName);
        return ResponseEntity.ok(response);
    }
}
