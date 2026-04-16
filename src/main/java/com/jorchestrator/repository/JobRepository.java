package com.jorchestrator.repository;

import com.jorchestrator.model.job.Job;
import com.jorchestrator.model.job.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * OOAD — DIP (Dependency Inversion Principle).
 *
 * <p>This interface is the abstraction that high-level services depend on.
 * Spring Data auto-generates the concrete implementation at runtime; services
 * never import or reference that concrete class.
 */
public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findByStatus(JobStatus status);

    long countByStatus(JobStatus status);
}