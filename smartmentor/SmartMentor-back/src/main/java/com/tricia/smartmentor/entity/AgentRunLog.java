package com.tricia.smartmentor.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "agent_run_log")
public class AgentRunLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_name", length = 80, nullable = false)
    private String agentName;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "diagnostic_id", length = 100)
    private String diagnosticId;

    @Column(length = 100)
    private String module;

    @Column(name = "prompt_hash", length = 64)
    private String promptHash;

    @Column(name = "prompt_version", length = 80)
    private String promptVersion;

    @Column(name = "prompt_length")
    private Integer promptLength;

    @Column(name = "response_length")
    private Integer responseLength;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "success")
    private Boolean success;

    @Column(name = "fallback_used")
    private Boolean fallbackUsed;

    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(name = "event", length = 80)
    private String event;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "input_summary", length = 1000)
    private String inputSummary;

    @Column(name = "output_summary", length = 1000)
    private String outputSummary;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
