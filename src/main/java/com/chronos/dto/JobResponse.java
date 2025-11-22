package com.chronos.dto;

import com.chronos.model.JobStatus;
import com.chronos.model.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {
    private Long id;
    private String name;
    private String owner;
    private JobType type;
    private JobStatus status;
    private String description;
    private String schedule;
    private Boolean isRecurring;
    private Integer maxRetries;
    private Integer currentRetries;
    private String jobData;
    private String config;
    private Integer version;
    private LocalDateTime nextRunTime;
    private LocalDateTime lastRunTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<JobRunResponse> recentRuns;
}

