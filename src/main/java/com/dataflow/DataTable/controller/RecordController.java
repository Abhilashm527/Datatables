package com.dataflow.DataTable.controller;

import com.dataflow.DataTable.model.DataTableRecord;
import com.dataflow.DataTable.service.DataTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/datatables")
@CrossOrigin(origins = "*")
public class RecordController {

    @Autowired
    private DataTableService dataTableService;

    @PostMapping("/tables/{tableId}/records")
    public ResponseEntity<DataTableRecord> insertRecord(@PathVariable String tableId,
            @RequestBody Map<String, Object> data) {
        try {
            DataTableRecord record = dataTableService.insertRecord(tableId, data);
            return ResponseEntity.status(HttpStatus.CREATED).body(record);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/tables/{tableId}/records/batch")
    public ResponseEntity<List<DataTableRecord>> insertRecords(@PathVariable String tableId,
            @RequestBody List<Map<String, Object>> records) {
        try {
            List<DataTableRecord> createdRecords = dataTableService.insertRecords(tableId, records);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdRecords);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/tables/{tableId}/records")
    public ResponseEntity<Page<DataTableRecord>> getRecords(
            @PathVariable String tableId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<DataTableRecord> records = dataTableService.getRecords(tableId, pageable);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/tables/{tableId}/records/all")
    public ResponseEntity<Page<DataTableRecord>> getAllRecords(
            @PathVariable String tableId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<DataTableRecord> records = dataTableService.getRecords(tableId, pageable);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/records/{recordId}")
    public ResponseEntity<DataTableRecord> getRecord(@PathVariable String recordId) {
        Optional<DataTableRecord> record = dataTableService.getRecord(recordId);
        return record.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/records/{recordId}")
    public ResponseEntity<DataTableRecord> updateRecord(@PathVariable String recordId,
            @RequestBody Map<String, Object> data) {
        try {
            DataTableRecord updatedRecord = dataTableService.updateRecord(recordId, data);
            return ResponseEntity.ok(updatedRecord);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @DeleteMapping("/records/{recordId}")
    public ResponseEntity<Void> deleteRecord(@PathVariable String recordId) {
        try {
            dataTableService.deleteRecord(recordId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/records/batch")
    public ResponseEntity<Map<String, String>> deleteRecords(@RequestBody List<String> recordIds) {
        try {
            dataTableService.deleteRecords(recordIds);
            return ResponseEntity.ok(Map.of("message", "Records deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete records: " + e.getMessage()));
        }
    }

    @GetMapping("/tables/{tableId}/count")
    public ResponseEntity<Map<String, Long>> getRecordCount(@PathVariable String tableId) {
        long count = dataTableService.getRecordCount(tableId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
