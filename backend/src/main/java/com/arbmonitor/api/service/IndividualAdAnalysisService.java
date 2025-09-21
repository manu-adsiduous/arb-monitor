package com.arbmonitor.api.service;

import com.arbmonitor.api.model.AdAnalysis;
import com.arbmonitor.api.model.Domain;
import com.arbmonitor.api.model.ScrapedAd;
import com.arbmonitor.api.repository.AdAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
    private OpenAIAnalysisService openAIAnalysisService;
    
    @Autowired
    private LandingPageScreenshotService landingPageScreenshotService;
    

    
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
            
            // Scrape landing page content
            String landingPageContent = scrapeLandingPageContent(scrapedAd.getLandingPageUrl());
            analysis.setLandingPageContent(landingPageContent);
            
            // Capture landing page screenshot
            logger.info("Capturing landing page screenshot for ad: {}", scrapedAd.getMetaAdId());
            String screenshotPath = landingPageScreenshotService.captureScreenshot(
                scrapedAd.getLandingPageUrl(), 
                scrapedAd.getMetaAdId()
            );
            if (screenshotPath != null) {
                analysis.setLandingPageScreenshotPath(screenshotPath);
                logger.info("Screenshot captured successfully: {}", screenshotPath);
            } else {
                logger.warn("Failed to capture screenshot for ad: {}", scrapedAd.getMetaAdId());
            }
            
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
    
    /**
     * Scrape landing page content for analysis
     */
    private String scrapeLandingPageContent(String landingPageUrl) {
        if (landingPageUrl == null || landingPageUrl.trim().isEmpty()) {
            return "No landing page URL provided";
        }
        
        try {
            logger.info("Scraping landing page content: {}", landingPageUrl);
            
            // Clean URL by removing or replacing template variables
            String cleanUrl = cleanUrlTemplateVariables(landingPageUrl);
            logger.info("Cleaned URL for scraping: {}", cleanUrl);
            
            // Add comprehensive headers to avoid blocking and handle redirects
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            headers.set("Accept-Language", "en-US,en;q=0.5");
            // Remove Accept-Encoding to avoid compression issues
            headers.set("DNT", "1");
            headers.set("Connection", "keep-alive");
            headers.set("Upgrade-Insecure-Requests", "1");
            
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            
            // Configure RestTemplate to follow redirects and handle compression
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            
            // Add message converters to handle different content types properly
            restTemplate.getMessageConverters().add(0, new org.springframework.http.converter.StringHttpMessageConverter(java.nio.charset.StandardCharsets.UTF_8));
            
            // Create URI using the cleaned URL
            java.net.URI uri = java.net.URI.create(cleanUrl);
            
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                uri, 
                org.springframework.http.HttpMethod.GET, 
                entity, 
                String.class
            );
            
            String htmlContent = response.getBody();
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                return "Landing page returned empty content";
            }
            
            // Extract text content from HTML (basic implementation)
            String textContent = extractTextFromHtml(htmlContent);
            
            if (textContent == null || textContent.trim().isEmpty()) {
                return "No readable text content found on landing page";
            }
            
            // Limit content length to avoid overwhelming the analysis
            if (textContent.length() > 5000) {
                textContent = textContent.substring(0, 5000) + "... [content truncated]";
            }
            
            logger.info("Successfully scraped landing page content ({} characters): {}", 
                       textContent.length(), landingPageUrl);
            
            return textContent;
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.warn("HTTP error scraping landing page {}: {} - {}", landingPageUrl, e.getStatusCode(), e.getMessage());
            return "Landing page not accessible: " + e.getStatusCode() + " " + e.getMessage();
        } catch (Exception e) {
            logger.error("Error scraping landing page {}: {}", landingPageUrl, e.getMessage());
            return "Failed to scrape landing page: " + e.getMessage();
        }
    }
    
    /**
     * Clean URL by removing or replacing template variables like {{campaign.id}}
     */
    private String cleanUrlTemplateVariables(String url) {
        if (url == null) return null;
        
        try {
            // Remove template variables like {{campaign.id}}, {{adset.id}}, {{ad.id}}
            // We'll remove the entire parameter if it contains template variables
            String cleanUrl = url;
            
            // Split URL into base and query parameters
            String[] urlParts = url.split("\\?", 2);
            if (urlParts.length == 2) {
                String baseUrl = urlParts[0];
                String queryString = urlParts[1];
                
                // Process query parameters
                String[] params = queryString.split("&");
                StringBuilder cleanParams = new StringBuilder();
                
                for (String param : params) {
                    // Skip parameters that contain template variables
                    if (!param.contains("{{") && !param.contains("}}")) {
                        if (cleanParams.length() > 0) {
                            cleanParams.append("&");
                        }
                        cleanParams.append(param);
                    }
                }
                
                // Reconstruct URL
                cleanUrl = baseUrl;
                if (cleanParams.length() > 0) {
                    cleanUrl += "?" + cleanParams.toString();
                }
            }
            
            return cleanUrl;
            
        } catch (Exception e) {
            logger.warn("Error cleaning URL template variables from {}: {}", url, e.getMessage());
            // Fallback: just remove template variables in place
            return url.replaceAll("\\{\\{[^}]+\\}\\}", "placeholder");
        }
    }
    
    /**
     * Extract readable text content from HTML
     */
    private String extractTextFromHtml(String htmlContent) {
        if (htmlContent == null) return null;
        
        try {
            // Check if content appears to be binary/corrupted
            if (containsBinaryData(htmlContent)) {
                logger.warn("HTML content appears to be binary or corrupted, attempting to decode");
                return "Content appears to be binary or corrupted - unable to extract readable text";
            }
            
            // Enhanced HTML tag removal and text extraction
            String text = htmlContent
                // Remove script and style content (case insensitive, multiline)
                .replaceAll("(?is)<script[^>]*>.*?</script>", "")
                .replaceAll("(?is)<style[^>]*>.*?</style>", "")
                // Remove HTML comments
                .replaceAll("<!--.*?-->", "")
                // Remove common non-content tags
                .replaceAll("(?is)<head[^>]*>.*?</head>", "")
                .replaceAll("(?is)<nav[^>]*>.*?</nav>", "")
                .replaceAll("(?is)<footer[^>]*>.*?</footer>", "")
                // Remove HTML tags but preserve line breaks
                .replaceAll("<br[^>]*>", "\n")
                .replaceAll("<p[^>]*>", "\n")
                .replaceAll("</p>", "\n")
                .replaceAll("<div[^>]*>", "\n")
                .replaceAll("</div>", "\n")
                .replaceAll("<[^>]+>", " ")
                // Decode HTML entities
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&copy;", "©")
                .replace("&reg;", "®")
                .replace("&trade;", "™")
                // Clean up whitespace but preserve paragraph breaks
                .replaceAll("[ \\t]+", " ")  // Multiple spaces/tabs to single space
                .replaceAll("\\n\\s*\\n", "\n\n")  // Multiple newlines to double newline
                .replaceAll("^\\s+|\\s+$", "")  // Trim start/end
                .trim();
            
            // Filter out very short or suspicious content
            if (text.length() < 10) {
                return "Extracted text too short - may indicate parsing issues";
            }
            
            return text;
            
        } catch (Exception e) {
            logger.warn("Error extracting text from HTML: {}", e.getMessage());
            return "Failed to extract text from HTML content: " + e.getMessage();
        }
    }
    
    /**
     * Check if content contains binary data or is corrupted
     */
    private boolean containsBinaryData(String content) {
        if (content == null || content.length() < 100) return false;
        
        // Count non-printable characters
        long nonPrintableCount = content.chars()
            .filter(c -> c < 32 && c != 9 && c != 10 && c != 13)  // Exclude tab, LF, CR
            .count();
        
        // If more than 10% of characters are non-printable, likely binary
        return (nonPrintableCount * 100.0 / content.length()) > 10;
    }
}
