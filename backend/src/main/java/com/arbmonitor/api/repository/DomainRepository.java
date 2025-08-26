package com.arbmonitor.api.repository;

import com.arbmonitor.api.model.Domain;
import com.arbmonitor.api.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DomainRepository extends JpaRepository<Domain, Long> {
    
    /**
     * Find all domains for a specific user
     */
    List<Domain> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Find domains by user ID
     */
    List<Domain> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Find domain by share token (for public reports)
     */
    Optional<Domain> findByShareToken(String shareToken);
    
    /**
     * Find domain by name and user
     */
    Optional<Domain> findByDomainNameAndUser(String domainName, User user);
    
    /**
     * Find domain by name (for processing status updates)
     */
    Domain findByDomainName(String domainName);
    
    /**
     * Check if domain exists for user
     */
    boolean existsByDomainNameAndUser(String domainName, User user);
    
    /**
     * Find domains with pagination
     */
    Page<Domain> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    /**
     * Search domains by name pattern for a user
     */
    @Query("SELECT d FROM Domain d WHERE d.user = :user AND d.domainName LIKE %:searchTerm% ORDER BY d.createdAt DESC")
    List<Domain> searchByDomainName(@Param("user") User user, @Param("searchTerm") String searchTerm);
    
    /**
     * Find domains that need checking (last checked before given time)
     */
    @Query("SELECT d FROM Domain d WHERE d.status = 'ACTIVE' AND (d.lastChecked IS NULL OR d.lastChecked < :checkTime)")
    List<Domain> findDomainsNeedingCheck(@Param("checkTime") LocalDateTime checkTime);
    
    /**
     * Find domain with ad analyses (fetch join)
     */
    @Query("SELECT d FROM Domain d LEFT JOIN FETCH d.adAnalyses WHERE d.id = :domainId")
    Optional<Domain> findByIdWithAdAnalyses(@Param("domainId") Long domainId);
    
    /**
     * Get domain statistics for a user
     */
    @Query("SELECT COUNT(d), AVG(d.complianceScore), " +
           "SUM(CASE WHEN d.status = 'ACTIVE' THEN 1 ELSE 0 END) " +
           "FROM Domain d WHERE d.user = :user")
    Object[] getDomainStatsByUser(@Param("user") User user);
    
    /**
     * Find domains with low compliance scores
     */
    @Query("SELECT d FROM Domain d WHERE d.user = :user AND d.complianceScore < :threshold ORDER BY d.complianceScore ASC")
    List<Domain> findDomainsWithLowCompliance(@Param("user") User user, @Param("threshold") Double threshold);
    
    /**
     * Count total domains for user
     */
    long countByUser(User user);
    
    /**
     * Count active domains for user
     */
    @Query("SELECT COUNT(d) FROM Domain d WHERE d.user = :user AND d.status = 'ACTIVE'")
    long countActiveByUser(@Param("user") User user);
}

