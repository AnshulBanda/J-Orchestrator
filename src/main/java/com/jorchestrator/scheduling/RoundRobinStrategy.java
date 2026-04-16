package com.jorchestrator.scheduling;

import com.jorchestrator.model.job.Job;
import com.jorchestrator.model.node.WorkerNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Component("roundRobinStrategy")
public class RoundRobinStrategy implements SchedulingStrategy {

    private final AtomicInteger currentIndex = new AtomicInteger(0);

    @Override
    public Optional<WorkerNode> selectNode(Job job, List<WorkerNode> candidateNodes) {
        if (candidateNodes.isEmpty()) return Optional.empty();

        int requiredCpu = job.getResourceConstraints().getRequiredCpuCores();
        int requiredMem = job.getResourceConstraints().getRequiredMemoryMb();

        // Find capable nodes
        List<WorkerNode> capableNodes = candidateNodes.stream()
                .filter(node -> node.getCpuCores() >= requiredCpu && node.getMemoryMb() >= requiredMem)
                .toList();

        if (capableNodes.isEmpty()) return Optional.empty();

        // Pick next sequentially
        int index = currentIndex.getAndUpdate(i -> (i + 1) % capableNodes.size());
        return Optional.of(capableNodes.get(index));
    }
}