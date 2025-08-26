package com.arbmonitor.api.controller;

import com.arbmonitor.api.service.DatabaseBackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for database backup operations
 */
@RestController
@RequestMapping("/api/backup")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class BackupController {

    @Autowired
    private DatabaseBackupService backupService;

    /**
     * Create a manual backup of all domains
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createBackup() {
        try {
            backupService.createManualBackup();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Manual backup created successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Failed to create backup: " + e.getMessage()
            ));
        }
    }

    /**
     * List all available backups
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listBackups() {
        try {
            List<String> backups = backupService.listBackups();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "backups", backups
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Failed to list backups: " + e.getMessage()
            ));
        }
    }
}
