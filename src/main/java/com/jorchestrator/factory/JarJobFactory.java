package com.jorchestrator.factory;

import com.jorchestrator.dto.JobSubmissionRequest;
import com.jorchestrator.model.job.JarJob;
import com.jorchestrator.model.job.Job;
import com.jorchestrator.model.job.ResourceConstraints;
import org.springframework.stereotype.Component;

@Component
public class JarJobFactory extends JobFactory {

    @Override
    protected Job createJob(JobSubmissionRequest request) {
         ResourceConstraints constraints = ResourceConstraints.builder()
                .requiredCpuCores(request.getResourceConstraints().getRequiredCpuCores())
                .requiredMemoryMb(request.getResourceConstraints().getRequiredMemoryMb())
                .maxExecutionSeconds(request.getResourceConstraints().getMaxExecutionSeconds())
                .build();

        return JarJob.builder()
                .name(request.getName())
                .jarFilePath(request.getJarFilePath())
                .mainClass(request.getMainClass())
                .jvmArgs(request.getJvmArgs())
                .resourceConstraints(constraints)
                .build();
    }
}