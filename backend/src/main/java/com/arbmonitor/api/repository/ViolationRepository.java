package com.arbmonitor.api.repository;

import com.arbmonitor.api.model.AdAnalysis;
import com.arbmonitor.api.model.ComplianceRule;
import com.arbmonitor.api.model.Domain;
import com.arbmonitor.api.model.Violation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ViolationRepository extends JpaRepository<Violation, Long> {
    
    /**
     * Find violations by analysis
     */
    List<Violation> findByAnalysisOrderBySeverityDesc(AdAnalysis analysis);
    
    /**
     * Find violations by rule
     */
    List<Violation> findByRuleOrderByCreatedAtDesc(ComplianceRule rule);
    
    /**
     * Find violations by severity
     */
    List<Violation> findBySeverityOrderByCreatedAtDesc(ComplianceRule.RuleSeverity severity);
    
    /**
     * Find violations for a domain
     */
    @Query("SELECT v FROM Violation v WHERE v.analysis.domain = :domain ORDER BY v.severity DESC, v.createdAt DESC")
    List<Violation> findByDomainOrderBySeverityDesc(Domain domain);
    
    /**
     * Count violations by rule for a domain
     */
    @Query("SELECT COUNT(v) FROM Violation v WHERE v.analysis.domain = :domain AND v.rule = :rule")
    long countByDomainAndRule(Domain domain, ComplianceRule rule);
    
    /**
     * Count violations by severity for a domain
     */
    @Query("SELECT COUNT(v) FROM Violation v WHERE v.analysis.domain = :domain AND v.severity = :severity")
    long countByDomainAndSeverity(Domain domain, ComplianceRule.RuleSeverity severity);
    
    /**
     * Find most common violations (by rule) in the last 30 days
     */
    @Query("SELECT v.rule, COUNT(v) as violationCount FROM Violation v WHERE v.createdAt >= :since GROUP BY v.rule ORDER BY violationCount DESC")
    List<Object[]> findMostCommonViolationsSince(LocalDateTime since);
    
    /**
     * Find violations by domain and rule
     */
    @Query("SELECT v FROM Violation v WHERE v.analysis.domain = :domain AND v.rule = :rule ORDER BY v.createdAt DESC")
    List<Violation> findByDomainAndRule(Domain domain, ComplianceRule rule);
    
    /**
     * Find recent critical violations
     */
    @Query("SELECT v FROM Violation v WHERE v.severity = 'CRITICAL' AND v.createdAt >= :since ORDER BY v.createdAt DESC")
    List<Violation> findRecentCriticalViolations(LocalDateTime since);
    
    /**
     * Delete violations by analysis
     */
    void deleteByAnalysis(AdAnalysis analysis);
    
    /**
     * Count violations by analysis domain
     */
    @Query("SELECT COUNT(v) FROM Violation v WHERE v.analysis.domain = :domain")
    long countByAnalysisDomain(Domain domain);
}