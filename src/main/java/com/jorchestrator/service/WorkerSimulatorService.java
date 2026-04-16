package com.jorchestrator.service;

import com.jorchestrator.aggregator.LogAggregator;
import com.jorchestrator.model.job.ExecutionStatus;
import com.jorchestrator.model.job.JobExecution;
import com.jorchestrator.model.job.JobStatus;
import com.jorchestrator.model.job.ScriptJob;
import com.jorchestrator.model.job.Job;
import com.jorchestrator.model.node.NodeStatus;
import com.jorchestrator.model.node.WorkerNode;
import com.jorchestrator.repository.JobExecutionRepository;
import com.jorchestrator.repository.JobRepository;
import com.jorchestrator.repository.WorkerNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    public void pickupAssignedJobs() {
        List<JobExecution> assigned = executionRepo.findByStatus(ExecutionStatus.ASSIGNED);
        for (JobExecution exec : assigned) {
            exec.setStatus(ExecutionStatus.RUNNING);
            exec.getJob().setStatus(JobStatus.RUNNING);
            executionRepo.save(exec);
            jobRepo.save(Objects.requireNonNull(exec.getJob()));
            
            logAggregator.appendLog(exec.getId(), "SYSTEM", "Worker picked up job. Initiating OS Process...");

            // Because FetchType is EAGER, this is now guaranteed to be the true subclass, not a proxy
            if (exec.getJob() instanceof ScriptJob scriptJob) {
                // Fire and forget a background thread to actually run the shell command
                CompletableFuture.runAsync(() ->
                    executeRealProcess(exec.getId(), scriptJob.getInterpreter(), scriptJob.getScriptContent(), exec.getAssignedNode().getId())
                );
            } else {
                logAggregator.appendLog(exec.getId(), "SYSTEM", "Only SCRIPT jobs are supported by the real-execution simulator.");
                finalizeExecution(exec.getId(), -1, exec.getAssignedNode().getId());
            }
        }
    }

    private void executeRealProcess(UUID executionId, String interpreter, String script, UUID nodeId) {
        try {
            ProcessBuilder pb = new ProcessBuilder(interpreter, "-c", script);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 1. Send it to the database
                    logAggregator.appendLog(executionId, "STDOUT", line);
                    // 2. Print it to your IDE terminal so you can see it!
                    log.info("[Node: {}] {}", nodeId.toString().substring(0, 8), line);
                }
            }
            
            int exitCode = process.waitFor();
            finalizeExecution(executionId, exitCode, nodeId);
            
        } catch (Exception e) {
            log.error("Process execution failed", e);
            finalizeExecution(executionId, 1, nodeId);
        }
    }

    // Removed @Transactional. We handle the DB saves manually to avoid Proxy traps.
    public void finalizeExecution(UUID executionId, int exitCode, UUID nodeId) {
        try {
            JobExecution exec = executionRepo.findById(Objects.requireNonNull(executionId)).orElseThrow();
            exec.setStatus(exitCode == 0 ? ExecutionStatus.COMPLETED : ExecutionStatus.FAILED);
            exec.setCompletedAt(Instant.now());
            exec.setExitCode(exitCode);
            
            // Explicit DB fetch avoids the LazyInitializationException crash
            Job job = jobRepo.findById(Objects.requireNonNull(exec.getJob().getId())).orElseThrow();
            job.setStatus(exitCode == 0 ? JobStatus.COMPLETED : JobStatus.FAILED);
            
            WorkerNode node = nodeRepo.findById(Objects.requireNonNull(nodeId)).orElseThrow();
            if (node.getCurrentLoad() > 0) {
                node.setCurrentLoad(node.getCurrentLoad() - 1);
            }
            
            executionRepo.save(exec);
            jobRepo.save(job);
            nodeRepo.save(node);
            
            log.info("Execution {} fully completed with exit code {}", executionId, exitCode);
        } catch (Exception e) {
            log.error("Failed to update database after process execution!", e);
        }
    }

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void simulateHeartbeats() {
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