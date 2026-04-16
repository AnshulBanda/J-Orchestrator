package com.jorchestrator.model.job;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OOAD — Composition: {@code ResourceConstraints} has no identity outside a {@link Job}.
 * It is destroyed when the owning {@code Job} is deleted.
 *
 * <p>JPA maps this as an {@link Embeddable}: its columns live in the {@code jobs} table
 * alongside the owning entity's columns (no separate table, no FK — true composition).
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceConstraints {

    /** Minimum CPU cores required to run this job. */
    private int requiredCpuCores;

    /** Minimum heap memory in megabytes required to run this job. */
    private int requiredMemoryMb;

    /** Hard wall-clock limit in seconds; execution is killed if exceeded. */
    private int maxExecutionSeconds;
}