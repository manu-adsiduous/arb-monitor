package com.arbmonitor.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class MediaStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(MediaStorageService.class);
    
    @Value("${app.media.storage.path:./media}")
    private String mediaStoragePath;
    
    private final RestTemplate restTemplate;
    
    public MediaStorageService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Download and store an image file locally
     * @param imageUrl The URL of the image to download
     * @param domainName The domain name for organization
     * @param adId The ad ID for reference
     * @return The local file path or null if failed
     */
    public String downloadAndStoreImage(String imageUrl, String domainName, String adId) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Create directory structure: media/images/domain/YYYY-MM-DD/
            String dateFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path imageDir = Paths.get(mediaStoragePath, "images", sanitizeDomainName(domainName), dateFolder);
            Files.createDirectories(imageDir);
            
            // Generate unique filename
            String fileExtension = getFileExtension(imageUrl);
            String fileName = String.format("%s_%s.%s", adId, UUID.randomUUID().toString().substring(0, 8), fileExtension);
            Path filePath = imageDir.resolve(fileName);
            
            // Download the image
            logger.info("Downloading image: {} -> {}", imageUrl, filePath);
            
            try (InputStream inputStream = new URL(imageUrl).openStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Successfully downloaded image: {}", filePath);
                return filePath.toString();
            }
            
        } catch (Exception e) {
            logger.error("Failed to download image from {}: {}", imageUrl, e.getMessage());
            return null;
        }
    }
    
    /**
     * Download and store a video file locally
     * @param videoUrl The URL of the video to download
     * @param domainName The domain name for organization
     * @param adId The ad ID for reference
     * @return The local file path or null if failed
     */
    public String downloadAndStoreVideo(String videoUrl, String domainName, String adId) {
        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Create directory structure: media/videos/domain/YYYY-MM-DD/
            String dateFolder = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Path videoDir = Paths.get(mediaStoragePath, "videos", sanitizeDomainName(domainName), dateFolder);
            Files.createDirectories(videoDir);
            
            // Generate unique filename
            String fileExtension = getFileExtension(videoUrl);
            if (fileExtension.isEmpty()) {
                fileExtension = "mp4"; // Default for videos
            }
            String fileName = String.format("%s_%s.%s", adId, UUID.randomUUID().toString().substring(0, 8), fileExtension);
            Path filePath = videoDir.resolve(fileName);
            
            // Download the video
            logger.info("Downloading video: {} -> {}", videoUrl, filePath);
            
            try (InputStream inputStream = new URL(videoUrl).openStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Successfully downloaded video: {}", filePath);
                return filePath.toString();
            }
            
        } catch (Exception e) {
            logger.error("Failed to download video from {}: {}", videoUrl, e.getMessage());
            return null;
        }
    }
    
    /**
     * Get file extension from URL
     */
    private String getFileExtension(String url) {
        try {
            String path = new URL(url).getPath();
            int lastDot = path.lastIndexOf('.');
            if (lastDot > 0 && lastDot < path.length() - 1) {
                return path.substring(lastDot + 1).toLowerCase();
            }
        } catch (Exception e) {
            logger.debug("Could not extract file extension from URL: {}", url);
        }
        
        // Default extensions based on common patterns
        if (url.contains("image") || url.contains("photo")) {
            return "jpg";
        } else if (url.contains("video")) {
            return "mp4";
        }
        
        return "jpg"; // Default
    }
    
    /**
     * Sanitize domain name for use as folder name
     */
    private String sanitizeDomainName(String domainName) {
        if (domainName == null) {
            return "unknown";
        }
        return domainName.replaceAll("[^a-zA-Z0-9.-]", "_").toLowerCase();
    }
    
    /**
     * Get the relative path for serving files via web
     */
    public String getWebPath(String localPath) {
        if (localPath == null) {
            return null;
        }
        
        try {
            Path fullPath = Paths.get(localPath);
            Path mediaPath = Paths.get(mediaStoragePath);
            Path relativePath = mediaPath.relativize(fullPath);
            return "/media/" + relativePath.toString().replace("\\", "/");
        } catch (Exception e) {
            logger.error("Failed to generate web path for: {}", localPath);
            return null;
        }
    }
    
    /**
     * Clean up old media files (older than specified days)
     */
    public void cleanupOldMedia(int daysToKeep) {
        try {
            Path mediaPath = Paths.get(mediaStoragePath);
            if (!Files.exists(mediaPath)) {
                return;
            }
            
            LocalDate cutoffDate = LocalDate.now().minusDays(daysToKeep);
            logger.info("Cleaning up media files older than {}", cutoffDate);
            
            // This is a simplified cleanup - in production you'd want more sophisticated logic
            Files.walk(mediaPath)
                .filter(Files::isDirectory)
                .filter(path -> {
                    String folderName = path.getFileName().toString();
                    try {
                        LocalDate folderDate = LocalDate.parse(folderName, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        return folderDate.isBefore(cutoffDate);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.walk(path)
                            .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                            .forEach(file -> {
                                try {
                                    Files.deleteIfExists(file);
                                } catch (Exception e) {
                                    logger.warn("Failed to delete file: {}", file);
                                }
                            });
                    } catch (Exception e) {
                        logger.warn("Failed to cleanup directory: {}", path);
                    }
                });
                
        } catch (Exception e) {
            logger.error("Failed to cleanup old media files: {}", e.getMessage());
        }
    }
}

