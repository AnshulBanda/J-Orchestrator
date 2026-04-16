package com.jorchestrator.repository;

import com.jorchestrator.model.node.NodeStatus;
import com.jorchestrator.model.node.WorkerNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** DIP abstraction for {@link WorkerNode} persistence. */
public interface WorkerNodeRepository extends JpaRepository<WorkerNode, UUID> {

    List<WorkerNode> findByStatus(NodeStatus status);

    long countByStatus(NodeStatus status);
}