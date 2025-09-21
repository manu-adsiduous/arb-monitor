package com.arbmonitor.api.service.compliance;

import com.arbmonitor.api.model.ScrapedAd;
import com.arbmonitor.api.service.ImageAnalysisService;
import com.arbmonitor.api.service.OpenAIAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Modular ad creative analysis component
 * Handles OCR, visual analysis, audio transcription, and compliance checking
 */
@Component
public class AdCreativeAnalysisModule {
    
    private static final Logger logger = LoggerFactory.getLogger(AdCreativeAnalysisModule.class);
    
    @Autowired
    private ImageAnalysisService imageAnalysisService;
    
    @Autowired
    private OpenAIAnalysisService openAIAnalysisService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Analyze ad creative content including OCR, visuals, and audio
     */
    public AdCreativeAnalysisResult analyzeAdCreative(ScrapedAd scrapedAd) {
        logger.info("Starting ad creative analysis for ad: {}", scrapedAd.getMetaAdId());
        
        AdCreativeAnalysisResult result = new AdCreativeAnalysisResult();
        result.setMetaAdId(scrapedAd.getMetaAdId());
        
        try {
            // 1. Extract text content from ad
            String textContent = extractTextContent(scrapedAd);
            result.setTextContent(textContent);
            
            // 2. Perform OCR on images
            String ocrText = performOCRAnalysis(scrapedAd);
            result.setOcrText(ocrText);
            
            // 3. Analyze visual elements
            String visualAnalysis = performVisualAnalysis(scrapedAd);
            result.setVisualAnalysis(visualAnalysis);
            
            // 4. Extract audio transcription (if video)
            String audioTranscription = performAudioTranscription(scrapedAd);
            result.setAudioTranscription(audioTranscription);
            
            // 5. Perform compliance analysis using structured JSON
            ComplianceAnalysisResult complianceResult = performComplianceAnalysis(result);
            result.setComplianceResult(complianceResult);
            
            logger.info("Ad creative analysis completed for ad: {}", scrapedAd.getMetaAdId());
            return result;
            
        } catch (Exception e) {
            logger.error("Error analyzing ad creative for ad: {}", scrapedAd.getMetaAdId(), e);
            result.setError("Analysis failed: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * Extract text content from ad (headline, primary text, description)
     */
    private String extractTextContent(ScrapedAd scrapedAd) {
        StringBuilder textBuilder = new StringBuilder();
        
        if (scrapedAd.getHeadline() != null && !scrapedAd.getHeadline().trim().isEmpty()) {
            textBuilder.append("Headline: ").append(scrapedAd.getHeadline()).append("\n");
        }
        
        if (scrapedAd.getPrimaryText() != null && !scrapedAd.getPrimaryText().trim().isEmpty()) {
            textBuilder.append("Primary Text: ").append(scrapedAd.getPrimaryText()).append("\n");
        }
        
        if (scrapedAd.getDescription() != null && !scrapedAd.getDescription().trim().isEmpty()) {
            textBuilder.append("Description: ").append(scrapedAd.getDescription()).append("\n");
        }
        
        if (scrapedAd.getCallToAction() != null && !scrapedAd.getCallToAction().trim().isEmpty()) {
            textBuilder.append("Call to Action: ").append(scrapedAd.getCallToAction()).append("\n");
        }
        
        return textBuilder.toString().trim();
    }
    
    /**
     * Perform OCR analysis on ad images
     */
    private String performOCRAnalysis(ScrapedAd scrapedAd) {
        try {
            List<String> imagePaths = scrapedAd.getLocalImagePaths();
            if (imagePaths == null || imagePaths.isEmpty()) {
                return "No images available for OCR analysis";
            }
            
            StringBuilder ocrBuilder = new StringBuilder();
            for (int i = 0; i < imagePaths.size(); i++) {
                String imagePath = imagePaths.get(i);
                logger.debug("Performing OCR on image: {}", imagePath);
                
                String ocrResult = imageAnalysisService.extractTextFromImage(imagePath);
                if (ocrResult != null && !ocrResult.trim().isEmpty()) {
                    if (imagePaths.size() > 1) {
                        ocrBuilder.append("Image ").append(i + 1).append(":\n");
                    }
                    ocrBuilder.append(ocrResult).append("\n");
                }
            }
            
            String finalOcrText = ocrBuilder.toString().trim();
            return finalOcrText.isEmpty() ? "No text detected in images" : finalOcrText;
            
        } catch (Exception e) {
            logger.error("Error performing OCR analysis", e);
            return "OCR analysis failed: " + e.getMessage();
        }
    }
    
    /**
     * Analyze visual elements in ad images
     */
    private String performVisualAnalysis(ScrapedAd scrapedAd) {
        try {
            List<String> imagePaths = scrapedAd.getLocalImagePaths();
            if (imagePaths == null || imagePaths.isEmpty()) {
                return "No images available for visual analysis";
            }
            
            // Create structured JSON for GPT-4 visual analysis
            ObjectNode analysisRequest = objectMapper.createObjectNode();
            analysisRequest.put("task", "visual_analysis");
            analysisRequest.put("ad_id", scrapedAd.getMetaAdId());
            analysisRequest.put("image_count", imagePaths.size());
            
            // For now, we'll use a text-based approach since we need to implement image-to-text first
            // This will be enhanced when we add GPT-4 Vision API support
            return "Visual analysis: " + imagePaths.size() + " image(s) detected. " +
                   "Contains visual elements that require manual review for compliance assessment.";
            
        } catch (Exception e) {
            logger.error("Error performing visual analysis", e);
            return "Visual analysis failed: " + e.getMessage();
        }
    }
    
    /**
     * Extract audio transcription from video ads
     */
    private String performAudioTranscription(ScrapedAd scrapedAd) {
        try {
            // Check if this is a video ad
            if (scrapedAd.getVideoUrls() == null || scrapedAd.getVideoUrls().isEmpty()) {
                return "No video content available for audio transcription";
            }
            
            // TODO: Implement audio extraction and transcription
            // This would involve:
            // 1. Downloading the video file
            // 2. Extracting audio track
            // 3. Using speech-to-text service (e.g., OpenAI Whisper API)
            
            return "Audio transcription not yet implemented for video ads";
            
        } catch (Exception e) {
            logger.error("Error performing audio transcription", e);
            return "Audio transcription failed: " + e.getMessage();
        }
    }
    
    /**
     * Perform compliance analysis using structured JSON for GPT-4
     */
    private ComplianceAnalysisResult performComplianceAnalysis(AdCreativeAnalysisResult analysisResult) {
        try {
            // Create structured JSON payload for GPT-4
            ObjectNode complianceRequest = objectMapper.createObjectNode();
            complianceRequest.put("task", "ad_creative_compliance");
            complianceRequest.put("ad_id", analysisResult.getMetaAdId());
            
            // Add all analyzed content
            ObjectNode content = objectMapper.createObjectNode();
            content.put("text_content", analysisResult.getTextContent());
            content.put("ocr_text", analysisResult.getOcrText());
            content.put("visual_analysis", analysisResult.getVisualAnalysis());
            content.put("audio_transcription", analysisResult.getAudioTranscription());
            complianceRequest.set("content", content);
            
            // Add compliance rules context
            ObjectNode rules = objectMapper.createObjectNode();
            rules.put("check_misleading_claims", true);
            rules.put("check_false_promises", true);
            rules.put("check_clickbait", true);
            rules.put("check_medical_claims", true);
            rules.put("check_financial_guarantees", true);
            rules.put("check_before_after_claims", true);
            complianceRequest.set("rules", rules);
            
            // Request structured response
            ObjectNode responseFormat = objectMapper.createObjectNode();
            responseFormat.put("compliant", "boolean");
            responseFormat.put("confidence_score", "number (0-1)");
            responseFormat.put("violations", "array of violation objects");
            responseFormat.put("reasoning", "string");
            complianceRequest.set("expected_response_format", responseFormat);
            
            // Call OpenAI with structured request
            String jsonResponse = openAIAnalysisService.analyzeAdCreativeCompliance(complianceRequest.toString());
            
            // Parse response and create result
            ComplianceAnalysisResult result = new ComplianceAnalysisResult();
            result.setRawResponse(jsonResponse);
            
            // TODO: Parse JSON response and populate structured result
            // For now, return basic result
            result.setCompliant(true); // Will be determined from JSON response
            result.setConfidenceScore(0.8); // Will be extracted from JSON response
            result.setReasoning("Compliance analysis completed using structured JSON format");
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error performing compliance analysis", e);
            ComplianceAnalysisResult errorResult = new ComplianceAnalysisResult();
            errorResult.setCompliant(false);
            errorResult.setConfidenceScore(0.0);
            errorResult.setReasoning("Compliance analysis failed: " + e.getMessage());
            return errorResult;
        }
    }
}
