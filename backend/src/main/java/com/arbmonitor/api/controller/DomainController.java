package com.arbmonitor.api.controller;

import com.arbmonitor.api.model.Domain;
import com.arbmonitor.api.service.DomainService;
import com.arbmonitor.api.service.ApifyScrapingService;
import com.arbmonitor.api.repository.ScrapedAdRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/domains")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class DomainController {
    
    private static final Logger logger = LoggerFactory.getLogger(DomainController.class);
    
    @Autowired
    private DomainService domainService;
    
    @Autowired
    private ApifyScrapingService apifyScrapingService;

    @Autowired
    private ScrapedAdRepository scrapedAdRepository;
    
    /**
     * Pause domain scraping
     */
    @PostMapping("/{domainId}/pause")
    public ResponseEntity<?> pauseDomainScraping(@PathVariable Long domainId) {
        try {
            Optional<Domain> domainOpt = domainService.getDomainById(domainId);
            if (!domainOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Domain domain = domainOpt.get();
            boolean paused = apifyScrapingService.pauseScrapingForDomain(domain.getDomainName());
            
            if (paused) {
                // Ensure domain status is updated in the controller transaction
                domain.setProcessingStatus(Domain.ProcessingStatus.PAUSED);
                domain.setProcessingMessage("Scraping paused by user");
                domainService.saveDomain(domain);
                
                logger.info("Paused scraping for domain: {}", domain.getDomainName());
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Scraping paused",
                    "domainName", domain.getDomainName()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No active scraping to pause"
                ));
            }
            
        } catch (Exception e) {
            logger.error("Error pausing scraping for domain ID: {}", domainId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to pause scraping: " + e.getMessage()));
        }
    }
    
    /**
     * Cancel domain scraping completely
     */
    @PostMapping("/{domainId}/cancel")
    public ResponseEntity<?> cancelDomainScraping(@PathVariable Long domainId) {
        try {
            Optional<Domain> domainOpt = domainService.getDomainById(domainId);
            if (!domainOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Domain domain = domainOpt.get();
            
            // Stop any active scraping
            apifyScrapingService.stopScrapingForDomain(domain.getDomainName());
            
            // Set status to completed with cancellation message
            long adCount = scrapedAdRepository.countByDomainName(domain.getDomainName());
            domain.setProcessingStatus(Domain.ProcessingStatus.COMPLETED);
            domain.setProcessingMessage(String.format("Scraping cancelled by user - found %d ads", adCount));
            domainService.saveDomain(domain);
            
            logger.info("Cancelled scraping for domain: {}", domain.getDomainName());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Scraping cancelled",
                "domainName", domain.getDomainName(),
                "adCount", adCount
            ));
            
        } catch (Exception e) {
            logger.error("Error cancelling scraping for domain ID: {}", domainId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to cancel scraping: " + e.getMessage()));
        }
    }
    
    /**
     * Resume domain scraping
     */
    @PostMapping("/{domainId}/resume")
    public ResponseEntity<?> resumeDomainScraping(@PathVariable Long domainId) {
        try {
            Optional<Domain> domainOpt = domainService.getDomainById(domainId);
            if (!domainOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Domain domain = domainOpt.get();
            
            if (domain.getProcessingStatus() == Domain.ProcessingStatus.PAUSED) {
                apifyScrapingService.resumeScrapingForDomain(domain.getDomainName());
                logger.info("Resumed scraping for domain: {}", domain.getDomainName());
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Scraping resumed",
                    "domainName", domain.getDomainName()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Domain is not in paused state"
                ));
            }
            
        } catch (Exception e) {
            logger.error("Error resuming scraping for domain ID: {}", domainId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to resume scraping: " + e.getMessage()));
        }
    }
    
    /**
     * Force complete domain processing (for stuck domains)
     */
    @PostMapping("/{domainId}/force-complete")
    public ResponseEntity<?> forceCompleteDomainProcessing(@PathVariable Long domainId) {
        try {
            Optional<Domain> domainOpt = domainService.getDomainById(domainId);
            if (!domainOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Domain domain = domainOpt.get();
            
            // Stop any active scraping for this domain
            apifyScrapingService.stopScrapingForDomain(domain.getDomainName());
            
            long adCount = scrapedAdRepository.countByDomainName(domain.getDomainName());
            
            domain.setProcessingStatus(Domain.ProcessingStatus.COMPLETED);
            domain.setProcessingMessage(String.format("Manually completed - found %d ads", adCount));
            domainService.saveDomain(domain);
            
            logger.info("Force completed processing for domain: {} with {} ads", domain.getDomainName(), adCount);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Domain processing force completed",
                "domainName", domain.getDomainName(),
                "adCount", adCount
            ));
            
        } catch (Exception e) {
            logger.error("Error force completing domain processing for domain ID: {}", domainId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to force complete domain processing: " + e.getMessage()));
        }
    }
    
    /**
     * Manually complete domain processing (for debugging)
     */
    @PostMapping("/{domainId}/complete-processing")
    public ResponseEntity<?> completeDomainProcessing(@PathVariable Long domainId) {
        try {
            Optional<Domain> domainOpt = domainService.getDomainById(domainId);
            if (!domainOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Domain domain = domainOpt.get();
            long adCount = scrapedAdRepository.countByDomainName(domain.getDomainName());
            
            domain.setProcessingStatus(Domain.ProcessingStatus.COMPLETED);
            domain.setProcessingMessage(String.format("Found %d ads - ready for monitoring", adCount));
            domainService.saveDomain(domain);
            
            logger.info("Manually completed processing for domain: {} with {} ads", domain.getDomainName(), adCount);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Domain processing completed",
                "domainName", domain.getDomainName(),
                "adCount", adCount
            ));
            
        } catch (Exception e) {
            logger.error("Error completing domain processing for domain ID: {}", domainId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to complete domain processing: " + e.getMessage()));
        }
    }
    
    /**
     * Update RAC parameter for a domain
     */
    @PutMapping("/{domainId}/rac-parameter")
    public ResponseEntity<?> updateRacParameter(
            @PathVariable Long domainId,
            @RequestHeader("X-User-ID") Long userId,
            @RequestBody Map<String, String> request) {
        
        try {
            String racParameter = request.get("racParameter");
            logger.info("Updating RAC parameter for domain ID {} to '{}'", domainId, racParameter);
            
            Optional<Domain> domainOpt = domainService.getDomainById(domainId);
            if (!domainOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Domain domain = domainOpt.get();
            domain.setRacParameter(racParameter);
            domainService.saveDomain(domain);
            
            logger.info("Updated RAC parameter for domain: {} to '{}'", domain.getDomainName(), racParameter);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "RAC parameter updated successfully",
                "domainName", domain.getDomainName(),
                "racParameter", racParameter
            ));
            
        } catch (Exception e) {
            logger.error("Error updating RAC parameter for domain ID: {}", domainId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update RAC parameter: " + e.getMessage()));
        }
    }
    
    /**
     * Add a new domain
     */
    @PostMapping
    public ResponseEntity<?> addDomain(
            @RequestHeader("X-User-ID") Long userId,
            @RequestBody @Valid DomainRequest request) {
        
        try {
            logger.info("Adding domain {} for user {}", request.getDomainName(), userId);
            
            Domain domain = domainService.addDomain(userId, request.getDomainName());
            
            // Set processing status to indicate ad fetching has started
            domain.setProcessingStatus(Domain.ProcessingStatus.FETCHING_ADS);
            domain.setProcessingMessage("Fetching ads from Facebook...");
            domainService.saveDomain(domain);
            
            // Automatically start ad scraping for the new domain
            logger.info("Auto-starting ad scraping for domain: {}", domain.getDomainName());
            apifyScrapingService.scrapeAdsUsingApify(domain.getDomainName());
            
            return ResponseEntity.ok(new DomainResponse(domain));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to add domain: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error adding domain", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to add domain"));
        }
    }
    
    /**
     * Get all domains for user
     */
    @GetMapping
    public ResponseEntity<?> getUserDomains(
            @RequestHeader("X-User-ID") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        
        try {
            List<Domain> domains;
            
            if (search != null && !search.trim().isEmpty()) {
                domains = domainService.searchUserDomains(userId, search.trim());
            } else if (page > 0 || size < 100) {
                Pageable pageable = PageRequest.of(page, size);
                Page<Domain> domainPage = domainService.getUserDomains(userId, pageable);
                return ResponseEntity.ok(Map.of(
                    "domains", domainPage.getContent().stream()
                        .map(domain -> {
                            int activeAdsCount = (int) scrapedAdRepository.countByDomainName(domain.getDomainName());
                            int violationsCount = 0; // TODO: Calculate from compliance analysis
                            return new DomainResponse(domain, activeAdsCount, violationsCount);
                        })
                        .toList(),
                    "totalElements", domainPage.getTotalElements(),
                    "totalPages", domainPage.getTotalPages(),
                    "currentPage", page
                ));
            } else {
                domains = domainService.getUserDomains(userId);
            }
            
            List<DomainResponse> domainResponses = domains.stream()
                .map(domain -> {
                    int activeAdsCount = (int) scrapedAdRepository.countByDomainName(domain.getDomainName());
                    int violationsCount = 0; // TODO: Calculate from compliance analysis
                    return new DomainResponse(domain, activeAdsCount, violationsCount);
                })
                .toList();
            
            return ResponseEntity.ok(Map.of("domains", domainResponses));
            
        } catch (Exception e) {
            logger.error("Error getting user domains", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get domains"));
        }
    }
    
    /**
     * Get domain by ID
     */
    @GetMapping("/{domainId}")
    public ResponseEntity<?> getDomain(
            @RequestHeader("X-User-ID") Long userId,
            @PathVariable Long domainId) {
        
        try {
            // Verify ownership
            if (!domainService.verifyDomainOwnership(domainId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }
            
            Optional<Domain> domain = domainService.getDomainByIdWithAdAnalyses(domainId);
            if (domain.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Domain domainEntity = domain.get();
            int activeAdsCount = (int) scrapedAdRepository.countByDomainName(domainEntity.getDomainName());
            int violationsCount = 0; // TODO: Calculate from compliance analysis
            return ResponseEntity.ok(new DomainDetailResponse(domainEntity, activeAdsCount, violationsCount));
            
        } catch (Exception e) {
            logger.error("Error getting domain", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get domain"));
        }
    }
    
    /**
     * Update domain status
     */
    @PutMapping("/{domainId}/status")
    public ResponseEntity<?> updateDomainStatus(
            @RequestHeader("X-User-ID") Long userId,
            @PathVariable Long domainId,
            @RequestBody @Valid StatusUpdateRequest request) {
        
        try {
            // Verify ownership
            if (!domainService.verifyDomainOwnership(domainId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }
            
            Domain domain = domainService.updateDomainStatus(domainId, request.getStatus());
            return ResponseEntity.ok(new DomainResponse(domain));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating domain status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update domain status"));
        }
    }
    
    /**
     * Delete domain
     */
    @DeleteMapping("/{domainId}")
    public ResponseEntity<?> deleteDomain(
            @RequestHeader("X-User-ID") Long userId,
            @PathVariable Long domainId) {
        
        try {
            domainService.deleteDomain(domainId, userId);
            return ResponseEntity.ok(Map.of("message", "Domain deleted successfully"));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting domain", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete domain"));
        }
    }
    
    /**
     * Get domain statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getDomainStats(@RequestHeader("X-User-ID") Long userId) {
        try {
            Map<String, Object> stats = domainService.getUserDomainStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting domain stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get domain statistics"));
        }
    }
    
    /**
     * Regenerate share token
     */
    @PostMapping("/{domainId}/regenerate-token")
    public ResponseEntity<?> regenerateShareToken(
            @RequestHeader("X-User-ID") Long userId,
            @PathVariable Long domainId) {
        
        try {
            // Verify ownership
            if (!domainService.verifyDomainOwnership(domainId, userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }
            
            Domain domain = domainService.regenerateShareToken(domainId);
            return ResponseEntity.ok(Map.of(
                "shareToken", domain.getShareToken(),
                "message", "Share token regenerated successfully"
            ));
            
        } catch (Exception e) {
            logger.error("Error regenerating share token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to regenerate share token"));
        }
    }
    
    // Request/Response DTOs
    
    public static class DomainRequest {
        @NotBlank(message = "Domain name is required")
        private String domainName;
        
        public String getDomainName() { return domainName; }
        public void setDomainName(String domainName) { this.domainName = domainName; }
    }
    
    public static class StatusUpdateRequest {
        private Domain.MonitoringStatus status;
        
        public Domain.MonitoringStatus getStatus() { return status; }
        public void setStatus(Domain.MonitoringStatus status) { this.status = status; }
    }
    
    public static class DomainResponse {
        private Long id;
        private String domainName;
        private Double complianceScore;
        private String lastChecked;
        private String shareToken;
        private String status;
        private String processingStatus;
        private String processingMessage;
        private String racParameter;
        private String createdAt;
        private String updatedAt;
        private int activeAds;
        private int violations;
        
        public DomainResponse(Domain domain) {
            this.id = domain.getId();
            this.domainName = domain.getDomainName();
            this.complianceScore = domain.getComplianceScore();
            this.lastChecked = domain.getLastChecked() != null ? domain.getLastChecked().toString() : null;
            this.shareToken = domain.getShareToken();
            this.status = domain.getStatus().name();
            this.processingStatus = domain.getProcessingStatus().name();
            this.processingMessage = domain.getProcessingMessage();
            this.racParameter = domain.getRacParameter();
            this.createdAt = domain.getCreatedAt() != null ? domain.getCreatedAt().toString() : null;
            this.updatedAt = domain.getUpdatedAt() != null ? domain.getUpdatedAt().toString() : null;
            // TODO: Fix this to get actual ad count from ScrapedAdRepository
            this.activeAds = domain.getAdAnalyses() != null ? domain.getAdAnalyses().size() : 0;
            this.violations = 0;
        }

        public DomainResponse(Domain domain, int activeAdsCount, int violationsCount) {
            this.id = domain.getId();
            this.domainName = domain.getDomainName();
            this.complianceScore = domain.getComplianceScore();
            this.lastChecked = domain.getLastChecked() != null ? domain.getLastChecked().toString() : null;
            this.shareToken = domain.getShareToken();
            this.status = domain.getStatus().name();
            this.processingStatus = domain.getProcessingStatus().name();
            this.processingMessage = domain.getProcessingMessage();
            this.racParameter = domain.getRacParameter();
            this.createdAt = domain.getCreatedAt() != null ? domain.getCreatedAt().toString() : null;
            this.updatedAt = domain.getUpdatedAt() != null ? domain.getUpdatedAt().toString() : null;
            this.activeAds = activeAdsCount;
            this.violations = violationsCount;
        }
        
        // Getters
        public Long getId() { return id; }
        public String getDomainName() { return domainName; }
        public Double getComplianceScore() { return complianceScore; }
        public String getLastChecked() { return lastChecked; }
        public String getShareToken() { return shareToken; }
        public String getStatus() { return status; }
        public String getProcessingStatus() { return processingStatus; }
        public String getProcessingMessage() { return processingMessage; }
        public String getRacParameter() { return racParameter; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public int getActiveAds() { return activeAds; }
        public int getViolations() { return violations; }
    }
    
    public static class DomainDetailResponse extends DomainResponse {
        public DomainDetailResponse(Domain domain) {
            super(domain);
            // Additional detail fields can be added here
        }
        
        public DomainDetailResponse(Domain domain, int activeAdsCount, int violationsCount) {
            super(domain, activeAdsCount, violationsCount);
            // Additional detail fields can be added here
        }
    }
}

