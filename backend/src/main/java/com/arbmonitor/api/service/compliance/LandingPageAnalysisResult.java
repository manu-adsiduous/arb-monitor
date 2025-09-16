package com.arbmonitor.api.service.compliance;

/**
 * Result object for landing page analysis
 * Contains page content, screenshot path, and compliance results
 */
public class LandingPageAnalysisResult {
    
    private String metaAdId;
    private String landingPageUrl;
    private String pageContent;
    private String screenshotPath;
    private ComplianceAnalysisResult complianceResult;
    private String error;
    
    // Constructors
    public LandingPageAnalysisResult() {}
    
    public LandingPageAnalysisResult(String metaAdId, String landingPageUrl) {
        this.metaAdId = metaAdId;
        this.landingPageUrl = landingPageUrl;
    }
    
    // Getters and Setters
    public String getMetaAdId() {
        return metaAdId;
    }
    
    public void setMetaAdId(String metaAdId) {
        this.metaAdId = metaAdId;
    }
    
    public String getLandingPageUrl() {
        return landingPageUrl;
    }
    
    public void setLandingPageUrl(String landingPageUrl) {
        this.landingPageUrl = landingPageUrl;
    }
    
    public String getPageContent() {
        return pageContent;
    }
    
    public void setPageContent(String pageContent) {
        this.pageContent = pageContent;
    }
    
    public String getScreenshotPath() {
        return screenshotPath;
    }
    
    public void setScreenshotPath(String screenshotPath) {
        this.screenshotPath = screenshotPath;
    }
    
    public ComplianceAnalysisResult getComplianceResult() {
        return complianceResult;
    }
    
    public void setComplianceResult(ComplianceAnalysisResult complianceResult) {
        this.complianceResult = complianceResult;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    // Utility methods
    public boolean hasError() {
        return error != null && !error.trim().isEmpty();
    }
    
    public boolean hasPageContent() {
        return pageContent != null && !pageContent.trim().isEmpty();
    }
    
    public boolean hasScreenshot() {
        return screenshotPath != null && !screenshotPath.trim().isEmpty();
    }
    
    public boolean isAccessible() {
        return hasPageContent() && !hasError();
    }
    
    @Override
    public String toString() {
        return "LandingPageAnalysisResult{" +
                "metaAdId='" + metaAdId + '\'' +
                ", landingPageUrl='" + landingPageUrl + '\'' +
                ", hasPageContent=" + hasPageContent() +
                ", hasScreenshot=" + hasScreenshot() +
                ", hasError=" + hasError() +
                '}';
    }
}


