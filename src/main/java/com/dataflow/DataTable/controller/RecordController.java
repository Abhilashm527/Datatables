package com.dataflow.DataTable.controller;

import com.dataflow.DataTable.service.RecordService;
import com.dataflow.DataTable.util.Response;
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

import static com.dataflow.DataTable.config.APIConstants.RECORD_BASE_PATH;

@RestController
@RequestMapping(RECORD_BASE_PATH)
@CrossOrigin(origins = "*")
public class RecordController {

    @Autowired
    private RecordService recordService;

    @PostMapping("/{tableName}")
    public ResponseEntity<Response> insertRecord(
            @PathVariable String tableName,
            @RequestBody Map<String, Object> data) {
        try {
            return Response.createResponse(recordService.insertRecord(tableName, data));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Response.builder().code(400).message(e.getMessage()).build());
        }
    }

    @PostMapping("/{tableName}/batch")
    public ResponseEntity<Response> insertRecords(
            @PathVariable String tableName,
            @RequestBody List<Map<String, Object>> records) {
        try {
            return Response.createResponse(recordService.insertRecords(tableName, records));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Response.builder().code(400).message(e.getMessage()).build());
        }
    }

    @GetMapping("/{tableName}")
    public ResponseEntity<Response> getRecords(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "_id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Map<String, Object>> records = recordService.getRecords(tableName, pageable);
        return Response.getResponse(records);
    }

    @GetMapping("/{tableName}/{recordId}")
    public ResponseEntity<Response> getRecord(
            @PathVariable String tableName,
            @PathVariable String recordId) {
        Optional<Map<String, Object>> record = recordService.getRecord(tableName, recordId);
        return record.map(Response::getResponse)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response.builder().code(404).message("Record not found").build()));
    }

    @PutMapping("/{tableName}/{recordId}")
    public ResponseEntity<Response> updateRecord(
            @PathVariable String tableName,
            @PathVariable String recordId,
            @RequestBody Map<String, Object> data) {
        try {
            return Response.updateResponse(recordService.updateRecord(tableName, recordId, data));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Response.builder().code(400).message(e.getMessage()).build());
        }
    }

    @DeleteMapping("/{tableName}/{recordId}")
    public ResponseEntity<Response> deleteRecord(
            @PathVariable String tableName,
            @PathVariable String recordId) {
        try {
            recordService.deleteRecord(tableName, recordId);
            return ResponseEntity.status(HttpStatus.OK).body(Response.builder().code(200).message("Record deleted successfully").build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response.builder().code(404).message(e.getMessage()).build());
        }
    }

    @DeleteMapping("/{tableName}/batch")
    public ResponseEntity<Response> deleteRecords(
            @PathVariable String tableName,
            @RequestBody List<String> recordIds) {
        try {
            recordService.deleteRecords(tableName, recordIds);
            return ResponseEntity.status(HttpStatus.OK).body(Response.builder().code(200).message("Records deleted successfully").build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Response.builder().code(500).message("Failed to delete records: " + e.getMessage()).build());
        }
    }

    @GetMapping("/{tableName}/search/text")
    public ResponseEntity<Response> searchRecordsByText(
            @PathVariable String tableName,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "_id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return Response.getResponse(recordService.searchRecordsByText(tableName, query, pageable));
    }

    @GetMapping("/{tableName}/count")
    public ResponseEntity<Response> getRecordCount(@PathVariable String tableName) {
        return Response.getResponse(Map.of("count", recordService.getRecordCount(tableName)));
    }
}
