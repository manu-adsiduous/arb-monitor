package com.arbmonitor.api.service;

import com.arbmonitor.api.model.AdAnalysis;
import com.arbmonitor.api.model.Domain;
import com.arbmonitor.api.model.ScrapedAd;
import com.arbmonitor.api.repository.AdAnalysisRepository;
import com.arbmonitor.api.repository.ViolationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Separate service for individual ad analysis with isolated transactions
 */
@Service
public class IndividualAdAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(IndividualAdAnalysisService.class);
    
    @Autowired
    private AdAnalysisRepository adAnalysisRepository;
    
    @Autowired
    private ViolationRepository violationRepository;
    
    @Autowired
    private OpenAIAnalysisService openAIAnalysisService;
    

    
    /**
     * Analyze a single ad with a completely new transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AdAnalysis analyzeAdWithNewTransaction(ScrapedAd scrapedAd, Domain domain) {
        logger.info("Starting isolated compliance analysis for ad: {} from domain: {}", 
                   scrapedAd.getMetaAdId(), domain.getDomainName());
        
        try {
            // Delete any existing analysis first in a separate operation
            cleanupExistingAnalysis(scrapedAd.getMetaAdId(), domain);
            
            // Create completely new analysis
            AdAnalysis analysis = new AdAnalysis();
            analysis.setDomain(domain);
            analysis.setMetaAdId(scrapedAd.getMetaAdId());

            // Set analysis data from scraped ad (with DPA template handling)
            String headline = scrapedAd.getHeadline();
            if (headline == null || headline.trim().isEmpty()) {
                headline = "No headline";
            } else if (isDynamicProductAd(headline)) {
                // Handle DPA templates - convert to readable format
                headline = convertDpaTemplate(headline);
            }
            analysis.setHeadline(headline);
            
            String primaryText = scrapedAd.getPrimaryText();
            if (primaryText != null && isDynamicProductAd(primaryText)) {
                primaryText = convertDpaTemplate(primaryText);
            }
            analysis.setPrimaryText(primaryText);
            
            // Use locally stored image path instead of original URL
            String imageUrl = null;
            if (scrapedAd.getLocalImagePaths() != null && !scrapedAd.getLocalImagePaths().isEmpty()) {
                imageUrl = scrapedAd.getLocalImagePaths().get(0);
                logger.debug("Using local image path: {}", imageUrl);
            } else if (scrapedAd.getImageUrls() != null && !scrapedAd.getImageUrls().isEmpty()) {
                imageUrl = scrapedAd.getImageUrls().get(0);
                logger.debug("Using original image URL (no local path available)");
            }
            analysis.setImageUrl(imageUrl);
            analysis.setLandingPageUrl(scrapedAd.getLandingPageUrl());
            
            // Use pre-extracted text from media processing
            String imageText = scrapedAd.getExtractedImageText();
            analysis.setImageText(imageText);
            
            // Combine all text content for compliance analysis
            StringBuilder combinedText = new StringBuilder();
            if (analysis.getPrimaryText() != null && !analysis.getPrimaryText().trim().isEmpty()) {
                combinedText.append(analysis.getPrimaryText());
            }
            
            // Add pre-extracted image text
            if (imageText != null && !imageText.trim().isEmpty()) {
                if (combinedText.length() > 0) combinedText.append("\n\n");
                combinedText.append("IMAGE TEXT:\n").append(imageText);
            }
            
            // Add pre-extracted video text
            String videoText = scrapedAd.getExtractedVideoText();
            if (videoText != null && !videoText.trim().isEmpty()) {
                if (combinedText.length() > 0) combinedText.append("\n\n");
                combinedText.append("VIDEO CONTENT:\n").append(videoText);
            }
            
            // Update primary text with combined content
            if (combinedText.length() > 0) {
                analysis.setPrimaryText(combinedText.toString());
            }
            
            // Simple landing page content for now
            String landingPageContent = "Landing page content analysis pending";
            analysis.setLandingPageContent(landingPageContent);
            
            // Check if RAC analysis is enabled for this domain
            boolean racEnabled = domain.getRacParameter() != null && !domain.getRacParameter().trim().isEmpty();
            
            // Use OpenAI GPT-4 for intelligent compliance analysis
            String adText = buildAdText(scrapedAd, combinedText.toString());
            String racValue = racEnabled ? scrapedAd.getReferrerAdCreative() : null;
            
            try {
                OpenAIAnalysisService.ComplianceAnalysisResult aiResult = openAIAnalysisService.analyzeAdCompliance(
                        adText, landingPageContent, racValue, racEnabled
                );
                
                // Set binary compliance results
                analysis.setAdCreativeCompliant(aiResult.isAdCreativeCompliant());
                analysis.setAdCreativeReason(aiResult.getAdCreativeReason());
                analysis.setLandingPageRelevant(aiResult.isLandingPageRelevant());
                analysis.setLandingPageReason(aiResult.getLandingPageReason());
                analysis.setRacRelevant(aiResult.isRacRelevant());
                analysis.setRacReason(aiResult.getRacReason());
                analysis.setOverallCompliant(aiResult.isOverallCompliant());
                
                // Set legacy fields for backward compatibility
                analysis.setComplianceScore(aiResult.isOverallCompliant() ? 100.0 : 0.0);
                analysis.setComplianceStatus(aiResult.isOverallCompliant() ?
                        AdAnalysis.ComplianceStatus.EXCELLENT : AdAnalysis.ComplianceStatus.CRITICAL);
                
                // Build detailed analysis notes
                String analysisNotes = buildAnalysisNotes(aiResult);
                analysis.setAnalysisNotes(analysisNotes);
                
            } catch (Exception e) {
                logger.error("OpenAI analysis failed for ad {}: {}", scrapedAd.getMetaAdId(), e.getMessage());
                
                // Set fallback compliance results
                analysis.setAdCreativeCompliant(false);
                analysis.setAdCreativeReason("Analysis failed: " + e.getMessage());
                analysis.setLandingPageRelevant(false);
                analysis.setLandingPageReason("Analysis failed: " + e.getMessage());
                analysis.setRacRelevant(racEnabled ? false : true);
                analysis.setRacReason(racEnabled ? "Analysis failed: " + e.getMessage() : "RAC analysis turned off");
                analysis.setOverallCompliant(false);
                
                // Set legacy fields
                analysis.setComplianceScore(0.0);
                analysis.setComplianceStatus(AdAnalysis.ComplianceStatus.CRITICAL);
                analysis.setAnalysisNotes("Automated analysis failed. Manual review required.");
            }
            
            // Save the analysis
            AdAnalysis savedAnalysis = adAnalysisRepository.save(analysis);
            logger.info("Successfully completed compliance analysis for ad: {} (ID: {})", 
                       scrapedAd.getMetaAdId(), savedAnalysis.getId());
            
            return savedAnalysis;
            
        } catch (Exception e) {
            logger.error("Critical error in isolated ad analysis for ad {}: {}", 
                        scrapedAd.getMetaAdId(), e.getMessage(), e);
            throw new RuntimeException("Failed to analyze ad: " + scrapedAd.getMetaAdId(), e);
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void cleanupExistingAnalysis(String metaAdId, Domain domain) {
        try {
            Optional<AdAnalysis> existingAnalysis = adAnalysisRepository.findByMetaAdIdAndDomain(metaAdId, domain);
            if (existingAnalysis.isPresent()) {
                logger.debug("Deleting existing analysis for ad: {}", metaAdId);
                adAnalysisRepository.delete(existingAnalysis.get());
                adAnalysisRepository.flush();
            }
        } catch (Exception e) {
            logger.warn("Could not cleanup existing analysis for ad {}: {}", metaAdId, e.getMessage());
            // Continue anyway - we'll create a new one
        }
    }
    
    /**
     * Check if text contains DPA template variables
     */
    private boolean isDynamicProductAd(String text) {
        if (text == null) return false;
        return text.contains("{{") && text.contains("}}");
    }
    
    /**
     * Convert DPA template variables to readable format
     */
    private String convertDpaTemplate(String text) {
        if (text == null) return null;
        
        return text
            .replaceAll("\\{\\{product\\.name\\}\\}", "[Product Name]")
            .replaceAll("\\{\\{product\\.brand\\}\\}", "[Brand]")
            .replaceAll("\\{\\{product\\.price\\}\\}", "[Price]")
            .replaceAll("\\{\\{product\\.category\\}\\}", "[Category]")
            .replaceAll("\\{\\{campaign\\.name\\}\\}", "[Campaign]")
            .replaceAll("\\{\\{[^}]+\\}\\}", "[Dynamic Content]");
    }
    
    /**
     * Build ad text for analysis
     */
    private String buildAdText(ScrapedAd scrapedAd, String combinedText) {
        StringBuilder adTextBuilder = new StringBuilder();
        adTextBuilder.append("Headline: ").append(scrapedAd.getHeadline()).append("\n");
        adTextBuilder.append("Primary Text: ").append(combinedText).append("\n");
        if (scrapedAd.getCallToAction() != null && !scrapedAd.getCallToAction().trim().isEmpty()) {
            adTextBuilder.append("Call to Action: ").append(scrapedAd.getCallToAction()).append("\n");
        }
        return adTextBuilder.toString();
    }
    
    /**
     * Build analysis notes from AI result
     */
    private String buildAnalysisNotes(OpenAIAnalysisService.ComplianceAnalysisResult aiResult) {
        StringBuilder notes = new StringBuilder();
        notes.append("Overall Compliance: ").append(aiResult.isOverallCompliant() ? "Compliant" : "Not Compliant").append("\n");
        notes.append("Ad Creative: ").append(aiResult.isAdCreativeCompliant() ? "Compliant" : "Not Compliant").append("\n");
        if (!aiResult.isAdCreativeCompliant()) {
            notes.append("  Reason: ").append(aiResult.getAdCreativeReason()).append("\n");
        }
        notes.append("Landing Page: ").append(aiResult.isLandingPageRelevant() ? "Relevant" : "Not Relevant").append("\n");
        if (!aiResult.isLandingPageRelevant()) {
            notes.append("  Reason: ").append(aiResult.getLandingPageReason()).append("\n");
        }
        notes.append("RAC Relevance: ").append(aiResult.isRacRelevant() ? "Relevant" : "Turned off").append("\n");
        if (!aiResult.isRacRelevant() && !aiResult.getRacReason().contains("turned off")) {
            notes.append("  Reason: ").append(aiResult.getRacReason()).append("\n");
        }
        notes.append("\nBased on Google AdSense for Search & RSOC compliance guidelines");
        return notes.toString();
    }
}
