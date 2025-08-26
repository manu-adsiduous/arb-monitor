package com.arbmonitor.api.service;

import com.arbmonitor.api.model.Domain;
import com.arbmonitor.api.repository.DomainRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for creating automatic backups of critical domain data
 * to prevent data loss during schema changes or system failures.
 */
@Service
public class DatabaseBackupService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupService.class);
    private static final String BACKUP_DIR = "./backups";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Creates a backup of all domains every hour
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void createAutomaticBackup() {
        try {
            createDomainBackup("automatic");
            logger.info("Automatic domain backup created successfully");
        } catch (Exception e) {
            logger.error("Failed to create automatic backup", e);
        }
    }

    /**
     * Creates a manual backup of all domains
     */
    public void createManualBackup() {
        try {
            createDomainBackup("manual");
            logger.info("Manual domain backup created successfully");
        } catch (Exception e) {
            logger.error("Failed to create manual backup", e);
            throw new RuntimeException("Backup creation failed", e);
        }
    }

    /**
     * Creates a backup before schema migrations
     */
    public void createPreMigrationBackup() {
        try {
            createDomainBackup("pre-migration");
            logger.info("Pre-migration domain backup created successfully");
        } catch (Exception e) {
            logger.error("Failed to create pre-migration backup", e);
            throw new RuntimeException("Pre-migration backup failed", e);
        }
    }

    /**
     * Creates a startup backup when application starts
     */
    public void createStartupBackup() {
        try {
            createDomainBackup("startup");
            logger.info("Startup domain backup created successfully");
        } catch (Exception e) {
            logger.error("Failed to create startup backup", e);
            // Don't fail startup if backup fails
        }
    }

    private void createDomainBackup(String type) throws IOException {
        // Ensure backup directory exists
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        // Get all domains
        List<Domain> domains = domainRepository.findAll();
        
        // Create backup filename with timestamp
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String filename = String.format("domains_backup_%s_%s.json", type, timestamp);
        File backupFile = new File(backupDir, filename);

        // Write domains to JSON file
        try (FileWriter writer = new FileWriter(backupFile)) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, domains);
        }

        logger.info("Domain backup created: {} ({} domains)", backupFile.getAbsolutePath(), domains.size());
        
        // Keep only the last 10 backups of each type
        cleanupOldBackups(type);
    }

    private void cleanupOldBackups(String type) {
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) return;

        File[] backupFiles = backupDir.listFiles((dir, name) -> 
            name.startsWith("domains_backup_" + type + "_") && name.endsWith(".json"));
        
        if (backupFiles != null && backupFiles.length > 10) {
            // Sort by last modified date (oldest first)
            java.util.Arrays.sort(backupFiles, (a, b) -> 
                Long.compare(a.lastModified(), b.lastModified()));
            
            // Delete oldest files, keep only last 10
            for (int i = 0; i < backupFiles.length - 10; i++) {
                if (backupFiles[i].delete()) {
                    logger.debug("Deleted old backup: {}", backupFiles[i].getName());
                }
            }
        }
    }

    /**
     * Lists all available backups
     */
    public List<String> listBackups() {
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) {
            return List.of();
        }

        File[] backupFiles = backupDir.listFiles((dir, name) -> 
            name.startsWith("domains_backup_") && name.endsWith(".json"));
        
        if (backupFiles == null) {
            return List.of();
        }

        return java.util.Arrays.stream(backupFiles)
            .map(File::getName)
            .sorted()
            .toList();
    }
}
