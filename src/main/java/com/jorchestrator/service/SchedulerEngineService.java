package com.jorchestrator.service;

import com.jorchestrator.model.job.Job;
import com.jorchestrator.model.job.JobExecution;
import com.jorchestrator.model.job.JobStatus;
import com.jorchestrator.model.node.NodeStatus;
import com.jorchestrator.model.node.WorkerNode;
import com.jorchestrator.repository.JobExecutionRepository;
import com.jorchestrator.repository.JobRepository;
import com.jorchestrator.repository.WorkerNodeRepository;
import com.jorchestrator.scheduling.SchedulingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SchedulerEngineService {
    private static final Logger log = LoggerFactory.getLogger(SchedulerEngineService.class);
    
    private final JobRepository jobRepository;
    private final WorkerNodeRepository nodeRepository;
    private final JobExecutionRepository executionRepository;
    private final SchedulingStrategy schedulingStrategy;

    public SchedulerEngineService(JobRepository jobRepository, 
                                  WorkerNodeRepository nodeRepository, 
                                  JobExecutionRepository executionRepository,
                                  @Qualifier("leastLoadStrategy") SchedulingStrategy schedulingStrategy) {
        this.jobRepository = jobRepository;
        this.nodeRepository = nodeRepository;
        this.executionRepository = executionRepository;
        this.schedulingStrategy = schedulingStrategy;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void dispatchPendingJobs() {
        List<Job> pendingJobs = jobRepository.findByStatus(JobStatus.PENDING);
        if (pendingJobs.isEmpty()) return;

        List<WorkerNode> availableNodes = nodeRepository.findByStatus(NodeStatus.AVAILABLE);
        
        for (Job job : pendingJobs) {
            Optional<WorkerNode> selectedNode = schedulingStrategy.selectNode(job, availableNodes);
            
            if (selectedNode.isPresent()) {
                WorkerNode node = selectedNode.get();
                
                job.setStatus(JobStatus.SCHEDULED);
                jobRepository.save(job);
                
                JobExecution execution = JobExecution.builder()
                    .job(job)
                    .assignedNode(node)
                    .build();
                executionRepository.save(execution);
                
                node.setCurrentLoad(node.getCurrentLoad() + 1);
                nodeRepository.save(node);
                
                log.info("Assigned Job {} to Node {}", job.getId(), node.getHostname());
            }
        }
    }
}