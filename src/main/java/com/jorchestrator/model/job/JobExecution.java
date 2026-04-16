package com.jorchestrator.model.job;

import com.jorchestrator.model.node.WorkerNode;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * OOAD — Composition: a {@code JobExecution} cannot exist without a parent {@link Job}.
 * It is created by the {@link com.jorchestrator.service.SchedulerEngineService} at assignment
 * time and destroyed if the job is deleted (cascade).
 *
 * <p>OOAD — Aggregation: the {@link WorkerNode} is referenced via FK. The node outlives
 * any individual execution.
 */
@Entity
@Table(name = "job_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
        * Composition: cascade DELETE ensures executions are removed with the job.
        * Changed to EAGER so Hibernate reads the discriminator column immediately
        * and instantiates the true polymorphic subclass (ScriptJob or JarJob).
     */
        @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    /**
     * Aggregation: references an independent {@link WorkerNode}.
     * Nullable — set to NULL if the node is evicted before assignment is confirmed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id")
    private WorkerNode assignedNode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    private Instant startedAt;

    private Instant completedAt;

    /** Process exit code; null until the execution finishes. */
    private Integer exitCode;

    @PrePersist
    private void prePersist() {
        if (status == null) status = ExecutionStatus.ASSIGNED;
        if (startedAt == null) startedAt = Instant.now();
    }
}