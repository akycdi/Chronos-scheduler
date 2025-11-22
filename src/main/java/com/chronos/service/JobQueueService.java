package com.chronos.service;

import com.chronos.model.Job;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobQueueService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${chronos.scheduler.worker.queue-name:job.execution.queue}")
    private String queueName;

    public void enqueueJob(Job job) {
        try {
            String message = objectMapper.writeValueAsString(job.getId());
            rabbitTemplate.convertAndSend(queueName, message);
            log.info("Enqueued job {} to queue {}", job.getId(), queueName);
        } catch (Exception e) {
            log.error("Error enqueueing job {}", job.getId(), e);
            throw new RuntimeException("Failed to enqueue job", e);
        }
    }
}

