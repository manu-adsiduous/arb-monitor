package com.arbmonitor.api.service;

import com.arbmonitor.api.model.ScrapedAd;
import com.arbmonitor.api.model.Domain;
import com.arbmonitor.api.repository.ScrapedAdRepository;
import com.arbmonitor.api.repository.DomainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for extracting Referrer Ad Creative (RAC) using headless browser
 */
@Service
public class RacExtractionService {
    
    private static final Logger logger = LoggerFactory.getLogger(RacExtractionService.class);
    
    @Autowired
    private ScrapedAdRepository scrapedAdRepository;
    
    @Autowired
    private DomainSpecificRacExtractionService domainSpecificRacService;
    
    @Autowired
    private DomainRepository domainRepository;
    

    
    /**
     * Process RAC extraction for all ads in a domain using domain-specific parameter detection
     */
    public void processRacForDomain(String domainName) {
        try {
            logger.info("🔄 Starting RAC extraction for domain: {}", domainName);
            
            // Get domain to access user-defined RAC parameter
            Domain domain = domainRepository.findByDomainName(domainName);
            if (domain == null) {
                throw new RuntimeException("Domain not found: " + domainName);
            }
            
            List<ScrapedAd> ads = scrapedAdRepository.findByDomainNameOrderByScrapedAtDesc(domainName);
            logger.info("Found {} ads for RAC extraction", ads.size());
            
            int processedCount = 0;
            
            for (ScrapedAd ad : ads) {
                try {
                    // Clear existing RAC value to force re-extraction
                    ad.setReferrerAdCreative(null);
                    
                    if (ad.getReferrerAdCreative() == null || ad.getReferrerAdCreative().trim().isEmpty()) {
                        logger.info("🔍 Extracting RAC for ad {}: {}", ad.getMetaAdId(), ad.getLandingPageUrl());
                        
                        // Extract RAC using domain-specific parameter detection with user-defined parameter
                        String racFromUrl = domainSpecificRacService.extractRacFromUrl(ad.getLandingPageUrl(), domain.getRacParameter());
                        if (racFromUrl != null) {
                            ad.setReferrerAdCreative(racFromUrl);
                            scrapedAdRepository.save(ad);
                            processedCount++;
                            logger.info("✅ Extracted RAC from URL for ad {}: '{}'", ad.getMetaAdId(), racFromUrl);
                        } else {
                            logger.info("❌ No RAC found for ad {} - parameter detection failed", ad.getMetaAdId());
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to extract RAC for ad {}: {}", ad.getMetaAdId(), e.getMessage());
                }
            }
            
            logger.info("✅ RAC extraction completed for domain: {}. Processed {} ads", domainName, processedCount);
            
        } catch (Exception e) {
            logger.error("Error during RAC extraction for domain {}: {}", domainName, e.getMessage());
        }
    }
    

}
