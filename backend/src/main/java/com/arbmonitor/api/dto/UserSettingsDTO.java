package com.arbmonitor.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class UserSettingsDTO {
    
    private Long userId;
    private String email;
    private String name;
    private String subscriptionStatus;
    
    @NotBlank(message = "Meta API key is required")
    @Size(min = 20, message = "Meta API key must be at least 20 characters")
    private String metaApiKey;
    
    private Boolean apiKeyValid;
    private LocalDateTime apiKeyLastVerified;
    
    private Boolean emailNotifications = true;
    private Boolean weeklyReports = true;
    private Boolean criticalAlerts = true;
    
    // Constructors
    public UserSettingsDTO() {}
    
    public UserSettingsDTO(String metaApiKey, Boolean apiKeyValid, LocalDateTime apiKeyLastVerified) {
        this.metaApiKey = metaApiKey;
        this.apiKeyValid = apiKeyValid;
        this.apiKeyLastVerified = apiKeyLastVerified;
    }
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }
    
    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }
    
    public String getMetaApiKey() {
        return metaApiKey;
    }
    
    public void setMetaApiKey(String metaApiKey) {
        this.metaApiKey = metaApiKey;
    }
    
    public Boolean getApiKeyValid() {
        return apiKeyValid;
    }
    
    public void setApiKeyValid(Boolean apiKeyValid) {
        this.apiKeyValid = apiKeyValid;
    }
    
    public LocalDateTime getApiKeyLastVerified() {
        return apiKeyLastVerified;
    }
    
    public void setApiKeyLastVerified(LocalDateTime apiKeyLastVerified) {
        this.apiKeyLastVerified = apiKeyLastVerified;
    }
    
    public Boolean getEmailNotifications() {
        return emailNotifications;
    }
    
    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }
    
    public Boolean getWeeklyReports() {
        return weeklyReports;
    }
    
    public void setWeeklyReports(Boolean weeklyReports) {
        this.weeklyReports = weeklyReports;
    }
    
    public Boolean getCriticalAlerts() {
        return criticalAlerts;
    }
    
    public void setCriticalAlerts(Boolean criticalAlerts) {
        this.criticalAlerts = criticalAlerts;
    }
}
