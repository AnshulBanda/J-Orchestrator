package com.jorchestrator.controller;

import com.jorchestrator.aggregator.LogAggregator;
import com.jorchestrator.model.log.LogEntry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/logs")
public class LogController {
    private final LogAggregator aggregator;

    public LogController(LogAggregator aggregator) { 
        this.aggregator = aggregator; 
    }

    @PostMapping("/{executionId}")
    public ResponseEntity<Void> addLog(@PathVariable UUID executionId, @RequestBody Map<String, String> payload) {
        String stream = payload.getOrDefault("stream", "STDOUT");
        String message = payload.get("message");
        
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        aggregator.appendLog(executionId, stream, message);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{executionId}")
    public ResponseEntity<List<LogEntry>> getLogs(@PathVariable UUID executionId) {
        return ResponseEntity.ok(aggregator.getLogs(executionId));
    }
}