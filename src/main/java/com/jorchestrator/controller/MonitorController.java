package com.jorchestrator.controller;

import com.jorchestrator.facade.SystemMonitorFacade;
import com.jorchestrator.model.log.LogEntry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * OOAD — Facade Pattern (Client Controller).
 *
 * <p>The UI or monitoring dashboard calls this single controller instead of
 * wiring directly to four different services. All calls are delegated to
 * {@link SystemMonitorFacade}, which hides the internal subsystem complexity.
 *
 * <p>Actor: DevOps Engineer and Cluster Admin — read-only cluster overview.
 */
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final SystemMonitorFacade facade;

    public MonitorController(SystemMonitorFacade facade) {
        this.facade = facade;
    }

    /**
     * Single-call cluster health snapshot — node counts, job counts, active executions.
     * Replaces the need for the UI to call /api/nodes, /api/jobs, and /api/executions
     * separately and aggregate them client-side.
     */
    @GetMapping("/health")
    public ResponseEntity<SystemMonitorFacade.ClusterHealthSnapshot> health() {
        return ResponseEntity.ok(facade.getClusterHealth());
    }

    /** Full log dump for a given execution (delegated to LogAggregator via Facade). */
    @GetMapping("/executions/{executionId}/logs")
    public List<LogEntry> executionLogs(@PathVariable UUID executionId) {
        return facade.getExecutionLogs(executionId);
    }

    /** Live-tail: last N log lines for a given execution. */
    @GetMapping("/executions/{executionId}/logs/tail")
    public List<LogEntry> tailLogs(@PathVariable UUID executionId,
                                   @RequestParam(defaultValue = "50") int n) {
        return facade.tailExecutionLogs(executionId, n);
    }
}