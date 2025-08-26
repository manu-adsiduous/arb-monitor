package com.arbmonitor.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "domains")
@EntityListeners(AuditingEntityListener.class)
public class Domain {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Invalid domain format")
    private String domainName;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
    
    @Column(name = "compliance_score")
    private Double complianceScore;
    
    @Column(name = "last_checked")
    private LocalDateTime lastChecked;
    
    @Column(name = "share_token", unique = true)
    private String shareToken;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonitoringStatus status = MonitoringStatus.ACTIVE;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;
    
    @Column(name = "processing_message", columnDefinition = "TEXT")
    private String processingMessage;
    
    @Column(name = "rac_parameter")
    private String racParameter;
    
    @OneToMany(mappedBy = "domain", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<AdAnalysis> adAnalyses = new HashSet<>();
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // Constructors
    public Domain() {
        this.shareToken = UUID.randomUUID().toString();
    }
    
    public Domain(String domainName, User user) {
        this();
        this.domainName = domainName;
        this.user = user;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getDomainName() {
        return domainName;
    }
    
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Double getComplianceScore() {
        return complianceScore;
    }
    
    public void setComplianceScore(Double complianceScore) {
        this.complianceScore = complianceScore;
    }
    
    public LocalDateTime getLastChecked() {
        return lastChecked;
    }
    
    public void setLastChecked(LocalDateTime lastChecked) {
        this.lastChecked = lastChecked;
    }
    
    public String getShareToken() {
        return shareToken;
    }
    
    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }
    
    public MonitoringStatus getStatus() {
        return status;
    }
    
    public void setStatus(MonitoringStatus status) {
        this.status = status;
    }
    
    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }
    
    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }
    
    public String getProcessingMessage() {
        return processingMessage;
    }
    
    public void setProcessingMessage(String processingMessage) {
        this.processingMessage = processingMessage;
    }
    
    public String getRacParameter() {
        return racParameter;
    }
    
    public void setRacParameter(String racParameter) {
        this.racParameter = racParameter;
    }
    
    public Set<AdAnalysis> getAdAnalyses() {
        return adAnalyses;
    }
    
    public void setAdAnalyses(Set<AdAnalysis> adAnalyses) {
        this.adAnalyses = adAnalyses;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public enum MonitoringStatus {
        ACTIVE, PAUSED, INACTIVE
    }
    
    public enum ProcessingStatus {
        PENDING,           // Just created, waiting to start
        FETCHING_ADS,      // Currently scraping ads from Facebook
        PAUSED,            // Scraping paused by user
        SCANNING_COMPLIANCE, // Analyzing ads for compliance violations
        COMPLETED,         // All processing finished successfully
        FAILED            // Processing failed with error
    }
}
