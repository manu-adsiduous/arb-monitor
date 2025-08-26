package com.arbmonitor.api.dto;

import com.arbmonitor.api.model.ComplianceRule.RuleCategory;
import com.arbmonitor.api.model.ComplianceRule.RuleSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateComplianceRuleDTO {
    
    @NotBlank(message = "Rule name is required")
    @Size(max = 100, message = "Rule name must not exceed 100 characters")
    private String ruleName;
    
    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    @Size(max = 1000, message = "Examples must not exceed 1000 characters")
    private String examples;
    
    @NotNull(message = "Category is required")
    private RuleCategory category;
    
    @NotNull(message = "Severity is required")
    private RuleSeverity severity;
    
    private Boolean active = true;
    
    @Size(max = 200, message = "Rule pattern must not exceed 200 characters")
    private String rulePattern;
    
    // Default constructor
    public CreateComplianceRuleDTO() {}
    
    // Constructor with all fields
    public CreateComplianceRuleDTO(String ruleName, String description, String examples, 
                                 RuleCategory category, RuleSeverity severity, Boolean active, String rulePattern) {
        this.ruleName = ruleName;
        this.description = description;
        this.examples = examples;
        this.category = category;
        this.severity = severity;
        this.active = active;
        this.rulePattern = rulePattern;
    }
    
    // Getters and setters
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
}