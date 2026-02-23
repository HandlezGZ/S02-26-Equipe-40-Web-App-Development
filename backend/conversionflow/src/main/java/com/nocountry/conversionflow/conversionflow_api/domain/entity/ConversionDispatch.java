package com.nocountry.conversionflow.conversionflow_api.domain.entity;

import com.nocountry.conversionflow.conversionflow_api.domain.enums.DispatchStatus;
import com.nocountry.conversionflow.conversionflow_api.domain.enums.Provider;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversion_dispatch")
public class ConversionDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long leadId;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Enumerated(EnumType.STRING)
    private DispatchStatus status;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private int attemptCount;

    private String errorMessage;

    private LocalDateTime lastAttemptAt;

    private LocalDateTime createdAt;

    public ConversionDispatch() {
    }

    public ConversionDispatch(Long leadId, Provider provider, String payload) {
        this.leadId = leadId;
        this.provider = provider;
        this.payload = payload;
        this.status = DispatchStatus.PENDING;
        this.attemptCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    // ===== Business Methods =====

    public void markSuccess() {
        this.status = DispatchStatus.SUCCESS;
        this.lastAttemptAt = LocalDateTime.now();
    }

    public void markFailure(String error) {
        this.status = DispatchStatus.FAILED;
        this.errorMessage = error;
        this.lastAttemptAt = LocalDateTime.now();
        this.attemptCount++;
    }

    public boolean canRetry(int maxAttempts) {
        return this.attemptCount < maxAttempts;
    }

    // ===== Getters and Setters =====

    public Long getId() {
        return id;
    }

    public Long getLeadId() {
        return leadId;
    }

    public void setLeadId(Long leadId) {
        this.leadId = leadId;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public DispatchStatus getStatus() {
        return status;
    }

    public void setStatus(DispatchStatus status) {
        this.status = status;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(LocalDateTime lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}