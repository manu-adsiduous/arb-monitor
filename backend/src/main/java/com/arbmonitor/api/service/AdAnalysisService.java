package com.arbmonitor.api.service;

import com.arbmonitor.api.dto.MetaAdDTO;
import com.arbmonitor.api.model.AdAnalysis;
import com.arbmonitor.api.model.ComplianceRule;
import com.arbmonitor.api.model.Domain;
import com.arbmonitor.api.model.User;
import com.arbmonitor.api.model.Violation;
import com.arbmonitor.api.repository.AdAnalysisRepository;
import com.arbmonitor.api.repository.ComplianceRuleRepository;
import com.arbmonitor.api.repository.ViolationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Service for analyzing ads and checking compliance
 */
@Service
@Transactional
public class AdAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(AdAnalysisService.class);
    
    private final AdAnalysisRepository adAnalysisRepository;
    private final ComplianceRuleRepository complianceRuleRepository;
    private final ViolationRepository violationRepository;
    private final MetaApiService metaApiService;
    
    public AdAnalysisService(
            AdAnalysisRepository adAnalysisRepository,
            ComplianceRuleRepository complianceRuleRepository,
            ViolationRepository violationRepository,
            MetaApiService metaApiService) {
        this.adAnalysisRepository = adAnalysisRepository;
        this.complianceRuleRepository = complianceRuleRepository;
        this.violationRepository = violationRepository;
        this.metaApiService = metaApiService;
    }
    
    /**
     * Analyzes ads for a domain using the user's Meta API key
     */
    public Mono<Integer> analyzeAdsForDomain(Domain domain, User user) {
        if (user.getMetaApiKey() == null || user.getMetaApiKey().isEmpty()) {
            logger.warn("User {} does not have a Meta API key configured", user.getId());
            return Mono.just(0);
        }
        
        logger.info("Starting ad analysis for domain: {} (ID: {})", domain.getDomainName(), domain.getId());
        
        return metaApiService.fetchAdsForDomain(user.getMetaApiKey(), domain.getDomainName())
            .flatMap(ads -> {
                if (ads.isEmpty()) {
                    logger.info("No ads found for domain: {}", domain.getDomainName());
                    return Mono.just(0);
                }
                
                logger.info("Found {} ads for domain: {}, starting analysis", ads.size(), domain.getDomainName());
                
                // Process each ad
                int processedCount = 0;
                for (MetaAdDTO metaAd : ads) {
                    try {
                        AdAnalysis analysis = processMetaAd(metaAd, domain);
                        if (analysis != null) {
                            processedCount++;
                        }
                    } catch (Exception e) {
                        logger.error("Error processing ad {}: {}", metaAd.getId(), e.getMessage());
                    }
                }
                
                // Update domain statistics
                updateDomainStatistics(domain);
                
                logger.info("Completed ad analysis for domain: {}, processed {} ads", domain.getDomainName(), processedCount);
                return Mono.just(processedCount);
            })
            .onErrorResume(error -> {
                logger.error("Error analyzing ads for domain: {}", domain.getDomainName(), error);
                return Mono.just(0);
            });
    }
    
    /**
     * Processes a single Meta ad and creates an AdAnalysis record
     */
    private AdAnalysis processMetaAd(MetaAdDTO metaAd, Domain domain) {
        // Check if we already have this ad
        Optional<AdAnalysis> existingAnalysis = adAnalysisRepository.findByMetaAdIdAndDomain(metaAd.getId(), domain);
        
        AdAnalysis analysis;
        if (existingAnalysis.isPresent()) {
            analysis = existingAnalysis.get();
            logger.debug("Updating existing analysis for ad: {}", metaAd.getId());
        } else {
            analysis = new AdAnalysis();
            analysis.setMetaAdId(metaAd.getId());
            analysis.setDomain(domain);
            logger.debug("Creating new analysis for ad: {}", metaAd.getId());
        }
        
        // Extract ad content
        analysis.setHeadline(metaAd.getHeadline() != null ? metaAd.getHeadline() : "");
        analysis.setPrimaryText(metaAd.getPrimaryText() != null ? metaAd.getPrimaryText() : "");
        analysis.setImageUrl(metaAd.getImageUrl());
        
        // Parse creation time
        if (metaAd.getAdCreationTime() != null) {
            try {
                // Meta API typically returns ISO format: "2024-01-15T10:30:00+0000"
                LocalDateTime creationTime = LocalDateTime.parse(
                    metaAd.getAdCreationTime().replace("+0000", ""),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
                );
                // Note: We're storing this in our own createdAt field, not a separate field
            } catch (DateTimeParseException e) {
                logger.warn("Could not parse ad creation time: {}", metaAd.getAdCreationTime());
            }
        }
        
        // Analyze compliance
        analyzeCompliance(analysis);
        
        // Save the analysis
        analysis = adAnalysisRepository.save(analysis);
        
        return analysis;
    }
    
    /**
     * Analyzes compliance for an ad and creates violation records
     */
    private void analyzeCompliance(AdAnalysis analysis) {
        List<ComplianceRule> activeRules = complianceRuleRepository.findByActiveTrue();
        
        if (activeRules.isEmpty()) {
            logger.warn("No active compliance rules found, creating default rules");
            createDefaultComplianceRules();
            activeRules = complianceRuleRepository.findByActiveTrue();
        }
        
        int totalViolations = 0;
        int criticalViolations = 0;
        
        // Check each rule
        for (ComplianceRule rule : activeRules) {
            List<String> violations = checkRuleViolations(analysis, rule);
            
            for (String violatedText : violations) {
                Violation violation = new Violation();
                violation.setAnalysis(analysis);
                violation.setRule(rule);
                violation.setSeverity(rule.getSeverity());
                violation.setDescription(generateViolationDescription(rule, violatedText));
                violation.setViolatedText(violatedText);
                
                violationRepository.save(violation);
                
                totalViolations++;
                if (rule.getSeverity() == ComplianceRule.RuleSeverity.CRITICAL) {
                    criticalViolations++;
                }
            }
        }
        
        // Calculate compliance score and status
        double complianceScore = calculateComplianceScore(totalViolations, criticalViolations, activeRules.size());
        analysis.setComplianceScore(complianceScore);
        analysis.setComplianceStatus(determineComplianceStatus(complianceScore, criticalViolations));
        
        logger.debug("Ad {} compliance analysis: Score={}, Violations={}, Critical={}", 
            analysis.getMetaAdId(), complianceScore, totalViolations, criticalViolations);
    }
    
    /**
     * Checks if an ad violates a specific compliance rule
     */
    private List<String> checkRuleViolations(AdAnalysis analysis, ComplianceRule rule) {
        java.util.List<String> violations = new java.util.ArrayList<>();
        
        String rulePattern = rule.getRulePattern();
        if (rulePattern == null || rulePattern.isEmpty()) {
            return violations;
        }
        
        try {
            Pattern pattern = Pattern.compile(rulePattern, Pattern.CASE_INSENSITIVE);
            
            // Check headline
            if (analysis.getHeadline() != null && pattern.matcher(analysis.getHeadline()).find()) {
                violations.add(analysis.getHeadline());
            }
            
            // Check primary text
            if (analysis.getPrimaryText() != null && pattern.matcher(analysis.getPrimaryText()).find()) {
                violations.add(analysis.getPrimaryText());
            }
            
            // Check image text if available
            if (analysis.getImageText() != null && pattern.matcher(analysis.getImageText()).find()) {
                violations.add(analysis.getImageText());
            }
            
        } catch (Exception e) {
            logger.warn("Error checking rule pattern '{}': {}", rulePattern, e.getMessage());
        }
        
        return violations;
    }
    
    /**
     * Calculates compliance score based on violations
     */
    private double calculateComplianceScore(int totalViolations, int criticalViolations, int totalRules) {
        if (totalRules == 0) return 100.0;
        
        // Heavy penalty for critical violations
        double penalty = (criticalViolations * 25.0) + (totalViolations * 5.0);
        double score = Math.max(0.0, 100.0 - penalty);
        
        return Math.round(score * 100.0) / 100.0; // Round to 2 decimal places
    }
    
    /**
     * Determines compliance status based on score and violations
     */
    private AdAnalysis.ComplianceStatus determineComplianceStatus(double score, int criticalViolations) {
        if (criticalViolations > 0) {
            return AdAnalysis.ComplianceStatus.CRITICAL;
        } else if (score >= 90) {
            return AdAnalysis.ComplianceStatus.EXCELLENT;
        } else if (score >= 75) {
            return AdAnalysis.ComplianceStatus.GOOD;
        } else if (score >= 60) {
            return AdAnalysis.ComplianceStatus.WARNING;
        } else {
            return AdAnalysis.ComplianceStatus.POOR;
        }
    }
    
    /**
     * Generates a human-readable violation description
     */
    private String generateViolationDescription(ComplianceRule rule, String violatedText) {
        return String.format("Violation of rule '%s': Found potentially non-compliant content", rule.getRuleName());
    }
    
    /**
     * Updates domain-level statistics after analysis
     */
    private void updateDomainStatistics(Domain domain) {
        long totalAds = adAnalysisRepository.countByDomain(domain);
        
        if (totalAds > 0) {
            // Calculate average compliance score
            List<AdAnalysis> analyses = adAnalysisRepository.findByDomainOrderByCreatedAtDesc(domain);
            
            double averageScore = analyses.stream()
                .filter(a -> a.getComplianceScore() != null)
                .mapToDouble(AdAnalysis::getComplianceScore)
                .average()
                .orElse(0.0);
            
            domain.setComplianceScore(averageScore);
            domain.setLastChecked(LocalDateTime.now());
        }
    }
    
    /**
     * Creates default compliance rules if none exist
     */
    private void createDefaultComplianceRules() {
        logger.info("Creating default compliance rules");
        
        // Rule 1: Suspicious claims
        ComplianceRule rule1 = new ComplianceRule();
        rule1.setRuleName("Suspicious Claims");
        rule1.setCategory(ComplianceRule.RuleCategory.CREATIVE_CONTENT);
        rule1.setSeverity(ComplianceRule.RuleSeverity.CRITICAL);
        rule1.setDescription("Detects potentially misleading or exaggerated claims");
        rule1.setRulePattern("(?i)(guaranteed|100%|instant|miracle|secret|amazing results|doctors hate|one weird trick)");
        rule1.setActive(true);
        complianceRuleRepository.save(rule1);
        
        // Rule 2: Financial claims
        ComplianceRule rule2 = new ComplianceRule();
        rule2.setRuleName("Unsubstantiated Financial Claims");
        rule2.setCategory(ComplianceRule.RuleCategory.FINANCIAL_CLAIMS);
        rule2.setSeverity(ComplianceRule.RuleSeverity.MAJOR);
        rule2.setDescription("Detects unsubstantiated financial or investment claims");
        rule2.setRulePattern("(?i)(make money|get rich|passive income|guaranteed profit|risk-free|double your money)");
        rule2.setActive(true);
        complianceRuleRepository.save(rule2);
        
        // Rule 3: Health claims
        ComplianceRule rule3 = new ComplianceRule();
        rule3.setRuleName("Unsubstantiated Health Claims");
        rule3.setCategory(ComplianceRule.RuleCategory.MEDICAL_CLAIMS);
        rule3.setSeverity(ComplianceRule.RuleSeverity.CRITICAL);
        rule3.setDescription("Detects potentially false or misleading health claims");
        rule3.setRulePattern("(?i)(cure|heal|lose weight fast|burn fat|anti-aging|fountain of youth|medical breakthrough)");
        rule3.setActive(true);
        complianceRuleRepository.save(rule3);
        
        logger.info("Created {} default compliance rules", 3);
    }
}



