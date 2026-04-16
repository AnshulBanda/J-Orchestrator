package com.jorchestrator.facade;

import com.jorchestrator.model.job.ExecutionStatus;
import com.jorchestrator.model.job.JobStatus;
import com.jorchestrator.model.log.LogEntry;
import com.jorchestrator.model.node.NodeStatus;
import com.jorchestrator.repository.JobExecutionRepository;
import com.jorchestrator.repository.JobRepository;
import com.jorchestrator.repository.LogEntryRepository;
import com.jorchestrator.repository.WorkerNodeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Facade that provides read-only monitoring data through a single entry point.
 */
@Service
@Transactional(readOnly = true)
public class SystemMonitorFacade {

	private final WorkerNodeRepository workerNodeRepository;
	private final JobRepository jobRepository;
	private final JobExecutionRepository jobExecutionRepository;
	private final LogEntryRepository logEntryRepository;

	public SystemMonitorFacade(WorkerNodeRepository workerNodeRepository,
							   JobRepository jobRepository,
							   JobExecutionRepository jobExecutionRepository,
							   LogEntryRepository logEntryRepository) {
		this.workerNodeRepository = workerNodeRepository;
		this.jobRepository = jobRepository;
		this.jobExecutionRepository = jobExecutionRepository;
		this.logEntryRepository = logEntryRepository;
	}

	public ClusterHealthSnapshot getClusterHealth() {
		long totalNodes = workerNodeRepository.count();
		long availableNodes = workerNodeRepository.countByStatus(NodeStatus.AVAILABLE);
		long busyNodes = workerNodeRepository.countByStatus(NodeStatus.BUSY);
		long offlineNodes = workerNodeRepository.countByStatus(NodeStatus.OFFLINE);
		long pendingApprovalNodes = workerNodeRepository.countByStatus(NodeStatus.PENDING_APPROVAL);

		long totalJobs = jobRepository.count();
		long pendingJobs = jobRepository.countByStatus(JobStatus.PENDING);
		long scheduledJobs = jobRepository.countByStatus(JobStatus.SCHEDULED);
		long runningJobs = jobRepository.countByStatus(JobStatus.RUNNING);
		long completedJobs = jobRepository.countByStatus(JobStatus.COMPLETED);
		long failedJobs = jobRepository.countByStatus(JobStatus.FAILED);
		long cancelledJobs = jobRepository.countByStatus(JobStatus.CANCELLED);

		long assignedExecutions = jobExecutionRepository.countByStatus(ExecutionStatus.ASSIGNED);
		long runningExecutions = jobExecutionRepository.countByStatus(ExecutionStatus.RUNNING);

		return new ClusterHealthSnapshot(
				totalNodes,
				availableNodes,
				busyNodes,
				offlineNodes,
				pendingApprovalNodes,
				totalJobs,
				pendingJobs,
				scheduledJobs,
				runningJobs,
				completedJobs,
				failedJobs,
				cancelledJobs,
				assignedExecutions,
				runningExecutions,
				assignedExecutions + runningExecutions
		);
	}

	public List<LogEntry> getExecutionLogs(UUID executionId) {
		return logEntryRepository.findByExecutionIdOrderByTimestampAsc(executionId);
	}

	public List<LogEntry> tailExecutionLogs(UUID executionId, int n) {
		int limit = Math.max(1, n);
		List<LogEntry> newestFirst = logEntryRepository.findByExecutionIdOrderByTimestampDesc(
				executionId,
				PageRequest.of(0, limit)
		);

		// Convert newest-first page to chronological order for UI rendering.
		List<LogEntry> chronological = new ArrayList<>(newestFirst);
		Collections.reverse(chronological);
		return chronological;
	}

	public record ClusterHealthSnapshot(
			long totalNodes,
			long availableNodes,
			long busyNodes,
			long offlineNodes,
			long pendingApprovalNodes,
			long totalJobs,
			long pendingJobs,
			long scheduledJobs,
			long runningJobs,
			long completedJobs,
			long failedJobs,
			long cancelledJobs,
			long assignedExecutions,
			long runningExecutions,
			long activeExecutions
	) {
	}
}
