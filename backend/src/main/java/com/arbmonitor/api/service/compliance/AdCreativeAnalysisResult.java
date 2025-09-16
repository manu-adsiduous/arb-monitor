package com.arbmonitor.api.service.compliance;

/**
 * Result object for ad creative analysis
 * Contains OCR text, visual analysis, audio transcription, and compliance results
 */
public class AdCreativeAnalysisResult {
    
    private String metaAdId;
    private String textContent;
    private String ocrText;
    private String visualAnalysis;
    private String audioTranscription;
    private ComplianceAnalysisResult complianceResult;
    private String error;
    
    // Constructors
    public AdCreativeAnalysisResult() {}
    
    public AdCreativeAnalysisResult(String metaAdId) {
        this.metaAdId = metaAdId;
    }
    
    // Getters and Setters
    public String getMetaAdId() {
        return metaAdId;
    }
    
    public void setMetaAdId(String metaAdId) {
        this.metaAdId = metaAdId;
    }
    
    public String getTextContent() {
        return textContent;
    }
    
    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }
    
    public String getOcrText() {
        return ocrText;
    }
    
    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }
    
    public String getVisualAnalysis() {
        return visualAnalysis;
    }
    
    public void setVisualAnalysis(String visualAnalysis) {
        this.visualAnalysis = visualAnalysis;
    }
    
    public String getAudioTranscription() {
        return audioTranscription;
    }
    
    public void setAudioTranscription(String audioTranscription) {
        this.audioTranscription = audioTranscription;
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
    
    public boolean hasOcrText() {
        return ocrText != null && !ocrText.trim().isEmpty() && 
               !ocrText.contains("No text detected") && !ocrText.contains("failed");
    }
    
    public boolean hasAudioTranscription() {
        return audioTranscription != null && !audioTranscription.trim().isEmpty() && 
               !audioTranscription.contains("No video content") && !audioTranscription.contains("not yet implemented");
    }
    
    public boolean hasVisualAnalysis() {
        return visualAnalysis != null && !visualAnalysis.trim().isEmpty() && 
               !visualAnalysis.contains("failed");
    }
    
    @Override
    public String toString() {
        return "AdCreativeAnalysisResult{" +
                "metaAdId='" + metaAdId + '\'' +
                ", hasOcrText=" + hasOcrText() +
                ", hasVisualAnalysis=" + hasVisualAnalysis() +
                ", hasAudioTranscription=" + hasAudioTranscription() +
                ", hasError=" + hasError() +
                '}';
    }
}


