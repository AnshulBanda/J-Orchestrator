package com.jorchestrator.factory;

import com.jorchestrator.dto.JobSubmissionRequest;
import com.jorchestrator.model.job.Job;
import com.jorchestrator.model.job.ResourceConstraints;
import com.jorchestrator.model.job.ScriptJob;
import org.springframework.stereotype.Component;

@Component
public class ScriptJobFactory extends JobFactory {

    @Override
    protected Job createJob(JobSubmissionRequest request) {
        ResourceConstraints constraints = ResourceConstraints.builder()
                .requiredCpuCores(request.getResourceConstraints().getRequiredCpuCores())
                .requiredMemoryMb(request.getResourceConstraints().getRequiredMemoryMb())
                .maxExecutionSeconds(request.getResourceConstraints().getMaxExecutionSeconds())
                .build();

        return ScriptJob.builder()
                .name(request.getName())
                .scriptContent(request.getScriptContent())
                .interpreter(request.getInterpreter())
                .resourceConstraints(constraints)
                .build();
    }
}