package com.arbmonitor.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ad_analyses")
@EntityListeners(AuditingEntityListener.class)
public class AdAnalysis {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    @JsonIgnore
    private Domain domain;
    
    @Column(name = "meta_ad_id", nullable = false)
    @NotBlank
    private String metaAdId;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    // @NotBlank - temporarily disabled for debugging
    private String headline;
    
    @Column(name = "primary_text", columnDefinition = "TEXT")
    private String primaryText;
    
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;
    
    @Column(name = "image_text", columnDefinition = "TEXT")
    private String imageText; // Extracted via OCR
    
    @Column(name = "landing_page_url", columnDefinition = "TEXT")
    private String landingPageUrl;
    
    @Column(name = "compliance_score")
    private Double complianceScore;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_status")
    private ComplianceStatus complianceStatus;
    
    // New binary compliance fields
    @Column(name = "ad_creative_compliant")
    private Boolean adCreativeCompliant;
    
    @Column(name = "ad_creative_reason", columnDefinition = "TEXT")
    private String adCreativeReason;
    
    @Column(name = "landing_page_relevant")
    private Boolean landingPageRelevant;
    
    @Column(name = "landing_page_reason", columnDefinition = "TEXT")
    private String landingPageReason;
    
    @Column(name = "rac_relevant")
    private Boolean racRelevant;
    
    @Column(name = "rac_reason", columnDefinition = "TEXT")
    private String racReason;
    
    @Column(name = "overall_compliant")
    private Boolean overallCompliant;
    
    @Column(name = "referrer_ad_creative", columnDefinition = "TEXT")
    private String referrerAdCreative;
    
    @Column(name = "landing_page_content", columnDefinition = "TEXT")
    private String landingPageContent;
    
    @Column(name = "analysis_notes", columnDefinition = "TEXT")
    private String analysisNotes;
    
    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Violation> violations = new HashSet<>();
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // Constructors
    public AdAnalysis() {}
    
    public AdAnalysis(Domain domain, String metaAdId, String headline) {
        this.domain = domain;
        this.metaAdId = metaAdId;
        this.headline = headline;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Domain getDomain() {
        return domain;
    }
    
    public void setDomain(Domain domain) {
        this.domain = domain;
    }
    
    public String getMetaAdId() {
        return metaAdId;
    }
    
    public void setMetaAdId(String metaAdId) {
        this.metaAdId = metaAdId;
    }
    
    public String getHeadline() {
        return headline;
    }
    
    public void setHeadline(String headline) {
        this.headline = headline;
    }
    
    public String getPrimaryText() {
        return primaryText;
    }
    
    public void setPrimaryText(String primaryText) {
        this.primaryText = primaryText;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getImageText() {
        return imageText;
    }
    
    public void setImageText(String imageText) {
        this.imageText = imageText;
    }
    
    public String getLandingPageUrl() {
        return landingPageUrl;
    }
    
    public void setLandingPageUrl(String landingPageUrl) {
        this.landingPageUrl = landingPageUrl;
    }
    
    public Double getComplianceScore() {
        return complianceScore;
    }
    
    public void setComplianceScore(Double complianceScore) {
        this.complianceScore = complianceScore;
    }
    
    public ComplianceStatus getComplianceStatus() {
        return complianceStatus;
    }
    
    public void setComplianceStatus(ComplianceStatus complianceStatus) {
        this.complianceStatus = complianceStatus;
    }
    
    public String getReferrerAdCreative() {
        return referrerAdCreative;
    }
    
    public void setReferrerAdCreative(String referrerAdCreative) {
        this.referrerAdCreative = referrerAdCreative;
    }
    
    public String getLandingPageContent() {
        return landingPageContent;
    }
    
    public void setLandingPageContent(String landingPageContent) {
        this.landingPageContent = landingPageContent;
    }
    
    public String getAnalysisNotes() {
        return analysisNotes;
    }
    
    public void setAnalysisNotes(String analysisNotes) {
        this.analysisNotes = analysisNotes;
    }
    
    public Boolean getAdCreativeCompliant() {
        return adCreativeCompliant;
    }
    
    public void setAdCreativeCompliant(Boolean adCreativeCompliant) {
        this.adCreativeCompliant = adCreativeCompliant;
    }
    
    public String getAdCreativeReason() {
        return adCreativeReason;
    }
    
    public void setAdCreativeReason(String adCreativeReason) {
        this.adCreativeReason = adCreativeReason;
    }
    
    public Boolean getLandingPageRelevant() {
        return landingPageRelevant;
    }
    
    public void setLandingPageRelevant(Boolean landingPageRelevant) {
        this.landingPageRelevant = landingPageRelevant;
    }
    
    public String getLandingPageReason() {
        return landingPageReason;
    }
    
    public void setLandingPageReason(String landingPageReason) {
        this.landingPageReason = landingPageReason;
    }
    
    public Boolean getRacRelevant() {
        return racRelevant;
    }
    
    public void setRacRelevant(Boolean racRelevant) {
        this.racRelevant = racRelevant;
    }
    
    public String getRacReason() {
        return racReason;
    }
    
    public void setRacReason(String racReason) {
        this.racReason = racReason;
    }
    
    public Boolean getOverallCompliant() {
        return overallCompliant;
    }
    
    public void setOverallCompliant(Boolean overallCompliant) {
        this.overallCompliant = overallCompliant;
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
    
    public enum ComplianceStatus {
        EXCELLENT,  // 90-100
        GOOD,       // 75-89
        WARNING,    // 60-74
        POOR,       // 40-59
        CRITICAL    // 0-39
    }
}
