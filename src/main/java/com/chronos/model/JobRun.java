package com.chronos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_runs", indexes = {
    @Index(name = "idx_job_id", columnList = "job_id"),
    @Index(name = "idx_run_status", columnList = "status"),
    @Index(name = "idx_started_at", columnList = "startedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String output; // Job execution output

    @Column(columnDefinition = "TEXT")
    private String error; // Error message if failed

    @Column
    private Integer retryAttempt;

    @Column
    private String workerId; // Identifier of the worker that executed this run

    @Column
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

