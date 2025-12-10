package com.chronos.service;

import com.chronos.dto.JobRequest;
import com.chronos.dto.JobResponse;
import com.chronos.dto.JobRunResponse;
import com.chronos.dto.RescheduleRequest;
import com.chronos.model.Job;
import com.chronos.model.JobRun;
import com.chronos.model.JobStatus;
import com.chronos.repository.JobRepository;
import com.chronos.repository.JobRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobRunRepository jobRunRepository;
    private final ScheduleService scheduleService;
    private final JobQueueService jobQueueService;
    private final NotificationService notificationService;

    @Transactional
    public JobResponse createJob(JobRequest request) {
        log.info("Creating job: {} for owner: {}", request.getName(), request.getOwner());

        // Validate recurring jobs have cron expression
        if (Boolean.TRUE.equals(request.getIsRecurring()) && request.getSchedule() != null) {
            if (!scheduleService.isValidCronExpression(request.getSchedule())) {
                throw new IllegalArgumentException(
                        "Invalid cron expression for recurring job: " + request.getSchedule());
            }
        }

        Job job = Job.builder()
                .name(request.getName())
                .owner(request.getOwner())
                .type(request.getType())
                .description(request.getDescription())
                .schedule(request.getSchedule())
                .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false)
                .maxRetries(request.getMaxRetries() != null ? request.getMaxRetries() : 3)
                .jobData(request.getJobData())
                .config(request.getConfig())
                .status(JobStatus.PENDING)
                .build();

        // Calculate next run time
        if (job.getIsRecurring() && job.getSchedule() != null) {
            scheduleService.getNextRunTime(job.getSchedule())
                    .ifPresent(job::setNextRunTime);
            job.setStatus(JobStatus.SCHEDULED);
        } else if (!job.getIsRecurring() && job.getSchedule() != null) {
            scheduleService.parseDateTime(job.getSchedule())
                    .ifPresent(job::setNextRunTime);
            job.setStatus(JobStatus.SCHEDULED);
        } else {
            // Immediate execution
            job.setNextRunTime(LocalDateTime.now());
            job.setStatus(JobStatus.PENDING);
        }

        job = jobRepository.save(job);

        // If immediate execution, enqueue immediately
        if (job.getNextRunTime() != null &&
                !job.getNextRunTime().isAfter(LocalDateTime.now())) {
            jobQueueService.enqueueJob(job);
        }

        return toJobResponse(job);
    }

    public Optional<JobResponse> getJob(Long id) {
        return jobRepository.findById(id)
                .map(this::toJobResponse);
    }

    public Page<JobResponse> getJobs(String owner, Pageable pageable) {
        Page<Job> jobs;
        if (owner != null && !owner.isEmpty()) {
            jobs = jobRepository.findByOwner(owner, pageable);
        } else {
            jobs = jobRepository.findAll(pageable);
        }
        return jobs.map(this::toJobResponse);
    }

    @Transactional
    public boolean cancelJob(Long id) {
        log.info("Cancelling job: {}", id);
        return jobRepository.findById(id)
                .map(job -> {
                    if (job.getStatus() == JobStatus.RUNNING ||
                            job.getStatus() == JobStatus.SCHEDULED ||
                            job.getStatus() == JobStatus.PENDING) {
                        job.setStatus(JobStatus.CANCELLED);
                        jobRepository.save(job);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    @Transactional
    public Optional<JobResponse> rescheduleJob(Long id, RescheduleRequest request) {
        log.info("Rescheduling job: {}", id);
        return jobRepository.findById(id)
                .map(job -> {
                    job.setSchedule(request.getSchedule());
                    if (request.getIsRecurring() != null) {
                        job.setIsRecurring(request.getIsRecurring());
                    }

                    // Calculate next run time
                    if (job.getIsRecurring()) {
                        scheduleService.getNextRunTime(job.getSchedule())
                                .ifPresent(job::setNextRunTime);
                    } else {
                        scheduleService.parseDateTime(job.getSchedule())
                                .ifPresent(job::setNextRunTime);
                    }

                    job.setVersion(job.getVersion() + 1);
                    job.setStatus(JobStatus.SCHEDULED);
                    job = jobRepository.save(job);
                    return toJobResponse(job);
                });
    }

    @Transactional
    public boolean runJob(Long id) {
        log.info("Manually triggering job: {}", id);
        return jobRepository.findById(id)
                .map(job -> {
                    job.setNextRunTime(LocalDateTime.now());
                    job.setStatus(JobStatus.PENDING);
                    jobRepository.save(job);
                    jobQueueService.enqueueJob(job);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public void markJobAsRunning(Long jobId) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(JobStatus.RUNNING);
            job.setLastRunTime(LocalDateTime.now());
            jobRepository.save(job);
        });
    }

    @Transactional
    public void markJobAsCompleted(Long jobId, String output) {
        jobRepository.findById(jobId).ifPresent(job -> {
            if (job.getIsRecurring()) {
                // Calculate next run time for recurring jobs
                scheduleService.getNextRunTime(job.getSchedule())
                        .ifPresent(job::setNextRunTime);
                job.setStatus(JobStatus.SCHEDULED);
            } else {
                job.setStatus(JobStatus.COMPLETED);
            }
            job.setCurrentRetries(0); // Reset retries on success
            jobRepository.save(job);
        });
    }

    @Transactional
    public void markJobAsFailed(Long jobId, String error, boolean shouldRetry) {
        jobRepository.findById(jobId).ifPresent(job -> {
            if (shouldRetry && job.getCurrentRetries() < job.getMaxRetries()) {
                job.setStatus(JobStatus.RETRYING);
                job.setCurrentRetries(job.getCurrentRetries() + 1);

                // Calculate retry delay
                long retryDelaySeconds = 5;
                try {
                    if (job.getConfig() != null) {
                        com.fasterxml.jackson.databind.JsonNode configNode = new com.fasterxml.jackson.databind.ObjectMapper()
                                .readTree(job.getConfig());
                        if (configNode.has("retryDelaySeconds")) {
                            retryDelaySeconds = configNode.get("retryDelaySeconds").asLong();
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse job config for retry delay", e);
                }

                // Schedule retry after delay
                job.setNextRunTime(LocalDateTime.now().plusSeconds(retryDelaySeconds));
            } else {
                job.setStatus(JobStatus.FAILED);
                // Notify user about final failure
                notificationService.notifyJobFailure(job, error);
            }
            jobRepository.save(job);
        });
    }

    private JobResponse toJobResponse(Job job) {
        List<JobRunResponse> recentRuns = jobRunRepository
                .findByJobOrderByCreatedAtDesc(job, PageRequest.of(0, 5))
                .stream()
                .map(this::toJobRunResponse)
                .collect(Collectors.toList());

        return JobResponse.builder()
                .id(job.getId())
                .name(job.getName())
                .owner(job.getOwner())
                .type(job.getType())
                .status(job.getStatus())
                .description(job.getDescription())
                .schedule(job.getSchedule())
                .isRecurring(job.getIsRecurring())
                .maxRetries(job.getMaxRetries())
                .currentRetries(job.getCurrentRetries())
                .jobData(job.getJobData())
                .config(job.getConfig())
                .version(job.getVersion())
                .nextRunTime(job.getNextRunTime())
                .lastRunTime(job.getLastRunTime())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .recentRuns(recentRuns)
                .build();
    }

    private JobRunResponse toJobRunResponse(JobRun run) {
        return JobRunResponse.builder()
                .id(run.getId())
                .jobId(run.getJob().getId())
                .status(run.getStatus())
                .startedAt(run.getStartedAt())
                .completedAt(run.getCompletedAt())
                .output(run.getOutput())
                .error(run.getError())
                .retryAttempt(run.getRetryAttempt())
                .workerId(run.getWorkerId())
                .build();
    }
}
