package com.chronos.controller;

import com.chronos.dto.JobRequest;
import com.chronos.dto.JobResponse;
import com.chronos.dto.RescheduleRequest;
import com.chronos.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest request) {
        log.info("POST /api/jobs - Creating job: {}", request.getName());
        JobResponse response = jobService.createJob(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable Long id) {
        log.info("GET /api/jobs/{}", id);
        return jobService.getJob(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<JobResponse>> getJobs(
            @RequestParam(required = false) String owner,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/jobs?owner={}&page={}&size={}", owner, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<JobResponse> jobs = jobService.getJobs(owner, pageable);
        return ResponseEntity.ok(jobs);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelJob(@PathVariable Long id) {
        log.info("POST /api/jobs/{}/cancel", id);
        boolean cancelled = jobService.cancelJob(id);
        if (cancelled) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/reschedule")
    public ResponseEntity<JobResponse> rescheduleJob(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleRequest request) {
        log.info("POST /api/jobs/{}/reschedule", id);
        return jobService.rescheduleJob(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

