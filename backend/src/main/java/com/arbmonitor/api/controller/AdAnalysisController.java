package com.arbmonitor.api.controller;

import com.arbmonitor.api.model.Domain;
import com.arbmonitor.api.model.User;
import com.arbmonitor.api.repository.DomainRepository;
import com.arbmonitor.api.repository.UserRepository;
import com.arbmonitor.api.service.AdAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

/**
 * Controller for ad analysis operations
 */
@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "false")
public class AdAnalysisController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdAnalysisController.class);
    
    private final AdAnalysisService adAnalysisService;
    private final DomainRepository domainRepository;
    private final UserRepository userRepository;
    
    public AdAnalysisController(
            AdAnalysisService adAnalysisService,
            DomainRepository domainRepository,
            UserRepository userRepository) {
        this.adAnalysisService = adAnalysisService;
        this.domainRepository = domainRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Triggers ad analysis for a specific domain
     */
    @PostMapping("/domains/{domainId}/analyze")
    public Mono<ResponseEntity<Map<String, Object>>> analyzeDomain(
            @RequestHeader("X-User-ID") Long userId,
            @PathVariable Long domainId) {
        
        logger.info("Received request to analyze domain {} for user {}", domainId, userId);
        
        // Find domain and user
        Optional<Domain> domainOpt = domainRepository.findById(domainId);
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (domainOpt.isEmpty()) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        
        if (userOpt.isEmpty()) {
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("error", "User not found");
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }
        
        Domain domain = domainOpt.get();
        User user = userOpt.get();
        
        // Verify domain belongs to user
        if (!domain.getUser().getId().equals(user.getId())) {
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("error", "Domain does not belong to user");
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }
        
        // Check if user has Meta API key
        if (user.getMetaApiKey() == null || user.getMetaApiKey().isEmpty()) {
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("error", "Meta API key not configured");
            errorResponse.put("message", "Please configure your Meta API key in settings before running analysis");
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }
        
        // Start analysis
        return adAnalysisService.analyzeAdsForDomain(domain, user)
            .map(processedCount -> {
                Map<String, Object> response = new java.util.HashMap<>();
                if (processedCount > 0) {
                    response.put("success", true);
                    response.put("message", "Analysis completed successfully");
                    response.put("processedAds", processedCount);
                    response.put("domainId", domainId);
                } else {
                    response.put("success", true);
                    response.put("message", "No ads found for this domain");
                    response.put("processedAds", 0);
                    response.put("domainId", domainId);
                }
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                logger.error("Error during analysis: {}", error.getMessage());
                Map<String, Object> errorResponse = new java.util.HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Analysis failed");
                errorResponse.put("message", "An error occurred during ad analysis");
                return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
            });
    }
    
    /**
     * Gets analysis status for a domain
     */
    @GetMapping("/domains/{domainId}/status")
    public ResponseEntity<Map<String, Object>> getAnalysisStatus(
            @RequestHeader("X-User-ID") Long userId,
            @PathVariable Long domainId) {
        
        Optional<Domain> domainOpt = domainRepository.findById(domainId);
        if (domainOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Domain domain = domainOpt.get();
        
        // Verify domain belongs to user
        if (!domain.getUser().getId().equals(userId)) {
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("error", "Domain does not belong to user");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("domainId", domainId);
        response.put("domainName", domain.getDomainName());
        response.put("lastChecked", domain.getLastChecked());
        response.put("complianceScore", domain.getComplianceScore());
        response.put("status", domain.getStatus());
        return ResponseEntity.ok(response);
    }
}
