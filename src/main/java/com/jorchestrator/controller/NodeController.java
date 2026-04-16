package com.jorchestrator.controller;

import com.jorchestrator.model.node.WorkerNode;
import com.jorchestrator.service.NodeRegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/nodes")
public class NodeController {
    private final NodeRegistryService service;

    public NodeController(NodeRegistryService service) { 
        this.service = service; 
    }

    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<Void> heartbeat(@PathVariable UUID id) {
        service.processHeartbeat(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<WorkerNode>> getNodes() {
        return ResponseEntity.ok(service.getAllNodes());
    }

    @PostMapping("/spawn")
    public ResponseEntity<WorkerNode> spawnNode() {
        return ResponseEntity.ok(service.spawnSimulatedNode());
    }
}