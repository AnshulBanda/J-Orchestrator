package com.jorchestrator.repository;

import com.jorchestrator.model.job.ExecutionStatus;
import com.jorchestrator.model.job.JobExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** DIP abstraction for {@link JobExecution} persistence. */
public interface JobExecutionRepository extends JpaRepository<JobExecution, UUID> {

    List<JobExecution> findByStatus(ExecutionStatus status);

    List<JobExecution> findByAssignedNodeId(UUID nodeId);

    Optional<JobExecution> findByJobId(UUID jobId);

    long countByStatus(ExecutionStatus status);
}