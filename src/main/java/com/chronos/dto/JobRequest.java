package com.chronos.dto;

import com.chronos.model.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobRequest {
    @NotBlank(message = "Job name is required")
    private String name;

    @NotBlank(message = "Owner is required")
    private String owner;

    @NotNull(message = "Job type is required")
    private JobType type;

    private String description;

    private String schedule; // Cron expression for recurring jobs, or ISO datetime for one-time

    private Boolean isRecurring = false;

    private Integer maxRetries = 3;

    private String jobData; // JSON payload

    private String config; // Additional configuration JSON
}

