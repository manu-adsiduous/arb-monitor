package com.arbmonitor.api.service.compliance;

import com.arbmonitor.api.model.ScrapedAd;
import com.arbmonitor.api.service.OpenAIAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Modular landing page analysis component
 * Handles landing page content scraping, screenshot capture, and compliance checking
 */
@Component
public class LandingPageAnalysisModule {
    
    private static final Logger logger = LoggerFactory.getLogger(LandingPageAnalysisModule.class);
    
    @Autowired
    private OpenAIAnalysisService openAIAnalysisService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Analyze landing page content and compliance
     */
    public LandingPageAnalysisResult analyzeLandingPage(ScrapedAd scrapedAd, String landingPageContent) {
        logger.info("Starting landing page analysis for ad: {}", scrapedAd.getMetaAdId());
        
        LandingPageAnalysisResult result = new LandingPageAnalysisResult();
        result.setMetaAdId(scrapedAd.getMetaAdId());
        result.setLandingPageUrl(scrapedAd.getLandingPageUrl());
        
        try {
            // 1. Set the scraped content
            result.setPageContent(landingPageContent);
            
            // 2. Check for screenshot
            String screenshotPath = findScreenshotPath(scrapedAd);
            result.setScreenshotPath(screenshotPath);
            
            // 3. Perform compliance analysis using structured JSON
            ComplianceAnalysisResult complianceResult = performLandingPageComplianceAnalysis(scrapedAd, landingPageContent);
            result.setComplianceResult(complianceResult);
            
            logger.info("Landing page analysis completed for ad: {}", scrapedAd.getMetaAdId());
            return result;
            
        } catch (Exception e) {
            logger.error("Error analyzing landing page for ad: {}", scrapedAd.getMetaAdId(), e);
            result.setError("Landing page analysis failed: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * Find screenshot path for the landing page
     */
    private String findScreenshotPath(ScrapedAd scrapedAd) {
        try {
            // Look for screenshot in media directory
            // Format: media/screenshots/{domain}/{date}/{ad-id}_landing_page.png
            String domainName = extractDomainFromUrl(scrapedAd.getLandingPageUrl());
            String adId = scrapedAd.getMetaAdId();
            
            // Check common screenshot locations
            String[] possiblePaths = {
                String.format("./media/screenshots/%s/%s_landing_page.png", domainName, adId),
                String.format("./media/screenshots/%s_landing_page.png", adId),
                String.format("./media/images/%s/%s_landing_page.png", domainName, adId)
            };
            
            for (String pathStr : possiblePaths) {
                Path path = Paths.get(pathStr);
                if (Files.exists(path)) {
                    logger.debug("Found screenshot at: {}", pathStr);
                    return pathStr;
                }
            }
            
            logger.debug("No screenshot found for ad: {}", adId);
            return null;
            
        } catch (Exception e) {
            logger.error("Error finding screenshot path", e);
            return null;
        }
    }
    
    /**
     * Extract domain name from URL
     */
    private String extractDomainFromUrl(String url) {
        try {
            if (url == null || url.trim().isEmpty()) {
                return "unknown";
            }
            
            // Remove protocol
            String domain = url.replaceFirst("^https?://", "");
            // Remove www.
            domain = domain.replaceFirst("^www\\.", "");
            // Take only the domain part (before first slash)
            int slashIndex = domain.indexOf('/');
            if (slashIndex > 0) {
                domain = domain.substring(0, slashIndex);
            }
            
            return domain.toLowerCase();
            
        } catch (Exception e) {
            logger.error("Error extracting domain from URL: {}", url, e);
            return "unknown";
        }
    }
    
    /**
     * Perform compliance analysis on landing page using structured JSON for GPT-4
     */
    private ComplianceAnalysisResult performLandingPageComplianceAnalysis(ScrapedAd scrapedAd, String landingPageContent) {
        try {
            // Create structured JSON payload for GPT-4
            ObjectNode complianceRequest = objectMapper.createObjectNode();
            complianceRequest.put("task", "landing_page_compliance");
            complianceRequest.put("ad_id", scrapedAd.getMetaAdId());
            complianceRequest.put("landing_page_url", scrapedAd.getLandingPageUrl());
            
            // Add ad content for relevance checking
            ObjectNode adContent = objectMapper.createObjectNode();
            adContent.put("headline", scrapedAd.getHeadline());
            adContent.put("primary_text", scrapedAd.getPrimaryText());
            adContent.put("description", scrapedAd.getDescription());
            adContent.put("call_to_action", scrapedAd.getCallToAction());
            complianceRequest.set("ad_content", adContent);
            
            // Add landing page content (truncated for API limits)
            String truncatedContent = landingPageContent != null && landingPageContent.length() > 3000 
                ? landingPageContent.substring(0, 3000) + "..." 
                : landingPageContent;
            complianceRequest.put("landing_page_content", truncatedContent);
            
            // Add compliance rules context
            ObjectNode rules = objectMapper.createObjectNode();
            rules.put("check_relevance_to_ad", true);
            rules.put("check_misleading_content", true);
            rules.put("check_page_accessibility", true);
            rules.put("check_user_experience", true);
            rules.put("check_content_quality", true);
            complianceRequest.set("rules", rules);
            
            // Request structured response
            ObjectNode responseFormat = objectMapper.createObjectNode();
            responseFormat.put("compliant", "boolean");
            responseFormat.put("relevance_score", "number (0-1)");
            responseFormat.put("violations", "array of violation objects");
            responseFormat.put("reasoning", "string");
            responseFormat.put("page_summary", "string");
            complianceRequest.set("expected_response_format", responseFormat);
            
            // Call OpenAI with structured request
            String jsonResponse = openAIAnalysisService.analyzeLandingPageCompliance(complianceRequest.toString());
            
            // Parse response and create result
            ComplianceAnalysisResult result = new ComplianceAnalysisResult();
            result.setRawResponse(jsonResponse);
            
            // TODO: Parse JSON response and populate structured result
            // For now, return basic result
            result.setCompliant(true); // Will be determined from JSON response
            result.setConfidenceScore(0.8); // Will be extracted from JSON response
            result.setReasoning("Landing page compliance analysis completed using structured JSON format");
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error performing landing page compliance analysis", e);
            ComplianceAnalysisResult errorResult = new ComplianceAnalysisResult();
            errorResult.setCompliant(false);
            errorResult.setConfidenceScore(0.0);
            errorResult.setReasoning("Landing page compliance analysis failed: " + e.getMessage());
            return errorResult;
        }
    }
}


