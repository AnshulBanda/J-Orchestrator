package com.jorchestrator.model.log;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A single line of output captured from a running worker process.
 *
 * <p>Written by the {@link com.jorchestrator.service.WorkerSimulatorService} (simulating
 * the Worker Agent) and read by {@link com.jorchestrator.aggregator.LogAggregator}.
 */
@Entity
@Table(name = "log_entries", indexes = {
        @Index(name = "idx_log_execution_id", columnList = "execution_id"),
        @Index(name = "idx_log_timestamp",    columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(nullable = false)
    private Instant timestamp;

    /** Output channel: {@code STDOUT} or {@code STDERR}. */
    @Column(nullable = false, length = 6)
    private String stream;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @PrePersist
    private void prePersist() {
        if (timestamp == null) timestamp = Instant.now();
    }
}