package com.arbmonitor.api.repository;

import com.arbmonitor.api.model.ComplianceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplianceRuleRepository extends JpaRepository<ComplianceRule, Long> {
    
    /**
     * Find all active compliance rules
     */
    List<ComplianceRule> findByActiveTrue();
    
    /**
     * Find rules by category
     */
    List<ComplianceRule> findByCategoryAndActiveTrue(ComplianceRule.RuleCategory category);
    
    /**
     * Find rules by severity
     */
    List<ComplianceRule> findBySeverityAndActiveTrue(ComplianceRule.RuleSeverity severity);
    
    /**
     * Find rules that have pattern matching capabilities
     */
    @Query("SELECT r FROM ComplianceRule r WHERE r.active = true AND r.rulePattern IS NOT NULL AND r.rulePattern != ''")
    List<ComplianceRule> findActiveRulesWithPatterns();
    
    /**
     * Find rules by category and severity
     */
    List<ComplianceRule> findByCategoryAndSeverityAndActiveTrue(
        ComplianceRule.RuleCategory category, 
        ComplianceRule.RuleSeverity severity
    );
    
    /**
     * Count active rules by category
     */
    @Query("SELECT COUNT(r) FROM ComplianceRule r WHERE r.active = true AND r.category = :category")
    long countActiveByCategoryAndActiveTrue(ComplianceRule.RuleCategory category);
}