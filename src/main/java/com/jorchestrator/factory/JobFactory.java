package com.jorchestrator.factory;

import com.jorchestrator.dto.JobSubmissionRequest;
import com.jorchestrator.model.job.Job;

/**
 * OOAD — Factory Method (Abstract Creator).
 *
 * <p>Defines the factory method {@link #createJob(JobSubmissionRequest)} that concrete
 * subclasses must override to instantiate the correct {@link Job} subtype.
 *
 * <p>The template method {@link #build(JobSubmissionRequest)} provides a shared hook
 * (e.g. setting common fields) and then delegates the type-specific construction to the
 * overriding subclass — callers never call {@code new JarJob()} or {@code new ScriptJob()} directly.
 *
 * <p><b>OCP compliance:</b> To support a new job type (e.g. {@code DockerJob}), a developer
 * creates a new {@code DockerJobFactory extends JobFactory} and registers it in
 * {@link JobFactoryRegistry}. No existing factory class is modified.
 */
public abstract class JobFactory {

    /**
     * Factory Method — override to construct the concrete {@link Job} subtype.
     *
     * @param request the validated submission payload
     * @return a new, unpersisted {@link Job} instance
     */
    protected abstract Job createJob(JobSubmissionRequest request);

    /**
     * Template method: applies common defaults then calls the factory method.
     * Callers (e.g. {@link com.jorchestrator.service.JobSubmissionService}) use this.
     */
    public Job build(JobSubmissionRequest request) {
        Job job = createJob(request);
        // Common post-creation defaults are already set via @PrePersist on Job,
        // but any cross-cutting enrichment (e.g. audit tagging) could live here.
        return job;
    }
}