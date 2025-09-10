package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.*;
import com.example.javapingpongelo.events.EasterEggFoundEvent;
import com.example.javapingpongelo.models.dto.*;
import com.example.javapingpongelo.repositories.*;
import com.example.javapingpongelo.services.DTOMappingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Core service for managing the easter egg hunting system.
 * Handles egg spawning, claiming, statistics, and leaderboards.
 */
@Service
@Slf4j
public class EasterEggService {
    
    @Autowired
    private EasterEggRepository easterEggRepository;
    
    @Autowired
    private EasterEggFindRepository easterEggFindRepository;
    
    @Autowired
    private EasterEggStatsRepository easterEggStatsRepository;
    
    @Autowired
    private PlayerRepository playerRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private EasterEggWebSocketService webSocketService;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private DTOMappingService dtoMappingService;
    
    // Scheduler for coordinated egg spawning
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    // Page locations where eggs can spawn
    private static final String[] PAGE_LOCATIONS = {
        "home", "leaderboard", "achievements", "statistics", "match-builder", 
        "profile-settings", "player-profile", "game-details"
    };
    
    // Testing mode - predictable egg spawning for debugging
    private boolean isTestingMode = false;
    private int testEggCounter = 0;
    
    // Rarity-based intelligent placement zones
    private static final Map<String, Map<EasterEgg.EggType, EggPlacementZone[]>> RARITY_PLACEMENT_ZONES = Map.of(
        "home", Map.of(
            EasterEgg.EggType.COMMON, new EggPlacementZone[]{
                new EggPlacementZone("mat-card", 0.9, 0.5, "right-center"), // Simple edge peek
                new EggPlacementZone("mat-toolbar", 0.8, 1.0, "right-bottom") // Behind toolbar
            },
            EasterEgg.EggType.UNCOMMON, new EggPlacementZone[]{
                new EggPlacementZone(".stats-section", 0.1, 0.8, "left-bottom"), // Partial hide in stats
                new EggPlacementZone("mat-button", 0.05, 0.5, "left-center") // Button edge
            },
            EasterEgg.EggType.RARE, new EggPlacementZone[]{
                new EggPlacementZone("mat-card", 0.95, 0.2, "right-top"), // Subtle corner peek
                new EggPlacementZone(".page-header", 0.3, 1.02, "center-bottom") // Just below header
            },
            EasterEgg.EggType.EPIC, new EggPlacementZone[]{
                new EggPlacementZone(".main-content", 0.98, 0.15, "right-top"), // Very subtle corner
                new EggPlacementZone("mat-card", 0.02, 0.85, "left-bottom") // Hidden in card shadow
            },
            EasterEgg.EggType.LEGENDARY, new EggPlacementZone[]{
                new EggPlacementZone("mat-toolbar", 0.99, 0.5, "right-center"), // Almost invisible
                new EggPlacementZone(".stats-section", 0.01, 0.99, "left-bottom") // Deep corner hide
            },
            EasterEgg.EggType.MYTHICAL, new EggPlacementZone[]{
                new EggPlacementZone(".main-content", 0.995, 0.05, "right-top"), // Master level hiding
                new EggPlacementZone("mat-card", 0.005, 0.995, "left-bottom") // Nearly invisible
            }
        ),
        "any", Map.of(
            EasterEgg.EggType.COMMON, new EggPlacementZone[]{
                new EggPlacementZone("mat-card", 0.85, 0.5, "right-center"),
                new EggPlacementZone("mat-button", 0.9, 0.5, "right-center")
            },
            EasterEgg.EggType.UNCOMMON, new EggPlacementZone[]{
                new EggPlacementZone("mat-button", 0.05, 0.5, "left-center"),
                new EggPlacementZone(".main-content", 0.9, 0.8, "right-bottom")
            },
            EasterEgg.EggType.RARE, new EggPlacementZone[]{
                new EggPlacementZone("mat-toolbar", 0.95, 0.3, "right-top"),
                new EggPlacementZone("mat-card", 0.1, 0.9, "left-bottom")
            },
            EasterEgg.EggType.EPIC, new EggPlacementZone[]{
                new EggPlacementZone(".main-content", 0.97, 0.1, "right-top"),
                new EggPlacementZone("mat-toolbar", 0.03, 0.97, "left-bottom")
            },
            EasterEgg.EggType.LEGENDARY, new EggPlacementZone[]{
                new EggPlacementZone("mat-card", 0.98, 0.05, "right-top"),
                new EggPlacementZone("mat-button", 0.02, 0.95, "left-bottom")
            },
            EasterEgg.EggType.MYTHICAL, new EggPlacementZone[]{
                new EggPlacementZone(".main-content", 0.998, 0.02, "right-top"),
                new EggPlacementZone("mat-toolbar", 0.002, 0.998, "left-bottom")
            }
        )
    );
    
    // Inner class for placement zone configuration
    private static class EggPlacementZone {
        final String cssSelector;
        final double xRatio; // 0.0 = left edge, 1.0 = right edge
        final double yRatio; // 0.0 = top edge, 1.0 = bottom edge  
        final String anchorPoint; // "right-center", "left-bottom", etc.
        
        EggPlacementZone(String cssSelector, double xRatio, double yRatio, String anchorPoint) {
            this.cssSelector = cssSelector;
            this.xRatio = xRatio;
            this.yRatio = yRatio;
            this.anchorPoint = anchorPoint;
        }
    }
    
    /**
     * Get the currently active easter egg (should be only one at a time)
     */
    @Transactional
    public Optional<EasterEgg> getCurrentActiveEgg() {
        try {
            List<EasterEgg> activeEggs = easterEggRepository.findAllActiveEggs();
            
            if (activeEggs.isEmpty()) {
                return Optional.empty();
            }
            
            // If multiple active eggs exist, log warning and clean up duplicates
            if (activeEggs.size() > 1) {
                log.warn("Found {} active eggs, expected only 1. Cleaning up duplicates...", activeEggs.size());
                
                // Keep the most recent one, deactivate the rest
                EasterEgg mostRecent = activeEggs.get(0); // List is ordered by spawnedAt DESC
                for (int i = 1; i < activeEggs.size(); i++) {
                    EasterEgg duplicate = activeEggs.get(i);
                    duplicate.setActive(false);
                    easterEggRepository.save(duplicate);
                    log.info("Deactivated duplicate egg {} spawned at {}", duplicate.getId(), duplicate.getSpawnedAt());
                }
                
                return Optional.of(mostRecent);
            }
            
            return Optional.of(activeEggs.get(0));
        } catch (Exception e) {
            log.error("Error getting current active egg", e);
            return Optional.empty();
        }
    }
    
    /**
     * Spawn a new random easter egg
     */
    @Transactional
    public EasterEgg spawnNewEgg() {
        try {
            // Remove any existing active eggs first
            removeAllActiveEggs();
            
            // Generate new egg
            String pageLocation;
            String cssSelector;
            EasterEgg.EggType eggType;
            
            SharedEggPosition sharedPosition;
            if (isTestingMode) {
                // Predictable spawning for testing - use shared positioning
                pageLocation = "any"; // Allow spawning on any page
                eggType = EasterEgg.EggType.COMMON; // Always common for testing
                sharedPosition = generateSharedPosition(pageLocation, eggType, testEggCounter);
                testEggCounter++;
                log.info("TESTING MODE: Spawning test egg #{} with shared position on {}", testEggCounter, pageLocation);
            } else {
                // Normal random spawning with shared positions
                pageLocation = selectRandomPageLocation();
                eggType = determineEggTypeByRarity();
                sharedPosition = generateSharedPosition(pageLocation, eggType, -1);
            }
            
            cssSelector = sharedPosition.cssSelector;
            String coordinates = sharedPosition.coordinatesJson;
            String secretMessage = generateSecretMessage(eggType);
            
            EasterEgg newEgg = EasterEgg.builder()
                .pageLocation(pageLocation)
                .cssSelector(cssSelector)
                .coordinates(coordinates)
                .type(eggType)
                .pointValue(eggType.getBasePoints())
                .isActive(true)
                .spawnedAt(new Date())
                .secretMessage(secretMessage)
                .build();
            
            EasterEgg savedEgg = easterEggRepository.save(newEgg);
            log.info("Spawned new {} egg on {} page with {} points", 
                eggType, pageLocation, eggType.getBasePoints());
            
            // Broadcast egg spawned event via WebSocket
            try {
                EasterEggEvent spawnEvent = EasterEggEvent.spawned(savedEgg);
                webSocketService.broadcastEasterEggEvent(spawnEvent);
            } catch (Exception e) {
                log.error("Failed to broadcast egg spawn event for egg {}", savedEgg.getId(), e);
            }
            
            return savedEgg;
            
        } catch (Exception e) {
            log.error("Error spawning new easter egg", e);
            throw new RuntimeException("Failed to spawn new easter egg", e);
        }
    }
    
    /**
     * Attempt to claim an easter egg
     */
    @Transactional
    public EggClaimResult claimEgg(UUID eggId, UUID playerId) {
        try {
            // Validate player exists and hunting is enabled
            Optional<Player> playerOpt = playerRepository.findById(playerId);
            if (playerOpt.isEmpty()) {
                return EggClaimResult.builder()
                    .success(false)
                    .reason(EggClaimResult.ClaimReason.PLAYER_NOT_FOUND)
                    .message("Player not found")
                    .build();
            }
            
            Player player = playerOpt.get();
            if (!player.isEasterEggHuntingEnabled()) {
                return EggClaimResult.huntingDisabled();
            }
            
            // Find and claim the egg (race condition protected)
            Optional<EasterEgg> eggOpt = easterEggRepository.findByIdAndIsActive(eggId, true);
            if (eggOpt.isEmpty()) {
                // Check if egg was already found
                Optional<EasterEgg> foundEgg = easterEggRepository.findById(eggId);
                if (foundEgg.isPresent() && !foundEgg.get().isActive()) {
                    // Egg was found by someone else
                    Player finder = playerRepository.findById(foundEgg.get().getFoundByPlayerId()).orElse(null);
                    String finderName = finder != null ? finder.getFullName() : "Another player";
                    return EggClaimResult.alreadyClaimed(finderName);
                }
                return EggClaimResult.notFound();
            }
            
            EasterEgg egg = eggOpt.get();
            
            // Mark egg as found
            egg.setActive(false);
            egg.setFoundAt(new Date());
            egg.setFoundByPlayerId(playerId);
            easterEggRepository.save(egg);
            
            // Award points to player
            awardEggPoints(player, egg);
            
            // Record the find
            recordEggFind(egg, player);
            
            // Update/create player stats
            updatePlayerStats(playerId, egg);
            
            log.info("Player {} found {} egg for {} points", 
                player.getFullName(), egg.getType(), egg.getPointValue());
            
            // Broadcast egg claimed event via WebSocket
            try {
                EasterEggEvent claimEvent = EasterEggEvent.claimed(egg.getId(), player.getFullName());
                webSocketService.broadcastEasterEggEvent(claimEvent);
                
                // Schedule next egg spawn after delay (5 seconds)
                scheduleNextEggSpawn();
            } catch (Exception e) {
                log.error("Failed to broadcast egg claim event for egg {}", egg.getId(), e);
            }
            
            // Get player's updated total after points were awarded
            Player updatedPlayer = playerRepository.findById(playerId).orElse(player);
            
            // Fire achievement evaluation event
            try {
                EasterEggFoundEvent eggFoundEvent = new EasterEggFoundEvent(
                    updatedPlayer, 
                    egg, 
                    egg.getPointValue(), 
                    updatedPlayer.getTotalEggsFound(),
                    updatedPlayer.getEasterEggPoints()
                );
                eventPublisher.publishEvent(eggFoundEvent);
                log.debug("Published EasterEggFoundEvent for player {} finding {} egg", 
                         updatedPlayer.getPlayerId(), egg.getType());
            } catch (Exception e) {
                log.error("Failed to publish easter egg found event for player {}: {}", 
                         updatedPlayer.getPlayerId(), e.getMessage());
            }
            
            return EggClaimResult.success(
                egg.getPointValue(), 
                updatedPlayer.getEasterEggPoints(),
                egg.getType(),
                egg.getSecretMessage()
            );
            
        } catch (Exception e) {
            log.error("Error claiming easter egg {} for player {}", eggId, playerId, e);
            return EggClaimResult.builder()
                .success(false)
                .reason(EggClaimResult.ClaimReason.SERVER_ERROR)
                .message("Server error occurred while claiming egg")
                .build();
        }
    }
    
    /**
     * Get player's easter egg statistics
     */
    @Transactional(readOnly = true)
    public Optional<EasterEggStats> getPlayerStats(UUID playerId) {
        return easterEggStatsRepository.findByPlayerId(playerId);
    }
    
    /**
     * Get the secret leaderboard of top egg hunters
     */
    @Transactional(readOnly = true)
    public List<EggHunterLeaderboardDto> getSecretLeaderboard(int limit) {
        try {
            List<EasterEggStats> topHunters = easterEggStatsRepository.findTopEggHunters(
                PageRequest.of(0, limit));
            
            List<EggHunterLeaderboardDto> leaderboard = new ArrayList<>();
            int rank = 1;
            
            for (EasterEggStats stats : topHunters) {
                Optional<Player> playerOpt = playerRepository.findById(stats.getPlayerId());
                if (playerOpt.isPresent()) {
                    Player player = playerOpt.get();
                    
                    EggHunterLeaderboardDto dto = EggHunterLeaderboardDto.builder()
                        .rank(rank++)
                        .playerName(player.getFullName())
                        .username(player.getUsername())
                        .totalEggsFound(stats.getTotalEggsFound())
                        .totalPoints(stats.getTotalPointsEarned())
                        .lastEggFound(stats.getLastEggFound())
                        .firstEggFound(stats.getFirstEggFound())
                        .commonEggsFound(stats.getCommonEggsFound())
                        .uncommonEggsFound(stats.getUncommonEggsFound() != null ? stats.getUncommonEggsFound() : 0)
                        .rareEggsFound(stats.getRareEggsFound() != null ? stats.getRareEggsFound() : 0)
                        .epicEggsFound(stats.getEpicEggsFound() != null ? stats.getEpicEggsFound() : 0)
                        .legendaryEggsFound(stats.getLegendaryEggsFound() != null ? stats.getLegendaryEggsFound() : 0)
                        .mythicalEggsFound(stats.getMythicalEggsFound() != null ? stats.getMythicalEggsFound() : 0)
                        .longestStreak(stats.getLongestStreak())
                        .favoriteHuntingPage(stats.getFavoriteHuntingPage())
                        .build();
                    
                    dto.assignSpecialBadge();
                    leaderboard.add(dto);
                }
            }
            
            return leaderboard;
            
        } catch (Exception e) {
            log.error("Error getting secret leaderboard", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get recent egg finds for activity display
     */
    @Transactional(readOnly = true)
    public List<RecentEggFindDto> getRecentFinds(int limit) {
        try {
            List<EasterEggFind> recentFinds = easterEggFindRepository.findRecentFinds(
                PageRequest.of(0, limit));
            
            return recentFinds.stream()
                .map(dtoMappingService::toRecentFindDto)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Error getting recent finds", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Remove expired eggs (cleanup task)
     */
    @Transactional
    public void removeExpiredEggs() {
        try {
            // Remove eggs older than 5 minutes
            Date cutoffTime = new Date(System.currentTimeMillis() - (5 * 60 * 1000));
            List<EasterEgg> expiredEggs = easterEggRepository.findExpiredActiveEggs(cutoffTime);
            
            for (EasterEgg expiredEgg : expiredEggs) {
                expiredEgg.setActive(false);
                easterEggRepository.save(expiredEgg);
                log.info("Removed expired egg {} from {}", expiredEgg.getId(), expiredEgg.getPageLocation());
            }
            
        } catch (Exception e) {
            log.error("Error removing expired eggs", e);
        }
    }
    
    // Private helper methods
    
    private void removeAllActiveEggs() {
        List<EasterEgg> activeEggs = easterEggRepository.findAll().stream()
            .filter(EasterEgg::isActive)
            .collect(Collectors.toList());
            
        for (EasterEgg egg : activeEggs) {
            egg.setActive(false);
            easterEggRepository.save(egg);
        }
    }
    
    /**
     * Schedule the next egg spawn after a delay for coordinated multiplayer experience
     */
    private void scheduleNextEggSpawn() {
        final int SPAWN_DELAY_SECONDS = 5;
        
        scheduler.schedule(() -> {
            try {
                log.info("🥚 Spawning new egg after {} second delay...", SPAWN_DELAY_SECONDS);
                EasterEgg newEgg = spawnNewEgg();
                log.info("🥚 New egg spawned successfully: {}", newEgg.getId());
            } catch (Exception e) {
                log.error("Error spawning scheduled egg", e);
            }
        }, SPAWN_DELAY_SECONDS, TimeUnit.SECONDS);
        
        log.info("🥚 Next egg spawn scheduled in {} seconds", SPAWN_DELAY_SECONDS);
    }
    
    private String selectRandomPageLocation() {
        Random random = new Random();
        // 60% chance to spawn on "any" page, 40% chance for specific pages
        if (random.nextDouble() < 0.6) {
            return "any";
        }
        return PAGE_LOCATIONS[random.nextInt(PAGE_LOCATIONS.length)];
    }
    
    /**
     * Generate a rarity-based shared position that all clients will use identically
     */
    private SharedEggPosition generateSharedPosition(String pageLocation, EasterEgg.EggType eggType, int testCounter) {
        Map<EasterEgg.EggType, EggPlacementZone[]> pageZones = RARITY_PLACEMENT_ZONES.get(pageLocation);
        if (pageZones == null) {
            // Fallback to "any" page zones if specific page not found
            pageZones = RARITY_PLACEMENT_ZONES.get("any");
        }
        
        EggPlacementZone[] zones = pageZones.get(eggType);
        if (zones == null || zones.length == 0) {
            // Fallback to common zones if rarity not found
            zones = pageZones.get(EasterEgg.EggType.COMMON);
        }
        
        EggPlacementZone selectedZone;
        if (testCounter >= 0) {
            // For testing, cycle through zones predictably
            selectedZone = zones[testCounter % zones.length];
        } else {
            // For normal spawning, select randomly but deterministically
            Random random = new Random();
            selectedZone = zones[random.nextInt(zones.length)];
        }
        
        // Create shared positioning data that all clients will interpret the same way
        String coordinatesJson = String.format(
            "{\"cssSelector\":\"%s\",\"xRatio\":%.2f,\"yRatio\":%.2f,\"anchorPoint\":\"%s\"}",
            selectedZone.cssSelector,
            selectedZone.xRatio,
            selectedZone.yRatio,
            selectedZone.anchorPoint
        );
        
        log.info("Generated {} egg position: selector={}, xRatio={}, yRatio={}, anchor={}", 
            eggType, selectedZone.cssSelector, selectedZone.xRatio, selectedZone.yRatio, selectedZone.anchorPoint);
        
        return new SharedEggPosition(selectedZone.cssSelector, coordinatesJson);
    }
    
    // Helper class for shared positioning data
    private static class SharedEggPosition {
        final String cssSelector;
        final String coordinatesJson;
        
        SharedEggPosition(String cssSelector, String coordinatesJson) {
            this.cssSelector = cssSelector;
            this.coordinatesJson = coordinatesJson;
        }
    }
    
    private EasterEgg.EggType determineEggTypeByRarity() {
        Random random = new Random();
        float roll = random.nextFloat() * 100; // 0.0 to 100.0
        
        // Cumulative probability system
        float cumulative = 0.0f;
        for (EasterEgg.EggType type : EasterEgg.EggType.values()) {
            cumulative += type.getRarityPercent();
            if (roll < cumulative) {
                log.debug("🎲 Rarity roll: {:.2f} -> {} ({}%)", roll, type, type.getRarityPercent());
                return type;
            }
        }
        
        // Fallback to common if something goes wrong
        return EasterEgg.EggType.COMMON;
    }
    
    private String generateSafeCoordinates() {
        // Generate JSON coordinates for safe placement
        Random random = new Random();
        Map<String, Object> coords = Map.of(
            "x", random.nextInt(80) + 10,  // 10-90% from left
            "y", random.nextInt(60) + 20,  // 20-80% from top
            "zIndex", 1000
        );
        
        try {
            return objectMapper.writeValueAsString(coords);
        } catch (Exception e) {
            return "{\"x\": 50, \"y\": 50, \"zIndex\": 1000}"; // Fallback
        }
    }
    
    private String generateSecretMessage(EasterEgg.EggType eggType) {
        String[] messages = switch (eggType) {
            case COMMON -> new String[]{
                "🥚 Well done! Every egg hunter starts somewhere! 🥚",
                "👏 Good find! Keep hunting for rarer eggs! 👏",
                "🎯 Nice work! You're getting the hang of this! 🎯"
            };
            case UNCOMMON -> new String[]{
                "🟠 Nice find! This uncommon egg has extra shine! 🟠",
                "✨ Good hunting! Uncommon eggs are getting interesting! ✨",
                "🔸 Sweet! You're finding better eggs now! 🔸"
            };
            case RARE -> new String[]{
                "🌟 Great job! You found a rare star egg! 🌟",
                "🔍 Nice hunting skills! Rare eggs are special! 🔍",
                "💜 Excellent! This rare egg sparkles beautifully! 💜"
            };
            case EPIC -> new String[]{
                "💎 EPIC FIND! This diamond egg is truly spectacular! 💎",
                "🚀 Incredible! Epic eggs are extremely rare! 🚀",
                "🌟 Outstanding! You've found an epic treasure! 🌟"
            };
            case LEGENDARY -> new String[]{
                "✨ LEGENDARY! You discovered an amazing legendary egg! ✨",
                "🏆 Legendary hunter! This is a special find! 🏆",
                "🔥 AMAZING! Legendary eggs are the stuff of myths! 🔥"
            };
            case MYTHICAL -> new String[]{
                "🔮 MYTHICAL! You found the rarest egg of all! 🔮",
                "💫 INCREDIBLE! This mythical egg is one in a thousand! 💫",
                "🌌 PHENOMENAL! Mythical eggs are beyond legendary! 🌌"
            };
        };
        
        Random random = new Random();
        return messages[random.nextInt(messages.length)];
    }
    
    private void awardEggPoints(Player player, EasterEgg egg) {
        player.setEasterEggPoints(player.getEasterEggPoints() + egg.getPointValue());
        player.setTotalEggsFound(player.getTotalEggsFound() + 1);
        player.setLastEggFound(new Date());
        playerRepository.save(player);
    }
    
    private void recordEggFind(EasterEgg egg, Player player) {
        EasterEggFind find = EasterEggFind.builder()
            .playerId(player.getPlayerId())
            .eggId(egg.getId())
            .foundAt(new Date())
            .pointsAwarded(egg.getPointValue())
            .pageFoundOn(egg.getPageLocation())
            .eggType(egg.getType())
            .build();
            
        easterEggFindRepository.save(find);
    }
    
    private void updatePlayerStats(UUID playerId, EasterEgg egg) {
        Optional<EasterEggStats> statsOpt = easterEggStatsRepository.findByPlayerId(playerId);
        
        EasterEggStats stats;
        if (statsOpt.isPresent()) {
            stats = statsOpt.get();
        } else {
            stats = EasterEggStats.builder()
                .playerId(playerId)
                .build();
        }
        
        stats.recordEggFound(egg.getType(), egg.getPointValue(), egg.getPageLocation());
        easterEggStatsRepository.save(stats);
    }
    
    // Note: Conversion methods moved to DTOMappingService for centralization
}