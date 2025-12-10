package com.chronos.controller;

import com.chronos.model.JobStatus;
import com.chronos.repository.JobRepository;
import com.chronos.repository.JobRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final JobRepository jobRepository;
    private final JobRunRepository jobRunRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("GET /api/monitoring/stats");
        
        Map<String, Object> stats = new HashMap<>();
        
        // Job statistics
        Map<String, Long> jobStats = new HashMap<>();
        for (JobStatus status : JobStatus.values()) {
            long count = jobRepository.countByStatus(status);
            jobStats.put(status.name().toLowerCase(), count);
        }
        jobStats.put("total", jobRepository.count());
        stats.put("jobs", jobStats);
        
        // Run statistics
        Map<String, Object> runStats = new HashMap<>();
        runStats.put("total", jobRunRepository.count());
        
        Map<String, Long> runStatusStats = new HashMap<>();
        for (JobStatus status : JobStatus.values()) {
            // Count runs by status - in production, add repository method
            long count = jobRunRepository.findAll().stream()
                    .filter(run -> run.getStatus() == status)
                    .count();
            runStatusStats.put(status.name().toLowerCase(), count);
        }
        runStats.put("byStatus", runStatusStats);
        stats.put("runs", runStats);
        
        // System health
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("database", "CONNECTED");
        stats.put("health", health);
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Chronos Job Scheduler");
        return ResponseEntity.ok(health);
    }
}

