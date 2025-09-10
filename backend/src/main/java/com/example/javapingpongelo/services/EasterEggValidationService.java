package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.EasterEgg;
import com.example.javapingpongelo.models.EasterEggFind;
import com.example.javapingpongelo.repositories.EasterEggFindRepository;
import com.example.javapingpongelo.repositories.EasterEggRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for validating easter egg operations.
 * Ensures business rules are enforced and prevents fraud.
 */
@Service
@Slf4j
public class EasterEggValidationService {

    @Autowired
    private EasterEggRepository easterEggRepository;

    @Autowired
    private EasterEggFindRepository easterEggFindRepository;

    // Maximum time an egg can be active before expiring (5 minutes)
    private static final long MAX_EGG_LIFETIME_MS = 5 * 60 * 1000L;

    /**
     * Validation result for egg claiming
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final EasterEgg egg;

        private ValidationResult(boolean valid, String errorMessage, EasterEgg egg) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.egg = egg;
        }

        public static ValidationResult success(EasterEgg egg) {
            return new ValidationResult(true, null, egg);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage, null);
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public EasterEgg getEgg() { return egg; }
    }

    /**
     * Validate that a player can claim a specific easter egg
     */
    @Transactional(readOnly = true)
    public ValidationResult validateEggClaim(UUID eggId, UUID playerId) {
        log.debug("Validating egg claim: egg={}, player={}", eggId, playerId);

        // 1. Check if egg exists
        Optional<EasterEgg> eggOpt = easterEggRepository.findById(eggId);
        if (eggOpt.isEmpty()) {
            return ValidationResult.failure("Easter egg not found");
        }

        EasterEgg egg = eggOpt.get();

        // 2. Check if egg is still active
        if (!egg.isActive()) {
            return ValidationResult.failure("Easter egg is no longer active");
        }

        // 3. Check if egg has already been claimed by someone else
        if (egg.getFoundByPlayerId() != null) {
            if (egg.getFoundByPlayerId().equals(playerId)) {
                return ValidationResult.failure("You have already claimed this easter egg");
            } else {
                return ValidationResult.failure("Easter egg has already been claimed by another player");
            }
        }

        // 4. Check if egg has expired (older than 24 hours)
        long eggAge = System.currentTimeMillis() - egg.getSpawnedAt().getTime();
        if (eggAge > MAX_EGG_LIFETIME_MS) {
            log.warn("Egg {} has expired (age: {}h)", eggId, eggAge / (60 * 60 * 1000));
            return ValidationResult.failure("Easter egg has expired");
        }

        // 5. Check if player has already found this specific egg
        // (Additional safety check beyond the egg's foundByPlayerId)
        Optional<EasterEggFind> existingFind = easterEggFindRepository
            .findByPlayerIdAndEggId(playerId, eggId);
        if (existingFind.isPresent()) {
            return ValidationResult.failure("You have already claimed this easter egg");
        }

        // 6. Rate limit check - ensure player hasn't found too many eggs too quickly
        if (isPlayerClaimingTooFast(playerId)) {
            return ValidationResult.failure("Please slow down - wait a moment before claiming another egg");
        }

        // All validations passed
        log.debug("Egg claim validation successful for egg {} by player {}", eggId, playerId);
        return ValidationResult.success(egg);
    }

    /**
     * Validate that an egg spawn location is appropriate
     */
    public boolean validateSpawnLocation(String pageLocation, String cssSelector) {
        // Check if page location is allowed
        if (pageLocation == null || pageLocation.trim().isEmpty()) {
            log.warn("Invalid page location: null or empty");
            return false;
        }

        // Check if CSS selector is valid (basic validation)
        if (cssSelector == null || cssSelector.trim().isEmpty()) {
            log.warn("Invalid CSS selector: null or empty");
            return false;
        }

        // Prevent spawning in sensitive areas
        String lowerSelector = cssSelector.toLowerCase();
        if (lowerSelector.contains("password") || 
            lowerSelector.contains("credit-card") || 
            lowerSelector.contains("sensitive")) {
            log.warn("Attempted to spawn egg in sensitive area: {}", cssSelector);
            return false;
        }

        return true;
    }

    /**
     * Check if a player is claiming eggs too rapidly (potential bot behavior)
     */
    @Transactional(readOnly = true)
    private boolean isPlayerClaimingTooFast(UUID playerId) {
        // Get recent finds by this player (last 5 minutes)
        Date fiveMinutesAgo = new Date(System.currentTimeMillis() - 5 * 60 * 1000L);
        
        long recentClaims = easterEggFindRepository.countByPlayerIdAndFoundAtAfter(playerId, fiveMinutesAgo);
        
        // Allow max 3 claims per 5 minutes
        if (recentClaims >= 3) {
            log.warn("Player {} is claiming eggs too fast: {} claims in last 5 minutes", 
                playerId, recentClaims);
            return true;
        }
        
        return false;
    }

    /**
     * Validate coordinates for egg placement
     */
    public boolean validateCoordinates(String coordinatesJson) {
        if (coordinatesJson == null || coordinatesJson.trim().isEmpty()) {
            return false;
        }

        try {
            // Basic JSON validation - could parse and validate specific fields
            return coordinatesJson.startsWith("{") && coordinatesJson.endsWith("}");
        } catch (Exception e) {
            log.warn("Invalid coordinates JSON: {}", coordinatesJson, e);
            return false;
        }
    }

    /**
     * Validate that an egg type is supported
     */
    public boolean validateEggType(EasterEgg.EggType eggType) {
        return eggType != null;
    }

    /**
     * Validate point value for an egg type
     */
    public boolean validatePointValue(EasterEgg.EggType eggType, int pointValue) {
        if (pointValue <= 0) {
            return false;
        }

        // Ensure point value is reasonable for the egg type
        int minPoints = eggType.getBasePoints() / 2;
        int maxPoints = eggType.getBasePoints() * 2;
        
        return pointValue >= minPoints && pointValue <= maxPoints;
    }

    /**
     * Check if the system should allow new egg spawns
     */
    @Transactional(readOnly = true)
    public boolean shouldAllowSpawning() {
        // Could implement business logic like:
        // - Time of day restrictions
        // - Maximum eggs per hour
        // - Server load considerations
        return true;
    }
}