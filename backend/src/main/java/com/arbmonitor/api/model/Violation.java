package com.arbmonitor.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "violations")
@EntityListeners(AuditingEntityListener.class)
public class Violation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    @JsonIgnore
    private AdAnalysis analysis;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private ComplianceRule rule;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplianceRule.RuleSeverity severity;
    
    @Column(name = "violated_text", columnDefinition = "TEXT")
    private String violatedText; // The specific text that caused the violation
    
    @Column(name = "context_info", columnDefinition = "TEXT")
    private String contextInfo; // Additional context about where the violation was found
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Constructors
    public Violation() {}
    
    public Violation(AdAnalysis analysis, ComplianceRule rule, String description) {
        this.analysis = analysis;
        this.rule = rule;
        this.description = description;
        this.severity = rule.getSeverity();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public AdAnalysis getAnalysis() {
        return analysis;
    }
    
    public void setAnalysis(AdAnalysis analysis) {
        this.analysis = analysis;
    }
    
    public ComplianceRule getRule() {
        return rule;
    }
    
    public void setRule(ComplianceRule rule) {
        this.rule = rule;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public ComplianceRule.RuleSeverity getSeverity() {
        return severity;
    }
    
    public void setSeverity(ComplianceRule.RuleSeverity severity) {
        this.severity = severity;
    }
    
    public String getViolatedText() {
        return violatedText;
    }
    
    public void setViolatedText(String violatedText) {
        this.violatedText = violatedText;
    }
    
    public String getContextInfo() {
        return contextInfo;
    }
    
    public void setContextInfo(String contextInfo) {
        this.contextInfo = contextInfo;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
