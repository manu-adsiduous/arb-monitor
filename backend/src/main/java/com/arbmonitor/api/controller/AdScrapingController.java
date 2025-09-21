package com.arbmonitor.api.controller;

import com.arbmonitor.api.dto.ScrapedAdWithAnalysisDTO;
import com.arbmonitor.api.model.AdAnalysis;
import com.arbmonitor.api.model.ScrapedAd;
import com.arbmonitor.api.repository.AdAnalysisRepository;
import com.arbmonitor.api.repository.ScrapedAdRepository;
import com.arbmonitor.api.service.ApifyScrapingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ads")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class AdScrapingController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdScrapingController.class);
    
    @Autowired
    private ApifyScrapingService apifyScrapingService;
    
    @Autowired
    private ScrapedAdRepository scrapedAdRepository;
    
    @Autowired
    private AdAnalysisRepository adAnalysisRepository;
    
    /**
     * Get detailed ad information with compliance analysis
     */
    @GetMapping("/ad/{adId}/details")
    public ResponseEntity<?> getAdDetails(@PathVariable Long adId) {
        try {
            Optional<ScrapedAd> adOpt = scrapedAdRepository.findById(adId);
            if (!adOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            ScrapedAd ad = adOpt.get();
            
            // Build ad data manually to avoid circular references
            Map<String, Object> adData = new HashMap<>();
            adData.put("id", ad.getId());
            adData.put("metaAdId", ad.getMetaAdId());
            adData.put("headline", ad.getHeadline());
            adData.put("primaryText", ad.getPrimaryText());
            adData.put("description", ad.getDescription());
            adData.put("callToAction", ad.getCallToAction());
            adData.put("landingPageUrl", ad.getLandingPageUrl());
            adData.put("imageUrls", ad.getImageUrls());
            adData.put("videoUrls", ad.getVideoUrls());
            adData.put("localImagePaths", ad.getLocalImagePaths());
            adData.put("localVideoPaths", ad.getLocalVideoPaths());
            adData.put("pageName", ad.getPageName());
            adData.put("extractedImageText", ad.getExtractedImageText());
            adData.put("extractedVideoText", ad.getExtractedVideoText());
            adData.put("referrerAdCreative", ad.getReferrerAdCreative());
            
            // Get analysis data manually
            Map<String, Object> analysisData = null;
            Optional<AdAnalysis> analysisOpt = adAnalysisRepository.findFirstByMetaAdIdOrderByCreatedAtDesc(ad.getMetaAdId());
            if (analysisOpt.isPresent()) {
                AdAnalysis analysis = analysisOpt.get();
                analysisData = new HashMap<>();
                analysisData.put("id", analysis.getId());
                analysisData.put("complianceScore", analysis.getComplianceScore());
                analysisData.put("complianceStatus", analysis.getComplianceStatus());
                analysisData.put("analysisNotes", analysis.getAnalysisNotes());
                analysisData.put("adCreativeCompliant", analysis.getAdCreativeCompliant());
                analysisData.put("adCreativeReason", analysis.getAdCreativeReason());
                analysisData.put("landingPageRelevant", analysis.getLandingPageRelevant());
                analysisData.put("landingPageReason", analysis.getLandingPageReason());
                analysisData.put("racRelevant", analysis.getRacRelevant());
                analysisData.put("racReason", analysis.getRacReason());
                analysisData.put("overallCompliant", analysis.getOverallCompliant());
                analysisData.put("landingPageContent", analysis.getLandingPageContent());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("ad", adData);
            response.put("analysis", analysisData);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching ad details for ID: {}", adId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch ad details: " + e.getMessage()));
        }
    }
    
    /**
     * Get scraped ads for a specific domain
     */
    @GetMapping("/domain/{domainName}")
    public ResponseEntity<Map<String, Object>> getAdsForDomain(
            @PathVariable String domainName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean includeAnalysis) {
        
        try {
            logger.info("Fetching ads for domain: {} (page: {}, size: {}, includeAnalysis: {})", 
                       domainName, page, size, includeAnalysis);
            
            Pageable pageable = PageRequest.of(page, size);
            
            Map<String, Object> response = new HashMap<>();
            
            if (includeAnalysis) {
                // Fetch ads with compliance analysis data
                Page<ScrapedAd> adsPage = scrapedAdRepository.findByDomainNameOrderByScrapedAtDesc(domainName, pageable);
                
                // Convert to DTOs with analysis data
                List<ScrapedAdWithAnalysisDTO> adsWithAnalysis = adsPage.getContent().stream()
                    .map(ad -> {
                        // Find corresponding analysis
                        // We need to find the domain first, then use the proper method
                        // For now, let's use a simpler approach and find by metaAdId only
                        AdAnalysis analysis = adAnalysisRepository.findFirstByMetaAdIdOrderByCreatedAtDesc(ad.getMetaAdId())
                            .orElse(null);
                        return new ScrapedAdWithAnalysisDTO(ad, analysis);
                    })
                    .collect(Collectors.toList());
                
                response.put("ads", adsWithAnalysis);
                response.put("currentPage", adsPage.getNumber());
                response.put("totalPages", adsPage.getTotalPages());
                response.put("totalElements", adsPage.getTotalElements());
                response.put("hasNext", adsPage.hasNext());
                response.put("hasPrevious", adsPage.hasPrevious());
            } else {
                // Fetch ads without analysis data (original behavior)
                Page<ScrapedAd> adsPage = apifyScrapingService.getScrapedAdsForDomain(domainName, pageable);
                
                response.put("ads", adsPage.getContent());
                response.put("currentPage", adsPage.getNumber());
                response.put("totalPages", adsPage.getTotalPages());
                response.put("totalElements", adsPage.getTotalElements());
                response.put("hasNext", adsPage.hasNext());
                response.put("hasPrevious", adsPage.hasPrevious());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching ads for domain {}: {}", domainName, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to fetch ads: " + e.getMessage()));
        }
    }
    
    /**
     * Trigger professional ad scraping for a domain using Apify
     */
    @PostMapping("/scrape/{domainName}")
    public ResponseEntity<Map<String, Object>> scrapeAdsForDomain(@PathVariable String domainName) {
        try {
            logger.info("Triggering professional ad scraping for domain: {} using Apify", domainName);
            
            // Start async scraping using Apify
            apifyScrapingService.scrapeAdsUsingApify(domainName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", String.format("Professional ad scraping started for domain: %s using Apify", domainName));
            response.put("status", "in_progress");
            response.put("method", "apify");
            response.put("domain", domainName);
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            logger.error("Error starting ad scraping for domain {}: {}", domainName, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to start scraping: " + e.getMessage()));
        }
    }
    
    /**
     * Force refresh ads for a domain
     */
    @PostMapping("/refresh/{domainName}")
    public ResponseEntity<Map<String, Object>> refreshAdsForDomain(@PathVariable String domainName) {
        try {
            logger.info("Force refreshing ads for domain: {}", domainName);
            
            // Start async refresh using Apify
            apifyScrapingService.scrapeAdsUsingApify(domainName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Ad refresh started for domain: " + domainName);
            response.put("status", "refreshing");
            response.put("domain", domainName);
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            logger.error("Error refreshing ads for domain {}: {}", domainName, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to refresh ads: " + e.getMessage()));
        }
    }
    
    /**
     * Re-process landing pages for existing ads to extract RAC values using GPT-4
     */
    @PostMapping("/reprocess-rac/{domainName}")
    public ResponseEntity<Map<String, Object>> reprocessRacForDomain(@PathVariable String domainName) {
        try {
            logger.info("Re-processing RAC extraction for domain: {}", domainName);
            
            // Start async RAC re-processing
            apifyScrapingService.reprocessRacForDomain(domainName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("domain", domainName);
            response.put("message", "RAC re-processing started for domain: " + domainName);
            response.put("status", "processing");
            
            return ResponseEntity.accepted().body(response);
        } catch (Exception e) {
            logger.error("Error re-processing RAC for domain {}: {}", domainName, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to start RAC re-processing: " + e.getMessage()));
        }
    }
    
    /**
     * Analyze domain URLs to identify RAC parameter pattern using GPT-4
     */
    @PostMapping("/analyze-rac-pattern/{domainName}")
    public ResponseEntity<Map<String, Object>> analyzeDomainRacPattern(@PathVariable String domainName) {
        try {
            logger.info("Analyzing RAC pattern for domain: {}", domainName);
            
            // Start async pattern analysis
            apifyScrapingService.analyzeDomainRacPattern(domainName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("domain", domainName);
            response.put("message", "RAC pattern analysis started for domain: " + domainName);
            response.put("status", "analyzing");
            
            return ResponseEntity.accepted().body(response);
        } catch (Exception e) {
            logger.error("Error analyzing RAC pattern for domain {}: {}", domainName, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to start RAC pattern analysis: " + e.getMessage()));
        }
    }
    
    /**
     * Get ad statistics for a domain
     */
    @GetMapping("/stats/{domainName}")
    public ResponseEntity<Map<String, Object>> getAdStatsForDomain(@PathVariable String domainName) {
        try {
            logger.info("Fetching ad statistics for domain: {}", domainName);
            
            long totalAds = apifyScrapingService.getAdCountForDomain(domainName);
            
            // Get a sample of ads to calculate additional stats
            Pageable samplePageable = PageRequest.of(0, 100);
            Page<ScrapedAd> sampleAds = apifyScrapingService.getScrapedAdsForDomain(domainName, samplePageable);
            
            long imageAds = sampleAds.getContent().stream()
                .filter(ad -> ad.getImageUrls() != null && !ad.getImageUrls().isEmpty())
                .count();
            
            long videoAds = sampleAds.getContent().stream()
                .filter(ad -> ad.getVideoUrls() != null && !ad.getVideoUrls().isEmpty())
                .count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalAds", totalAds);
            stats.put("imageAds", imageAds);
            stats.put("videoAds", videoAds);
            stats.put("domain", domainName);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error fetching ad stats for domain {}: {}", domainName, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to fetch stats: " + e.getMessage()));
        }
    }
    
    /**
     * Get a specific ad by ID
     */
    @GetMapping("/{adId}")
    public ResponseEntity<ScrapedAd> getAdById(@PathVariable Long adId) {
        try {
            // This would require adding a method to the service
            // For now, return a placeholder response
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            logger.error("Error fetching ad {}: {}", adId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    


    /**
     * Create mock ads for demo purposes
     */
    @PostMapping("/mock/{domainName}")
    public ResponseEntity<Map<String, Object>> createMockAds(@PathVariable String domainName) {
        try {
            logger.info("Creating mock ads for domain: {}", domainName);
            
            String result = apifyScrapingService.createMockAdsForDomain(domainName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", result);
            response.put("status", "completed");
            response.put("domain", domainName);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error creating mock ads for domain {}: {}", domainName, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to create mock ads: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Ad Scraping Service");
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }
}
