package com.arbmonitor.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating visual descriptions of ad creatives using GPT-4 Vision
 */
@Service
public class VisualDescriptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(VisualDescriptionService.class);
    
    @Value("${app.openai.api-key}")
    private String openaiApiKey;
    
    @Autowired
    private OpenAICostTrackingService costTrackingService;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public VisualDescriptionService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Generate visual description for an ad creative image
     */
    public String generateVisualDescription(String imagePath) {
        try {
            logger.info("Generating visual description for image: {}", imagePath);
            
            // Check if image file exists
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                logger.warn("Image file not found: {}", imagePath);
                return "Image file not available for visual analysis";
            }
            
            // Convert image to base64
            String base64Image = encodeImageToBase64(imageFile);
            if (base64Image == null) {
                return "Failed to process image for visual analysis";
            }
            
            // Call GPT-4 Vision API
            String description = callGPT4Vision(base64Image, imagePath);
            
            logger.info("Successfully generated visual description for: {}", imagePath);
            return description;
            
        } catch (Exception e) {
            logger.error("Error generating visual description for {}: {}", imagePath, e.getMessage());
            return "Visual analysis failed: " + e.getMessage();
        }
    }
    
    /**
     * Generate visual descriptions for multiple images
     */
    public String generateVisualDescription(List<String> imagePaths) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return "No images available for visual analysis";
        }
        
        StringBuilder combinedDescription = new StringBuilder();
        
        for (int i = 0; i < imagePaths.size(); i++) {
            String imagePath = imagePaths.get(i);
            String description = generateVisualDescription(imagePath);
            
            if (imagePaths.size() > 1) {
                combinedDescription.append("Image ").append(i + 1).append(": ");
            }
            combinedDescription.append(description);
            
            if (i < imagePaths.size() - 1) {
                combinedDescription.append("\n\n");
            }
        }
        
        return combinedDescription.toString();
    }
    
    /**
     * Encode image file to base64 string
     */
    private String encodeImageToBase64(File imageFile) {
        try {
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            logger.error("Failed to encode image to base64: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Call GPT-4 Vision API to analyze image
     */
    private String callGPT4Vision(String base64Image, String imagePath) {
        long startTime = System.currentTimeMillis();
        String modelName = "gpt-4o";
        
        // Start cost tracking
        com.arbmonitor.api.model.OpenAIUsage usage = costTrackingService.trackRequest(
            modelName, "visual_description", 1L, null, null, startTime
        );
        
        try {
            // Determine image format
            String imageFormat = getImageFormat(imagePath);
            String mimeType = "image/" + imageFormat;
            
            // Build request payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", modelName);
            payload.put("max_tokens", 500);
            
            // Build messages array
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            
            // Build content array with text and image
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", buildVisualAnalysisPrompt());
            
            Map<String, Object> imageContent = new HashMap<>();
            imageContent.put("type", "image_url");
            
            Map<String, String> imageUrl = new HashMap<>();
            imageUrl.put("url", "data:" + mimeType + ";base64," + base64Image);
            imageContent.put("image_url", imageUrl);
            
            message.put("content", List.of(textContent, imageContent));
            payload.put("messages", List.of(message));
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);
            
            // Make API call
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions", 
                request, 
                String.class
            );
            
            // Parse response
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode choices = responseJson.get("choices");
            
            if (choices != null && choices.size() > 0) {
                JsonNode message_response = choices.get(0).get("message");
                if (message_response != null && message_response.has("content")) {
                    String responseText = message_response.get("content").asText().trim();
                    
                    // Update cost tracking with successful response
                    costTrackingService.updateAndSaveUsage(usage, responseJson, responseText, true, null);
                    
                    return responseText;
                }
            }
            
            logger.warn("Unexpected GPT-4 Vision response format");
            String fallbackResponse = "Visual analysis completed but response format was unexpected";
            
            // Update cost tracking with partial success
            costTrackingService.updateAndSaveUsage(usage, responseJson, fallbackResponse, false, "Unexpected response format");
            
            return fallbackResponse;
            
        } catch (Exception e) {
            logger.error("Error calling GPT-4 Vision API: {}", e.getMessage());
            String errorResponse = "Visual analysis failed due to API error";
            
            // Update cost tracking with error
            costTrackingService.updateAndSaveUsage(usage, null, null, false, e.getMessage());
            
            return errorResponse;
        }
    }
    
    /**
     * Get image format from file path
     */
    private String getImageFormat(String imagePath) {
        String extension = imagePath.toLowerCase();
        if (extension.endsWith(".jpg") || extension.endsWith(".jpeg")) {
            return "jpeg";
        } else if (extension.endsWith(".png")) {
            return "png";
        } else if (extension.endsWith(".gif")) {
            return "gif";
        } else if (extension.endsWith(".webp")) {
            return "webp";
        }
        return "jpeg"; // Default fallback
    }
    
    /**
     * Build prompt for visual analysis
     */
    private String buildVisualAnalysisPrompt() {
        return """
            Analyze this advertising creative image and provide a detailed description of its visual elements for compliance analysis purposes.
            
            Focus on describing:
            1. **Main Visual Elements**: What objects, people, products, or scenes are shown
            2. **Text Elements**: Any visible text, headlines, or call-to-action buttons (describe placement and style)
            3. **Color Scheme**: Dominant colors and their emotional impact
            4. **Layout & Design**: How elements are arranged, visual hierarchy
            5. **Style & Tone**: Professional, casual, medical, financial, etc.
            6. **Compliance-Relevant Details**: Any medical claims, before/after comparisons, testimonials, or potentially misleading visual elements
            
            Provide a clear, objective description that would help a compliance analyst understand what the ad visually communicates to viewers. Be specific about any text you can read in the image.
            
            Keep the description concise but comprehensive (2-3 paragraphs maximum).
            """;
    }
}
