package com.arbmonitor.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledTaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);
    
    @Autowired
    private MediaStorageService mediaStorageService;
    
    /**
     * Clean up old media files daily at 2 AM
     * Keep files for 30 days
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldMediaFiles() {
        logger.info("Starting scheduled cleanup of old media files");
        try {
            mediaStorageService.cleanupOldMedia(30); // Keep files for 30 days
            logger.info("Completed scheduled cleanup of old media files");
        } catch (Exception e) {
            logger.error("Error during scheduled media cleanup: {}", e.getMessage());
        }
    }
}

