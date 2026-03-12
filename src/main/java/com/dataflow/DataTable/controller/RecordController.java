package com.dataflow.DataTable.controller;

import com.dataflow.DataTable.model.DataTableRecord;
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

    @PostMapping("/table/{tableId}")
    public ResponseEntity<Response> insertRecord(
            @PathVariable String tableId,
            @RequestBody Map<String, Object> data) {
        try {
            DataTableRecord record = recordService.insertRecord(tableId, data);
            return Response.createResponse(record);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Response.builder().code(400).message(e.getMessage()).build());
        }
    }

    @PostMapping("/{tableId}/batch")
    public ResponseEntity<Response> insertRecords(
            @PathVariable String tableId,
            @RequestBody List<Map<String, Object>> records) {
        try {
            List<DataTableRecord> createdRecords = recordService.insertRecords(tableId, records);
            return Response.createResponse(createdRecords);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Response.builder().code(400).message(e.getMessage()).build());
        }
    }

    @GetMapping("/table/{tableId}")
    public ResponseEntity<Response> getRecords(
            @PathVariable String tableId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<DataTableRecord> records = recordService.getRecords(tableId, pageable);
        return Response.getResponse(records);
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<Response> getRecord(@PathVariable String recordId) {
        Optional<DataTableRecord> record = recordService.getRecord(recordId);
        return record.map(Response::getResponse)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response.builder().code(404).message("Record not found").build()));
    }

    @PutMapping("/{recordId}")
    public ResponseEntity<Response> updateRecord(@PathVariable String recordId,
            @RequestBody Map<String, Object> data) {
        try {
            DataTableRecord updatedRecord = recordService.updateRecord(recordId, data);
            return Response.updateResponse(updatedRecord);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Response.builder().code(400).message(e.getMessage()).build());
        }
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<Response> deleteRecord(@PathVariable String recordId) {
        try {
            recordService.deleteRecord(recordId);
            return ResponseEntity.status(HttpStatus.OK).body(Response.builder().code(200).message("Record deleted successfully").build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response.builder().code(404).message(e.getMessage()).build());
        }
    }

    @DeleteMapping("/batch")
    public ResponseEntity<Response> deleteRecords(@RequestBody List<String> recordIds) {
        try {
            recordService.deleteRecords(recordIds);
            return ResponseEntity.status(HttpStatus.OK).body(Response.builder().code(200).message("Records deleted successfully").build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Response.builder().code(500).message("Failed to delete records: " + e.getMessage()).build());
        }
    }

    @GetMapping("/{tableId}/count")
    public ResponseEntity<Response> getRecordCount(@PathVariable String tableId) {
        long count = recordService.getRecordCount(tableId);
        return Response.getResponse(Map.of("count", count));
    }
}
