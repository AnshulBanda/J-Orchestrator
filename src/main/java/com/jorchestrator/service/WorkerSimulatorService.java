package com.jorchestrator.service;

import com.jorchestrator.aggregator.LogAggregator;
import com.jorchestrator.model.job.ExecutionStatus;
import com.jorchestrator.model.job.JobExecution;
import com.jorchestrator.model.job.JobStatus;
import com.jorchestrator.model.node.WorkerNode;
import com.jorchestrator.model.node.NodeStatus;
import com.jorchestrator.repository.JobExecutionRepository;
import com.jorchestrator.repository.JobRepository;
import com.jorchestrator.repository.WorkerNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class WorkerSimulatorService {
    private static final Logger log = LoggerFactory.getLogger(WorkerSimulatorService.class);

    private final JobExecutionRepository executionRepo;
    private final JobRepository jobRepo;
    private final WorkerNodeRepository nodeRepo;
    private final LogAggregator logAggregator;

    public WorkerSimulatorService(JobExecutionRepository executionRepo, JobRepository jobRepo, 
                                  WorkerNodeRepository nodeRepo, LogAggregator logAggregator) {
        this.executionRepo = executionRepo;
        this.jobRepo = jobRepo;
        this.nodeRepo = nodeRepo;
        this.logAggregator = logAggregator;
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void simulateExecution() {
        // 1. Pick up newly assigned jobs and start them
        List<JobExecution> assigned = executionRepo.findByStatus(ExecutionStatus.ASSIGNED);
        for (JobExecution exec : assigned) {
            exec.setStatus(ExecutionStatus.RUNNING);
            exec.getJob().setStatus(JobStatus.RUNNING);
            executionRepo.save(exec);
            jobRepo.save(exec.getJob());
            
            logAggregator.appendLog(exec.getId(), "STDOUT", "Worker agent initialized. Downloading job payload...");
            log.info("Simulator: Started execution {}", exec.getId());
        }

        // 2. Progress running jobs and complete them
        List<JobExecution> running = executionRepo.findByStatus(ExecutionStatus.RUNNING);
        for (JobExecution exec : running) {
            // Simulate that jobs take about 10 seconds to run
            if (exec.getStartedAt().plusSeconds(10).isBefore(Instant.now())) {
                exec.setStatus(ExecutionStatus.COMPLETED);
                exec.setCompletedAt(Instant.now());
                exec.setExitCode(0);
                
                exec.getJob().setStatus(JobStatus.COMPLETED);
                
                // Free up the node's capacity
                WorkerNode node = exec.getAssignedNode();
                if (node != null && node.getCurrentLoad() > 0) {
                    node.setCurrentLoad(node.getCurrentLoad() - 1);
                    nodeRepo.save(node);
                }

                logAggregator.appendLog(exec.getId(), "STDOUT", "Process exited with code 0. Job Complete.");
                executionRepo.save(exec);
                jobRepo.save(exec.getJob());
                log.info("Simulator: Completed execution {}", exec.getId());
            } else {
                // Just append some heartbeat logs while running
                logAggregator.appendLog(exec.getId(), "STDOUT", "Processing chunk... [memory usage normal]");
            }
        }
    }

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void simulateHeartbeats() {
        // Keep simulated workers alive so scheduler can continue assigning jobs.
        List<WorkerNode> nodes = nodeRepo.findAll();
        for (WorkerNode node : nodes) {
            if (node.getStatus() != NodeStatus.PENDING_APPROVAL) {
                node.setLastHeartbeat(Instant.now());
                if (node.getStatus() == NodeStatus.OFFLINE) {
                    node.setStatus(NodeStatus.AVAILABLE);
                }
                nodeRepo.save(node);
            }
        }
    }
}