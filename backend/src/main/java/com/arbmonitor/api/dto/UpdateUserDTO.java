package com.arbmonitor.api.dto;

import com.arbmonitor.api.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UpdateUserDTO {
    
    @Email(message = "Invalid email format")
    private String email;
    
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    private User.SubscriptionStatus subscriptionStatus;
    
    private User.UserRole role;
    
    private String metaApiKey;
    
    // Constructors
    public UpdateUserDTO() {}
    
    // Getters and Setters
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
    
    public User.SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }
    
    public void setSubscriptionStatus(User.SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }
    
    public User.UserRole getRole() {
        return role;
    }
    
    public void setRole(User.UserRole role) {
        this.role = role;
    }
    
    public String getMetaApiKey() {
        return metaApiKey;
    }
    
    public void setMetaApiKey(String metaApiKey) {
        this.metaApiKey = metaApiKey;
    }
}