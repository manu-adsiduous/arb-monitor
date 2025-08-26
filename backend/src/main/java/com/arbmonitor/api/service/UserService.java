package com.arbmonitor.api.service;

import com.arbmonitor.api.dto.UserSettingsDTO;
import com.arbmonitor.api.model.User;
import com.arbmonitor.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Create a new user
     */
    public User createUser(String email, String name, String cognitoSub) {
        logger.info("Creating new user with email: {}", email);
        
        // Check if user already exists
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }
        
        if (cognitoSub != null && userRepository.existsByCognitoSub(cognitoSub)) {
            throw new IllegalArgumentException("User with Cognito sub " + cognitoSub + " already exists");
        }
        
        User user = new User(email, name, cognitoSub);
        return userRepository.save(user);
    }
    
    /**
     * Find user by ID
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * Find user by email
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * Find user by Cognito subject
     */
    @Transactional(readOnly = true)
    public Optional<User> findByCognitoSub(String cognitoSub) {
        return userRepository.findByCognitoSub(cognitoSub);
    }
    
    /**
     * Find user with domains
     */
    @Transactional(readOnly = true)
    public Optional<User> findByIdWithDomains(Long userId) {
        return userRepository.findByIdWithDomains(userId);
    }
    
    /**
     * Update user profile
     */
    public User updateUser(Long userId, String name) {
        logger.info("Updating user profile for ID: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        user.setName(name);
        return userRepository.save(user);
    }
    
    /**
     * Update user Meta API key
     */
    public User updateMetaApiKey(Long userId, String metaApiKey) {
        logger.info("Updating Meta API key for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        user.setMetaApiKey(metaApiKey);
        user.setApiKeyValid(false); // Reset validation status
        user.setApiKeyLastVerified(null);
        
        return userRepository.save(user);
    }
    
    /**
     * Validate and update Meta API key status
     */
    public User validateApiKey(Long userId, boolean isValid) {
        logger.info("Validating API key for user ID: {}, valid: {}", userId, isValid);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        user.setApiKeyValid(isValid);
        user.setApiKeyLastVerified(LocalDateTime.now());
        
        return userRepository.save(user);
    }
    
    /**
     * Get user settings as DTO
     */
    @Transactional(readOnly = true)
    public UserSettingsDTO getUserSettings(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        UserSettingsDTO settings = new UserSettingsDTO();
        settings.setUserId(user.getId());
        settings.setEmail(user.getEmail());
        settings.setName(user.getName());
        settings.setMetaApiKey(user.getMetaApiKey());
        settings.setApiKeyValid(user.getApiKeyValid());
        settings.setApiKeyLastVerified(user.getApiKeyLastVerified());
        settings.setSubscriptionStatus(user.getSubscriptionStatus().name());
        
        return settings;
    }
    
    /**
     * Update user settings from DTO
     */
    public User updateUserSettings(Long userId, UserSettingsDTO settingsDTO) {
        logger.info("Updating user settings for ID: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        // Update basic profile info
        if (settingsDTO.getName() != null) {
            user.setName(settingsDTO.getName());
        }
        
        // Update Meta API key if provided
        if (settingsDTO.getMetaApiKey() != null) {
            user.setMetaApiKey(settingsDTO.getMetaApiKey());
            user.setApiKeyValid(false); // Reset validation
            user.setApiKeyLastVerified(null);
        }
        
        return userRepository.save(user);
    }
    
    /**
     * Update subscription status
     */
    public User updateSubscriptionStatus(Long userId, User.SubscriptionStatus status) {
        logger.info("Updating subscription status for user ID: {} to {}", userId, status);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        user.setSubscriptionStatus(status);
        return userRepository.save(user);
    }
    
    /**
     * Get all users with valid API keys
     */
    @Transactional(readOnly = true)
    public List<User> getUsersWithValidApiKeys() {
        return userRepository.findUsersWithValidApiKeys();
    }
    
    /**
     * Delete user (soft delete by deactivating)
     */
    public void deactivateUser(Long userId) {
        logger.info("Deactivating user with ID: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        user.setSubscriptionStatus(User.SubscriptionStatus.INACTIVE);
        userRepository.save(user);
    }
    
    /**
     * Check if user exists by email
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    /**
     * Get or create user (for authentication)
     */
    public User getOrCreateUser(String email, String name, String cognitoSub) {
        logger.info("Getting or creating user with email: {}", email);
        
        // First try to find by cognito sub
        if (cognitoSub != null) {
            Optional<User> existingUser = userRepository.findByCognitoSub(cognitoSub);
            if (existingUser.isPresent()) {
                return existingUser.get();
            }
        }
        
        // Then try to find by email
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Update cognito sub if it was null
            if (user.getCognitoSub() == null && cognitoSub != null) {
                user.setCognitoSub(cognitoSub);
                return userRepository.save(user);
            }
            return user;
        }
        
        // Create new user
        return createUser(email, name, cognitoSub);
    }
}

