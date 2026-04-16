package com.jorchestrator.scheduling;

import com.jorchestrator.model.job.Job;
import com.jorchestrator.model.node.WorkerNode;
import java.util.List;
import java.util.Optional;

/**
 * Strategy Pattern for selecting which node gets which job.
 */
public interface SchedulingStrategy {
    
    /**
     * Given a job and a list of healthy, available nodes, select the best node.
     * @return Empty Optional if no node has the capacity for the job.
     */
    Optional<WorkerNode> selectNode(Job job, List<WorkerNode> candidateNodes);
}