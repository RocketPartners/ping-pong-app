package com.example.javapingpongelo.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the easter egg hunting system.
 * Allows external configuration of spawn rates, timing, and limits.
 */
@Configuration
@ConfigurationProperties(prefix = "easter-egg")
@Data
public class EasterEggConfigProperties {

    private Spawn spawn = new Spawn();
    private int maxActiveEggs = 3;
    private int maxClaimsPerMinute = 5;
    private Cleanup cleanup = new Cleanup();

    @Data
    public static class Spawn {
        private double commonRate = 45.0;
        private double uncommonRate = 30.0;
        private double rareRate = 15.0;
        private double epicRate = 7.0;
        private double legendaryRate = 2.5;
        private double mythicalRate = 0.5;
        private int minIntervalSeconds = 300;  // 5 minutes
        private int maxIntervalSeconds = 1800; // 30 minutes
    }

    @Data
    public static class Cleanup {
        private int inactiveEggsHours = 24;
        private int inactiveEggsMinutes = 5;
        
        public int getInactiveEggsMinutes() {
            return inactiveEggsMinutes;
        }
    }

    /**
     * Get spawn rate for a specific egg type
     */
    public double getRateForType(String eggType) {
        return switch (eggType.toUpperCase()) {
            case "COMMON" -> spawn.commonRate;
            case "UNCOMMON" -> spawn.uncommonRate;
            case "RARE" -> spawn.rareRate;
            case "EPIC" -> spawn.epicRate;
            case "LEGENDARY" -> spawn.legendaryRate;
            case "MYTHICAL" -> spawn.mythicalRate;
            default -> spawn.commonRate;
        };
    }

    /**
     * Validate that all spawn rates add up to 100%
     */
    public boolean validateSpawnRates() {
        double total = spawn.commonRate + spawn.uncommonRate + spawn.rareRate + 
                      spawn.epicRate + spawn.legendaryRate + spawn.mythicalRate;
        return Math.abs(total - 100.0) < 0.1; // Allow small floating point differences
    }
}