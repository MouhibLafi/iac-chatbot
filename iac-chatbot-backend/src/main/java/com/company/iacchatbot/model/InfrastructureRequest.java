package com.company.iacchatbot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité représentant une demande d'infrastructure
 */
@Entity
@Table(name = "infrastructure_requests")
public class InfrastructureRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String userMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType resourceType;

    @Column(columnDefinition = "TEXT")
    private String extractedParams;

    @Column(columnDefinition = "TEXT")
    private String generatedCode;

    @Column(length = 50)
    private String targetPlatform; // terraform, openshift

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime processedAt;

    @Column(length = 20)
    private String status; // pending, processing, completed, failed, PENDING_APPROVAL, CODE_GENERATED, RUNNING, SUCCESS, CANCELLED

    /**
     * Utilisateur propriétaire de la demande (nullable pour compatibilité)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    @JsonIgnore
    private User user;

    /**
     * Date de fin du déploiement (succès, échec ou annulation)
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Message d'erreur en cas d'échec ou de rejet
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Logs de déploiement associés (traçabilité UC-10)
     */
    @OneToMany(mappedBy = "deploymentRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<DeploymentLog> logs = new ArrayList<>();

    // Constructors
    public InfrastructureRequest() {
        this.createdAt = LocalDateTime.now();
        this.status = "pending";
    }

    public InfrastructureRequest(String userMessage) {
        this();
        this.userMessage = userMessage;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getExtractedParams() {
        return extractedParams;
    }

    public void setExtractedParams(String extractedParams) {
        this.extractedParams = extractedParams;
    }

    public String getGeneratedCode() {
        return generatedCode;
    }

    public void setGeneratedCode(String generatedCode) {
        this.generatedCode = generatedCode;
    }

    public String getTargetPlatform() {
        return targetPlatform;
    }

    public void setTargetPlatform(String targetPlatform) {
        this.targetPlatform = targetPlatform;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<DeploymentLog> getLogs() {
        return logs;
    }

    public void setLogs(List<DeploymentLog> logs) {
        this.logs = logs;
    }

    @Override
    public String toString() {
        return "InfrastructureRequest{" +
                "id=" + id +
                ", resourceType=" + resourceType +
                ", status='" + status + '\'' +
                ", targetPlatform='" + targetPlatform + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
