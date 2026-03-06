package com.dataflow.DataTable.controller;

import com.dataflow.DataTable.model.DataTableSchema;
import com.dataflow.DataTable.service.DataTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/datatables")
@CrossOrigin(origins = "*")
public class TableController {

    @Autowired
    private DataTableService dataTableService;

    @PostMapping("/tables")
    public ResponseEntity<DataTableSchema> createTable(@RequestBody DataTableSchema schema) {
        try {
            DataTableSchema createdTable = dataTableService.createTable(schema);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTable);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/tables")
    public ResponseEntity<List<DataTableSchema>> getAllTables() {
        List<DataTableSchema> tables = dataTableService.getAllTables();
        return ResponseEntity.ok(tables);
    }

    @GetMapping("/tables/{id}")
    public ResponseEntity<DataTableSchema> getTable(@PathVariable String id) {
        Optional<DataTableSchema> table = dataTableService.getTableById(id);
        return table.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tables/name/{tableName}")
    public ResponseEntity<DataTableSchema> getTableByName(@PathVariable String tableName) {
        Optional<DataTableSchema> table = dataTableService.getTableByName(tableName);
        return table.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/tables/{id}")
    public ResponseEntity<DataTableSchema> updateTable(@PathVariable String id, @RequestBody DataTableSchema schema) {
        try {
            DataTableSchema updatedTable = dataTableService.updateTable(id, schema);
            return ResponseEntity.ok(updatedTable);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @DeleteMapping("/tables/{id}")
    public ResponseEntity<Void> deleteTable(@PathVariable String id) {
        try {
            dataTableService.deleteTable(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
