package com.arbmonitor.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Service for analyzing image content using OCR to extract text
 */
@Service
public class ImageAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageAnalysisService.class);
    
    /**
     * Extract text from an image using Tesseract OCR
     * @param imagePath Path to the image file
     * @return Extracted text or empty string if no text found
     */
    public String extractTextFromImage(String imagePath) {
        logger.info("Extracting text from image: {}", imagePath);
        
        try {
            // Validate image file exists
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                logger.warn("Image file not found: {}", imagePath);
                return "";
            }
            
            // Run Tesseract OCR
            String extractedText = runTesseractOcr(imagePath);
            
            if (extractedText.trim().isEmpty()) {
                logger.debug("No text found in image: {}", imagePath);
            } else {
                logger.info("Extracted {} characters of text from image: {}", 
                           extractedText.length(), imagePath);
            }
            
            return extractedText;
            
        } catch (Exception e) {
            logger.error("Error extracting text from image: {}", imagePath, e);
            return "";
        }
    }
    
    /**
     * Run Tesseract OCR on an image file
     */
    private String runTesseractOcr(String imagePath) throws IOException, InterruptedException {
        logger.debug("Running Tesseract OCR on: {}", imagePath);
        
        String[] command = {
            "tesseract",
            imagePath,
            "stdout", // Output to stdout instead of file
            "-l", "eng", // English language
            "--psm", "6", // Assume uniform block of text
            "-c", "tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 .,!?@#$%^&*()_+-=[]{}|;':\"<>?/~`"
        };
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip Tesseract warning/info lines
                if (!line.startsWith("Tesseract Open Source OCR") && 
                    !line.startsWith("Warning") && 
                    !line.contains("OEM") &&
                    !line.trim().isEmpty()) {
                    output.append(line).append("\n");
                }
            }
        }
        
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Tesseract OCR timed out for image: " + imagePath);
        }
        
        if (process.exitValue() != 0) {
            logger.warn("Tesseract OCR completed with exit code {} for image: {}", 
                       process.exitValue(), imagePath);
            // Don't throw exception - Tesseract sometimes returns non-zero even when successful
        }
        
        return output.toString().trim();
    }
    
    /**
     * Check if Tesseract is available on the system
     */
    public boolean isTesseractAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("tesseract", "--version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            
            return process.exitValue() == 0;
            
        } catch (Exception e) {
            logger.debug("Tesseract not available: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if FFmpeg is available on the system
     */
    public boolean isFfmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            
            return process.exitValue() == 0;
            
        } catch (Exception e) {
            logger.debug("FFmpeg not available: {}", e.getMessage());
            return false;
        }
    }
}
