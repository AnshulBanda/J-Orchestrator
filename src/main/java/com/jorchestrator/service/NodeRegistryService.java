package com.jorchestrator.service;

import com.jorchestrator.model.node.NodeStatus;
import com.jorchestrator.model.node.WorkerNode;
import com.jorchestrator.repository.WorkerNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NodeRegistryService {
    private static final Logger log = LoggerFactory.getLogger(NodeRegistryService.class);
    private final WorkerNodeRepository nodeRepository;
    private static final int HEARTBEAT_TIMEOUT = 30;

    public NodeRegistryService(WorkerNodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    @Transactional
    public void processHeartbeat(UUID nodeId) {
        WorkerNode node = nodeRepository.findById(nodeId)
            .orElseThrow(() -> new IllegalArgumentException("Node not found"));
        
        node.setLastHeartbeat(Instant.now());
        if (node.getStatus() == NodeStatus.OFFLINE) {
            node.setStatus(NodeStatus.AVAILABLE);
        }
        nodeRepository.save(node);
    }

    public List<WorkerNode> getAllNodes() {
        return nodeRepository.findAll();
    }

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void checkHeartbeats() {
        List<WorkerNode> activeNodes = nodeRepository.findAll();
        for (WorkerNode node : activeNodes) {
            if (node.getStatus() != NodeStatus.OFFLINE && !node.isHealthy(HEARTBEAT_TIMEOUT)) {
                log.warn("Node {} timed out. Marking OFFLINE.", node.getHostname());
                node.setStatus(NodeStatus.OFFLINE);
                nodeRepository.save(node);
            }
        }
    }

    // Add this to NodeRegistryService.java
    @Transactional
    public WorkerNode spawnSimulatedNode() {
        long count = nodeRepository.count() + 1;
        WorkerNode newNode = new WorkerNode(
            "worker-node-" + count,
            "10.0.0." + count,
            NodeStatus.AVAILABLE,
            Instant.now(),
            (int) (Math.random() * 8) + 2, // Random cores between 2 and 9
            8192,
            0
        );
        log.info("Dynamically spawned new worker node: {}", newNode.getHostname());
        return nodeRepository.save(newNode);
    }
}