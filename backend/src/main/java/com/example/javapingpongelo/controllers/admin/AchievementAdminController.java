package com.example.javapingpongelo.controllers.admin;

import com.example.javapingpongelo.models.*;
import com.example.javapingpongelo.services.achievements.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Admin controller for achievement management and configuration.
 * Provides endpoints for loading configurations, viewing analytics, and managing achievements.
 */
@RestController
@RequestMapping("/api/admin/achievements")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AchievementAdminController {

    @Autowired
    private AchievementConfigurationService configurationService;

    @Autowired
    private AchievementAnalyticsService analyticsService;

    @Autowired
    private IAchievementService achievementService;

    /**
     * Load achievement configurations from YAML file
     */
    @PostMapping("/config/load")
    public ResponseEntity<Map<String, Object>> loadConfigurations(@RequestParam String filename) {
        try {
            log.info("Loading achievement configurations from file: {}", filename);
            
            configurationService.loadConfigurationsFromFile(filename);
            Map<String, AchievementConfiguration> loaded = configurationService.getLoadedConfigurations();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Successfully loaded " + loaded.size() + " configurations");
            response.put("configurationsLoaded", loaded.size());
            response.put("configurations", loaded.keySet());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error loading configurations", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error loading configurations: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Apply loaded configurations to the database
     */
    @PostMapping("/config/apply")
    public ResponseEntity<Map<String, Object>> applyConfigurations() {
        try {
            log.info("Applying loaded achievement configurations");
            
            configurationService.applyConfigurations();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Successfully applied configurations to database");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error applying configurations", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error applying configurations: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Load and apply configurations in one step
     */
    @PostMapping("/config/load-and-apply")
    public ResponseEntity<Map<String, Object>> loadAndApplyConfigurations(
            @RequestParam(defaultValue = "achievements-config.yaml") String filename) {
        try {
            log.info("Loading and applying achievement configurations from: {}", filename);
            
            configurationService.loadConfigurationsFromFile(filename);
            Map<String, AchievementConfiguration> loaded = configurationService.getLoadedConfigurations();
            configurationService.applyConfigurations();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Successfully loaded and applied " + loaded.size() + " configurations");
            response.put("configurationsProcessed", loaded.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error loading and applying configurations", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error processing configurations: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Validate loaded configurations
     */
    @GetMapping("/config/validate")
    public ResponseEntity<Map<String, Object>> validateConfigurations() {
        try {
            Map<String, List<String>> validationResults = configurationService.validateAllConfigurations();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", validationResults.isEmpty());
            response.put("validationErrors", validationResults);
            response.put("totalConfigurations", configurationService.getLoadedConfigurations().size());
            response.put("configurationWithErrors", validationResults.size());
            
            if (validationResults.isEmpty()) {
                response.put("message", "All configurations are valid");
            } else {
                response.put("message", validationResults.size() + " configurations have validation errors");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error validating configurations", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error validating configurations: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Export current achievements to YAML
     */
    @GetMapping("/config/export")
    public ResponseEntity<String> exportConfigurations() {
        try {
            String yamlConfig = configurationService.exportAchievementsToYaml();
            return ResponseEntity.ok()
                    .header("Content-Type", "application/yaml")
                    .header("Content-Disposition", "attachment; filename=achievements-export.yaml")
                    .body(yamlConfig);
                    
        } catch (Exception e) {
            log.error("Error exporting configurations", e);
            return ResponseEntity.badRequest().body("Error exporting configurations: " + e.getMessage());
        }
    }

    /**
     * Get loaded configurations
     */
    @GetMapping("/config/loaded")
    public ResponseEntity<Map<String, AchievementConfiguration>> getLoadedConfigurations() {
        return ResponseEntity.ok(configurationService.getLoadedConfigurations());
    }

    /**
     * Recalculate analytics for all achievements
     */
    @PostMapping("/analytics/recalculate")
    public ResponseEntity<Map<String, Object>> recalculateAnalytics() {
        try {
            log.info("Manually triggering analytics recalculation");
            
            analyticsService.calculateAllAchievementAnalytics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Analytics recalculation triggered successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error recalculating analytics", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error recalculating analytics: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get analytics summary
     */
    @GetMapping("/analytics/summary")
    public ResponseEntity<Map<String, Object>> getAnalyticsSummary() {
        try {
            Map<String, Object> summary = analyticsService.getAnalyticsSummary();
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            log.error("Error getting analytics summary", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error getting analytics summary: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get detailed analytics for specific achievement
     */
    @GetMapping("/analytics/{achievementId}")
    public ResponseEntity<Map<String, Object>> getAchievementAnalytics(@PathVariable UUID achievementId) {
        try {
            Map<String, Object> analytics = analyticsService.getAchievementDetailedAnalytics(achievementId);
            return ResponseEntity.ok(analytics);
            
        } catch (Exception e) {
            log.error("Error getting achievement analytics for {}", achievementId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error getting achievement analytics: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get achievements needing attention
     */
    @GetMapping("/analytics/attention")
    public ResponseEntity<List<AchievementAnalytics>> getAchievementsNeedingAttention() {
        try {
            List<AchievementAnalytics> needingAttention = analyticsService.getAchievementsNeedingAttention();
            return ResponseEntity.ok(needingAttention);
            
        } catch (Exception e) {
            log.error("Error getting achievements needing attention", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Force recalculation of stale analytics
     */
    @PostMapping("/analytics/recalculate-stale")
    public ResponseEntity<Map<String, Object>> recalculateStaleAnalytics() {
        try {
            analyticsService.recalculateStaleAnalytics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stale analytics recalculation completed");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error recalculating stale analytics", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error recalculating stale analytics: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get all achievements with basic info
     */
    @GetMapping("/list")
    public ResponseEntity<List<Achievement>> getAllAchievements() {
        try {
            List<Achievement> achievements = achievementService.getAllAchievements();
            return ResponseEntity.ok(achievements);
            
        } catch (Exception e) {
            log.error("Error getting all achievements", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Test achievement evaluation for a specific player
     */
    @PostMapping("/test-evaluation/{playerId}")
    public ResponseEntity<Map<String, Object>> testAchievementEvaluation(@PathVariable UUID playerId) {
        try {
            log.info("Testing achievement evaluation for player: {}", playerId);
            
            // This would trigger a manual evaluation of all achievements for the player
            achievementService.evaluateAllAchievementsForPlayer(playerId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Achievement evaluation completed for player: " + playerId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error testing achievement evaluation for player {}", playerId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error testing achievement evaluation: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Clear all player achievement progress (DANGEROUS - for testing only)
     */
    @PostMapping("/reset-progress")
    public ResponseEntity<Map<String, Object>> resetAllProgress(@RequestParam(required = true) String confirmReset) {
        if (!"CONFIRM_RESET_ALL".equals(confirmReset)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Must confirm reset with 'CONFIRM_RESET_ALL' parameter");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            log.warn("DANGEROUS OPERATION: Resetting all player achievement progress");
            
            // This would clear all player achievement progress - implement with caution
            achievementService.resetAllPlayerProgress();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All player achievement progress has been reset");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error resetting all progress", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error resetting progress: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}