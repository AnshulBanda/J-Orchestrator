package com.jorchestrator.model.job;

/** Lifecycle states of a submitted {@link Job}. */
public enum JobStatus {
    /** Submitted, not yet assigned to a node. */
    PENDING,
    /** Assigned to a node; execution has not started. */
    SCHEDULED,
    /** Currently executing on a worker node. */
    RUNNING,
    /** Finished successfully. */
    COMPLETED,
    /** Finished with a non-zero exit code or an exception. */
    FAILED,
    /** Manually stopped by a DevOps Engineer. */
    CANCELLED
}