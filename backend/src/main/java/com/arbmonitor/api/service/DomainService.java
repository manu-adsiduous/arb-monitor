package com.arbmonitor.api.service;

import com.arbmonitor.api.model.Domain;
import com.arbmonitor.api.model.User;
import com.arbmonitor.api.repository.DomainRepository;
import com.arbmonitor.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class DomainService {
    
    private static final Logger logger = LoggerFactory.getLogger(DomainService.class);
    
    @Autowired
    private DomainRepository domainRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ApifyScrapingService apifyScrapingService;
    
    /**
     * Save domain changes
     */
    public Domain saveDomain(Domain domain) {
        return domainRepository.save(domain);
    }
    
    /**
     * Add a new domain for a user
     */
    public Domain addDomain(Long userId, String domainName) {
        logger.info("Adding domain {} for user ID: {}", domainName, userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        // Check if domain already exists for this user
        if (domainRepository.existsByDomainNameAndUser(domainName, user)) {
            throw new IllegalArgumentException("Domain " + domainName + " already exists for this user");
        }
        
        // Validate domain format
        if (!isValidDomain(domainName)) {
            throw new IllegalArgumentException("Invalid domain format: " + domainName);
        }
        
        Domain domain = new Domain(domainName, user);
        domain.setStatus(Domain.MonitoringStatus.ACTIVE);
        
        return domainRepository.save(domain);
    }
    
    /**
     * Get all domains for a user
     */
    @Transactional(readOnly = true)
    public List<Domain> getUserDomains(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        return domainRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    /**
     * Get domains with pagination
     */
    @Transactional(readOnly = true)
    public Page<Domain> getUserDomains(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        return domainRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }
    
    /**
     * Get domain by ID
     */
    @Transactional(readOnly = true)
    public Optional<Domain> getDomainById(Long domainId) {
        return domainRepository.findById(domainId);
    }
    
    /**
     * Get domain by ID with ad analyses
     */
    @Transactional(readOnly = true)
    public Optional<Domain> getDomainByIdWithAdAnalyses(Long domainId) {
        return domainRepository.findByIdWithAdAnalyses(domainId);
    }
    
    /**
     * Get domain by share token (for public reports)
     */
    @Transactional(readOnly = true)
    public Optional<Domain> getDomainByShareToken(String shareToken) {
        return domainRepository.findByShareToken(shareToken);
    }
    
    /**
     * Update domain compliance score
     */
    public Domain updateComplianceScore(Long domainId, Double complianceScore) {
        logger.info("Updating compliance score for domain ID: {} to {}", domainId, complianceScore);
        
        Domain domain = domainRepository.findById(domainId)
            .orElseThrow(() -> new IllegalArgumentException("Domain not found with ID: " + domainId));
        
        domain.setComplianceScore(complianceScore);
        domain.setLastChecked(LocalDateTime.now());
        
        return domainRepository.save(domain);
    }
    
    /**
     * Update domain status
     */
    public Domain updateDomainStatus(Long domainId, Domain.MonitoringStatus status) {
        logger.info("Updating status for domain ID: {} to {}", domainId, status);
        
        Domain domain = domainRepository.findById(domainId)
            .orElseThrow(() -> new IllegalArgumentException("Domain not found with ID: " + domainId));
        
        domain.setStatus(status);
        return domainRepository.save(domain);
    }
    
    /**
     * Regenerate share token for domain
     */
    public Domain regenerateShareToken(Long domainId) {
        logger.info("Regenerating share token for domain ID: {}", domainId);
        
        Domain domain = domainRepository.findById(domainId)
            .orElseThrow(() -> new IllegalArgumentException("Domain not found with ID: " + domainId));
        
        domain.setShareToken(UUID.randomUUID().toString());
        return domainRepository.save(domain);
    }
    
    /**
     * Search domains by name
     */
    @Transactional(readOnly = true)
    public List<Domain> searchUserDomains(Long userId, String searchTerm) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        return domainRepository.searchByDomainName(user, searchTerm);
    }
    
    /**
     * Get domain statistics for a user
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserDomainStats(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        Object[] stats = domainRepository.getDomainStatsByUser(user);
        
        Long totalDomains = (Long) stats[0];
        Double avgCompliance = (Double) stats[1];
        Long activeDomains = (Long) stats[2];
        
        return Map.of(
            "totalDomains", totalDomains != null ? totalDomains : 0L,
            "averageCompliance", avgCompliance != null ? avgCompliance : 0.0,
            "activeDomains", activeDomains != null ? activeDomains : 0L,
            "inactiveDomains", (totalDomains != null ? totalDomains : 0L) - (activeDomains != null ? activeDomains : 0L)
        );
    }
    
    /**
     * Get domains with low compliance scores
     */
    @Transactional(readOnly = true)
    public List<Domain> getDomainsWithLowCompliance(Long userId, Double threshold) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        return domainRepository.findDomainsWithLowCompliance(user, threshold);
    }
    
    /**
     * Get domains that need checking
     */
    @Transactional(readOnly = true)
    public List<Domain> getDomainsNeedingCheck(int hoursAgo) {
        LocalDateTime checkTime = LocalDateTime.now().minusHours(hoursAgo);
        return domainRepository.findDomainsNeedingCheck(checkTime);
    }
    
    /**
     * Delete domain
     */
    public void deleteDomain(Long domainId, Long userId) {
        logger.info("Deleting domain ID: {} for user ID: {}", domainId, userId);
        
        Domain domain = domainRepository.findById(domainId)
            .orElseThrow(() -> new IllegalArgumentException("Domain not found with ID: " + domainId));
        
        // Verify ownership
        if (!domain.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("User does not own this domain");
        }
        
        // Stop any active Apify scraping for this domain
        String domainName = domain.getDomainName();
        apifyScrapingService.stopScrapingForDomain(domainName);
        logger.info("Stopped Apify scraping for domain: {}", domainName);
        
        domainRepository.delete(domain);
    }
    
    /**
     * Verify domain ownership
     */
    @Transactional(readOnly = true)
    public boolean verifyDomainOwnership(Long domainId, Long userId) {
        Optional<Domain> domain = domainRepository.findById(domainId);
        return domain.isPresent() && domain.get().getUser().getId().equals(userId);
    }
    
    /**
     * Mark domain as checked
     */
    public Domain markAsChecked(Long domainId) {
        Domain domain = domainRepository.findById(domainId)
            .orElseThrow(() -> new IllegalArgumentException("Domain not found with ID: " + domainId));
        
        domain.setLastChecked(LocalDateTime.now());
        return domainRepository.save(domain);
    }
    
    /**
     * Validate domain format
     */
    private boolean isValidDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return false;
        }
        
        // Domain validation regex that supports subdomains
        // Allows: example.com, sub.example.com, sub1.sub2.example.com, etc.
        String domainRegex = "^[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]?(\\.[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]?)*\\.[a-zA-Z]{2,}$";
        return domain.matches(domainRegex);
    }
    
    /**
     * Get user's domain count
     */
    @Transactional(readOnly = true)
    public long getUserDomainCount(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        return domainRepository.countByUser(user);
    }
    
    /**
     * Get user's active domain count
     */
    @Transactional(readOnly = true)
    public long getUserActiveDomainCount(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        
        return domainRepository.countActiveByUser(user);
    }
}

