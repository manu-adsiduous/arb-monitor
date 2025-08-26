package com.arbmonitor.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller for serving media files (images, videos)
 */
@RestController
@RequestMapping("/api/media")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class MediaController {
    
    private static final Logger logger = LoggerFactory.getLogger(MediaController.class);
    
    /**
     * Serve media files (images and videos)
     */
    @GetMapping("/{type}/{domain}/{date}/{filename}")
    public ResponseEntity<Resource> getMediaFile(
            @PathVariable String type,
            @PathVariable String domain,
            @PathVariable String date,
            @PathVariable String filename) {
        try {
            // Construct the full file path
            String path = String.format("%s/%s/%s/%s", type, domain, date, filename);
            Path filePath = Paths.get("./media/" + path);
            File file = filePath.toFile();
            
            logger.debug("Attempting to serve media file: {}", filePath.toAbsolutePath());
            
            if (!file.exists() || !file.isFile()) {
                logger.warn("Media file not found: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            // Security check - ensure file is within media directory
            Path mediaDir = Paths.get("./media").toAbsolutePath().normalize();
            Path requestedFile = filePath.toAbsolutePath().normalize();
            
            if (!requestedFile.startsWith(mediaDir)) {
                logger.warn("Security violation: Attempted to access file outside media directory: {}", requestedFile);
                return ResponseEntity.badRequest().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            // Determine content type based on file extension
            String contentType = getContentType(file.getName());
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600") // Cache for 1 hour
                .body(resource);
                
        } catch (Exception e) {
            logger.error("Error serving media file: {}/{}/{}/{}", type, domain, date, filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Determine content type based on file extension
     */
    private String getContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        
        switch (extension) {
            case "jpg":
            case "jpeg":
                return MediaType.IMAGE_JPEG_VALUE;
            case "png":
                return MediaType.IMAGE_PNG_VALUE;
            case "gif":
                return MediaType.IMAGE_GIF_VALUE;
            case "webp":
                return "image/webp";
            case "mp4":
                return "video/mp4";
            case "mov":
                return "video/quicktime";
            case "avi":
                return "video/x-msvideo";
            default:
                return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }
}