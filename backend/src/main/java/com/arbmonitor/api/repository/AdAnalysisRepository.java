package com.arbmonitor.api.repository;

import com.arbmonitor.api.model.AdAnalysis;
import com.arbmonitor.api.model.Domain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdAnalysisRepository extends JpaRepository<AdAnalysis, Long> {
    
    /**
     * Find analysis by Meta Ad ID (most recent)
     */
    Optional<AdAnalysis> findFirstByMetaAdIdOrderByCreatedAtDesc(String metaAdId);
    
    /**
     * Find analysis by Meta Ad ID and Domain
     */
    Optional<AdAnalysis> findByMetaAdIdAndDomain(String metaAdId, Domain domain);
    
    /**
     * Find all analyses for a domain
     */
    Page<AdAnalysis> findByDomain(Domain domain, Pageable pageable);
    
    /**
     * Find all analyses for a domain (non-paginated)
     */
    List<AdAnalysis> findByDomainOrderByCreatedAtDesc(Domain domain);
    
    /**
     * Find analyses by compliance status
     */
    List<AdAnalysis> findByComplianceStatusAndDomain(AdAnalysis.ComplianceStatus status, Domain domain);
    
    /**
     * Find analyses with compliance score below threshold
     */
    @Query("SELECT a FROM AdAnalysis a WHERE a.domain = :domain AND a.complianceScore < :threshold ORDER BY a.complianceScore ASC")
    List<AdAnalysis> findByDomainWithScoreBelow(Domain domain, Double threshold);
    
    /**
     * Get average compliance score for a domain
     */
    @Query("SELECT AVG(a.complianceScore) FROM AdAnalysis a WHERE a.domain = :domain AND a.complianceScore IS NOT NULL")
    Double getAverageComplianceScoreByDomain(Domain domain);
    
    /**
     * Count analyses by compliance status for a domain
     */
    @Query("SELECT COUNT(a) FROM AdAnalysis a WHERE a.domain = :domain AND a.complianceStatus = :status")
    long countByDomainAndComplianceStatus(Domain domain, AdAnalysis.ComplianceStatus status);
    
    /**
     * Find recent analyses (last 30 days)
     */
    @Query("SELECT a FROM AdAnalysis a WHERE a.domain = :domain AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AdAnalysis> findRecentByDomain(Domain domain, LocalDateTime since);
    
    /**
     * Find analyses that need re-analysis (older than specified date)
     */
    @Query("SELECT a FROM AdAnalysis a WHERE a.updatedAt < :olderThan ORDER BY a.updatedAt ASC")
    List<AdAnalysis> findAnalysesNeedingUpdate(LocalDateTime olderThan);
    
    /**
     * Delete analyses by domain
     */
    @Modifying
    @Transactional
    void deleteByDomain(Domain domain);
    
    /**
     * Count all analyses for a domain
     */
    long countByDomain(Domain domain);
}