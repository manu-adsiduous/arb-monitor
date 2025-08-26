package com.arbmonitor.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "compliance_rules")
@EntityListeners(AuditingEntityListener.class)
public class ComplianceRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    @NotBlank
    @Size(min = 3, max = 200)
    private String ruleName;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String examples;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleCategory category;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleSeverity severity;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(name = "rule_pattern", columnDefinition = "TEXT")
    private String rulePattern; // Regex or keyword patterns for automated detection
    
    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Violation> violations = new HashSet<>();
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // Constructors
    public ComplianceRule() {}
    
    public ComplianceRule(String ruleName, String description, RuleCategory category, RuleSeverity severity) {
        this.ruleName = ruleName;
        this.description = description;
        this.category = category;
        this.severity = severity;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getRuleName() {
        return ruleName;
    }
    
    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getExamples() {
        return examples;
    }
    
    public void setExamples(String examples) {
        this.examples = examples;
    }
    
    public RuleCategory getCategory() {
        return category;
    }
    
    public void setCategory(RuleCategory category) {
        this.category = category;
    }
    
    public RuleSeverity getSeverity() {
        return severity;
    }
    
    public void setSeverity(RuleSeverity severity) {
        this.severity = severity;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    public String getRulePattern() {
        return rulePattern;
    }
    
    public void setRulePattern(String rulePattern) {
        this.rulePattern = rulePattern;
    }
    
    public Set<Violation> getViolations() {
        return violations;
    }
    
    public void setViolations(Set<Violation> violations) {
        this.violations = violations;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public enum RuleCategory {
        CREATIVE_CONTENT,
        LANDING_PAGE,
        CLAIM_SUBSTANTIATION,
        REFERRER_PARAMETER,
        IMAGE_TEXT,
        MEDICAL_CLAIMS,
        FINANCIAL_CLAIMS,
        GENERAL_COMPLIANCE
    }
    
    public enum RuleSeverity {
        CRITICAL, // -20 points
        MAJOR,    // -10 points
        MINOR     // -5 points
    }
}
