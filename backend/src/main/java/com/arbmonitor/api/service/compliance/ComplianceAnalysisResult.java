package com.arbmonitor.api.service.compliance;

import java.util.List;
import java.util.ArrayList;

/**
 * Result object for compliance analysis
 * Contains structured compliance assessment results
 */
public class ComplianceAnalysisResult {
    
    private boolean compliant;
    private double confidenceScore;
    private String reasoning;
    private List<ComplianceViolation> violations;
    private String rawResponse;
    
    // Constructors
    public ComplianceAnalysisResult() {
        this.violations = new ArrayList<>();
    }
    
    public ComplianceAnalysisResult(boolean compliant, double confidenceScore, String reasoning) {
        this.compliant = compliant;
        this.confidenceScore = confidenceScore;
        this.reasoning = reasoning;
        this.violations = new ArrayList<>();
    }
    
    // Getters and Setters
    public boolean isCompliant() {
        return compliant;
    }
    
    public void setCompliant(boolean compliant) {
        this.compliant = compliant;
    }
    
    public double getConfidenceScore() {
        return confidenceScore;
    }
    
    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
    
    public String getReasoning() {
        return reasoning;
    }
    
    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
    
    public List<ComplianceViolation> getViolations() {
        return violations;
    }
    
    public void setViolations(List<ComplianceViolation> violations) {
        this.violations = violations != null ? violations : new ArrayList<>();
    }
    
    public String getRawResponse() {
        return rawResponse;
    }
    
    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }
    
    // Utility methods
    public void addViolation(ComplianceViolation violation) {
        if (this.violations == null) {
            this.violations = new ArrayList<>();
        }
        this.violations.add(violation);
    }
    
    public boolean hasViolations() {
        return violations != null && !violations.isEmpty();
    }
    
    public int getViolationCount() {
        return violations != null ? violations.size() : 0;
    }
    
    public long getCriticalViolationCount() {
        if (violations == null) return 0;
        return violations.stream()
                .filter(v -> "CRITICAL".equalsIgnoreCase(v.getSeverity()))
                .count();
    }
    
    @Override
    public String toString() {
        return "ComplianceAnalysisResult{" +
                "compliant=" + compliant +
                ", confidenceScore=" + confidenceScore +
                ", violationCount=" + getViolationCount() +
                ", criticalViolations=" + getCriticalViolationCount() +
                '}';
    }
    
    /**
     * Inner class for compliance violations
     */
    public static class ComplianceViolation {
        private String ruleType;
        private String severity;
        private String description;
        private String violatedText;
        
        public ComplianceViolation() {}
        
        public ComplianceViolation(String ruleType, String severity, String description, String violatedText) {
            this.ruleType = ruleType;
            this.severity = severity;
            this.description = description;
            this.violatedText = violatedText;
        }
        
        // Getters and Setters
        public String getRuleType() {
            return ruleType;
        }
        
        public void setRuleType(String ruleType) {
            this.ruleType = ruleType;
        }
        
        public String getSeverity() {
            return severity;
        }
        
        public void setSeverity(String severity) {
            this.severity = severity;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getViolatedText() {
            return violatedText;
        }
        
        public void setViolatedText(String violatedText) {
            this.violatedText = violatedText;
        }
        
        @Override
        public String toString() {
            return "ComplianceViolation{" +
                    "ruleType='" + ruleType + '\'' +
                    ", severity='" + severity + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        }
    }
}


