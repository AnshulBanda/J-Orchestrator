package com.jorchestrator.model.node;

/** Runtime status of a {@link WorkerNode}. */
public enum NodeStatus {
    /** Node is reachable and has capacity. */
    AVAILABLE,
    /** Node is reachable but all slots are occupied. */
    BUSY,
    /** Heartbeat not received within the configured timeout window. */
    OFFLINE,
    /** Newly registered; waiting for Cluster Admin approval. */
    PENDING_APPROVAL
}