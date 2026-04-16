// NodeEventPublisher.java
package com.jorchestrator.observer;

public interface NodeEventPublisher {
    void registerListener(NodeEventListener listener);
    void removeListener(NodeEventListener listener);
}