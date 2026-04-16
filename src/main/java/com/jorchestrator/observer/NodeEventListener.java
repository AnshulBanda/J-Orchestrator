// NodeEventListener.java
package com.jorchestrator.observer;

import com.jorchestrator.model.node.WorkerNode;

public interface NodeEventListener {
    /** Called when a node fails to send a heartbeat in time. */
    void onNodeOffline(WorkerNode node);
}