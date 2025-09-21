package com.arbmonitor.api.controller;

import com.arbmonitor.api.model.ApifyUsage;
import com.arbmonitor.api.model.OpenAIUsage;
import com.arbmonitor.api.service.ApifyCostTrackingService;
import com.arbmonitor.api.service.OpenAICostTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for OpenAI cost tracking and analytics
 */
@RestController
@RequestMapping("/api/cost-tracking")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class CostTrackingController {
    
    private static final Logger logger = LoggerFactory.getLogger(CostTrackingController.class);
    
    @Autowired
    private OpenAICostTrackingService openAICostTrackingService;
    
    @Autowired
    private ApifyCostTrackingService apifyCostTrackingService;
    
    /**
     * Get unified cost summary for a user (OpenAI + Apify)
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getCostSummary(@RequestHeader("X-User-ID") Long userId) {
        try {
            logger.info("Getting unified cost summary for user: {}", userId);
            
            Map<String, Object> openAISummary = openAICostTrackingService.getCostSummary(userId);
            Map<String, Object> apifySummary = apifyCostTrackingService.getCostSummary(userId);
            
            Map<String, Object> unifiedSummary = new HashMap<>();
            unifiedSummary.put("openai", openAISummary);
            unifiedSummary.put("apify", apifySummary);
            
            // Calculate totals
            BigDecimal totalOpenAICost = (BigDecimal) openAISummary.get("totalCost");
            BigDecimal totalApifyCost = (BigDecimal) apifySummary.get("totalCost");
            BigDecimal totalCost = totalOpenAICost.add(totalApifyCost);
            
            Long totalOpenAIRequests = (Long) openAISummary.get("totalRequests");
            Long totalApifyRequests = (Long) apifySummary.get("totalRequests");
            Long totalRequests = totalOpenAIRequests + totalApifyRequests;
            
            unifiedSummary.put("totalCost", totalCost);
            unifiedSummary.put("totalRequests", totalRequests);
            unifiedSummary.put("openaiCost", totalOpenAICost);
            unifiedSummary.put("apifyCost", totalApifyCost);
            
            return ResponseEntity.ok(unifiedSummary);
            
        } catch (Exception e) {
            logger.error("Error getting cost summary for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get cost summary: " + e.getMessage()));
        }
    }
    
    /**
     * Get recent OpenAI usage records for a user
     */
    @GetMapping("/recent/openai")
    public ResponseEntity<List<OpenAIUsage>> getRecentOpenAIUsage(
            @RequestHeader("X-User-ID") Long userId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        try {
            logger.info("Getting recent OpenAI usage for user: {} (limit: {}, offset: {})", userId, limit, offset);
            
            List<OpenAIUsage> recentUsage = offset > 0 ? 
                openAICostTrackingService.getRecentUsage(userId, limit, offset) :
                openAICostTrackingService.getRecentUsage(userId, limit);
            
            return ResponseEntity.ok(recentUsage);
            
        } catch (Exception e) {
            logger.error("Error getting recent OpenAI usage for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get recent Apify usage records for a user
     */
    @GetMapping("/recent/apify")
    public ResponseEntity<List<ApifyUsage>> getRecentApifyUsage(
            @RequestHeader("X-User-ID") Long userId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        try {
            logger.info("Getting recent Apify usage for user: {} (limit: {}, offset: {})", userId, limit, offset);
            
            List<ApifyUsage> recentUsage = offset > 0 ? 
                apifyCostTrackingService.getRecentUsage(userId, limit, offset) :
                apifyCostTrackingService.getRecentUsage(userId, limit);
            
            return ResponseEntity.ok(recentUsage);
            
        } catch (Exception e) {
            logger.error("Error getting recent Apify usage for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get unified cost analytics dashboard data
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData(@RequestHeader("X-User-ID") Long userId) {
        try {
            logger.info("Getting unified dashboard data for user: {}", userId);
            
            Map<String, Object> openAISummary = openAICostTrackingService.getCostSummary(userId);
            Map<String, Object> apifySummary = apifyCostTrackingService.getCostSummary(userId);
            List<OpenAIUsage> recentOpenAIUsage = openAICostTrackingService.getRecentUsage(userId, 10);
            List<ApifyUsage> recentApifyUsage = apifyCostTrackingService.getRecentUsage(userId, 10);
            
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("openai", Map.of(
                "summary", openAISummary,
                "recentUsage", recentOpenAIUsage
            ));
            dashboard.put("apify", Map.of(
                "summary", apifySummary,
                "recentUsage", recentApifyUsage
            ));
            
            // Calculate unified totals
            BigDecimal totalOpenAICost = (BigDecimal) openAISummary.get("totalCost");
            BigDecimal totalApifyCost = (BigDecimal) apifySummary.get("totalCost");
            BigDecimal totalCost = totalOpenAICost.add(totalApifyCost);
            
            dashboard.put("totalCost", totalCost);
            dashboard.put("openaiCost", totalOpenAICost);
            dashboard.put("apifyCost", totalApifyCost);
            
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            logger.error("Error getting dashboard data for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get dashboard data: " + e.getMessage()));
        }
    }
}
