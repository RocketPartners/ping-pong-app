package com.example.javapingpongelo.services;

import com.example.javapingpongelo.configuration.EasterEggConfigProperties;
import com.example.javapingpongelo.models.EasterEgg;
import com.example.javapingpongelo.models.EasterEgg.EggType;
import com.example.javapingpongelo.repositories.EasterEggRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service focused solely on spawning new easter eggs.
 * Handles egg placement, rarity calculation, and positioning logic.
 */
@Service
@Slf4j
public class EasterEggSpawningService {

    @Autowired
    private EasterEggRepository easterEggRepository;

    @Autowired
    private EasterEggConfigProperties config;

    @Autowired
    private ObjectMapper objectMapper;

    // Page locations where eggs can spawn
    private static final List<String> SPAWN_LOCATIONS = Arrays.asList(
        "home", "leaderboard", "achievements", "profile", "game-details",
        "player-statistics", "any"  // "any" means universal placement
    );

    // CSS selectors for egg placement zones
    private static final List<String> PLACEMENT_SELECTORS = Arrays.asList(
        ".main-content", ".sidebar", "mat-button", ".mat-card", 
        ".content-wrapper", "header", "footer"
    );

    /**
     * Spawn a new easter egg if conditions are met
     */
    @Transactional
    public EasterEgg spawnNewEgg() {
        // Check if we've reached the max active eggs limit
        long activeCount = easterEggRepository.countByIsActiveTrue();
        if (activeCount >= config.getMaxActiveEggs()) {
            log.debug("Max active eggs limit reached: {}/{}", activeCount, config.getMaxActiveEggs());
            throw new RuntimeException("Max active eggs limit reached");
        }

        // Generate egg properties
        EggType eggType = determineEggType();
        String pageLocation = selectRandomPageLocation();
        String cssSelector = selectRandomCssSelector();
        Map<String, Object> coordinates = generateCoordinates(cssSelector);
        int pointValue = calculatePointValue(eggType);
        String secretMessage = generateSecretMessage(eggType);

        // Create the egg
        EasterEgg egg = EasterEgg.builder()
            .pageLocation(pageLocation)
            .cssSelector(cssSelector)
            .coordinates(serializeCoordinates(coordinates))
            .type(eggType)
            .pointValue(pointValue)
            .secretMessage(secretMessage)
            .isActive(true)
            .spawnedAt(new Date())
            .build();

        // Save to database
        EasterEgg savedEgg = easterEggRepository.save(egg);
        
        log.info("Spawned new {} egg on {} page with {} points", 
            eggType, pageLocation, pointValue);
        
        return savedEgg;
    }

    /**
     * Determine egg type based on configured rarity rates
     */
    private EggType determineEggType() {
        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        log.debug("🎲 Rarity roll: {:.6f}", roll);

        double cumulative = 0.0;
        
        cumulative += config.getRateForType("MYTHICAL");
        if (roll < cumulative) return EggType.MYTHICAL;
        
        cumulative += config.getRateForType("LEGENDARY");
        if (roll < cumulative) return EggType.LEGENDARY;
        
        cumulative += config.getRateForType("EPIC");
        if (roll < cumulative) return EggType.EPIC;
        
        cumulative += config.getRateForType("RARE");
        if (roll < cumulative) return EggType.RARE;
        
        cumulative += config.getRateForType("UNCOMMON");
        if (roll < cumulative) return EggType.UNCOMMON;
        
        // Default to common if nothing else matches
        return EggType.COMMON;
    }

    /**
     * Select random page location for spawning
     */
    private String selectRandomPageLocation() {
        return SPAWN_LOCATIONS.get(ThreadLocalRandom.current().nextInt(SPAWN_LOCATIONS.size()));
    }

    /**
     * Select random CSS selector for placement
     */
    private String selectRandomCssSelector() {
        return PLACEMENT_SELECTORS.get(ThreadLocalRandom.current().nextInt(PLACEMENT_SELECTORS.size()));
    }

    /**
     * Generate coordinates for egg placement
     */
    private Map<String, Object> generateCoordinates(String cssSelector) {
        Map<String, Object> coords = new HashMap<>();
        
        // Generate relative positioning within the selector
        double xRatio = ThreadLocalRandom.current().nextDouble(0.1, 0.9);
        double yRatio = ThreadLocalRandom.current().nextDouble(0.1, 0.9);
        
        // Determine anchor point based on position
        String anchorPoint;
        if (xRatio < 0.3) {
            anchorPoint = yRatio < 0.5 ? "left-top" : "left-bottom";
        } else if (xRatio > 0.7) {
            anchorPoint = yRatio < 0.5 ? "right-top" : "right-bottom";
        } else {
            anchorPoint = yRatio < 0.5 ? "center-top" : "center-bottom";
        }

        coords.put("cssSelector", cssSelector);
        coords.put("xRatio", Math.round(xRatio * 100) / 100.0);
        coords.put("yRatio", Math.round(yRatio * 100) / 100.0);
        coords.put("anchorPoint", anchorPoint);
        
        log.info("Generated {} egg position: selector={}, xRatio={}, yRatio={}, anchor={}", 
            "EGG", cssSelector, coords.get("xRatio"), coords.get("yRatio"), anchorPoint);
        
        return coords;
    }

    /**
     * Calculate point value for egg type with some randomization
     */
    private int calculatePointValue(EggType eggType) {
        int basePoints = eggType.getBasePoints();
        // Add ±20% randomization to base points
        double multiplier = ThreadLocalRandom.current().nextDouble(0.8, 1.2);
        return Math.max(1, (int)(basePoints * multiplier));
    }

    /**
     * Generate appropriate secret message for egg type
     */
    private String generateSecretMessage(EggType eggType) {
        return switch (eggType) {
            case COMMON -> "🟢 Nice find! Keep hunting for rarer eggs! 🟢";
            case UNCOMMON -> "🟠 Nice find! This uncommon egg has extra shine! 🟠";
            case RARE -> "🟣 Wow! You found a rare star egg! Your eagle eyes are impressive! 🟣";
            case EPIC -> "🔵 EPIC DISCOVERY! 💎 This diamond egg is incredibly valuable! 🔵";
            case LEGENDARY -> "🔴 LEGENDARY FIND! ✨ You've discovered something truly special! 🔴";
            case MYTHICAL -> "🔮 MYTHICAL EGG FOUND! 🔮 This is the rarest of all discoveries! You are truly blessed by the egg gods! 🔮";
        };
    }

    /**
     * Serialize coordinates to JSON string
     */
    private String serializeCoordinates(Map<String, Object> coordinates) {
        try {
            return objectMapper.writeValueAsString(coordinates);
        } catch (Exception e) {
            log.error("Failed to serialize coordinates", e);
            return "{}";
        }
    }

    /**
     * Clean up old inactive eggs
     */
    @Transactional
    public int cleanupOldEggs() {
        Date cutoffDate = new Date(System.currentTimeMillis() - 
            config.getCleanup().getInactiveEggsMinutes() * 60 * 1000L);
        
        List<EasterEgg> oldEggs = easterEggRepository.findByIsActiveFalseAndSpawnedAtBefore(cutoffDate);
        if (!oldEggs.isEmpty()) {
            easterEggRepository.deleteAll(oldEggs);
            log.info("Cleaned up {} old inactive eggs", oldEggs.size());
        }
        
        return oldEggs.size();
    }
}