package com.chronos.repository;

import com.chronos.model.Job;
import com.chronos.model.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    Page<Job> findByOwner(String owner, Pageable pageable);

    List<Job> findByStatusAndNextRunTimeLessThanEqual(JobStatus status, LocalDateTime time);

    @Query("SELECT j FROM Job j WHERE j.status = :status AND j.nextRunTime <= :time AND j.isRecurring = true")
    List<Job> findRecurringJobsToExecute(@Param("status") JobStatus status, @Param("time") LocalDateTime time);

    @Query("SELECT j FROM Job j WHERE j.status = :status AND j.nextRunTime <= :time AND j.isRecurring = false")
    List<Job> findOneTimeJobsToExecute(@Param("status") JobStatus status, @Param("time") LocalDateTime time);

    long countByStatus(JobStatus status);
}

