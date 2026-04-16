package com.jorchestrator.controller;

import com.jorchestrator.dto.JobSubmissionRequest;
import com.jorchestrator.model.job.Job;
import com.jorchestrator.service.JobSubmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final JobSubmissionService service;

    public JobController(JobSubmissionService service) { 
        this.service = service; 
    }

    @PostMapping
    public ResponseEntity<Job> submitJob(@RequestBody JobSubmissionRequest request) {
        return ResponseEntity.ok(service.submitJob(request));
    }

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(service.getAllJobs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getJob(id));
    }
}