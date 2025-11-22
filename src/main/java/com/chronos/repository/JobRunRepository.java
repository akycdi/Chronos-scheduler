package com.chronos.repository;

import com.chronos.model.Job;
import com.chronos.model.JobRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRunRepository extends JpaRepository<JobRun, Long> {
    List<JobRun> findByJobOrderByCreatedAtDesc(Job job, Pageable pageable);

    Page<JobRun> findByJob(Job job, Pageable pageable);
}

