package com.jorchestrator.config;

import com.jorchestrator.model.node.NodeStatus;
import com.jorchestrator.model.node.WorkerNode;
import com.jorchestrator.repository.WorkerNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

/**
 * Seeds two pre-approved worker nodes on startup so the scheduler can immediately
 * dispatch submitted jobs without requiring a separate node-registration step.
 *
 * <p>In a production environment this initialiser would be disabled (or absent)
 * and real worker agents would register themselves via {@code POST /api/nodes}.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seedNodes(WorkerNodeRepository repo) {
        return args -> {
            if (repo.count() > 0) return; // Idempotent — skip if nodes already exist

                WorkerNode worker1 = new WorkerNode(
                    "worker-node-1",
                    "10.0.0.1",
                    NodeStatus.AVAILABLE,
                    Instant.now(),
                    8,
                    16384,
                    0
                );

                WorkerNode worker2 = new WorkerNode(
                    "worker-node-2",
                    "10.0.0.2",
                    NodeStatus.AVAILABLE,
                    Instant.now(),
                    4,
                    8192,
                    0
                );

            repo.save(worker1);
            repo.save(worker2);

            log.info("DataInitializer: seeded 2 worker nodes (AVAILABLE) for demo use");
        };
    }
}