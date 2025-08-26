package com.arbmonitor.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    @Email
    @NotBlank
    private String email;
    
    @Column(nullable = false)
    @NotBlank
    @Size(min = 2, max = 100)
    private String name;
    
    @Column(name = "cognito_sub", unique = true)
    private String cognitoSub;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.ACTIVE;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;
    
    @Column(name = "meta_api_key", columnDefinition = "TEXT")
    private String metaApiKey; // Encrypted Meta Ad Library API access token
    
    @Column(name = "api_key_valid")
    private Boolean apiKeyValid = false;
    
    @Column(name = "api_key_last_verified")
    private LocalDateTime apiKeyLastVerified;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Domain> domains = new HashSet<>();
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // Constructors
    public User() {}
    
    public User(String email, String name, String cognitoSub) {
        this.email = email;
        this.name = name;
        this.cognitoSub = cognitoSub;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public String getCognitoSub() {
        return cognitoSub;
    }
    
    public void setCognitoSub(String cognitoSub) {
        this.cognitoSub = cognitoSub;
    }
    
    public SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }
    
    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }
    
    public Set<Domain> getDomains() {
        return domains;
    }
    
    public void setDomains(Set<Domain> domains) {
        this.domains = domains;
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
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
    }
    
    public enum SubscriptionStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }
    
    public enum UserRole {
        USER, ADMIN
    }
}
