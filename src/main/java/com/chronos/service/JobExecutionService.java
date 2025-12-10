package com.chronos.service;

import com.chronos.model.Job;
import com.chronos.model.JobRun;
import com.chronos.model.JobStatus;
import com.chronos.repository.JobRepository;
import com.chronos.repository.JobRunRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutionService {

    private final JobRepository jobRepository;
    private final JobRunRepository jobRunRepository;
    private final JobService jobService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;
    private final MeterRegistry meterRegistry;

    @Value("${chronos.job.http.timeout-seconds:60}")
    private int defaultTimeout;

    @Transactional
    public void executeJob(Long jobId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        if (job.getStatus() == JobStatus.CANCELLED) {
            log.warn("Job {} is cancelled, skipping execution", jobId);
            return;
        }

        String workerId = UUID.randomUUID().toString();
        JobRun run = JobRun.builder()
                .job(job)
                .status(JobStatus.RUNNING)
                .startedAt(LocalDateTime.now())
                .workerId(workerId)
                .retryAttempt(job.getCurrentRetries())
                .build();
        run = jobRunRepository.save(run);

        jobService.markJobAsRunning(jobId);

        try {
            log.info("Executing job {} of type {}", jobId, job.getType());
            String output = executeJobByType(job);

            run.setStatus(JobStatus.COMPLETED);
            run.setCompletedAt(LocalDateTime.now());
            run.setOutput(output);
            jobRunRepository.save(run);

            jobService.markJobAsCompleted(jobId, output);
            sample.stop(Timer.builder("chronos.job.execution.time")
                    .tag("status", "success")
                    .tag("type", job.getType().name())
                    .register(meterRegistry));

            meterRegistry.counter("chronos.job.execution",
                    "status", "success",
                    "type", job.getType().name()).increment();

            log.info("Job {} completed successfully", jobId);
            notificationService.notifyJobCompletion(job);

        } catch (Exception e) {
            log.error("Job {} execution failed", jobId, e);

            run.setStatus(JobStatus.FAILED);
            run.setCompletedAt(LocalDateTime.now());
            run.setError(e.getMessage());
            jobRunRepository.save(run);

            boolean shouldRetry = job.getCurrentRetries() < job.getMaxRetries();
            jobService.markJobAsFailed(jobId, e.getMessage(), shouldRetry);

            sample.stop(Timer.builder("chronos.job.execution.time")
                    .tag("status", "failed")
                    .tag("type", job.getType().name())
                    .register(meterRegistry));

            meterRegistry.counter("chronos.job.execution",
                    "status", "failed",
                    "type", job.getType().name()).increment();

            if (shouldRetry) {
                log.info("Job {} will be retried (attempt {}/{})",
                        jobId, job.getCurrentRetries() + 1, job.getMaxRetries());
            } else {
                log.error("Job {} failed after {} retries", jobId, job.getMaxRetries());
                notificationService.notifyJobFailure(job, e.getMessage());
            }
        }
    }

    private String executeJobByType(Job job) throws Exception {
        switch (job.getType()) {
            case HTTP_REQUEST:
                return executeHttpRequest(job);
            case SHELL_SCRIPT:
                return executeShellScript(job);
            case JAVA_CLASS:
                return executeJavaClass(job);
            case PYTHON_SCRIPT:
                return executePythonScript(job);
            case CUSTOM:
                return executeCustomJob(job);
            default:
                throw new UnsupportedOperationException("Unsupported job type: " + job.getType());
        }
    }

    private String executeHttpRequest(Job job) throws Exception {
        log.info("Executing HTTP request job: {}", job.getName());

        try {
            JsonNode jobData = objectMapper.readTree(job.getJobData() != null ? job.getJobData() : "{}");
            String url = jobData.path("url").asText(null);
            String method = jobData.path("method").asText("GET");
            int timeout = jobData.path("timeout").asInt(defaultTimeout);
            JsonNode headers = jobData.has("headers") ? jobData.get("headers") : null;
            JsonNode body = jobData.has("body") ? jobData.get("body") : null;

            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("URL is required for HTTP_REQUEST job type");
            }

            WebClient webClient = webClientBuilder
                    .baseUrl(url)
                    .build();

            WebClient.RequestBodySpec requestSpec = webClient.method(
                    org.springframework.http.HttpMethod.valueOf(method.toUpperCase())).uri("");

            if (headers != null && headers.isObject()) {
                headers.fields()
                        .forEachRemaining(entry -> requestSpec.header(entry.getKey(), entry.getValue().asText()));
            }

            Mono<String> responseMono;
            if (body != null && !body.isNull()) {
                responseMono = requestSpec
                        .bodyValue(body.toString())
                        .retrieve()
                        .bodyToMono(String.class);
            } else {
                responseMono = requestSpec
                        .retrieve()
                        .bodyToMono(String.class);
            }

            String response = responseMono
                    .timeout(Duration.ofSeconds(timeout))
                    .block();

            return String.format("HTTP %s request to %s completed. Response: %s",
                    method, url, response != null ? response.substring(0, Math.min(200, response.length())) : "empty");
        } catch (Exception e) {
            log.error("HTTP request execution failed for job {}", job.getId(), e);
            throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    private String executeShellScript(Job job) throws Exception {
        log.info("Executing shell script job: {}", job.getName());

        try {
            JsonNode jobData = objectMapper.readTree(job.getJobData() != null ? job.getJobData() : "{}");
            String script = jobData.has("script") ? jobData.get("script").asText() : null;

            if (script == null || script.isEmpty()) {
                throw new IllegalArgumentException("Script is required for SHELL_SCRIPT job type");
            }

            ProcessBuilder processBuilder = new ProcessBuilder();
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                processBuilder.command("cmd.exe", "/c", script);
            } else {
                processBuilder.command("sh", "-c", script);
            }

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Script execution failed with exit code: " + exitCode);
            }

            return "Shell script executed successfully. Output: " + output.toString();
        } catch (Exception e) {
            log.error("Shell script execution failed for job {}", job.getId(), e);
            throw new RuntimeException("Shell script execution failed: " + e.getMessage(), e);
        }
    }

    private String executeJavaClass(Job job) throws Exception {
        log.info("Executing Java class job: {}", job.getName());

        // For Java class execution, you would typically:
        // 1. Load the class dynamically using ClassLoader
        // 2. Instantiate it
        // 3. Call a specific method
        // This is a simplified version

        JsonNode jobData = objectMapper.readTree(job.getJobData() != null ? job.getJobData() : "{}");
        String className = jobData.has("className") ? jobData.get("className").asText() : null;

        if (className == null || className.isEmpty()) {
            throw new IllegalArgumentException("ClassName is required for JAVA_CLASS job type");
        }

        // In production, implement dynamic class loading with proper security
        return String.format("Java class %s execution completed (placeholder implementation)", className);
    }

    private String executePythonScript(Job job) throws Exception {
        log.info("Executing Python script job: {}", job.getName());

        try {
            JsonNode jobData = objectMapper.readTree(job.getJobData() != null ? job.getJobData() : "{}");
            String script = jobData.has("script") ? jobData.get("script").asText() : null;
            String scriptPath = jobData.has("scriptPath") ? jobData.get("scriptPath").asText() : null;

            if ((script == null || script.isEmpty()) && (scriptPath == null || scriptPath.isEmpty())) {
                throw new IllegalArgumentException("Script or scriptPath is required for PYTHON_SCRIPT job type");
            }

            ProcessBuilder processBuilder = new ProcessBuilder();

            if (scriptPath != null && !scriptPath.isEmpty()) {
                processBuilder.command("python", scriptPath);
            } else {
                // Execute inline script
                processBuilder.command("python", "-c", script);
            }

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Python script execution failed with exit code: " + exitCode);
            }

            return "Python script executed successfully. Output: " + output.toString();
        } catch (Exception e) {
            log.error("Python script execution failed for job {}", job.getId(), e);
            throw new RuntimeException("Python script execution failed: " + e.getMessage(), e);
        }
    }

    private String executeCustomJob(Job job) throws Exception {
        log.info("Executing custom job: {}", job.getName());

        // Custom job execution - can be extended based on specific requirements
        JsonNode jobData = objectMapper.readTree(job.getJobData() != null ? job.getJobData() : "{}");

        // Placeholder for custom execution logic
        return "Custom job executed successfully. Job data: " + jobData.toString();
    }
}
