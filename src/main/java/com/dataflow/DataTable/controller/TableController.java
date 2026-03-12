package com.dataflow.DataTable.controller;

import com.dataflow.DataTable.model.DataTableSchema;
import com.dataflow.DataTable.service.DataTableService;
import com.dataflow.DataTable.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static com.dataflow.DataTable.config.APIConstants.DATATABLE_BASE_PATH;

@RestController
@RequestMapping(DATATABLE_BASE_PATH)
@CrossOrigin(origins = "*")
public class TableController {

    @Autowired
    private DataTableService dataTableService;

    @PostMapping()
    public ResponseEntity<Response> createTable(
            @RequestBody DataTableSchema schema,
            @RequestHeader("Authorization") String bearerToken) {
        try {
            DataTableSchema createdTable = dataTableService.createTable(schema, bearerToken);
            return Response.createResponse(createdTable);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Response.builder().code(400).message(e.getMessage()).build());
        }
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<Response> getAllTables(@PathVariable String applicationId) {
        List<DataTableSchema> tables = dataTableService.getTablesByApplicationId(applicationId);
        return Response.getResponse(tables);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getTable(@PathVariable String id) {
        Optional<DataTableSchema> table = dataTableService.getTableById(id);
        return table.map(Response::getResponse)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response.builder().code(404).message("Table not found").build()));
    }

    @GetMapping("/name/{tableName}")
    public ResponseEntity<Response> getTableByName(@PathVariable String tableName) {
        Optional<DataTableSchema> table = dataTableService.getTableByName(tableName);
        return table.map(Response::getResponse)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response.builder().code(404).message("Table not found").build()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Response> updateTable(@PathVariable String id, @RequestBody DataTableSchema schema) {
        try {
            DataTableSchema updatedTable = dataTableService.updateTable(id, schema);
            return Response.updateResponse(updatedTable);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Response.builder().code(400).message(e.getMessage()).build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response> deleteTable(@PathVariable String id) {
        try {
            dataTableService.deleteTable(id);
            return ResponseEntity.status(HttpStatus.OK).body(Response.builder().code(200).message("Table deleted successfully").build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response.builder().code(404).message(e.getMessage()).build());
        }
    }
}
