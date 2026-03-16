package com.dataflow.DataTable.controller;

import com.dataflow.DataTable.service.RecordService;
import com.dataflow.DataTable.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.dataflow.DataTable.config.APIConstants.DATATABLE_BASE_PATH;

@RestController
@RequestMapping(DATATABLE_BASE_PATH)
@CrossOrigin(origins = "*")
public class SearchController {

    @Autowired
    private RecordService recordService;

    @GetMapping("/{tableName}/search")
    public ResponseEntity<Response> searchRecords(
            @PathVariable String tableName,
            @RequestParam String field,
            @RequestParam String value,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "_id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Map<String, Object>> records = recordService.searchRecords(tableName, field, value, pageable);
        return Response.getResponse(records);
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
        Page<Map<String, Object>> records = recordService.searchRecordsByText(tableName, query, pageable);
        return Response.getResponse(records);
    }
}
