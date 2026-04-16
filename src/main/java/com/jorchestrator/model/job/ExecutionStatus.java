package com.jorchestrator.model.job;

/** Fine-grained status of a single {@link JobExecution} run. */
public enum ExecutionStatus {
    /** Task has been assigned; worker has not yet acknowledged. */
    ASSIGNED,
    /** Worker is actively running the task. */
    RUNNING,
    /** Task finished with exit code 0. */
    COMPLETED,
    /** Task finished with a non-zero exit code or threw an exception. */
    FAILED,
    /** Task was forcibly stopped because its node went OFFLINE. Re-queued as a new PENDING job. */
    EVICTED
}
