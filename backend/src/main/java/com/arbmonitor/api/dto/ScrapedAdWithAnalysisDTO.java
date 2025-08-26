package com.arbmonitor.api.dto;

import com.arbmonitor.api.model.AdAnalysis;
import com.arbmonitor.api.model.ScrapedAd;
import com.arbmonitor.api.model.Violation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ScrapedAdWithAnalysisDTO {
    
    // ScrapedAd fields
    private Long id;
    private String metaAdId;
    private String domainName;
    private String pageName;
    private String pageId;
    private String primaryText;
    private String headline;
    private String description;
    private String callToAction;
    private String landingPageUrl;
    private String displayUrl;
    private List<String> imageUrls;
    private List<String> videoUrls;
    private List<String> localImagePaths;
    private List<String> localVideoPaths;
    private String adFormat;
    private String fundingEntity;
    private LocalDateTime adCreationDate;
    private LocalDateTime adDeliveryStartDate;
    private LocalDateTime adDeliveryStopDate;
    private boolean isActive;
    private Integer spendRangeLower;
    private Integer spendRangeUpper;
    private Long impressionsRangeLower;
    private Long impressionsRangeUpper;
    private LocalDateTime scrapedAt;
    private LocalDateTime lastUpdated;
    
    // Compliance analysis data
    private ComplianceAnalysisDTO complianceAnalysis;
    
    public static class ComplianceAnalysisDTO {
        private Long id;
        private Double complianceScore;
        private AdAnalysis.ComplianceStatus complianceStatus;
        private String analysisNotes;
        private List<ViolationDTO> violations;
        
        // New binary compliance fields
        private Boolean adCreativeCompliant;
        private String adCreativeReason;
        private Boolean landingPageRelevant;
        private String landingPageReason;
        private Boolean racRelevant;
        private String racReason;
        private Boolean overallCompliant;
        
        // Constructors
        public ComplianceAnalysisDTO() {}
        
        public ComplianceAnalysisDTO(AdAnalysis analysis) {
            if (analysis != null) {
                this.id = analysis.getId();
                this.complianceScore = analysis.getComplianceScore();
                this.complianceStatus = analysis.getComplianceStatus();
                this.analysisNotes = analysis.getAnalysisNotes();
                this.violations = analysis.getViolations().stream()
                    .map(ViolationDTO::new)
                    .collect(Collectors.toList());
                
                // New binary compliance fields
                this.adCreativeCompliant = analysis.getAdCreativeCompliant();
                this.adCreativeReason = analysis.getAdCreativeReason();
                this.landingPageRelevant = analysis.getLandingPageRelevant();
                this.landingPageReason = analysis.getLandingPageReason();
                this.racRelevant = analysis.getRacRelevant();
                this.racReason = analysis.getRacReason();
                this.overallCompliant = analysis.getOverallCompliant();
            }
        }
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public Double getComplianceScore() { return complianceScore; }
        public void setComplianceScore(Double complianceScore) { this.complianceScore = complianceScore; }
        
        public AdAnalysis.ComplianceStatus getComplianceStatus() { return complianceStatus; }
        public void setComplianceStatus(AdAnalysis.ComplianceStatus complianceStatus) { this.complianceStatus = complianceStatus; }
        
        public String getAnalysisNotes() { return analysisNotes; }
        public void setAnalysisNotes(String analysisNotes) { this.analysisNotes = analysisNotes; }
        
        public List<ViolationDTO> getViolations() { return violations; }
        public void setViolations(List<ViolationDTO> violations) { this.violations = violations; }
        
        public Boolean getAdCreativeCompliant() { return adCreativeCompliant; }
        public void setAdCreativeCompliant(Boolean adCreativeCompliant) { this.adCreativeCompliant = adCreativeCompliant; }
        
        public String getAdCreativeReason() { return adCreativeReason; }
        public void setAdCreativeReason(String adCreativeReason) { this.adCreativeReason = adCreativeReason; }
        
        public Boolean getLandingPageRelevant() { return landingPageRelevant; }
        public void setLandingPageRelevant(Boolean landingPageRelevant) { this.landingPageRelevant = landingPageRelevant; }
        
        public String getLandingPageReason() { return landingPageReason; }
        public void setLandingPageReason(String landingPageReason) { this.landingPageReason = landingPageReason; }
        
        public Boolean getRacRelevant() { return racRelevant; }
        public void setRacRelevant(Boolean racRelevant) { this.racRelevant = racRelevant; }
        
        public String getRacReason() { return racReason; }
        public void setRacReason(String racReason) { this.racReason = racReason; }
        
        public Boolean getOverallCompliant() { return overallCompliant; }
        public void setOverallCompliant(Boolean overallCompliant) { this.overallCompliant = overallCompliant; }
    }
    
    public static class ViolationDTO {
        private Long id;
        private String description;
        private String severity;
        private String violatedText;
        private ComplianceRuleDTO rule;
        
        public ViolationDTO() {}
        
        public ViolationDTO(Violation violation) {
            if (violation != null) {
                this.id = violation.getId();
                this.description = violation.getDescription();
                this.severity = violation.getSeverity().toString();
                this.violatedText = violation.getViolatedText();
                if (violation.getRule() != null) {
                    this.rule = new ComplianceRuleDTO(violation.getRule());
                }
            }
        }
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        
        public String getViolatedText() { return violatedText; }
        public void setViolatedText(String violatedText) { this.violatedText = violatedText; }
        
        public ComplianceRuleDTO getRule() { return rule; }
        public void setRule(ComplianceRuleDTO rule) { this.rule = rule; }
    }
    
    public static class ComplianceRuleDTO {
        private Long id;
        private String ruleName;
        private String description;
        private String category;
        
        public ComplianceRuleDTO() {}
        
        public ComplianceRuleDTO(com.arbmonitor.api.model.ComplianceRule rule) {
            if (rule != null) {
                this.id = rule.getId();
                this.ruleName = rule.getRuleName();
                this.description = rule.getDescription();
                this.category = rule.getCategory().toString();
            }
        }
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
    
    // Constructor
    public ScrapedAdWithAnalysisDTO() {}
    
    public ScrapedAdWithAnalysisDTO(ScrapedAd ad, AdAnalysis analysis) {
        // Copy ScrapedAd fields
        this.id = ad.getId();
        this.metaAdId = ad.getMetaAdId();
        this.domainName = ad.getDomainName();
        this.pageName = ad.getPageName();
        this.pageId = ad.getPageId();
        this.primaryText = ad.getPrimaryText();
        this.headline = ad.getHeadline();
        this.description = ad.getDescription();
        this.callToAction = ad.getCallToAction();
        this.landingPageUrl = ad.getLandingPageUrl();
        this.displayUrl = ad.getDisplayUrl();
        this.imageUrls = ad.getImageUrls();
        this.videoUrls = ad.getVideoUrls();
        this.localImagePaths = ad.getLocalImagePaths();
        this.localVideoPaths = ad.getLocalVideoPaths();
        this.adFormat = ad.getAdFormat();
        this.fundingEntity = ad.getFundingEntity();
        this.adCreationDate = ad.getAdCreationDate();
        this.adDeliveryStartDate = ad.getAdDeliveryStartDate();
        this.adDeliveryStopDate = ad.getAdDeliveryStopDate();
        this.isActive = ad.getIsActive();
        this.spendRangeLower = ad.getSpendRangeLower();
        this.spendRangeUpper = ad.getSpendRangeUpper();
        this.impressionsRangeLower = ad.getImpressionsRangeLower();
        this.impressionsRangeUpper = ad.getImpressionsRangeUpper();
        this.scrapedAt = ad.getScrapedAt();
        this.lastUpdated = ad.getLastUpdated();
        
        // Set compliance analysis if available
        if (analysis != null) {
            this.complianceAnalysis = new ComplianceAnalysisDTO(analysis);
        }
    }
    
    // Getters and setters for ScrapedAd fields
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getMetaAdId() { return metaAdId; }
    public void setMetaAdId(String metaAdId) { this.metaAdId = metaAdId; }
    
    public String getDomainName() { return domainName; }
    public void setDomainName(String domainName) { this.domainName = domainName; }
    
    public String getPageName() { return pageName; }
    public void setPageName(String pageName) { this.pageName = pageName; }
    
    public String getPageId() { return pageId; }
    public void setPageId(String pageId) { this.pageId = pageId; }
    
    public String getPrimaryText() { return primaryText; }
    public void setPrimaryText(String primaryText) { this.primaryText = primaryText; }
    
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCallToAction() { return callToAction; }
    public void setCallToAction(String callToAction) { this.callToAction = callToAction; }
    
    public String getLandingPageUrl() { return landingPageUrl; }
    public void setLandingPageUrl(String landingPageUrl) { this.landingPageUrl = landingPageUrl; }
    
    public String getDisplayUrl() { return displayUrl; }
    public void setDisplayUrl(String displayUrl) { this.displayUrl = displayUrl; }
    
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    
    public List<String> getVideoUrls() { return videoUrls; }
    public void setVideoUrls(List<String> videoUrls) { this.videoUrls = videoUrls; }
    
    public List<String> getLocalImagePaths() { return localImagePaths; }
    public void setLocalImagePaths(List<String> localImagePaths) { this.localImagePaths = localImagePaths; }
    
    public List<String> getLocalVideoPaths() { return localVideoPaths; }
    public void setLocalVideoPaths(List<String> localVideoPaths) { this.localVideoPaths = localVideoPaths; }
    
    public String getAdFormat() { return adFormat; }
    public void setAdFormat(String adFormat) { this.adFormat = adFormat; }
    
    public String getFundingEntity() { return fundingEntity; }
    public void setFundingEntity(String fundingEntity) { this.fundingEntity = fundingEntity; }
    
    public LocalDateTime getAdCreationDate() { return adCreationDate; }
    public void setAdCreationDate(LocalDateTime adCreationDate) { this.adCreationDate = adCreationDate; }
    
    public LocalDateTime getAdDeliveryStartDate() { return adDeliveryStartDate; }
    public void setAdDeliveryStartDate(LocalDateTime adDeliveryStartDate) { this.adDeliveryStartDate = adDeliveryStartDate; }
    
    public LocalDateTime getAdDeliveryStopDate() { return adDeliveryStopDate; }
    public void setAdDeliveryStopDate(LocalDateTime adDeliveryStopDate) { this.adDeliveryStopDate = adDeliveryStopDate; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public Integer getSpendRangeLower() { return spendRangeLower; }
    public void setSpendRangeLower(Integer spendRangeLower) { this.spendRangeLower = spendRangeLower; }
    
    public Integer getSpendRangeUpper() { return spendRangeUpper; }
    public void setSpendRangeUpper(Integer spendRangeUpper) { this.spendRangeUpper = spendRangeUpper; }
    
    public Long getImpressionsRangeLower() { return impressionsRangeLower; }
    public void setImpressionsRangeLower(Long impressionsRangeLower) { this.impressionsRangeLower = impressionsRangeLower; }
    
    public Long getImpressionsRangeUpper() { return impressionsRangeUpper; }
    public void setImpressionsRangeUpper(Long impressionsRangeUpper) { this.impressionsRangeUpper = impressionsRangeUpper; }
    
    public LocalDateTime getScrapedAt() { return scrapedAt; }
    public void setScrapedAt(LocalDateTime scrapedAt) { this.scrapedAt = scrapedAt; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public ComplianceAnalysisDTO getComplianceAnalysis() { return complianceAnalysis; }
    public void setComplianceAnalysis(ComplianceAnalysisDTO complianceAnalysis) { this.complianceAnalysis = complianceAnalysis; }
}
