package com.chronos.consumer;

import com.chronos.service.JobExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobConsumer {

    private final JobExecutionService jobExecutionService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${chronos.scheduler.worker.queue-name:job.execution.queue}")
    public void consumeJob(String message) {
        try {
            Long jobId = objectMapper.readValue(message, Long.class);
            log.info("Received job execution request for job ID: {}", jobId);
            jobExecutionService.executeJob(jobId);
        } catch (Exception e) {
            log.error("Error processing job message: {}", message, e);
            throw new RuntimeException("Failed to process job", e);
        }
    }
}

