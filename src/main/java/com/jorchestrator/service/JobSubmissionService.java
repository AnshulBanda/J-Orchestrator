package com.jorchestrator.service;

import com.jorchestrator.dto.JobSubmissionRequest;
import com.jorchestrator.factory.JobFactory;
import com.jorchestrator.factory.JobFactoryRegistry;
import com.jorchestrator.model.job.Job;
import com.jorchestrator.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class JobSubmissionService {
    private final JobFactoryRegistry factoryRegistry;
    private final JobRepository jobRepository;

    public JobSubmissionService(JobFactoryRegistry factoryRegistry, JobRepository jobRepository) {
        this.factoryRegistry = factoryRegistry;
        this.jobRepository = jobRepository;
    }

    @Transactional
    public Job submitJob(JobSubmissionRequest request) {
        JobFactory factory = factoryRegistry.getFactory(request.getJobType())
            .orElseThrow(() -> new IllegalArgumentException("Unknown job type: " + request.getJobType()));
        
        Job job = factory.build(request);
        return jobRepository.save(job);
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public Job getJob(UUID id) {
        return jobRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Job not found"));
    }
}