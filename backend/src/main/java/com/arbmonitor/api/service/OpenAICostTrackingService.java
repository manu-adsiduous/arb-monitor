package com.arbmonitor.api.service;

import com.arbmonitor.api.model.OpenAIUsage;
import com.arbmonitor.api.repository.OpenAIUsageRepository;
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
 * Service for tracking OpenAI API costs and usage
 */
@Service
public class OpenAICostTrackingService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAICostTrackingService.class);
    
    @Autowired
    private OpenAIUsageRepository openAIUsageRepository;
    
    // OpenAI pricing per 1K tokens (as of September 2024)
    private static final Map<String, ModelPricing> MODEL_PRICING = new HashMap<>();
    
    static {
        // GPT-4o pricing
        MODEL_PRICING.put("gpt-4o", new ModelPricing(
            new BigDecimal("0.0025"), // $2.50 per 1K input tokens
            new BigDecimal("0.01")    // $10.00 per 1K output tokens
        ));
        
        // GPT-4 pricing
        MODEL_PRICING.put("gpt-4", new ModelPricing(
            new BigDecimal("0.03"),   // $30.00 per 1K input tokens
            new BigDecimal("0.06")    // $60.00 per 1K output tokens
        ));
        
        // GPT-4 Turbo pricing
        MODEL_PRICING.put("gpt-4-turbo", new ModelPricing(
            new BigDecimal("0.01"),   // $10.00 per 1K input tokens
            new BigDecimal("0.03")    // $30.00 per 1K output tokens
        ));
        
        // GPT-3.5 Turbo pricing
        MODEL_PRICING.put("gpt-3.5-turbo", new ModelPricing(
            new BigDecimal("0.0005"), // $0.50 per 1K input tokens
            new BigDecimal("0.0015")  // $1.50 per 1K output tokens
        ));
    }
    
    /**
     * Track an OpenAI API request and calculate costs
     */
    public OpenAIUsage trackRequest(String modelName, String requestType, Long userId, 
                                   String domainName, String metaAdId, long startTime) {
        OpenAIUsage usage = new OpenAIUsage(modelName, requestType, userId);
        usage.setDomainName(domainName);
        usage.setMetaAdId(metaAdId);
        usage.setRequestDurationMs(System.currentTimeMillis() - startTime);
        
        return usage;
    }
    
    /**
     * Update usage record with response data and save to database
     */
    public void updateAndSaveUsage(OpenAIUsage usage, JsonNode response, String responseText, boolean success, String errorMessage) {
        try {
            usage.setSuccess(success);
            usage.setErrorMessage(errorMessage);
            
            if (success && response != null) {
                // Extract token usage from OpenAI response
                JsonNode usageNode = response.get("usage");
                if (usageNode != null) {
                    usage.setPromptTokens(usageNode.get("prompt_tokens").asInt());
                    usage.setCompletionTokens(usageNode.get("completion_tokens").asInt());
                    usage.setTotalTokens(usageNode.get("total_tokens").asInt());
                }
                
                if (responseText != null) {
                    usage.setResponseSizeChars(responseText.length());
                }
                
                // Calculate estimated cost
                BigDecimal cost = calculateCost(usage.getModelName(), usage.getPromptTokens(), usage.getCompletionTokens());
                usage.setEstimatedCost(cost);
                
                logger.debug("OpenAI request tracked: {} tokens, estimated cost: ${}", 
                    usage.getTotalTokens(), cost);
            } else {
                // For failed requests, we can't get token usage, so estimate based on request size
                usage.setEstimatedCost(BigDecimal.ZERO);
            }
            
            // Save to database
            openAIUsageRepository.save(usage);
            
        } catch (Exception e) {
            logger.error("Error tracking OpenAI usage: {}", e.getMessage());
        }
    }
    
    /**
     * Calculate cost based on model and token usage
     */
    public BigDecimal calculateCost(String modelName, Integer promptTokens, Integer completionTokens) {
        ModelPricing pricing = MODEL_PRICING.get(modelName);
        if (pricing == null) {
            logger.warn("No pricing information for model: {}, using GPT-4o pricing as fallback", modelName);
            pricing = MODEL_PRICING.get("gpt-4o");
        }
        
        if (promptTokens == null) promptTokens = 0;
        if (completionTokens == null) completionTokens = 0;
        
        // Calculate cost: (prompt_tokens / 1000) * input_price + (completion_tokens / 1000) * output_price
        BigDecimal inputCost = BigDecimal.valueOf(promptTokens)
            .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
            .multiply(pricing.inputPrice);
            
        BigDecimal outputCost = BigDecimal.valueOf(completionTokens)
            .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
            .multiply(pricing.outputPrice);
            
        return inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP);
    }
    
    /**
     * Get cost summary for a user
     */
    public Map<String, Object> getCostSummary(Long userId) {
        Map<String, Object> summary = new HashMap<>();
        
        // Total cost
        BigDecimal totalCost = openAIUsageRepository.getTotalCostByUserId(userId);
        summary.put("totalCost", totalCost);
        
        // Total requests
        Long totalRequests = openAIUsageRepository.getTotalRequestsByUserId(userId);
        summary.put("totalRequests", totalRequests);
        
        // Successful requests
        Long successfulRequests = openAIUsageRepository.getSuccessfulRequestsByUserId(userId);
        summary.put("successfulRequests", successfulRequests);
        
        // Success rate
        double successRate = totalRequests > 0 ? (double) successfulRequests / totalRequests * 100 : 0;
        summary.put("successRate", Math.round(successRate * 100.0) / 100.0);
        
        // Total tokens
        Long totalTokens = openAIUsageRepository.getTotalTokensByUserId(userId);
        summary.put("totalTokens", totalTokens);
        
        // Cost breakdown by request type
        List<Object[]> requestTypeBreakdown = openAIUsageRepository.getCostBreakdownByRequestType(userId);
        Map<String, BigDecimal> requestTypeCosts = new HashMap<>();
        for (Object[] row : requestTypeBreakdown) {
            requestTypeCosts.put((String) row[0], (BigDecimal) row[1]);
        }
        summary.put("costByRequestType", requestTypeCosts);
        
        // Cost breakdown by model
        List<Object[]> modelBreakdown = openAIUsageRepository.getCostBreakdownByModel(userId);
        Map<String, BigDecimal> modelCosts = new HashMap<>();
        for (Object[] row : modelBreakdown) {
            modelCosts.put((String) row[0], (BigDecimal) row[1]);
        }
        summary.put("costByModel", modelCosts);
        
        // Daily costs for the last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Object[]> dailyCosts = openAIUsageRepository.getDailyCostSummary(userId, thirtyDaysAgo);
        summary.put("dailyCosts", dailyCosts);
        
        return summary;
    }
    
    /**
     * Get recent usage records for a user
     */
    public List<OpenAIUsage> getRecentUsage(Long userId, int limit) {
        return openAIUsageRepository.findByUserIdOrderByRequestTimestampDesc(userId, 
            org.springframework.data.domain.PageRequest.of(0, limit)).getContent();
    }
    
    public List<OpenAIUsage> getRecentUsage(Long userId, int limit, int offset) {
        int page = offset / limit;
        return openAIUsageRepository.findByUserIdOrderByRequestTimestampDesc(userId, 
            org.springframework.data.domain.PageRequest.of(page, limit)).getContent();
    }
    
    /**
     * Inner class to hold model pricing information
     */
    private static class ModelPricing {
        final BigDecimal inputPrice;  // Price per 1K input tokens
        final BigDecimal outputPrice; // Price per 1K output tokens
        
        ModelPricing(BigDecimal inputPrice, BigDecimal outputPrice) {
            this.inputPrice = inputPrice;
            this.outputPrice = outputPrice;
        }
    }
}
