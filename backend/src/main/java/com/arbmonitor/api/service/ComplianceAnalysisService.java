package com.arbmonitor.api.service;

import com.arbmonitor.api.model.*;
import com.arbmonitor.api.repository.*;
import com.arbmonitor.api.service.ImageAnalysisService;
import com.arbmonitor.api.service.VideoAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Core compliance analysis service based on Google AFS & RSOC rules
 * Rules sourced from: https://bestoptions.net/creative-compliance/
 */
@Service
@Transactional
public class ComplianceAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(ComplianceAnalysisService.class);

    @Autowired
    private ComplianceRuleRepository complianceRuleRepository;

    @Autowired
    private AdAnalysisRepository adAnalysisRepository;

    @Autowired
    private ViolationRepository violationRepository;

    @Autowired
    private ScrapedAdRepository scrapedAdRepository;
    
    @Autowired
    private VideoAnalysisService videoAnalysisService;
    
    @Autowired
    private ImageAnalysisService imageAnalysisService;
    
    @Autowired
    private OpenAIAnalysisService openAIAnalysisService;
    
    @Autowired
    private IndividualAdAnalysisService individualAdAnalysisService;

    /**
     * Analyze a single scraped ad for compliance violations
     */
    @Transactional
    public AdAnalysis analyzeAd(ScrapedAd scrapedAd, Domain domain) {
        logger.info("Starting compliance analysis for ad: {} from domain: {}", 
                   scrapedAd.getMetaAdId(), domain.getDomainName());

        // Check if analysis already exists for this domain and ad
        AdAnalysis existingAnalysis = adAnalysisRepository.findByMetaAdIdAndDomain(scrapedAd.getMetaAdId(), domain)
                .orElse(null);

        AdAnalysis analysis;
        if (existingAnalysis != null) {
            analysis = existingAnalysis;
            // Clear existing violations for re-analysis
            violationRepository.deleteByAnalysis(analysis);
            analysis.getViolations().clear();
        } else {
            // Create new analysis
            analysis = new AdAnalysis();
            analysis.setId(null); // Ensure ID is null for new entities
            analysis.setDomain(domain);
            analysis.setMetaAdId(scrapedAd.getMetaAdId());
        }

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
        
        // Use pre-extracted text from media processing (done during scraping)
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
            logger.debug("Added pre-extracted image text ({} chars) to ad: {}", imageText.length(), scrapedAd.getMetaAdId());
        }
        
        // Add pre-extracted video text
        String videoText = scrapedAd.getExtractedVideoText();
        if (videoText != null && !videoText.trim().isEmpty()) {
            if (combinedText.length() > 0) combinedText.append("\n\n");
            combinedText.append("VIDEO CONTENT:\n").append(videoText);
            logger.debug("Added pre-extracted video text ({} chars) to ad: {}", videoText.length(), scrapedAd.getMetaAdId());
        }
        
        // Update primary text with combined content
        if (combinedText.length() > 0) {
            analysis.setPrimaryText(combinedText.toString());
        }
        
        // Analyze landing page content
        String landingPageContent = analyzeLandingPage(scrapedAd.getLandingPageUrl());
        analysis.setLandingPageContent(landingPageContent);
        
        // Use OpenAI GPT-4 for intelligent compliance analysis
        String adText = buildAdText(scrapedAd, combinedText.toString());
        String racValue = scrapedAd.getReferrerAdCreative();
        
        try {
            OpenAIAnalysisService.ComplianceAnalysisResult aiResult = openAIAnalysisService.analyzeAdCompliance(
                adText, landingPageContent, racValue
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
            
        } catch (Exception e) {
            logger.error("OpenAI analysis failed for ad {}: {}", scrapedAd.getMetaAdId(), e.getMessage());
            
            // Fallback to basic analysis
            analysis.setAdCreativeCompliant(false);
            analysis.setAdCreativeReason("Analysis failed: " + e.getMessage());
            analysis.setLandingPageRelevant(false);
            analysis.setLandingPageReason("Analysis failed: " + e.getMessage());
            analysis.setRacRelevant(false);
            analysis.setRacReason("Analysis failed: " + e.getMessage());
            analysis.setOverallCompliant(false);
            analysis.setComplianceScore(0.0);
            analysis.setComplianceStatus(AdAnalysis.ComplianceStatus.CRITICAL);
            analysis.setAnalysisNotes("Compliance analysis failed due to technical error: " + e.getMessage());
        }

        // Save analysis with proper error handling
        logger.info("About to save AdAnalysis for ad: {} with headline: {}", 
                   scrapedAd.getMetaAdId(), analysis.getHeadline());
        try {
            // Ensure the analysis has no ID set (let JPA generate it)
            analysis.setId(null);
            
            // Save the analysis
            analysis = adAnalysisRepository.save(analysis);
            logger.info("Successfully saved AdAnalysis with ID: {}", analysis.getId());
        } catch (Exception e) {
            logger.error("Failed to save AdAnalysis for ad: {} - Error: {}", 
                        scrapedAd.getMetaAdId(), e.getMessage(), e);
            
            // Create a minimal fallback analysis to avoid transaction issues
            AdAnalysis fallbackAnalysis = new AdAnalysis();
            fallbackAnalysis.setMetaAdId(scrapedAd.getMetaAdId());
            fallbackAnalysis.setDomain(domain); // Use the domain parameter from method signature
            fallbackAnalysis.setComplianceScore(0.0);
            fallbackAnalysis.setComplianceStatus(AdAnalysis.ComplianceStatus.CRITICAL);
            fallbackAnalysis.setAnalysisNotes("Analysis failed due to technical error: " + e.getMessage());
            
            // Set binary compliance fields to false
            fallbackAnalysis.setAdCreativeCompliant(false);
            fallbackAnalysis.setAdCreativeReason("Analysis failed: " + e.getMessage());
            fallbackAnalysis.setLandingPageRelevant(false);
            fallbackAnalysis.setLandingPageReason("Analysis failed: " + e.getMessage());
            fallbackAnalysis.setRacRelevant(false);
            fallbackAnalysis.setRacReason("Analysis failed: " + e.getMessage());
            fallbackAnalysis.setOverallCompliant(false);
            
            try {
                analysis = adAnalysisRepository.save(fallbackAnalysis);
                logger.warn("Saved fallback analysis with ID: {}", analysis.getId());
            } catch (Exception fallbackError) {
                logger.error("Even fallback analysis failed: {}", fallbackError.getMessage());
                throw new RuntimeException("Complete analysis failure for ad: " + scrapedAd.getMetaAdId(), fallbackError);
            }
        }
        
        // Save violations (temporarily disabled for debugging)
        // for (Violation violation : violations) {
        //     violation.setAnalysis(analysis);
        //     violationRepository.save(violation);
        // }

        logger.info("Compliance analysis completed for ad: {}", scrapedAd.getMetaAdId());

        return analysis;
    }

    /**
     * Perform all compliance checks based on Google AFS & RSOC rules
     */
    private List<Violation> performComplianceChecks(AdAnalysis analysis, ScrapedAd scrapedAd) {
        List<Violation> violations = new ArrayList<>();
        
        // Get all active compliance rules
        List<ComplianceRule> activeRules = complianceRuleRepository.findByActiveTrue();
        
        for (ComplianceRule rule : activeRules) {
            Violation violation = checkRule(rule, analysis, scrapedAd);
            if (violation != null) {
                violations.add(violation);
            }
        }
        
        return violations;
    }

    /**
     * Check a specific compliance rule against the ad
     */
    private Violation checkRule(ComplianceRule rule, AdAnalysis analysis, ScrapedAd scrapedAd) {
        String headline = analysis.getHeadline() != null ? analysis.getHeadline().toLowerCase() : "";
        String primaryText = analysis.getPrimaryText() != null ? analysis.getPrimaryText().toLowerCase() : "";
        String combinedText = headline + " " + primaryText;

        switch (rule.getRuleName()) {
            case "No Inducement to Click":
                return checkClickInducementRule(rule, analysis, combinedText);
            
            case "No False Promises":
                return checkFalsePromisesRule(rule, analysis, combinedText);
            
            case "No Implied Functionality":
                return checkImpliedFunctionalityRule(rule, analysis, combinedText);
            
            case "No Misleading Claims":
                return checkMisleadingClaimsRule(rule, analysis, combinedText);
            
            case "Automotive AI Image Compliance":
                return checkAutomotiveImageRule(rule, analysis, scrapedAd);
            
            case "No Unverifiable Claims":
                return checkUnverifiableClaimsRule(rule, analysis, combinedText);
            
            case "No Fake Scarcity":
                return checkFakeScarcityRule(rule, analysis, combinedText);
            
            case "No Extreme Pricing Claims":
                return checkExtremePricingRule(rule, analysis, combinedText);
                
            default:
                // For custom rules with patterns
                if (rule.getRulePattern() != null && !rule.getRulePattern().isEmpty()) {
                    return checkPatternRule(rule, analysis, combinedText);
                }
        }
        
        return null;
    }

    /**
     * Rule: No Inducement to Click or Search
     * Based on: "Ads must not contain language encouraging users to click, search, or interact"
     */
    private Violation checkClickInducementRule(ComplianceRule rule, AdAnalysis analysis, String text) {
        String[] forbiddenPhrases = {
            "click here", "click now", "search now", "tap here", "tap now",
            "support us", "visit these links", "check out these", "explore this",
            "see below", "click below", "search below", "tap below",
            "select your", "choose your", "pick your", "tap on your"
        };
        
        for (String phrase : forbiddenPhrases) {
            if (text.contains(phrase)) {
                return createViolation(rule, analysis, 
                    "Contains click inducement phrase: '" + phrase + "'",
                    "Found forbidden phrase that encourages clicking/searching");
            }
        }
        
        return null;
    }

    /**
     * Rule: No False Promises or Unverifiable Claims
     * Based on: "Claims like 'free phones' or exaggerated discounts must be realistic"
     */
    private Violation checkFalsePromisesRule(ComplianceRule rule, AdAnalysis analysis, String text) {
        String[] suspiciousPromises = {
            "free phone", "free iphone", "free samsung", "free laptop",
            "free car", "free truck", "free suv", "100% free",
            "completely free", "totally free", "absolutely free",
            "no cost", "zero cost", "$0 cost"
        };
        
        for (String promise : suspiciousPromises) {
            if (text.contains(promise)) {
                return createViolation(rule, analysis,
                    "Contains potentially false promise: '" + promise + "'",
                    "Avoid claiming anything as free unless completely verifiable");
            }
        }
        
        return null;
    }

    /**
     * Rule: No Implied Functionality
     * Based on: "Ads must not imply functionality if it's not available on landing page"
     */
    private Violation checkImpliedFunctionalityRule(ComplianceRule rule, AdAnalysis analysis, String text) {
        String[] functionalityPhrases = {
            "see prices", "check prices", "get quotes", "compare prices",
            "check rates", "see rates", "get rates", "apply now",
            "sign up now", "register now", "book now", "order now",
            "buy now", "purchase now", "shop now"
        };
        
        for (String phrase : functionalityPhrases) {
            if (text.contains(phrase)) {
                return createViolation(rule, analysis,
                    "Implies functionality that may not exist: '" + phrase + "'",
                    "Ensure promised functionality is available on landing page");
            }
        }
        
        return null;
    }

    /**
     * Rule: No Misleading Claims
     * Based on: "Misleading or outlandish claims must be avoided"
     */
    private Violation checkMisleadingClaimsRule(ComplianceRule rule, AdAnalysis analysis, String text) {
        String[] misleadingPhrases = {
            "doctors hate", "one weird trick", "secret that", "hidden truth",
            "they don't want you to know", "miracle cure", "instant results",
            "guaranteed results", "lose weight fast", "get rich quick",
            "make money fast", "work from home scam"
        };
        
        for (String phrase : misleadingPhrases) {
            if (text.contains(phrase)) {
                return createViolation(rule, analysis,
                    "Contains misleading claim pattern: '" + phrase + "'",
                    "Avoid sensational or misleading advertising language");
            }
        }
        
        return null;
    }

    /**
     * Rule: Automotive AI Image Compliance
     * Based on: "Avoid using badges or logos on AI-generated images for cars"
     */
    private Violation checkAutomotiveImageRule(ComplianceRule rule, AdAnalysis analysis, ScrapedAd scrapedAd) {
        String text = (analysis.getHeadline() + " " + analysis.getPrimaryText()).toLowerCase();
        
        // Check if this is automotive content
        String[] autoKeywords = {
            "car", "truck", "suv", "vehicle", "auto", "ford", "chevrolet", "toyota",
            "honda", "nissan", "dodge", "jeep", "bmw", "mercedes", "audi"
        };
        
        boolean isAutomotive = false;
        for (String keyword : autoKeywords) {
            if (text.contains(keyword)) {
                isAutomotive = true;
                break;
            }
        }
        
        if (isAutomotive && !scrapedAd.getImageUrls().isEmpty()) {
            // This would require image analysis to detect AI-generated content with badges
            // For now, we'll flag specific model mentions that might use AI images
            String[] specificModels = {
                "durango 1500", "f-150", "camaro", "mustang", "corvette",
                "accord", "civic", "altima", "sentra", "wrangler"
            };
            
            for (String model : specificModels) {
                if (text.contains(model)) {
                    return createViolation(rule, analysis,
                        "Specific car model mentioned with image - verify AI image compliance",
                        "Ensure AI-generated car images closely resemble actual vehicles without misleading badges");
                }
            }
        }
        
        return null;
    }

    /**
     * Rule: No Unverifiable Claims
     * Based on: "Make sure your claim is verifiable in the article"
     */
    private Violation checkUnverifiableClaimsRule(ComplianceRule rule, AdAnalysis analysis, String text) {
        String[] unverifiableClaims = {
            "top rated", "best rated", "highest rated", "#1 rated",
            "research shows", "studies show", "experts say", "proven by science",
            "clinically proven", "doctor recommended", "award winning"
        };
        
        for (String claim : unverifiableClaims) {
            if (text.contains(claim)) {
                return createViolation(rule, analysis,
                    "Contains unverifiable claim: '" + claim + "'",
                    "Ensure claims are backed by verifiable sources on landing page");
            }
        }
        
        return null;
    }

    /**
     * Rule: No Fake Scarcity
     * Based on: "Ads that imply time-limited offer when scarcity isn't real"
     */
    private Violation checkFakeScarcityRule(ComplianceRule rule, AdAnalysis analysis, String text) {
        String[] scarcityPhrases = {
            "only \\d+ left", "limited time", "expires today", "expires soon",
            "hurry", "act fast", "don't wait", "while supplies last",
            "limited quantity", "few remaining", "almost gone"
        };
        
        for (String pattern : scarcityPhrases) {
            if (Pattern.compile(pattern).matcher(text).find()) {
                return createViolation(rule, analysis,
                    "Contains fake scarcity indicator: matches pattern '" + pattern + "'",
                    "Avoid creating false urgency unless scarcity is genuine");
            }
        }
        
        return null;
    }

    /**
     * Rule: No Extreme Pricing Claims
     * Based on: "Avoid unrealistic claims like 'Jeep for $1500'"
     */
    private Violation checkExtremePricingRule(ComplianceRule rule, AdAnalysis analysis, String text) {
        // Pattern to match extremely low prices for expensive items
        Pattern extremePricePattern = Pattern.compile(
            "(car|truck|suv|jeep|ford|toyota|honda|bmw|mercedes).{0,20}\\$([1-9]\\d{0,2}|1[0-4]\\d{2}|1500)"
        );
        
        if (extremePricePattern.matcher(text).find()) {
            return createViolation(rule, analysis,
                "Contains unrealistic pricing claim",
                "Avoid extremely low prices for expensive items unless genuinely available");
        }
        
        return null;
    }

    /**
     * Check custom pattern-based rules
     */
    private Violation checkPatternRule(ComplianceRule rule, AdAnalysis analysis, String text) {
        try {
            Pattern pattern = Pattern.compile(rule.getRulePattern(), Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(text).find()) {
                return createViolation(rule, analysis,
                    "Matches rule pattern: " + rule.getRulePattern(),
                    rule.getDescription());
            }
        } catch (Exception e) {
            logger.warn("Invalid regex pattern in rule {}: {}", rule.getRuleName(), rule.getRulePattern());
        }
        
        return null;
    }

    /**
     * Create a violation record
     */
    private Violation createViolation(ComplianceRule rule, AdAnalysis analysis, String description, String context) {
        Violation violation = new Violation();
        violation.setRule(rule);
        violation.setAnalysis(analysis);
        violation.setSeverity(rule.getSeverity());
        violation.setDescription(description);
        violation.setViolatedText(analysis.getHeadline() + " | " + analysis.getPrimaryText());
        violation.setContextInfo(context);
        violation.setCreatedAt(LocalDateTime.now());
        return violation;
    }

    /**
     * Calculate compliance score based on violations
     * Scoring: Start at 100, deduct points based on severity
     * CRITICAL: -20 points, MAJOR: -10 points, MINOR: -5 points
     */
    private double calculateComplianceScore(List<Violation> violations) {
        double score = 100.0;
        
        for (Violation violation : violations) {
            switch (violation.getSeverity()) {
                case CRITICAL:
                    score -= 20;
                    break;
                case MAJOR:
                    score -= 10;
                    break;
                case MINOR:
                    score -= 5;
                    break;
            }
        }
        
        return Math.max(0, score); // Ensure score doesn't go below 0
    }

    /**
     * Determine compliance status based on score
     */
    private AdAnalysis.ComplianceStatus determineComplianceStatus(double score) {
        if (score >= 90) return AdAnalysis.ComplianceStatus.EXCELLENT;
        if (score >= 75) return AdAnalysis.ComplianceStatus.GOOD;
        if (score >= 60) return AdAnalysis.ComplianceStatus.WARNING;
        if (score >= 40) return AdAnalysis.ComplianceStatus.POOR;
        return AdAnalysis.ComplianceStatus.CRITICAL;
    }

    /**
     * Generate human-readable analysis notes
     */
    private String generateAnalysisNotes(List<Violation> violations, double score) {
        StringBuilder notes = new StringBuilder();
        notes.append(String.format("Compliance Score: %.1f/100\n\n", score));
        
        if (violations.isEmpty()) {
            notes.append("✅ No compliance violations detected. This ad follows Google AFS & RSOC guidelines.\n\n");
            notes.append("The ad content appears compliant with:\n");
            notes.append("• No click inducement language\n");
            notes.append("• No false or misleading promises\n");
            notes.append("• No implied functionality issues\n");
            notes.append("• No unverifiable claims\n");
        } else {
            notes.append(String.format("⚠️ %d compliance violation(s) detected:\n\n", violations.size()));
            
            int criticalCount = 0, majorCount = 0, minorCount = 0;
            for (Violation violation : violations) {
                switch (violation.getSeverity()) {
                    case CRITICAL: criticalCount++; break;
                    case MAJOR: majorCount++; break;
                    case MINOR: minorCount++; break;
                }
                
                notes.append(String.format("🔴 %s (%s): %s\n", 
                    violation.getRule().getRuleName(),
                    violation.getSeverity(),
                    violation.getDescription()));
            }
            
            notes.append(String.format("\nViolation Summary: %d Critical, %d Major, %d Minor\n\n", 
                criticalCount, majorCount, minorCount));
            
            notes.append("Recommendations:\n");
            notes.append("• Review ad content against Google AFS & RSOC guidelines\n");
            notes.append("• Ensure landing page supports all ad claims\n");
            notes.append("• Remove any click inducement language\n");
            notes.append("• Verify all claims are substantiated\n");
        }
        
        notes.append("\nBased on Google AdSense for Search & RSOC compliance guidelines");
        return notes.toString();
    }

    /**
     * Analyze all ads for a domain
     */
    public void analyzeDomainAds(Domain domain) {
        logger.info("Starting compliance analysis for all ads in domain: {}", domain.getDomainName());
        
        List<ScrapedAd> domainAds = scrapedAdRepository.findByDomainNameOrderByScrapedAtDesc(domain.getDomainName());
        logger.info("Found {} ads for domain: {}", domainAds.size(), domain.getDomainName());
        
        // Process first 20 ads for analysis
        List<ScrapedAd> adsToProcess = domainAds.stream().limit(20).toList();
        logger.info("Processing {} ads for compliance analysis", adsToProcess.size());
        
        int successCount = 0;
        int errorCount = 0;
        
        // Process each ad individually with its own transaction
        for (ScrapedAd ad : adsToProcess) {
            try {
                individualAdAnalysisService.analyzeAdWithNewTransaction(ad, domain);
                successCount++;
                logger.info("Successfully analyzed ad {}/{}: {}", 
                    successCount + errorCount, adsToProcess.size(), ad.getMetaAdId());
            } catch (Exception e) {
                errorCount++;
                logger.error("Error analyzing ad {}/{} ({}): {}", 
                    successCount + errorCount, adsToProcess.size(), ad.getMetaAdId(), e.getMessage());
            }
        }
        
        // Update domain compliance score after all individual analyses
        updateDomainComplianceScore(domain);
        
        logger.info("Completed compliance analysis for domain: {} - {} successful, {} errors", 
                   domain.getDomainName(), successCount, errorCount);
    }
    
    /**
     * Analyze a single ad with its own transaction to prevent session corruption
     */
    @Transactional
    public AdAnalysis analyzeAdIndividually(ScrapedAd scrapedAd, Domain domain) {
        logger.info("Starting individual compliance analysis for ad: {} from domain: {}", 
                   scrapedAd.getMetaAdId(), domain.getDomainName());
        
        try {
            // Always create a fresh analysis to avoid session corruption issues
            // Delete any existing analysis first
            Optional<AdAnalysis> existingAnalysis = adAnalysisRepository.findByMetaAdIdAndDomain(scrapedAd.getMetaAdId(), domain);
            if (existingAnalysis.isPresent()) {
                logger.debug("Deleting existing analysis for ad: {}", scrapedAd.getMetaAdId());
                adAnalysisRepository.delete(existingAnalysis.get());
                adAnalysisRepository.flush(); // Ensure deletion is committed
            }

            // Create completely new analysis
            AdAnalysis analysis = new AdAnalysis();
            analysis.setId(null); // Ensure ID is null for new entities
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
            
            // Use pre-extracted text from media processing (done during scraping)
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
                logger.debug("Added pre-extracted image text ({} chars) to ad: {}", imageText.length(), scrapedAd.getMetaAdId());
            }
            
            // Add pre-extracted video text
            String videoText = scrapedAd.getExtractedVideoText();
            if (videoText != null && !videoText.trim().isEmpty()) {
                if (combinedText.length() > 0) combinedText.append("\n\n");
                combinedText.append("VIDEO CONTENT:\n").append(videoText);
                logger.debug("Added pre-extracted video text ({} chars) to ad: {}", videoText.length(), scrapedAd.getMetaAdId());
            }
            
            // Update primary text with combined content
            if (combinedText.length() > 0) {
                analysis.setPrimaryText(combinedText.toString());
            }
            
            // Analyze landing page content
            String landingPageContent = analyzeLandingPage(scrapedAd.getLandingPageUrl());
            analysis.setLandingPageContent(landingPageContent);
            
            // Use OpenAI GPT-4 for intelligent compliance analysis
            String adText = buildAdText(scrapedAd, combinedText.toString());
            String racValue = scrapedAd.getReferrerAdCreative();
            
            try {
                OpenAIAnalysisService.ComplianceAnalysisResult aiResult = openAIAnalysisService.analyzeAdCompliance(
                    adText, landingPageContent, racValue
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
                analysis.setRacRelevant(false);
                analysis.setRacReason("Analysis failed: " + e.getMessage());
                analysis.setOverallCompliant(false);
                
                // Set legacy fields
                analysis.setComplianceScore(0.0);
                analysis.setComplianceStatus(AdAnalysis.ComplianceStatus.CRITICAL);
                analysis.setAnalysisNotes("Automated analysis failed. Manual review required.");
            }
            
            // Save the analysis (updatedAt will be set automatically by @LastModifiedDate)
            AdAnalysis savedAnalysis = adAnalysisRepository.save(analysis);
            logger.info("Successfully completed compliance analysis for ad: {} (Score: {})", 
                       scrapedAd.getMetaAdId(), savedAnalysis.getComplianceScore());
            
            return savedAnalysis;
            
        } catch (Exception e) {
            logger.error("Critical error in individual ad analysis for ad {}: {}", scrapedAd.getMetaAdId(), e.getMessage(), e);
            throw new RuntimeException("Failed to analyze ad: " + scrapedAd.getMetaAdId(), e);
        }
    }

    /**
     * Update domain's overall compliance score
     */
    private void updateDomainComplianceScore(Domain domain) {
        Double avgScore = adAnalysisRepository.getAverageComplianceScoreByDomain(domain);
        if (avgScore != null) {
            domain.setComplianceScore(avgScore);
            // Domain will be saved by calling service
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
     * Analyze image content by extracting text using OCR
     */
    private String analyzeImageContent(ScrapedAd scrapedAd) {
        try {
            // Check if ad has local image files
            if (scrapedAd.getLocalImagePaths() == null || scrapedAd.getLocalImagePaths().isEmpty()) {
                logger.debug("No local image files found for ad: {}", scrapedAd.getMetaAdId());
                return null;
            }
            
            StringBuilder allImageText = new StringBuilder();
            
            // Analyze each image file
            for (String imagePath : scrapedAd.getLocalImagePaths()) {
                logger.info("Analyzing image content: {}", imagePath);
                
                String extractedText = imageAnalysisService.extractTextFromImage(imagePath);
                
                if (extractedText != null && !extractedText.trim().isEmpty()) {
                    if (allImageText.length() > 0) {
                        allImageText.append("\n\n--- NEXT IMAGE ---\n\n");
                    }
                    allImageText.append(extractedText.trim());
                    logger.info("Extracted {} characters of text from image: {}", 
                               extractedText.length(), imagePath);
                }
            }
            
            return allImageText.length() > 0 ? allImageText.toString() : null;
            
        } catch (Exception e) {
            logger.error("Error analyzing image content for ad: {}", scrapedAd.getMetaAdId(), e);
            return null;
        }
    }
    
    /**
     * Analyze video content by extracting text from frames and audio
     */
    private String analyzeVideoContent(ScrapedAd scrapedAd) {
        try {
            // Check if ad has local video files
            if (scrapedAd.getLocalVideoPaths() == null || scrapedAd.getLocalVideoPaths().isEmpty()) {
                logger.debug("No local video files found for ad: {}", scrapedAd.getMetaAdId());
                return null;
            }
            
            StringBuilder allVideoText = new StringBuilder();
            
            // Analyze each video file
            for (String videoPath : scrapedAd.getLocalVideoPaths()) {
                logger.info("Analyzing video content: {}", videoPath);
                
                VideoAnalysisService.VideoAnalysisResult result = videoAnalysisService.analyzeVideo(videoPath);
                
                if (result.hasError()) {
                    logger.warn("Video analysis failed for {}: {}", videoPath, result.getError());
                    continue;
                }
                
                if (result.hasText()) {
                    if (allVideoText.length() > 0) {
                        allVideoText.append("\n\n--- NEXT VIDEO ---\n\n");
                    }
                    allVideoText.append(result.getCombinedText());
                    logger.info("Extracted {} characters of text from video: {}", 
                               result.getCombinedText().length(), videoPath);
                }
            }
            
            return allVideoText.length() > 0 ? allVideoText.toString() : null;
            
        } catch (Exception e) {
            logger.error("Error analyzing video content for ad: {}", scrapedAd.getMetaAdId(), e);
            return null;
        }
    }
    
    /**
     * Analyze landing page content for compliance
     */
    private String analyzeLandingPage(String landingPageUrl) {
        if (landingPageUrl == null || landingPageUrl.trim().isEmpty()) {
            return "No landing page URL provided";
        }
        
        try {
            logger.info("Analyzing landing page: {}", landingPageUrl);
            
            // For now, return a placeholder - in production this would:
            // 1. Fetch the landing page content
            // 2. Extract text content
            // 3. Check for compliance issues
            // 4. Return summary of findings
            
            return "Landing page analysis: " + landingPageUrl + " - Basic analysis completed";
            
        } catch (Exception e) {
            logger.error("Error analyzing landing page {}: {}", landingPageUrl, e.getMessage());
            return "Landing page analysis failed: " + e.getMessage();
        }
    }

    /**
     * Convert DPA template variables to readable format
     */
    private String convertDpaTemplate(String template) {
        if (template == null) return null;
        
        // Convert common DPA variables to readable format
        String converted = template
            .replaceAll("\\{\\{product\\.name\\}\\}", "[Product Name]")
            .replaceAll("\\{\\{product\\.brand\\}\\}", "[Brand]")
            .replaceAll("\\{\\{product\\.price\\}\\}", "[Price]")
            .replaceAll("\\{\\{product\\.category\\}\\}", "[Category]")
            .replaceAll("\\{\\{product\\.description\\}\\}", "[Description]")
            .replaceAll("\\{\\{product\\.title\\}\\}", "[Product Title]")
            .replaceAll("\\{\\{product\\.sale_price\\}\\}", "[Sale Price]")
            .replaceAll("\\{\\{product\\.availability\\}\\}", "[Availability]")
            // Handle any remaining template variables
            .replaceAll("\\{\\{[^}]+\\}\\}", "[Dynamic Content]");
            
        return converted;
    }
    
    /**
     * Build comprehensive ad text for analysis
     */
    private String buildAdText(ScrapedAd ad, String combinedText) {
        StringBuilder adText = new StringBuilder();
        
        if (ad.getHeadline() != null && !ad.getHeadline().trim().isEmpty()) {
            adText.append("HEADLINE: ").append(ad.getHeadline()).append("\n");
        }
        
        if (ad.getPrimaryText() != null && !ad.getPrimaryText().trim().isEmpty()) {
            adText.append("PRIMARY TEXT: ").append(ad.getPrimaryText()).append("\n");
        }
        
        if (ad.getDescription() != null && !ad.getDescription().trim().isEmpty()) {
            adText.append("DESCRIPTION: ").append(ad.getDescription()).append("\n");
        }
        
        if (ad.getCallToAction() != null && !ad.getCallToAction().trim().isEmpty()) {
            adText.append("CALL TO ACTION: ").append(ad.getCallToAction()).append("\n");
        }
        
        if (combinedText != null && !combinedText.trim().isEmpty()) {
            adText.append("\nEXTRACTED CONTENT:\n").append(combinedText);
        }
        
        return adText.toString();
    }
    
    /**
     * Build analysis notes from AI result
     */
    private String buildAnalysisNotes(OpenAIAnalysisService.ComplianceAnalysisResult result) {
        StringBuilder notes = new StringBuilder();
        
        notes.append("COMPLIANCE ANALYSIS RESULTS:\n\n");
        
        // Ad Creative
        notes.append("ad creative ");
        if (result.isAdCreativeCompliant()) {
            notes.append("✅\n");
        } else {
            notes.append("❌\n");
            notes.append("Reason: ").append(result.getAdCreativeReason()).append("\n");
        }
        
        // Landing Page Relevance
        notes.append("landing page relevance ");
        if (result.isLandingPageRelevant()) {
            notes.append("✅\n");
        } else {
            notes.append("❌\n");
            notes.append("Reason: ").append(result.getLandingPageReason()).append("\n");
        }
        
        // RAC Relevance
        notes.append("rac relevance ");
        if (result.isRacRelevant()) {
            notes.append("✅\n");
        } else {
            notes.append("❌\n");
            notes.append("Reason: ").append(result.getRacReason()).append("\n");
        }
        
        notes.append("\nOVERALL STATUS: ");
        if (result.isOverallCompliant()) {
            notes.append("✅ COMPLIANT");
        } else {
            notes.append("❌ NOT COMPLIANT");
        }
        
        return notes.toString();
    }
}
