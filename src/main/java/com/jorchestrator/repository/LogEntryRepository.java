package com.jorchestrator.repository;

import com.jorchestrator.model.log.LogEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** DIP abstraction for {@link LogEntry} persistence. */
public interface LogEntryRepository extends JpaRepository<LogEntry, UUID> {

    List<LogEntry> findByExecutionIdOrderByTimestampAsc(UUID executionId);

    List<LogEntry> findByExecutionIdOrderByTimestampDesc(UUID executionId, Pageable pageable);
}