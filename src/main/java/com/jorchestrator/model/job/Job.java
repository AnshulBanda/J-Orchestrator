package com.jorchestrator.model.job;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * OOAD — Inheritance root / Factory Method Product.
 *
 * <p>{@code Job} is the abstract "Product" in the Factory Method pattern.
 * Concrete subclasses ({@link JarJob}, {@link ScriptJob}) are "Concrete Products".
 * They are created exclusively via their respective {@link com.jorchestrator.factory.JobFactory}
 * subclasses, so callers in {@link com.jorchestrator.service.JobSubmissionService} never
 * call {@code new JarJob()} directly.
 *
 * <p>JPA uses SINGLE_TABLE inheritance: all job columns are stored in one table,
 * and the discriminator column {@code job_type} identifies the subtype at runtime.
 *
 * <p>OOAD — Composition: every {@code Job} owns a {@link ResourceConstraints} instance.
 * The constraints have no lifecycle independent of the job.
 */
@Entity
@Table(name = "jobs")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "job_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public abstract class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    /**
     * OOAD — Composition: {@code ResourceConstraints} is embedded (not a separate entity).
     * No FK column; columns live directly in the {@code jobs} table.
     */
    @Embedded
    private ResourceConstraints resourceConstraints;

    @PrePersist
    private void prePersist() {
        if (submittedAt == null) submittedAt = Instant.now();
        if (status == null) status = JobStatus.PENDING;
    }
}