package com.arbmonitor.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for processing media files after download:
 * - Extract text from images using OCR
 * - Extract text from videos (frames + audio)
 * - Store extracted text in ScrapedAd entity
 */
@Service
public class MediaProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(MediaProcessingService.class);
    
    @Autowired
    private ImageAnalysisService imageAnalysisService;
    
    @Autowired
    private VideoAnalysisService videoAnalysisService;
    
    /**
     * Process all media files for a scraped ad and extract text content
     * This runs asynchronously after media download
     */
    @Async
    public CompletableFuture<MediaProcessingResult> processAdMedia(
            List<String> imagePaths, 
            List<String> videoPaths,
            String adId) {
        
        logger.info("Starting media processing for ad: {} (Images: {}, Videos: {})", 
                   adId, imagePaths != null ? imagePaths.size() : 0, 
                   videoPaths != null ? videoPaths.size() : 0);
        
        MediaProcessingResult result = new MediaProcessingResult();
        result.setAdId(adId);
        
        try {
            // Process images with OCR
            String imageText = processImages(imagePaths, adId);
            result.setExtractedImageText(imageText);
            
            // Process videos with frame OCR + audio transcription
            String videoText = processVideos(videoPaths, adId);
            result.setExtractedVideoText(videoText);
            
            result.setSuccess(true);
            logger.info("Media processing completed for ad: {} - Image text: {} chars, Video text: {} chars",
                       adId, 
                       imageText != null ? imageText.length() : 0,
                       videoText != null ? videoText.length() : 0);
            
        } catch (Exception e) {
            logger.error("Error processing media for ad: {}", adId, e);
            result.setSuccess(false);
            result.setError(e.getMessage());
        }
        
        return CompletableFuture.completedFuture(result);
    }
    
    /**
     * Process image files and extract text using OCR
     */
    private String processImages(List<String> imagePaths, String adId) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            logger.debug("No images to process for ad: {}", adId);
            return null;
        }
        
        StringBuilder allImageText = new StringBuilder();
        int processedCount = 0;
        
        for (String imagePath : imagePaths) {
            try {
                logger.debug("Processing image: {}", imagePath);
                String extractedText = imageAnalysisService.extractTextFromImage(imagePath);
                
                if (extractedText != null && !extractedText.trim().isEmpty()) {
                    if (allImageText.length() > 0) {
                        allImageText.append("\n\n--- IMAGE ").append(processedCount + 1).append(" ---\n");
                    }
                    allImageText.append(extractedText.trim());
                    processedCount++;
                    logger.debug("Extracted {} characters from image: {}", extractedText.length(), imagePath);
                }
                
            } catch (Exception e) {
                logger.warn("Failed to process image {}: {}", imagePath, e.getMessage());
            }
        }
        
        if (processedCount > 0) {
            logger.info("Successfully processed {} images for ad: {}, total text: {} characters", 
                       processedCount, adId, allImageText.length());
            return allImageText.toString();
        }
        
        return null;
    }
    
    /**
     * Process video files and extract text from frames + audio
     */
    private String processVideos(List<String> videoPaths, String adId) {
        if (videoPaths == null || videoPaths.isEmpty()) {
            logger.debug("No videos to process for ad: {}", adId);
            return null;
        }
        
        StringBuilder allVideoText = new StringBuilder();
        int processedCount = 0;
        
        for (String videoPath : videoPaths) {
            try {
                logger.debug("Processing video: {}", videoPath);
                VideoAnalysisService.VideoAnalysisResult result = videoAnalysisService.analyzeVideo(videoPath);
                
                if (result.hasError()) {
                    logger.warn("Video analysis failed for {}: {}", videoPath, result.getError());
                    continue;
                }
                
                if (result.hasText()) {
                    if (allVideoText.length() > 0) {
                        allVideoText.append("\n\n--- VIDEO ").append(processedCount + 1).append(" ---\n");
                    }
                    allVideoText.append(result.getCombinedText());
                    processedCount++;
                    logger.debug("Extracted {} characters from video: {}", 
                               result.getCombinedText().length(), videoPath);
                }
                
            } catch (Exception e) {
                logger.warn("Failed to process video {}: {}", videoPath, e.getMessage());
            }
        }
        
        if (processedCount > 0) {
            logger.info("Successfully processed {} videos for ad: {}, total text: {} characters", 
                       processedCount, adId, allVideoText.length());
            return allVideoText.toString();
        }
        
        return null;
    }
    
    /**
     * Result class for media processing
     */
    public static class MediaProcessingResult {
        private String adId;
        private String extractedImageText;
        private String extractedVideoText;
        private boolean success;
        private String error;
        
        // Getters and setters
        public String getAdId() { return adId; }
        public void setAdId(String adId) { this.adId = adId; }
        
        public String getExtractedImageText() { return extractedImageText; }
        public void setExtractedImageText(String extractedImageText) { this.extractedImageText = extractedImageText; }
        
        public String getExtractedVideoText() { return extractedVideoText; }
        public void setExtractedVideoText(String extractedVideoText) { this.extractedVideoText = extractedVideoText; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public boolean hasImageText() {
            return extractedImageText != null && !extractedImageText.trim().isEmpty();
        }
        
        public boolean hasVideoText() {
            return extractedVideoText != null && !extractedVideoText.trim().isEmpty();
        }
        
        public boolean hasAnyText() {
            return hasImageText() || hasVideoText();
        }
    }
}
