package com.arbmonitor.api.repository;

import com.arbmonitor.api.model.ScrapedAd;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScrapedAdRepository extends JpaRepository<ScrapedAd, Long> {
    
    // Find ads by domain name
    Page<ScrapedAd> findByDomainNameOrderByScrapedAtDesc(String domainName, Pageable pageable);
    
    // Find all ads by domain name (non-paginated)
    List<ScrapedAd> findByDomainNameOrderByScrapedAtDesc(String domainName);
    
    // Find active ads by domain name
    Page<ScrapedAd> findByDomainNameAndIsActiveTrueOrderByScrapedAtDesc(String domainName, Pageable pageable);
    
    // Find by Meta Ad ID
    Optional<ScrapedAd> findByMetaAdId(String metaAdId);
    
    // Check if ad exists by Meta Ad ID
    boolean existsByMetaAdId(String metaAdId);
    
    // Find ads scraped after a certain date
    List<ScrapedAd> findByDomainNameAndScrapedAtAfter(String domainName, LocalDateTime date);
    
    // Count ads by domain name
    long countByDomainName(String domainName);
    
    // Count active ads by domain name
    long countByDomainNameAndIsActiveTrue(String domainName);
    
    // Find ads that need to be updated (older than specified date)
    @Query("SELECT sa FROM ScrapedAd sa WHERE sa.domainName = :domainName AND sa.lastUpdated < :cutoffDate")
    List<ScrapedAd> findAdsNeedingUpdate(@Param("domainName") String domainName, @Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Find all unique domain names that have scraped ads
    @Query("SELECT DISTINCT sa.domainName FROM ScrapedAd sa")
    List<String> findAllDomainNamesWithAds();
    
    // Find ads by page name or page ID
    Page<ScrapedAd> findByPageNameContainingIgnoreCaseOrPageIdOrderByScrapedAtDesc(String pageName, String pageId, Pageable pageable);
    
    // Find ads by ad format
    Page<ScrapedAd> findByDomainNameAndAdFormatOrderByScrapedAtDesc(String domainName, String adFormat, Pageable pageable);
    
    // Delete old inactive ads
    @Modifying
    @Transactional
    @Query("DELETE FROM ScrapedAd sa WHERE sa.isActive = false AND sa.lastUpdated < :cutoffDate")
    void deleteOldInactiveAds(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Find top spending ads by domain
    @Query("SELECT sa FROM ScrapedAd sa WHERE sa.domainName = :domainName AND sa.spendRangeLower IS NOT NULL ORDER BY sa.spendRangeLower DESC")
    List<ScrapedAd> findTopSpendingAdsByDomain(@Param("domainName") String domainName, Pageable pageable);
    
    // Search ads by text content
    @Query("SELECT sa FROM ScrapedAd sa WHERE sa.domainName = :domainName AND " +
           "(LOWER(sa.primaryText) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(sa.headline) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(sa.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<ScrapedAd> searchAdsByTextContent(@Param("domainName") String domainName, 
                                          @Param("searchTerm") String searchTerm, 
                                          Pageable pageable);
    
    // Delete ads by domain name
    @Modifying
    @Transactional
    void deleteByDomainName(String domainName);
    
    // Find ads without RAC values for re-processing
    List<ScrapedAd> findByDomainNameAndReferrerAdCreativeIsNull(String domainName);
    
    // Find ads without RAC values for re-processing (paginated)
    Page<ScrapedAd> findByDomainNameAndReferrerAdCreativeIsNull(String domainName, Pageable pageable);
}

