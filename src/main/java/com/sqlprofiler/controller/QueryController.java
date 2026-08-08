//package com.sqlprofiler.controller;
//
//import com.sqlprofiler.model.QueryReport;
//import com.sqlprofiler.model.QueryRequest;
//import com.sqlprofiler.service.QueryAnalyzerService;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import com.sqlprofiler.model.QueryHistory;
//import com.sqlprofiler.repository.QueryHistoryRepository;
//
//import java.util.List;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api")
//@CrossOrigin(origins = "*")
//public class QueryController {
//
//    @Autowired
//    private QueryAnalyzerService analyzerService;
//    
//    @Autowired
//    private QueryHistoryRepository historyRepository;
//
//    @GetMapping("/health")
//    public ResponseEntity<Map<String, String>> health() {
//        return ResponseEntity.ok(Map.of("status", "running", "service", "SQL Query Profiler"));
//    }
//
//    @PostMapping("/analyze")
//    public ResponseEntity<QueryReport> analyze(@RequestBody QueryRequest request) {
//        if (request.getQuery() == null || request.getQuery().isBlank()) {
//            return ResponseEntity.badRequest().build();
//        }
//        QueryReport report = analyzerService.analyze(request.getQuery());
//        return ResponseEntity.ok(report);
//    }
//    
//    @GetMapping("/history")
//    public ResponseEntity<List<QueryHistory>> history() {
//        return ResponseEntity.ok(
//            historyRepository.findTop50ByOrderByAnalyzedAtDesc()
//        );
//    }
//
//    // Handle errors cleanly
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<Map<String, String>> handleError(Exception e) {
//        return ResponseEntity.internalServerError()
//            .body(Map.of("error", "An unexpected error occurred."));
//    }
//}


package com.sqlprofiler.controller;

import com.sqlprofiler.model.ComparisonReport;
import com.sqlprofiler.model.ComparisonRequest;
import com.sqlprofiler.model.QueryHistory;
import com.sqlprofiler.model.QueryReport;
import com.sqlprofiler.model.QueryRequest;
import com.sqlprofiler.repository.QueryHistoryRepository;
import com.sqlprofiler.service.QueryAnalyzerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class QueryController {

    @Autowired
    private QueryAnalyzerService analyzerService;

    @Autowired
    private QueryHistoryRepository historyRepository;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(
            Map.of(
                "status",
                "running",
                "service",
                "SQL Query Profiler"
            )
        );
    }

    @PostMapping("/analyze")
    public ResponseEntity<QueryReport> analyze(
        @RequestBody QueryRequest request
    ) {
        if (request.getQuery() == null
            || request.getQuery().isBlank()) {

            return ResponseEntity.badRequest().build();
        }

        QueryReport report = analyzerService.analyze(
            request.getQuery()
        );

        return ResponseEntity.ok(report);
    }

    @GetMapping("/history")
    public ResponseEntity<List<QueryHistory>> history() {
        return ResponseEntity.ok(
            historyRepository.findTop50ByOrderByAnalyzedAtDesc()
        );
    }

    @PostMapping("/compare")
    public ResponseEntity<?> compare(
        @RequestBody ComparisonRequest request
    ) {
        if (request == null
            || request.getBeforeHistoryId() == null
            || request.getAfterQuery() == null
            || request.getAfterQuery().isBlank()) {

            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error",
                    "beforeHistoryId and afterQuery are required."
                ));
        }

        ComparisonReport comparison =
            analyzerService.compare(
                request.getBeforeHistoryId(),
                request.getAfterQuery()
            );

        return ResponseEntity.ok(comparison);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
        NoSuchElementException exception
    ) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "error",
                exception.getMessage()
            ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
        IllegalArgumentException exception
    ) {
        return ResponseEntity
            .badRequest()
            .body(Map.of(
                "error",
                exception.getMessage()
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleError(
        Exception exception
    ) {
        return ResponseEntity
            .internalServerError()
            .body(Map.of(
                "error",
                "An unexpected error occurred."
            ));
    }
}
