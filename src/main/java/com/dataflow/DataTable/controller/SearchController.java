package com.dataflow.DataTable.controller;

import com.dataflow.DataTable.model.DataTableRecord;
import com.dataflow.DataTable.service.RecordService;
import com.dataflow.DataTable.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.dataflow.DataTable.config.APIConstants.DATATABLE_BASE_PATH;

@RestController
@RequestMapping(DATATABLE_BASE_PATH)
@CrossOrigin(origins = "*")
public class SearchController {

    @Autowired
    private RecordService recordService;

    @GetMapping("/{tableId}/search")
    public ResponseEntity<Response> searchRecords(
            @PathVariable String tableId,
            @RequestParam String field,
            @RequestParam String value,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<DataTableRecord> records = recordService.searchRecords(tableId, field, value, pageable);
        return Response.getResponse(records);
    }

    @GetMapping("/{tableId}/search/text")
    public ResponseEntity<Response> searchRecordsByText(
            @PathVariable String tableId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<DataTableRecord> records = recordService.searchRecords(tableId, query, pageable);
        return Response.getResponse(records);
    }
}
