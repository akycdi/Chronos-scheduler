package com.chronos.dto;

import com.chronos.model.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRunResponse {
    private Long id;
    private Long jobId;
    private JobStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String output;
    private String error;
    private Integer retryAttempt;
    private String workerId;
}

