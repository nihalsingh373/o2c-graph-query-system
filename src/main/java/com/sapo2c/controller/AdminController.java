package com.sapo2c.controller;

import com.sapo2c.service.DataIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminController {

    private final DataIngestionService ingestionService;

    // Trigger data ingestion
    @PostMapping("/ingest")
    public ResponseEntity<Map<String, String>> ingestData() {
        if (ingestionService.isDataLoaded()) {
            return ResponseEntity.ok(Map.of("status", "skipped", "message", "Data already loaded"));
        }
        try {
            new Thread(() -> ingestionService.ingestAllData()).start();
            return ResponseEntity.ok(Map.of("status", "started", "message", "Ingestion started in background"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // Trigger graph build
    @PostMapping("/build-graph")
    public ResponseEntity<Map<String, String>> buildGraph() {
        try {
            new Thread(() -> ingestionService.buildGraph()).start();
            return ResponseEntity.ok(Map.of("status", "started", "message", "Graph build started in background"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // Check status
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "dataLoaded", ingestionService.isDataLoaded(),
            "graphBuilt", ingestionService.isGraphBuilt()
        ));
    }
}
