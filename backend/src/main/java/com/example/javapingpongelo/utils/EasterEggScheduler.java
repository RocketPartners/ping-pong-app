package com.example.javapingpongelo.utils;

import com.example.javapingpongelo.models.EasterEgg;
import com.example.javapingpongelo.services.EasterEggService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Scheduler for automatically managing easter eggs in the application.
 * Handles spawning new eggs and removing expired ones.
 */
@Component
@Slf4j
public class EasterEggScheduler {
    
    @Autowired
    private EasterEggService easterEggService;
    
    /**
     * Check if we need to spawn a new easter egg every 30 seconds
     * Only spawn if there's no active egg currently
     */
    @Scheduled(fixedDelay = 30000) // 30 seconds
    public void checkAndSpawnEgg() {
        try {
            Optional<EasterEgg> currentEgg = easterEggService.getCurrentActiveEgg();
            
            if (currentEgg.isEmpty()) {
                // No active egg, spawn a new one
                EasterEgg newEgg = easterEggService.spawnNewEgg();
                log.info("Auto-spawned new {} egg on {} page for {} points", 
                    newEgg.getType(), 
                    newEgg.getPageLocation(), 
                    newEgg.getPointValue());
            } else {
                log.debug("Active egg already exists on {} page, skipping spawn", 
                    currentEgg.get().getPageLocation());
            }
            
        } catch (Exception e) {
            log.error("Error in egg spawn scheduler", e);
        }
    }
    
    /**
     * Clean up expired eggs every 30 minutes
     */
    @Scheduled(fixedDelay = 1800000) // 30 minutes
    public void cleanupExpiredEggs() {
        try {
            log.debug("Running expired egg cleanup...");
            easterEggService.removeExpiredEggs();
            
        } catch (Exception e) {
            log.error("Error cleaning up expired eggs", e);
        }
    }
    
    /**
     * Spawn initial egg when application starts (after 30 seconds delay)
     */
    @Scheduled(initialDelay = 30000, fixedDelay = Long.MAX_VALUE) // Run once after 30 seconds
    public void spawnInitialEgg() {
        try {
            Optional<EasterEgg> currentEgg = easterEggService.getCurrentActiveEgg();
            
            if (currentEgg.isEmpty()) {
                EasterEgg initialEgg = easterEggService.spawnNewEgg();
                log.info("Spawned initial {} egg on {} page for {} points", 
                    initialEgg.getType(), 
                    initialEgg.getPageLocation(), 
                    initialEgg.getPointValue());
            } else {
                log.info("Active egg already exists, skipping initial spawn");
            }
            
        } catch (Exception e) {
            log.error("Error spawning initial egg", e);
        }
    }
}