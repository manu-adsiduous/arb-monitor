package com.arbmonitor.api.dto;

import com.arbmonitor.api.model.ComplianceRule;
import jakarta.validation.constraints.Size;

public class UpdateComplianceRuleDTO {
    
    @Size(min = 3, max = 200, message = "Rule name must be between 3 and 200 characters")
    private String ruleName;
    
    private String description;
    
    private String examples;
    
    private ComplianceRule.RuleCategory category;
    
    private ComplianceRule.RuleSeverity severity;
    
    private Boolean active;
    
    private String rulePattern;
    
    // Constructors
    public UpdateComplianceRuleDTO() {}
    
    // Getters and Setters
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
    
    public ComplianceRule.RuleCategory getCategory() {
        return category;
    }
    
    public void setCategory(ComplianceRule.RuleCategory category) {
        this.category = category;
    }
    
    public ComplianceRule.RuleSeverity getSeverity() {
        return severity;
    }
    
    public void setSeverity(ComplianceRule.RuleSeverity severity) {
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
}