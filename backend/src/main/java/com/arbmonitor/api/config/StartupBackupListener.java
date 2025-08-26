package com.arbmonitor.api.config;

import com.arbmonitor.api.service.DatabaseBackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener that creates a backup when the application is ready
 */
@Component
public class StartupBackupListener {

    private static final Logger logger = LoggerFactory.getLogger(StartupBackupListener.class);

    @Autowired
    private DatabaseBackupService backupService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Application ready - creating startup backup...");
        backupService.createStartupBackup();
    }
}
