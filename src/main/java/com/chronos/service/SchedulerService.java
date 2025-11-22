package com.chronos.service;

import com.chronos.model.Job;
import com.chronos.model.JobStatus;
import com.chronos.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final JobRepository jobRepository;
    private final JobQueueService jobQueueService;

    @Value("${chronos.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Value("${chronos.scheduler.poll-interval-ms:5000}")
    private long pollIntervalMs;

    /**
     * Polls for jobs that are ready to execute and enqueues them
     */
    @Scheduled(fixedDelayString = "${chronos.scheduler.poll-interval-ms:5000}")
    @Transactional
    public void scheduleJobs() {
        if (!schedulerEnabled) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        
        // Find scheduled jobs ready to execute
        List<Job> jobsToExecute = jobRepository.findByStatusAndNextRunTimeLessThanEqual(
            JobStatus.SCHEDULED, now
        );

        // Also check for retrying jobs
        List<Job> retryingJobs = jobRepository.findByStatusAndNextRunTimeLessThanEqual(
            JobStatus.RETRYING, now
        );

        jobsToExecute.addAll(retryingJobs);

        for (Job job : jobsToExecute) {
            if (job.getNextRunTime() != null && !job.getNextRunTime().isAfter(now)) {
                log.info("Scheduling job {} for execution", job.getId());
                jobQueueService.enqueueJob(job);
            }
        }

        if (!jobsToExecute.isEmpty()) {
            log.debug("Scheduled {} jobs for execution", jobsToExecute.size());
        }
    }
}

