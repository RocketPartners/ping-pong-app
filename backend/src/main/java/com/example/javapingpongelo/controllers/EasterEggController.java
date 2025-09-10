package com.example.javapingpongelo.controllers;

import com.example.javapingpongelo.models.EasterEgg;
import com.example.javapingpongelo.models.EasterEggStats;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.dto.EggClaimResult;
import com.example.javapingpongelo.models.dto.EggHunterLeaderboardDto;
import com.example.javapingpongelo.models.dto.RecentEggFindDto;
import com.example.javapingpongelo.services.EasterEggService;
import com.example.javapingpongelo.services.IPlayerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * REST controller for easter egg hunting system.
 * Handles API requests for finding eggs, viewing stats, and leaderboards.
 */
@RestController
@RequestMapping("/api/easter-eggs")
@PreAuthorize("isAuthenticated()")
@Validated
@Slf4j
public class EasterEggController {
    
    @Autowired
    private EasterEggService easterEggService;
    
    @Autowired
    private IPlayerService playerService;
    
    // Rate limiting: max 5 claims per minute per player
    private final ConcurrentHashMap<String, AtomicInteger> claimCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastResetTime = new ConcurrentHashMap<>();
    private static final int MAX_CLAIMS_PER_MINUTE = 5;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000; // 1 minute
    
    /**
     * Get the currently active easter egg (if any)
     */
    @GetMapping("/current")
    public ResponseEntity<EasterEgg> getCurrentEgg() {
        try {
            Optional<EasterEgg> currentEgg = easterEggService.getCurrentActiveEgg();
            
            if (currentEgg.isPresent()) {
                return ResponseEntity.ok(currentEgg.get());
            } else {
                return ResponseEntity.noContent().build();
            }
            
        } catch (Exception e) {
            log.error("Error getting current easter egg", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Check if player has exceeded rate limit for claiming eggs
     */
    private boolean isRateLimited(String playerKey) {
        long currentTime = System.currentTimeMillis();
        
        // Clean up old entries and reset counters if window has passed
        lastResetTime.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > RATE_LIMIT_WINDOW_MS);
        
        claimCounts.entrySet().removeIf(entry -> 
            !lastResetTime.containsKey(entry.getKey()));
        
        // Check/update rate limit for this player
        AtomicInteger count = claimCounts.computeIfAbsent(playerKey, k -> new AtomicInteger(0));
        Long lastReset = lastResetTime.get(playerKey);
        
        if (lastReset == null || currentTime - lastReset > RATE_LIMIT_WINDOW_MS) {
            // Reset window
            lastResetTime.put(playerKey, currentTime);
            count.set(1);
            return false;
        } else {
            // Check if limit exceeded
            return count.incrementAndGet() > MAX_CLAIMS_PER_MINUTE;
        }
    }

    /**
     * Attempt to claim/find an easter egg
     */
    @PostMapping("/{eggId}/claim")
    public ResponseEntity<EggClaimResult> claimEgg(
            @PathVariable UUID eggId,
            Authentication authentication) {
        
        try {
            // Get current player from authentication
            String username = authentication.getName();
            Player currentPlayer = playerService.findPlayerByUsername(username);
            if (currentPlayer == null) {
                return ResponseEntity.badRequest().body(
                    EggClaimResult.builder()
                        .success(false)
                        .reason(EggClaimResult.ClaimReason.PLAYER_NOT_FOUND)
                        .message("Unable to identify player")
                        .build()
                );
            }
            
            // Check rate limit
            String playerKey = currentPlayer.getPlayerId().toString();
            if (isRateLimited(playerKey)) {
                log.warn("Player {} exceeded egg claim rate limit", currentPlayer.getFullName());
                return ResponseEntity.status(429).body( // Too Many Requests
                    EggClaimResult.builder()
                        .success(false)
                        .reason(EggClaimResult.ClaimReason.SERVER_ERROR)
                        .message("Too many claim attempts. Please wait before trying again.")
                        .build()
                );
            }
            
            // Attempt to claim the egg
            EggClaimResult result = easterEggService.claimEgg(eggId, currentPlayer.getPlayerId());
            
            if (result.isSuccess()) {
                log.info("Player {} successfully claimed egg {} for {} points", 
                    currentPlayer.getFullName(), eggId, result.getPointsEarned());
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            log.error("Error claiming easter egg {} ", eggId, e);
            return ResponseEntity.internalServerError().body(
                EggClaimResult.builder()
                    .success(false)
                    .reason(EggClaimResult.ClaimReason.SERVER_ERROR)
                    .message("Server error occurred")
                    .build()
            );
        }
    }
    
    /**
     * Get current player's easter egg statistics
     */
    @GetMapping("/my-stats")
    public ResponseEntity<EasterEggStats> getMyStats(Authentication authentication) {
        try {
            String username = authentication.getName();
            Player currentPlayer = playerService.findPlayerByUsername(username);
            if (currentPlayer == null) {
                return ResponseEntity.badRequest().build();
            }
            
            Optional<EasterEggStats> stats = easterEggService.getPlayerStats(currentPlayer.getPlayerId());
            
            if (stats.isPresent()) {
                return ResponseEntity.ok(stats.get());
            } else {
                // Return empty stats for new hunters
                EasterEggStats emptyStats = EasterEggStats.builder()
                    .playerId(currentPlayer.getPlayerId())
                    .build();
                return ResponseEntity.ok(emptyStats);
            }
            
        } catch (Exception e) {
            log.error("Error getting player stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get the secret leaderboard of top egg hunters
     * Only accessible to players who have found at least one egg
     */
    @GetMapping("/secret-leaderboard")
    public ResponseEntity<List<EggHunterLeaderboardDto>> getSecretLeaderboard(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            // Verify player has access to secret leaderboard
            String username = authentication.getName();
            Player currentPlayer = playerService.findPlayerByUsername(username);
            if (currentPlayer == null) {
                return ResponseEntity.badRequest().build();
            }
            
            // Check if player has found at least one egg to access secret leaderboard
            if (currentPlayer.getTotalEggsFound() < 1) {
                return ResponseEntity.status(403).build(); // Forbidden - haven't found any eggs yet
            }
            
            List<EggHunterLeaderboardDto> leaderboard = easterEggService.getSecretLeaderboard(Math.min(limit, 50));
            return ResponseEntity.ok(leaderboard);
            
        } catch (Exception e) {
            log.error("Error getting secret leaderboard", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get recent egg finds for activity display
     */
    @GetMapping("/recent-finds")
    public ResponseEntity<List<RecentEggFindDto>> getRecentFinds(
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            List<RecentEggFindDto> recentFinds = easterEggService.getRecentFinds(Math.min(limit, 20));
            return ResponseEntity.ok(recentFinds);
            
        } catch (Exception e) {
            log.error("Error getting recent finds", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Admin endpoint to manually spawn a new easter egg
     */
    @PostMapping("/admin/spawn")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EasterEgg> adminSpawnEgg() {
        try {
            EasterEgg newEgg = easterEggService.spawnNewEgg();
            log.info("Admin spawn: Created new {} egg on {} page", newEgg.getType(), newEgg.getPageLocation());
            return ResponseEntity.ok(newEgg);
            
        } catch (Exception e) {
            log.error("Error in admin egg spawn", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Health check endpoint for easter egg system
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        try {
            // Basic health check - verify we can access the database
            Optional<EasterEgg> currentEgg = easterEggService.getCurrentActiveEgg();
            String status = currentEgg.isPresent() ? 
                String.format("Easter egg system is healthy. Current egg: %s on %s page", 
                    currentEgg.get().getType(), currentEgg.get().getPageLocation()) :
                "Easter egg system is healthy. No active eggs currently.";
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("Easter egg system health check failed", e);
            return ResponseEntity.internalServerError().body("Easter egg system is unhealthy");
        }
    }
    
    /**
     * Admin endpoint to inspect current active eggs
     */
    @GetMapping("/admin/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getActiveEggDetails() {
        try {
            Optional<EasterEgg> currentEgg = easterEggService.getCurrentActiveEgg();
            Map<String, Object> response = new HashMap<>();
            
            if (currentEgg.isPresent()) {
                EasterEgg egg = currentEgg.get();
                response.put("hasActiveEgg", true);
                response.put("eggId", egg.getId());
                response.put("type", egg.getType());
                response.put("pageLocation", egg.getPageLocation());
                response.put("cssSelector", egg.getCssSelector());
                response.put("pointValue", egg.getPointValue());
                response.put("spawnedAt", egg.getSpawnedAt());
                response.put("coordinates", egg.getCoordinates());
            } else {
                response.put("hasActiveEgg", false);
                response.put("message", "No active eggs currently");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in admin active eggs endpoint", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Admin endpoint to force spawn an egg immediately
     */
    @PostMapping("/admin/force-spawn")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adminForceSpawnEgg() {
        try {
            EasterEgg newEgg = easterEggService.spawnNewEgg();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("eggId", newEgg.getId());
            response.put("type", newEgg.getType());
            response.put("pageLocation", newEgg.getPageLocation());
            response.put("pointValue", newEgg.getPointValue());
            response.put("message", "Egg spawned successfully on " + newEgg.getPageLocation() + " page");
            
            log.info("Admin force spawn: Created {} egg {} on {}", 
                newEgg.getType(), newEgg.getId(), newEgg.getPageLocation());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in admin force spawn", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to spawn egg");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}