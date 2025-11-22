package com.chronos.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private final MeterRegistry meterRegistry;
    private final AtomicLong activeJobs = new AtomicLong(0);
    private final AtomicLong queuedJobs = new AtomicLong(0);

    @Bean
    public Timer jobExecutionTimer() {
        return Timer.builder("chronos.job.execution.time")
                .description("Time taken to execute jobs")
                .register(meterRegistry);
    }

    @Bean
    public Counter jobSubmissionCounter() {
        return Counter.builder("chronos.job.submitted")
                .description("Total number of jobs submitted")
                .register(meterRegistry);
    }

    @Bean
    public Counter jobCompletionCounter() {
        return Counter.builder("chronos.job.completed")
                .description("Total number of jobs completed")
                .register(meterRegistry);
    }

    @Bean
    public Counter jobFailureCounter() {
        return Counter.builder("chronos.job.failed")
                .description("Total number of jobs failed")
                .register(meterRegistry);
    }

    @Bean
    public Gauge activeJobsGauge() {
        return Gauge.builder("chronos.job.active", activeJobs, AtomicLong::get)
                .description("Number of currently active jobs")
                .register(meterRegistry);
    }

    @Bean
    public Gauge queuedJobsGauge() {
        return Gauge.builder("chronos.job.queued", queuedJobs, AtomicLong::get)
                .description("Number of jobs in queue")
                .register(meterRegistry);
    }

    public void incrementActiveJobs() {
        activeJobs.incrementAndGet();
    }

    public void decrementActiveJobs() {
        activeJobs.decrementAndGet();
    }

    public void setQueuedJobs(long count) {
        queuedJobs.set(count);
    }
}

