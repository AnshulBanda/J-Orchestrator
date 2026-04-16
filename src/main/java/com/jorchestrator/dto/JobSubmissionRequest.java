package com.jorchestrator.dto;

import lombok.Data;

@Data
public class JobSubmissionRequest {
    private String name;
    private String jobType; // "JAR" or "SCRIPT"
    
    // JAR specific
    private String jarFilePath;
    private String mainClass;
    private String jvmArgs;
    
    // SCRIPT specific
    private String scriptContent;
    private String interpreter;
    
    // Shared Constraints
    private ResourceConstraintsDto resourceConstraints;

    @Data
    public static class ResourceConstraintsDto {
        private int requiredCpuCores;
        private int requiredMemoryMb;
        private int maxExecutionSeconds;
    }
}