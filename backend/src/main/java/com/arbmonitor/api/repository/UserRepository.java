package com.arbmonitor.api.repository;

import com.arbmonitor.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by email address
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Find user by Cognito subject identifier
     */
    Optional<User> findByCognitoSub(String cognitoSub);
    
    /**
     * Check if user exists by email
     */
    boolean existsByEmail(String email);
    
    /**
     * Check if user exists by Cognito subject
     */
    boolean existsByCognitoSub(String cognitoSub);
    
    /**
     * Find users with valid Meta API keys
     */
    @Query("SELECT u FROM User u WHERE u.apiKeyValid = true")
    java.util.List<User> findUsersWithValidApiKeys();
    
    /**
     * Find user with domains (fetch join to avoid N+1 problem)
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.domains WHERE u.id = :userId")
    Optional<User> findByIdWithDomains(@Param("userId") Long userId);
    
    /**
     * Find user by email with domains
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.domains WHERE u.email = :email")
    Optional<User> findByEmailWithDomains(@Param("email") String email);
    
    /**
     * Count users by role
     */
    long countByRole(User.UserRole role);
}

