package com.arbmonitor.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * Entity to track Apify API usage and costs
 */
@Entity
@Table(name = "apify_usage")
public class ApifyUsage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "request_timestamp", nullable = false)
    private LocalDateTime requestTimestamp;
    
    @Column(name = "actor_id", nullable = false)
    private String actorId; // Apify actor/scraper ID
    
    @Column(name = "run_id")
    private String runId; // Apify run ID
    
    @Column(name = "request_type", nullable = false)
    private String requestType; // "ad_scraping", "domain_analysis", etc.
    
    @Column(name = "compute_units")
    private BigDecimal computeUnits; // Apify compute units consumed
    
    @Column(name = "estimated_cost", precision = 10, scale = 6)
    private BigDecimal estimatedCost; // Cost in USD
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "domain_name")
    private String domainName;
    
    @Column(name = "ads_scraped")
    private Integer adsScraped; // Number of ads scraped in this run
    
    @Column(name = "request_duration_ms")
    private Long requestDurationMs;
    
    @Column(name = "success")
    private Boolean success;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "data_size_mb")
    private BigDecimal dataSizeMb; // Size of scraped data
    
    @Column(name = "memory_mb")
    private Integer memoryMb; // Memory used
    
    @Column(name = "cpu_seconds")
    private BigDecimal cpuSeconds; // CPU time used
    
    // Constructors
    public ApifyUsage() {}
    
    public ApifyUsage(String actorId, String requestType, Long userId, String domainName) {
        this.actorId = actorId;
        this.requestType = requestType;
        this.userId = userId;
        this.domainName = domainName;
        this.requestTimestamp = LocalDateTime.now();
        this.success = true;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDateTime getRequestTimestamp() {
        return requestTimestamp;
    }
    
    public void setRequestTimestamp(LocalDateTime requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }
    
    public String getActorId() {
        return actorId;
    }
    
    public void setActorId(String actorId) {
        this.actorId = actorId;
    }
    
    public String getRunId() {
        return runId;
    }
    
    public void setRunId(String runId) {
        this.runId = runId;
    }
    
    public String getRequestType() {
        return requestType;
    }
    
    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }
    
    public BigDecimal getComputeUnits() {
        return computeUnits;
    }
    
    public void setComputeUnits(BigDecimal computeUnits) {
        this.computeUnits = computeUnits;
    }
    
    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }
    
    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getDomainName() {
        return domainName;
    }
    
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }
    
    public Integer getAdsScraped() {
        return adsScraped;
    }
    
    public void setAdsScraped(Integer adsScraped) {
        this.adsScraped = adsScraped;
    }
    
    public Long getRequestDurationMs() {
        return requestDurationMs;
    }
    
    public void setRequestDurationMs(Long requestDurationMs) {
        this.requestDurationMs = requestDurationMs;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public BigDecimal getDataSizeMb() {
        return dataSizeMb;
    }
    
    public void setDataSizeMb(BigDecimal dataSizeMb) {
        this.dataSizeMb = dataSizeMb;
    }
    
    public Integer getMemoryMb() {
        return memoryMb;
    }
    
    public void setMemoryMb(Integer memoryMb) {
        this.memoryMb = memoryMb;
    }
    
    public BigDecimal getCpuSeconds() {
        return cpuSeconds;
    }
    
    public void setCpuSeconds(BigDecimal cpuSeconds) {
        this.cpuSeconds = cpuSeconds;
    }
}
