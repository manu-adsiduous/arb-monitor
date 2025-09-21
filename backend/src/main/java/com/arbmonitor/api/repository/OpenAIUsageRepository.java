package com.arbmonitor.api.repository;

import com.arbmonitor.api.model.OpenAIUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OpenAIUsageRepository extends JpaRepository<OpenAIUsage, Long> {
    
    /**
     * Find usage records by user ID, ordered by timestamp descending
     */
    Page<OpenAIUsage> findByUserIdOrderByRequestTimestampDesc(Long userId, Pageable pageable);
    
    /**
     * Find usage records by user ID and date range
     */
    @Query("SELECT u FROM OpenAIUsage u WHERE u.userId = :userId AND u.requestTimestamp BETWEEN :startDate AND :endDate ORDER BY u.requestTimestamp DESC")
    List<OpenAIUsage> findByUserIdAndDateRange(@Param("userId") Long userId, 
                                               @Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
    
    /**
     * Calculate total cost for a user
     */
    @Query("SELECT COALESCE(SUM(u.estimatedCost), 0) FROM OpenAIUsage u WHERE u.userId = :userId")
    BigDecimal getTotalCostByUserId(@Param("userId") Long userId);
    
    /**
     * Calculate total cost for a user within date range
     */
    @Query("SELECT COALESCE(SUM(u.estimatedCost), 0) FROM OpenAIUsage u WHERE u.userId = :userId AND u.requestTimestamp BETWEEN :startDate AND :endDate")
    BigDecimal getTotalCostByUserIdAndDateRange(@Param("userId") Long userId, 
                                                @Param("startDate") LocalDateTime startDate, 
                                                @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get cost breakdown by request type for a user
     */
    @Query("SELECT u.requestType, COALESCE(SUM(u.estimatedCost), 0) FROM OpenAIUsage u WHERE u.userId = :userId GROUP BY u.requestType")
    List<Object[]> getCostBreakdownByRequestType(@Param("userId") Long userId);
    
    /**
     * Get cost breakdown by model for a user
     */
    @Query("SELECT u.modelName, COALESCE(SUM(u.estimatedCost), 0) FROM OpenAIUsage u WHERE u.userId = :userId GROUP BY u.modelName")
    List<Object[]> getCostBreakdownByModel(@Param("userId") Long userId);
    
    /**
     * Get daily cost summary for the last 30 days
     */
    @Query("SELECT CAST(u.requestTimestamp AS date), COALESCE(SUM(u.estimatedCost), 0) FROM OpenAIUsage u WHERE u.userId = :userId AND u.requestTimestamp >= :since GROUP BY CAST(u.requestTimestamp AS date) ORDER BY CAST(u.requestTimestamp AS date)")
    List<Object[]> getDailyCostSummary(@Param("userId") Long userId, @Param("since") LocalDateTime since);
    
    /**
     * Count total requests for a user
     */
    @Query("SELECT COUNT(u) FROM OpenAIUsage u WHERE u.userId = :userId")
    Long getTotalRequestsByUserId(@Param("userId") Long userId);
    
    /**
     * Count successful requests for a user
     */
    @Query("SELECT COUNT(u) FROM OpenAIUsage u WHERE u.userId = :userId AND u.success = true")
    Long getSuccessfulRequestsByUserId(@Param("userId") Long userId);
    
    /**
     * Get total tokens used by a user
     */
    @Query("SELECT COALESCE(SUM(u.totalTokens), 0) FROM OpenAIUsage u WHERE u.userId = :userId")
    Long getTotalTokensByUserId(@Param("userId") Long userId);
}
