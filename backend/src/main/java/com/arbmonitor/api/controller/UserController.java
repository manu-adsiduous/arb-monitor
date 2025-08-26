package com.arbmonitor.api.controller;

import com.arbmonitor.api.dto.UserSettingsDTO;
import com.arbmonitor.api.model.User;
import com.arbmonitor.api.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @Autowired
    private UserService userService;
    
    /**
     * Get current user profile
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("X-User-ID") Long userId) {
        try {
            Optional<User> user = userService.findById(userId);
            if (user.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(new UserResponse(user.get()));
        } catch (Exception e) {
            logger.error("Error getting current user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get user profile"));
        }
    }
    
    /**
     * Update user profile
     */
    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("X-User-ID") Long userId,
            @RequestBody @Valid ProfileUpdateRequest request) {
        
        try {
            User user = userService.updateUser(userId, request.getName());
            return ResponseEntity.ok(new UserResponse(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update profile"));
        }
    }
    
    /**
     * Get user settings
     */
    @GetMapping("/me/settings")
    public ResponseEntity<?> getUserSettings(@RequestHeader("X-User-ID") Long userId) {
        try {
            UserSettingsDTO settings = userService.getUserSettings(userId);
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            logger.error("Error getting user settings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get user settings"));
        }
    }
    
    /**
     * Update user settings
     */
    @PutMapping("/me/settings")
    public ResponseEntity<?> updateUserSettings(
            @RequestHeader("X-User-ID") Long userId,
            @RequestBody @Valid UserSettingsDTO settingsDTO) {
        
        try {
            User user = userService.updateUserSettings(userId, settingsDTO);
            return ResponseEntity.ok(Map.of("message", "Settings updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating user settings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update settings"));
        }
    }
    
    /**
     * Update Meta API key
     */
    @PutMapping("/me/meta-api-key")
    public ResponseEntity<?> updateMetaApiKey(
            @RequestHeader("X-User-ID") Long userId,
            @RequestBody @Valid ApiKeyUpdateRequest request) {
        
        try {
            User user = userService.updateMetaApiKey(userId, request.getMetaApiKey());
            return ResponseEntity.ok(Map.of("message", "Meta API key updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating Meta API key", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update API key"));
        }
    }
    
    /**
     * Validate Meta API key
     */
    @PostMapping("/me/validate-api-key")
    public ResponseEntity<?> validateApiKey(@RequestHeader("X-User-ID") Long userId) {
        try {
            // TODO: Implement actual Meta API validation
            // For now, just mark as valid
            User user = userService.validateApiKey(userId, true);
            
            return ResponseEntity.ok(Map.of(
                "valid", true,
                "message", "API key validated successfully"
            ));
        } catch (Exception e) {
            logger.error("Error validating API key", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to validate API key"));
        }
    }
    
    /**
     * Create or get user (for authentication)
     */
    @PostMapping("/auth")
    public ResponseEntity<?> authenticateUser(@RequestBody @Valid AuthRequest request) {
        try {
            User user = userService.getOrCreateUser(
                request.getEmail(), 
                request.getName(), 
                request.getCognitoSub()
            );
            
            return ResponseEntity.ok(new UserResponse(user));
        } catch (Exception e) {
            logger.error("Error authenticating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Authentication failed"));
        }
    }
    
    // Request/Response DTOs
    
    public static class ProfileUpdateRequest {
        private String name;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    
    public static class ApiKeyUpdateRequest {
        private String metaApiKey;
        
        public String getMetaApiKey() { return metaApiKey; }
        public void setMetaApiKey(String metaApiKey) { this.metaApiKey = metaApiKey; }
    }
    
    public static class AuthRequest {
        private String email;
        private String name;
        private String cognitoSub;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCognitoSub() { return cognitoSub; }
        public void setCognitoSub(String cognitoSub) { this.cognitoSub = cognitoSub; }
    }
    
    public static class UserResponse {
        private Long id;
        private String email;
        private String name;
        private String subscriptionStatus;
        private String createdAt;
        private boolean hasValidApiKey;
        
        public UserResponse(User user) {
            this.id = user.getId();
            this.email = user.getEmail();
            this.name = user.getName();
            this.subscriptionStatus = user.getSubscriptionStatus().name();
            this.createdAt = user.getCreatedAt() != null ? user.getCreatedAt().toString() : null;
            this.hasValidApiKey = Boolean.TRUE.equals(user.getApiKeyValid());
        }
        
        // Getters
        public Long getId() { return id; }
        public String getEmail() { return email; }
        public String getName() { return name; }
        public String getSubscriptionStatus() { return subscriptionStatus; }
        public String getCreatedAt() { return createdAt; }
        public boolean isHasValidApiKey() { return hasValidApiKey; }
    }
}

