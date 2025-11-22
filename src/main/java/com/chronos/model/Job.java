package com.chronos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs", indexes = {
    @Index(name = "idx_owner", columnList = "owner"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_next_run", columnList = "nextRunTime")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String schedule; // Cron expression or ISO datetime

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRecurring = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(nullable = false)
    @Builder.Default
    private Integer currentRetries = 0;

    @Column(columnDefinition = "TEXT")
    private String jobData; // JSON payload for job execution

    @Column(columnDefinition = "TEXT")
    private String config; // Additional configuration

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column
    private LocalDateTime nextRunTime;

    @Column
    private LocalDateTime lastRunTime;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<JobRun> runs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

