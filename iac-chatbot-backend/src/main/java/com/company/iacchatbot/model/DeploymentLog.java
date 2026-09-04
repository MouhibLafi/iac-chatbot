package com.company.iacchatbot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entité représentant une étape de log d'un déploiement (traçabilité UC-10)
 */
@Entity
@Table(name = "deployment_logs")
public class DeploymentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deployment_request_id", nullable = false)
    @JsonIgnore
    private InfrastructureRequest deploymentRequest;

    @Column(nullable = false, length = 50)
    private String step; // VALIDATION, PLAN, APPLY, VERIFY, CANCEL, APPROVAL...

    @Enumerated(EnumType.STRING)
    @Column(name = "log_level", nullable = false, length = 10)
    private LogLevel logLevel = LogLevel.INFO;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    // Constructors
    public DeploymentLog() {
        this.timestamp = LocalDateTime.now();
    }

    public DeploymentLog(InfrastructureRequest deploymentRequest, String step, LogLevel logLevel, String message) {
        this();
        this.deploymentRequest = deploymentRequest;
        this.step = step;
        this.logLevel = logLevel;
        this.message = message;
    }

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InfrastructureRequest getDeploymentRequest() {
        return deploymentRequest;
    }

    public void setDeploymentRequest(InfrastructureRequest deploymentRequest) {
        this.deploymentRequest = deploymentRequest;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "DeploymentLog{" +
                "id=" + id +
                ", step='" + step + '\'' +
                ", logLevel=" + logLevel +
                ", timestamp=" + timestamp +
                '}';
    }
}
