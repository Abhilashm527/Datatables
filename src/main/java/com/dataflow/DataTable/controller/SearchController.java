package com.dataflow.DataTable.controller;

import com.dataflow.DataTable.model.DataTableRecord;
import com.dataflow.DataTable.service.DataTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/datatables")
@CrossOrigin(origins = "*")
public class SearchController {

    @Autowired
    private DataTableService dataTableService;

    @GetMapping("/tables/{tableId}/search")
    public ResponseEntity<Page<DataTableRecord>> searchRecords(
            @PathVariable String tableId,
            @RequestParam String field,
            @RequestParam String value,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<DataTableRecord> records = dataTableService.searchRecords(tableId, field, value, pageable);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/tables/{tableId}/search/text")
    public ResponseEntity<Page<DataTableRecord>> searchRecordsByText(
            @PathVariable String tableId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<DataTableRecord> records = dataTableService.searchRecords(tableId, query, pageable);
        return ResponseEntity.ok(records);
    }
}
