package com.jorchestrator.scheduling;

import com.jorchestrator.model.job.Job;
import com.jorchestrator.model.node.WorkerNode;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component("leastLoadStrategy")
public class LeastLoadStrategy implements SchedulingStrategy {

    @Override
    public Optional<WorkerNode> selectNode(Job job, List<WorkerNode> candidateNodes) {
        int requiredCpu = job.getResourceConstraints().getRequiredCpuCores();
        int requiredMem = job.getResourceConstraints().getRequiredMemoryMb();

        return candidateNodes.stream()
                // Filter out nodes that don't have enough total hardware capacity
                .filter(node -> node.getCpuCores() >= requiredCpu && node.getMemoryMb() >= requiredMem)
                // Sort by the ones currently running the fewest tasks
                .min(Comparator.comparingInt(WorkerNode::getCurrentLoad));
    }
}