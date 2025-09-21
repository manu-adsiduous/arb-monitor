package com.arbmonitor.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * Entity to track OpenAI API usage and costs
 */
@Entity
@Table(name = "openai_usage")
public class OpenAIUsage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "request_timestamp", nullable = false)
    private LocalDateTime requestTimestamp;
    
    @Column(name = "model_name", nullable = false)
    private String modelName;
    
    @Column(name = "request_type", nullable = false)
    private String requestType; // "compliance_analysis", "visual_description", "landing_page_analysis", etc.
    
    @Column(name = "prompt_tokens")
    private Integer promptTokens;
    
    @Column(name = "completion_tokens")
    private Integer completionTokens;
    
    @Column(name = "total_tokens")
    private Integer totalTokens;
    
    @Column(name = "estimated_cost", precision = 10, scale = 6)
    private BigDecimal estimatedCost; // Cost in USD
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "domain_name")
    private String domainName;
    
    @Column(name = "meta_ad_id")
    private String metaAdId;
    
    @Column(name = "request_duration_ms")
    private Long requestDurationMs;
    
    @Column(name = "success")
    private Boolean success;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "response_size_chars")
    private Integer responseSizeChars;
    
    // Constructors
    public OpenAIUsage() {}
    
    public OpenAIUsage(String modelName, String requestType, Long userId) {
        this.modelName = modelName;
        this.requestType = requestType;
        this.userId = userId;
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
    
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public String getRequestType() {
        return requestType;
    }
    
    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }
    
    public Integer getPromptTokens() {
        return promptTokens;
    }
    
    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }
    
    public Integer getCompletionTokens() {
        return completionTokens;
    }
    
    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }
    
    public Integer getTotalTokens() {
        return totalTokens;
    }
    
    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
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
    
    public String getMetaAdId() {
        return metaAdId;
    }
    
    public void setMetaAdId(String metaAdId) {
        this.metaAdId = metaAdId;
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
    
    public Integer getResponseSizeChars() {
        return responseSizeChars;
    }
    
    public void setResponseSizeChars(Integer responseSizeChars) {
        this.responseSizeChars = responseSizeChars;
    }
}
