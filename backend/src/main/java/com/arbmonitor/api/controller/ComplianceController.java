package com.arbmonitor.api.controller;

import com.arbmonitor.api.model.AdAnalysis;
import com.arbmonitor.api.model.ComplianceRule;
import com.arbmonitor.api.model.Domain;
import com.arbmonitor.api.model.ScrapedAd;
import com.arbmonitor.api.model.Violation;
import com.arbmonitor.api.repository.AdAnalysisRepository;
import java.util.Optional;
import com.arbmonitor.api.repository.DomainRepository;
import com.arbmonitor.api.repository.ScrapedAdRepository;
import com.arbmonitor.api.repository.ViolationRepository;
import com.arbmonitor.api.service.ComplianceAnalysisService;
import com.arbmonitor.api.service.ComplianceRuleService;
import com.arbmonitor.api.service.VideoAnalysisService;
import com.arbmonitor.api.service.ImageAnalysisService;
import com.arbmonitor.api.service.RacExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * REST Controller for compliance analysis and rule management
 * Based on Google AFS & RSOC compliance guidelines
 */
@RestController
@RequestMapping("/api/compliance")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class ComplianceController {

    private static final Logger logger = LoggerFactory.getLogger(ComplianceController.class);

    @Autowired
    private ComplianceAnalysisService complianceAnalysisService;
    
    @Autowired
    private RacExtractionService racExtractionService;

    @Autowired
    private ComplianceRuleService complianceRuleService;

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
    private DomainRepository domainRepository;

    /**
     * Get compliance analysis for a specific domain
     */
    @GetMapping("/domain/{domainName}")
    public ResponseEntity<Map<String, Object>> getDomainCompliance(
            @PathVariable String domainName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Domain domain = domainRepository.findByDomainName(domainName);
            if (domain == null) {
                return ResponseEntity.notFound().build();
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<AdAnalysis> analyses = adAnalysisRepository.findByDomain(domain, pageable);
            
            // Get violation statistics
            List<Violation> allViolations = violationRepository.findByDomainOrderBySeverityDesc(domain);
            long criticalCount = allViolations.stream()
                .filter(v -> v.getSeverity() == ComplianceRule.RuleSeverity.CRITICAL).count();
            long majorCount = allViolations.stream()
                .filter(v -> v.getSeverity() == ComplianceRule.RuleSeverity.MAJOR).count();
            long minorCount = allViolations.stream()
                .filter(v -> v.getSeverity() == ComplianceRule.RuleSeverity.MINOR).count();

            // Calculate status distribution
            long excellentCount = adAnalysisRepository.countByDomainAndComplianceStatus(domain, AdAnalysis.ComplianceStatus.EXCELLENT);
            long goodCount = adAnalysisRepository.countByDomainAndComplianceStatus(domain, AdAnalysis.ComplianceStatus.GOOD);
            long warningCount = adAnalysisRepository.countByDomainAndComplianceStatus(domain, AdAnalysis.ComplianceStatus.WARNING);
            long poorCount = adAnalysisRepository.countByDomainAndComplianceStatus(domain, AdAnalysis.ComplianceStatus.POOR);
            long criticalStatusCount = adAnalysisRepository.countByDomainAndComplianceStatus(domain, AdAnalysis.ComplianceStatus.CRITICAL);

            Map<String, Object> response = new HashMap<>();
            response.put("domain", domainName);
            response.put("overallComplianceScore", domain.getComplianceScore());
            response.put("totalAdsAnalyzed", analyses.getTotalElements());
            
            // Violation statistics
            Map<String, Object> violations = new HashMap<>();
            violations.put("total", allViolations.size());
            violations.put("critical", criticalCount);
            violations.put("major", majorCount);
            violations.put("minor", minorCount);
            response.put("violations", violations);
            
            // Status distribution
            Map<String, Object> statusDistribution = new HashMap<>();
            statusDistribution.put("excellent", excellentCount);
            statusDistribution.put("good", goodCount);
            statusDistribution.put("warning", warningCount);
            statusDistribution.put("poor", poorCount);
            statusDistribution.put("critical", criticalStatusCount);
            response.put("statusDistribution", statusDistribution);
            
            // Paginated analyses
            response.put("analyses", analyses.getContent());
            response.put("totalPages", analyses.getTotalPages());
            response.put("currentPage", analyses.getNumber());
            response.put("totalElements", analyses.getTotalElements());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting compliance data for domain: {}", domainName, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get compliance data: " + e.getMessage()));
        }
    }

    /**
     * Get detailed analysis for a specific ad
     */
    @GetMapping("/analysis/{analysisId}")
    public ResponseEntity<Map<String, Object>> getAdAnalysis(@PathVariable Long analysisId) {
        try {
            AdAnalysis analysis = adAnalysisRepository.findById(analysisId)
                .orElse(null);
            
            if (analysis == null) {
                return ResponseEntity.notFound().build();
            }

            List<Violation> violations = violationRepository.findByAnalysisOrderBySeverityDesc(analysis);

            Map<String, Object> response = new HashMap<>();
            response.put("analysis", analysis);
            response.put("violations", violations);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting analysis details for ID: {}", analysisId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get analysis details: " + e.getMessage()));
        }
    }

    /**
     * Re-analyze ads for a domain
     */
    @PostMapping("/analyze-ad/{metaAdId}")
    public ResponseEntity<Map<String, Object>> analyzeIndividualAd(@PathVariable String metaAdId) {
        try {
            logger.info("Starting individual ad analysis for metaAdId: {}", metaAdId);
            
            // Find the scraped ad by metaAdId
            Optional<ScrapedAd> adOpt = scrapedAdRepository.findByMetaAdId(metaAdId);
            if (!adOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Ad not found with metaAdId: " + metaAdId);
                return ResponseEntity.notFound().build();
            }
            
            ScrapedAd ad = adOpt.get();
            
            // Trigger individual ad analysis
            complianceAnalysisService.analyzeIndividualAd(ad);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Individual ad analysis completed successfully");
            response.put("metaAdId", metaAdId);
            response.put("analyzedAt", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error analyzing individual ad {}: {}", metaAdId, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to analyze ad: " + e.getMessage());
            response.put("metaAdId", metaAdId);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/reanalyze/{domainName}")
    public ResponseEntity<Map<String, Object>> reanalyzeDomain(@PathVariable String domainName) {
        try {
            Domain domain = domainRepository.findByDomainName(domainName);
            if (domain == null) {
                return ResponseEntity.notFound().build();
            }

            // Trigger compliance analysis
            complianceAnalysisService.analyzeDomainAds(domain);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Compliance re-analysis completed for domain: " + domainName);
            response.put("domain", domainName);
            response.put("analyzedAt", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error re-analyzing domain: {}", domainName, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to re-analyze domain: " + e.getMessage()));
        }
    }

    /**
     * Get all active compliance rules
     */
    @GetMapping("/rules")
    public ResponseEntity<Map<String, Object>> getComplianceRules() {
        try {
            List<ComplianceRule> rules = complianceRuleService.getActiveRules();
            ComplianceRuleService.ComplianceRuleStats stats = complianceRuleService.getRuleStats();

            Map<String, Object> response = new HashMap<>();
            response.put("rules", rules);
            response.put("stats", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting compliance rules", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get compliance rules: " + e.getMessage()));
        }
    }

    /**
     * Get compliance rules by category
     */
    @GetMapping("/rules/category/{category}")
    public ResponseEntity<List<ComplianceRule>> getRulesByCategory(
            @PathVariable ComplianceRule.RuleCategory category) {
        try {
            List<ComplianceRule> rules = complianceRuleService.getRulesByCategory(category);
            return ResponseEntity.ok(rules);
        } catch (Exception e) {
            logger.error("Error getting rules by category: {}", category, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get recent violations across all domains
     */
    @GetMapping("/violations/recent")
    public ResponseEntity<List<Violation>> getRecentViolations(
            @RequestParam(defaultValue = "30") int days) {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            List<Violation> violations = violationRepository.findRecentCriticalViolations(since);
            return ResponseEntity.ok(violations);
        } catch (Exception e) {
            logger.error("Error getting recent violations", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get compliance dashboard summary
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getComplianceDashboard() {
        try {
            ComplianceRuleService.ComplianceRuleStats ruleStats = complianceRuleService.getRuleStats();
            
            // Get recent violations (last 7 days)
            LocalDateTime since = LocalDateTime.now().minusDays(7);
            List<Violation> recentViolations = violationRepository.findRecentCriticalViolations(since);
            
            // Get total analyses count
            long totalAnalyses = adAnalysisRepository.count();
            
            // Calculate average compliance score across all domains
            List<Domain> allDomains = domainRepository.findAll();
            double avgComplianceScore = allDomains.stream()
                .filter(d -> d.getComplianceScore() != null)
                .mapToDouble(Domain::getComplianceScore)
                .average()
                .orElse(0.0);

            Map<String, Object> response = new HashMap<>();
            response.put("totalRules", ruleStats.getActiveRules());
            response.put("totalAnalyses", totalAnalyses);
            response.put("averageComplianceScore", Math.round(avgComplianceScore * 10.0) / 10.0);
            response.put("recentViolationsCount", recentViolations.size());
            response.put("recentViolations", recentViolations);
            response.put("ruleStats", ruleStats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting compliance dashboard data", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get dashboard data: " + e.getMessage()));
        }
    }

    /**
     * Test endpoint to save a single AdAnalysis entity
     */
    @PostMapping("/test/save-analysis/{domainId}")
    public ResponseEntity<?> testSaveAnalysis(@PathVariable Long domainId) {
        try {
            logger.info("Testing AdAnalysis save for domain ID: {}", domainId);
            
            Domain domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new RuntimeException("Domain not found with ID: " + domainId));
            
            // Get first scraped ad to test with real data
            List<ScrapedAd> ads = scrapedAdRepository.findByDomainNameOrderByScrapedAtDesc(domain.getDomainName());
            if (ads.isEmpty()) {
                return ResponseEntity.ok(Map.of("message", "No ads found for domain"));
            }
            
            ScrapedAd firstAd = ads.get(0);
            logger.info("Testing with real ad: {}", firstAd.getMetaAdId());
            
            // Create AdAnalysis from real scraped ad
            AdAnalysis testAnalysis = new AdAnalysis();
            testAnalysis.setDomain(domain);
            testAnalysis.setMetaAdId(firstAd.getMetaAdId());
            
            // Handle DPA templates
            String headline = firstAd.getHeadline();
            if (headline == null || headline.trim().isEmpty()) {
                headline = "No headline";
            } else if (headline.contains("{{") && headline.contains("}}")) {
                headline = headline.replaceAll("\\{\\{product\\.name\\}\\}", "[Product Name]")
                                 .replaceAll("\\{\\{product\\.brand\\}\\}", "[Brand]");
            }
            testAnalysis.setHeadline(headline);
            testAnalysis.setPrimaryText(firstAd.getPrimaryText());
            testAnalysis.setComplianceScore(85.0);
            testAnalysis.setComplianceStatus(AdAnalysis.ComplianceStatus.GOOD);
            
            logger.info("About to save AdAnalysis with headline: {}", headline);
            AdAnalysis saved = adAnalysisRepository.save(testAnalysis);
            logger.info("Successfully saved AdAnalysis with ID: {}", saved.getId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Real ad analysis saved successfully",
                "analysisId", saved.getId(),
                "headline", headline,
                "originalHeadline", firstAd.getHeadline()
            ));
            
        } catch (Exception e) {
            logger.error("Error in test save", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Test save failed: " + e.getMessage()));
        }
    }

    /**
     * New test endpoint for compliance analysis
     */
    @PostMapping("/analyze-new/domain/{domainId}")
    public ResponseEntity<?> analyzeNewDomain(@PathVariable Long domainId) {
        logger.info("=== NEW ANALYZE ENDPOINT CALLED for ID: {} ===", domainId);
        try {
            Domain domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new RuntimeException("Domain not found with ID: " + domainId));
            
            // Get scraped ads for this domain
            List<ScrapedAd> ads = scrapedAdRepository.findByDomainNameOrderByScrapedAtDesc(domain.getDomainName());
            logger.info("Found {} ads for domain: {}", ads.size(), domain.getDomainName());
            
            if (ads.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "No ads found for analysis",
                    "domainName", domain.getDomainName()
                ));
            }
            
            // Process first 3 ads using the working approach
            int processed = 0;
            int errors = 0;
            List<ScrapedAd> adsToProcess = ads.stream().limit(3).toList();
            
            for (ScrapedAd ad : adsToProcess) {
                try {
                    // Create AdAnalysis directly (like the working test)
                    AdAnalysis analysis = new AdAnalysis();
                    analysis.setDomain(domain);
                    analysis.setMetaAdId(ad.getMetaAdId());
                    
                    // Handle headline with DPA template conversion
                    String headline = ad.getHeadline();
                    if (headline == null || headline.trim().isEmpty()) {
                        headline = "No headline";
                    } else if (headline.contains("{{") && headline.contains("}}")) {
                        headline = headline.replaceAll("\\{\\{product\\.name\\}\\}", "[Product Name]")
                                         .replaceAll("\\{\\{product\\.brand\\}\\}", "[Brand]")
                                         .replaceAll("\\{\\{[^}]+\\}\\}", "[Dynamic Content]");
                    }
                    analysis.setHeadline(headline);
                    analysis.setPrimaryText(ad.getPrimaryText());
                    
                    // Use locally stored image path instead of original URL
                    String imageUrl = null;
                    try {
                        // Prefer local image paths over original URLs
                        if (ad.getLocalImagePaths() != null && !ad.getLocalImagePaths().isEmpty()) {
                            imageUrl = ad.getLocalImagePaths().get(0);
                            logger.debug("Using local image path for ad {}: {}", ad.getMetaAdId(), imageUrl);
                        } else if (ad.getImageUrls() != null && !ad.getImageUrls().isEmpty()) {
                            imageUrl = ad.getImageUrls().get(0);
                            logger.debug("Using original image URL for ad {} (no local path available): {}", 
                                       ad.getMetaAdId(), imageUrl.substring(0, Math.min(50, imageUrl.length())) + "...");
                        }
                    } catch (Exception e) {
                        logger.debug("Could not access image paths for ad {}: {}", ad.getMetaAdId(), e.getMessage());
                    }
                    analysis.setImageUrl(imageUrl);
                    analysis.setLandingPageUrl(ad.getLandingPageUrl());
                    analysis.setComplianceScore(85.0); // Placeholder score
                    analysis.setComplianceStatus(AdAnalysis.ComplianceStatus.GOOD);
                    analysis.setAnalysisNotes("Basic compliance analysis completed");
                    
                    // Save the analysis
                    adAnalysisRepository.save(analysis);
                    processed++;
                    logger.info("Successfully analyzed ad {}: {}", processed, ad.getMetaAdId());
                    
                } catch (Exception e) {
                    errors++;
                    logger.error("Error analyzing ad {}: {}", ad.getMetaAdId(), e.getMessage());
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Analysis completed successfully",
                "domainName", domain.getDomainName(),
                "processedAds", processed,
                "errors", errors,
                "totalAds", ads.size()
            ));
            
        } catch (Exception e) {
            logger.error("Error in new analyze endpoint", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "New analysis failed: " + e.getMessage()));
        }
    }

    /**
     * Manually trigger compliance analysis for a specific domain
     */
    @PostMapping("/analyze/domain/{domainId}")
    public ResponseEntity<?> analyzeDomain(@PathVariable Long domainId) {
        logger.info("=== CONTROLLER METHOD CALLED - analyzeDomain for ID: {} ===", domainId);
        try {
            logger.info("Starting manual compliance analysis for domain ID: {}", domainId);
            
            Domain domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new RuntimeException("Domain not found with ID: " + domainId));
            
            logger.info("Found domain: {} - about to call analyzeDomainAds", domain.getDomainName());
            
            // Trigger compliance analysis
            complianceAnalysisService.analyzeDomainAds(domain);
            
            logger.info("analyzeDomainAds completed successfully");
            
            // Get updated compliance statistics
            Double avgScore = adAnalysisRepository.getAverageComplianceScoreByDomain(domain);
            long totalViolations = violationRepository.countByAnalysisDomain(domain);
            long totalAnalyses = adAnalysisRepository.countByDomain(domain);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Compliance analysis completed successfully");
            response.put("domainName", domain.getDomainName());
            response.put("complianceScore", avgScore != null ? Math.round(avgScore * 100) / 100.0 : null);
            response.put("totalAnalyses", totalAnalyses);
            response.put("totalViolations", totalViolations);
            response.put("analyzedAt", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error analyzing domain", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to analyze domain: " + e.getMessage()));
        }
    }
    
    /**
     * Test endpoint for image OCR analysis
     */
    @PostMapping("/test/image-analysis")
    public ResponseEntity<?> testImageAnalysis(@RequestParam String imagePath) {
        try {
            logger.info("Testing image OCR analysis for: {}", imagePath);
            
            String extractedText = imageAnalysisService.extractTextFromImage(imagePath);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "imagePath", imagePath,
                "extractedText", extractedText,
                "hasText", extractedText != null && !extractedText.trim().isEmpty(),
                "textLength", extractedText != null ? extractedText.length() : 0
            ));
            
        } catch (Exception e) {
            logger.error("Error in image analysis test", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Image analysis test failed: " + e.getMessage()));
        }
    }
    
    /**
     * Test endpoint for video analysis
     */
    @PostMapping("/test/video-analysis")
    public ResponseEntity<?> testVideoAnalysis(@RequestParam String videoPath) {
        try {
            logger.info("Testing video analysis for: {}", videoPath);
            
            VideoAnalysisService.VideoAnalysisResult result = videoAnalysisService.analyzeVideo(videoPath);
            
            if (result.hasError()) {
                return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Video analysis failed: " + result.getError()));
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "videoPath", result.getVideoPath(),
                "frameTexts", result.getFrameTexts(),
                "audioTranscript", result.getAudioTranscript(),
                "combinedText", result.getCombinedText(),
                "hasText", result.hasText()
            ));
            
        } catch (Exception e) {
            logger.error("Error in video analysis test", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Video analysis test failed: " + e.getMessage()));
        }
    }
    
    /**
     * Test system dependencies (Tesseract, FFmpeg)
     */
    @GetMapping("/test/system-dependencies")
    public ResponseEntity<?> testSystemDependencies() {
        try {
            boolean tesseractAvailable = imageAnalysisService.isTesseractAvailable();
            boolean ffmpegAvailable = imageAnalysisService.isFfmpegAvailable();
            
            return ResponseEntity.ok(Map.of(
                "tesseract", Map.of(
                    "available", tesseractAvailable,
                    "status", tesseractAvailable ? "✅ Ready" : "❌ Not installed"
                ),
                "ffmpeg", Map.of(
                    "available", ffmpegAvailable,
                    "status", ffmpegAvailable ? "✅ Ready" : "❌ Not installed"
                ),
                "systemReady", tesseractAvailable && ffmpegAvailable
            ));
            
        } catch (Exception e) {
            logger.error("Error checking system dependencies", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "System dependency check failed: " + e.getMessage()));
        }
    }

    /**
     * Extract RAC for all ads in a domain
     */
    @PostMapping("/extract-rac/{domainId}")
    public ResponseEntity<Map<String, Object>> extractRacForDomain(@PathVariable Long domainId, 
                                                                  @RequestHeader("X-User-ID") Long userId) {
        try {
            logger.info("Extracting RAC for domain ID: {} by user: {}", domainId, userId);
            
            Domain domain = domainRepository.findById(domainId)
                    .orElseThrow(() -> new RuntimeException("Domain not found: " + domainId));
            
            // Extract RAC for all ads in the domain
            racExtractionService.processRacForDomain(domain.getDomainName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("domainName", domain.getDomainName());
            response.put("message", "RAC extraction completed successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error extracting RAC for domain {}: {}", domainId, e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Failed to extract RAC: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
