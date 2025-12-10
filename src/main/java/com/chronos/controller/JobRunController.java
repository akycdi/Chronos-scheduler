package com.chronos.controller;

import com.chronos.dto.JobRunResponse;
import com.chronos.repository.JobRepository;
import com.chronos.repository.JobRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/jobs/{jobId}/runs")
@RequiredArgsConstructor
public class JobRunController {

        private final JobRepository jobRepository;
        private final JobRunRepository jobRunRepository;

        @GetMapping
        public ResponseEntity<Page<JobRunResponse>> getJobRuns(
                        @PathVariable Long jobId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                log.info("GET /api/jobs/{}/runs?page={}&size={}", jobId, page, size);

                return jobRepository.findById(jobId)
                                .map(job -> {
                                        Pageable pageable = PageRequest.of(page, size);
                                        Page<JobRunResponse> runs = jobRunRepository.findByJob(job, pageable)
                                                        .map(run -> JobRunResponse.builder()
                                                                        .id(run.getId())
                                                                        .jobId(run.getJob().getId())
                                                                        .status(run.getStatus())
                                                                        .startedAt(run.getStartedAt())
                                                                        .completedAt(run.getCompletedAt())
                                                                        .output(run.getOutput())
                                                                        .error(run.getError())
                                                                        .retryAttempt(run.getRetryAttempt())
                                                                        .workerId(run.getWorkerId())
                                                                        .build());
                                        return ResponseEntity.ok(runs);
                                })
                                .orElse(ResponseEntity.notFound().build());
        }

        @GetMapping("/{runId}")
        public ResponseEntity<JobRunResponse> getJobRun(
                        @PathVariable Long jobId,
                        @PathVariable Long runId) {
                log.info("GET /api/jobs/{}/runs/{}", jobId, runId);

                return jobRunRepository.findById(runId)
                                .filter(run -> run.getJob().getId().equals(jobId))
                                .map(run -> JobRunResponse.builder()
                                                .id(run.getId())
                                                .jobId(run.getJob().getId())
                                                .status(run.getStatus())
                                                .startedAt(run.getStartedAt())
                                                .completedAt(run.getCompletedAt())
                                                .output(run.getOutput())
                                                .error(run.getError())
                                                .retryAttempt(run.getRetryAttempt())
                                                .workerId(run.getWorkerId())
                                                .build())
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }
}
