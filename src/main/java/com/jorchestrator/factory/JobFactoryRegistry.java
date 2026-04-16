package com.jorchestrator.factory;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobFactoryRegistry {
    private final Map<String, JobFactory> factories = new ConcurrentHashMap<>();

    // Inject the concrete factories and register them immediately
    public JobFactoryRegistry(ScriptJobFactory scriptFactory, JarJobFactory jarFactory) {
        registerFactory("SCRIPT", scriptFactory);
        registerFactory("JAR", jarFactory);
    }

    public void registerFactory(String jobType, JobFactory factory) {
        factories.put(jobType.toUpperCase(), factory);
    }

    public Optional<JobFactory> getFactory(String jobType) {
        return Optional.ofNullable(factories.get(jobType.toUpperCase()));
    }
}