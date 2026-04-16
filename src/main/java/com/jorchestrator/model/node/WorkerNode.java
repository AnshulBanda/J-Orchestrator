package com.jorchestrator.model.node;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * OOAD — GRASP Information Expert.
 *
 * <p>{@code WorkerNode} owns the data needed to answer "Is this node healthy?",
 * so the health check logic lives here as {@link #isHealthy(int)} rather than
 * being scattered across services. The {@link com.jorchestrator.service.NodeRegistryService}
 * delegates to this method — it does not re-implement the comparison.
 *
 * <p>OOAD — Aggregation: {@code WorkerNode} participates in
 * {@link com.jorchestrator.model.job.JobExecution} as a reference (FK). The node
 * has independent identity and lifecycle; it is not destroyed when an execution ends.
 */
@Entity
@Table(name = "worker_nodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerNode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String hostname;

    @Column(nullable = false)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NodeStatus status;

    /** Updated every time the worker agent sends a heartbeat ping. */
    private Instant lastHeartbeat;

    /** Total CPU cores available on this machine. */
    private int cpuCores;

    /** Total RAM in megabytes available on this machine. */
    private int memoryMb;

    /**
     * Number of tasks currently running on this node.
     * Used by {@link com.jorchestrator.scheduling.LeastLoadStrategy}.
     */
    private int currentLoad;

    public WorkerNode(String hostname,
                      String ipAddress,
                      NodeStatus status,
                      Instant lastHeartbeat,
                      int cpuCores,
                      int memoryMb,
                      int currentLoad) {
        this.hostname = hostname;
        this.ipAddress = ipAddress;
        this.status = status;
        this.lastHeartbeat = lastHeartbeat;
        this.cpuCores = cpuCores;
        this.memoryMb = memoryMb;
        this.currentLoad = currentLoad;
    }

    @PrePersist
    private void prePersist() {
        if (status == null) status = NodeStatus.PENDING_APPROVAL;
        if (lastHeartbeat == null) lastHeartbeat = Instant.now();
    }

    // -------------------------------------------------------------------------
    // GRASP Information Expert — health knowledge lives with the node
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} when the last heartbeat was received within the
     * configured {@code timeoutSeconds} window.
     *
     * <p>Placing this here satisfies the Information Expert principle: the node
     * holds {@code lastHeartbeat}, so it is best positioned to evaluate its own liveness.
     *
     * @param timeoutSeconds maximum allowed silence before declaring the node unhealthy
     */
    public boolean isHealthy(int timeoutSeconds) {
        if (lastHeartbeat == null) return false;
        return Instant.now().minusSeconds(timeoutSeconds).isBefore(lastHeartbeat);
    }

    /** Convenience: {@link NodeStatus#AVAILABLE} and the node is healthy. */
    public boolean isAvailable(int heartbeatTimeoutSeconds) {
        return status == NodeStatus.AVAILABLE && isHealthy(heartbeatTimeoutSeconds);
    }
}