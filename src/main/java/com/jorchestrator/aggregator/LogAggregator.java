package com.jorchestrator.aggregator;

import com.jorchestrator.model.log.LogEntry;
import com.jorchestrator.repository.LogEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class LogAggregator {
    private final LogEntryRepository logRepository;

    public LogAggregator(LogEntryRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Transactional
    public void appendLog(UUID executionId, String stream, String message) {
        LogEntry entry = LogEntry.builder()
            .executionId(executionId)
            .stream(stream)
            .message(message)
            .timestamp(Instant.now())
            .build();
        logRepository.save(entry);
    }

    public List<LogEntry> getLogs(UUID executionId) {
        return logRepository.findByExecutionIdOrderByTimestampAsc(executionId);
    }
}