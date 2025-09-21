package com.arbmonitor.api.service;

import com.arbmonitor.api.model.ApifyUsage;
import com.arbmonitor.api.repository.ApifyUsageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for tracking Apify API costs and usage
 */
@Service
public class ApifyCostTrackingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApifyCostTrackingService.class);
    
    @Autowired
    private ApifyUsageRepository apifyUsageRepository;
    
    // Apify pricing: $0.25 per compute unit (as of September 2024)
    private static final BigDecimal COST_PER_COMPUTE_UNIT = new BigDecimal("0.25");
    
    /**
     * Track an Apify scraping request
     */
    public ApifyUsage trackRequest(String actorId, String requestType, Long userId, String domainName, long startTime) {
        ApifyUsage usage = new ApifyUsage(actorId, requestType, userId, domainName);
        usage.setRequestDurationMs(System.currentTimeMillis() - startTime);
        
        return usage;
    }
    
    /**
     * Update usage record with Apify run data and save to database
     */
    public void updateAndSaveUsage(ApifyUsage usage, JsonNode runData, int adsScraped, boolean success, String errorMessage) {
        try {
            usage.setSuccess(success);
            usage.setErrorMessage(errorMessage);
            usage.setAdsScraped(adsScraped);
            
            if (success && runData != null) {
                // Extract usage stats from Apify run data
                JsonNode stats = runData.get("stats");
                if (stats != null) {
                    // Get compute units (CU) consumed
                    JsonNode computeUnitsNode = stats.get("computeUnits");
                    if (computeUnitsNode != null) {
                        BigDecimal computeUnits = new BigDecimal(computeUnitsNode.asText());
                        usage.setComputeUnits(computeUnits);
                        
                        // Calculate cost: compute_units * $0.25
                        BigDecimal cost = computeUnits.multiply(COST_PER_COMPUTE_UNIT).setScale(6, RoundingMode.HALF_UP);
                        usage.setEstimatedCost(cost);
                    }
                    
                    // Get memory usage
                    JsonNode memoryNode = stats.get("memoryAvgBytes");
                    if (memoryNode != null) {
                        // Convert bytes to MB
                        long memoryBytes = memoryNode.asLong();
                        int memoryMb = (int) (memoryBytes / (1024 * 1024));
                        usage.setMemoryMb(memoryMb);
                    }
                    
                    // Get CPU time
                    JsonNode cpuNode = stats.get("cpuUsage");
                    if (cpuNode != null) {
                        BigDecimal cpuSeconds = new BigDecimal(cpuNode.asText());
                        usage.setCpuSeconds(cpuSeconds);
                    }
                }
                
                // Get run ID
                JsonNode idNode = runData.get("id");
                if (idNode != null) {
                    usage.setRunId(idNode.asText());
                }
                
                // Estimate data size based on number of ads (rough estimate: 50KB per ad)
                if (adsScraped > 0) {
                    BigDecimal estimatedSizeMb = new BigDecimal(adsScraped * 50).divide(new BigDecimal(1024), 2, RoundingMode.HALF_UP);
                    usage.setDataSizeMb(estimatedSizeMb);
                }
                
                logger.debug("Apify request tracked: {} compute units, {} ads scraped, estimated cost: ${}", 
                    usage.getComputeUnits(), adsScraped, usage.getEstimatedCost());
            } else {
                // For failed requests, we might still have partial compute unit usage
                usage.setEstimatedCost(BigDecimal.ZERO);
            }
            
            // Save to database
            apifyUsageRepository.save(usage);
            
        } catch (Exception e) {
            logger.error("Error tracking Apify usage: {}", e.getMessage());
        }
    }
    
    /**
     * Calculate estimated cost based on compute units
     */
    public BigDecimal calculateCost(BigDecimal computeUnits) {
        if (computeUnits == null) {
            return BigDecimal.ZERO;
        }
        return computeUnits.multiply(COST_PER_COMPUTE_UNIT).setScale(6, RoundingMode.HALF_UP);
    }
    
    /**
     * Get cost summary for a user
     */
    public Map<String, Object> getCostSummary(Long userId) {
        Map<String, Object> summary = new HashMap<>();
        
        // Total cost
        BigDecimal totalCost = apifyUsageRepository.getTotalCostByUserId(userId);
        summary.put("totalCost", totalCost);
        
        // Total requests
        Long totalRequests = apifyUsageRepository.getTotalRequestsByUserId(userId);
        summary.put("totalRequests", totalRequests);
        
        // Successful requests
        Long successfulRequests = apifyUsageRepository.getSuccessfulRequestsByUserId(userId);
        summary.put("successfulRequests", successfulRequests);
        
        // Success rate
        double successRate = totalRequests > 0 ? (double) successfulRequests / totalRequests * 100 : 0;
        summary.put("successRate", Math.round(successRate * 100.0) / 100.0);
        
        // Total compute units
        BigDecimal totalComputeUnits = apifyUsageRepository.getTotalComputeUnitsByUserId(userId);
        summary.put("totalComputeUnits", totalComputeUnits);
        
        // Total ads scraped
        Long totalAdsScraped = apifyUsageRepository.getTotalAdsScrapedByUserId(userId);
        summary.put("totalAdsScraped", totalAdsScraped);
        
        // Cost breakdown by request type
        List<Object[]> requestTypeBreakdown = apifyUsageRepository.getCostBreakdownByRequestType(userId);
        Map<String, BigDecimal> requestTypeCosts = new HashMap<>();
        for (Object[] row : requestTypeBreakdown) {
            requestTypeCosts.put((String) row[0], (BigDecimal) row[1]);
        }
        summary.put("costByRequestType", requestTypeCosts);
        
        // Cost breakdown by actor
        List<Object[]> actorBreakdown = apifyUsageRepository.getCostBreakdownByActor(userId);
        Map<String, BigDecimal> actorCosts = new HashMap<>();
        for (Object[] row : actorBreakdown) {
            actorCosts.put((String) row[0], (BigDecimal) row[1]);
        }
        summary.put("costByActor", actorCosts);
        
        // Daily costs for the last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Object[]> dailyCosts = apifyUsageRepository.getDailyCostSummary(userId, thirtyDaysAgo);
        summary.put("dailyCosts", dailyCosts);
        
        return summary;
    }
    
    /**
     * Get recent usage records for a user
     */
    public List<ApifyUsage> getRecentUsage(Long userId, int limit) {
        return apifyUsageRepository.findByUserIdOrderByRequestTimestampDesc(userId, 
            org.springframework.data.domain.PageRequest.of(0, limit)).getContent();
    }
    
    public List<ApifyUsage> getRecentUsage(Long userId, int limit, int offset) {
        int page = offset / limit;
        return apifyUsageRepository.findByUserIdOrderByRequestTimestampDesc(userId, 
            org.springframework.data.domain.PageRequest.of(page, limit)).getContent();
    }
}
