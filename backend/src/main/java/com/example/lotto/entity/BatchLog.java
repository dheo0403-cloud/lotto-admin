package com.example.lotto.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "batch_log")
@Getter
@Setter
@NoArgsConstructor
public class BatchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_type", nullable = false, length = 100)
    private String jobType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "executed_at", nullable = false, updatable = false)
    private LocalDateTime executedAt = LocalDateTime.now();
    
    // factory method
    public static BatchLog of(String jobType, String status, String message) {
        BatchLog log = new BatchLog();
        log.setJobType(jobType);
        log.setStatus(status);
        log.setMessage(message);
        return log;
    }
}
