package com.example.javapingpongelo.tournament.engine.impl;

import com.example.javapingpongelo.models.Tournament;
import com.example.javapingpongelo.models.TournamentPlayer;
import com.example.javapingpongelo.services.IPlayerService;
import com.example.javapingpongelo.tournament.engine.SeedingEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ELO-based seeding engine that seeds players based on their current ELO ratings
 * Higher ELO players get better (lower numbered) seeds
 */
@Component
@Slf4j
public class EloBasedSeedingEngine implements SeedingEngine {
    
    @Autowired
    private IPlayerService playerService;
    
    @Override
    public Tournament.SeedingMethod getSupportedSeedingMethod() {
        return Tournament.SeedingMethod.RATING_BASED;
    }
    
    @Override
    public List<TournamentPlayer> seedParticipants(Tournament tournament, List<TournamentPlayer> participants) {
        validateSeedingRequirements(tournament, participants);
        
        log.info("Seeding {} participants based on ELO ratings for tournament {}", 
                participants.size(), tournament.getName());
        
        // Get current ELO ratings for all participants
        Map<UUID, Integer> playerRatings = getPlayerRatings(participants, tournament);
        
        // Sort participants by ELO rating (highest first)
        List<TournamentPlayer> seededParticipants = participants.stream()
            .sorted((p1, p2) -> {
                int rating1 = playerRatings.getOrDefault(p1.getPlayerId(), 1200);
                int rating2 = playerRatings.getOrDefault(p2.getPlayerId(), 1200);
                return Integer.compare(rating2, rating1); // Descending order
            })
            .collect(Collectors.toList());
        
        // Assign seed numbers (1 = best seed)
        for (int i = 0; i < seededParticipants.size(); i++) {
            TournamentPlayer player = seededParticipants.get(i);
            player.setSeed(i + 1);
            
            UUID playerId = player.getPlayerId();
            int rating = playerRatings.getOrDefault(playerId, 1200);
            log.debug("Assigned seed {} to player {} (ELO: {})", 
                    player.getSeed(), playerId, rating);
        }
        
        log.info("Completed ELO-based seeding. Top seed has ELO: {}, Bottom seed has ELO: {}",
                playerRatings.getOrDefault(seededParticipants.get(0).getPlayerId(), 1200),
                playerRatings.getOrDefault(seededParticipants.get(seededParticipants.size() - 1).getPlayerId(), 1200));
        
        return seededParticipants;
    }
    
    @Override
    public List<TournamentPlayer> reseedParticipants(Tournament tournament, 
                                                    List<TournamentPlayer> remainingParticipants, 
                                                    int currentRound) {
        if (!tournament.isEnableReseeding()) {
            log.debug("Re-seeding disabled for tournament {}", tournament.getName());
            return remainingParticipants;
        }
        
        log.info("Re-seeding {} remaining participants after round {} for tournament {}", 
                remainingParticipants.size(), currentRound, tournament.getName());
        
        // For re-seeding, we could consider current tournament performance
        // For now, we'll just re-seed based on original ELO ratings
        return seedParticipants(tournament, remainingParticipants);
    }
    
    @Override
    public List<List<TournamentPlayer>> generateOptimalPairings(Tournament tournament,
                                                               List<TournamentPlayer> participants,
                                                               int currentRound) {
        log.info("Generating optimal pairings for {} participants in round {}", 
                participants.size(), currentRound);
        
        if (participants.size() % 2 != 0) {
            throw new IllegalArgumentException("Cannot pair odd number of participants without bye handling");
        }
        
        List<List<TournamentPlayer>> pairings = new ArrayList<>();
        
        // Standard seeding pairing strategy:
        // 1 vs n, 2 vs (n-1), 3 vs (n-2), etc.
        // This ensures balanced matches in early rounds
        
        int n = participants.size();
        for (int i = 0; i < n / 2; i++) {
            List<TournamentPlayer> pair = new ArrayList<>(Arrays.asList(
                participants.get(i),           // Higher seed
                participants.get(n - 1 - i)   // Lower seed
            ));
            pairings.add(pair);
            
            log.debug("Paired seed {} vs seed {} for round {}", 
                    participants.get(i).getSeed(), 
                    participants.get(n - 1 - i).getSeed(), 
                    currentRound);
        }
        
        return pairings;
    }
    
    @Override
    public void validateSeedingRequirements(Tournament tournament, List<TournamentPlayer> participants) {
        if (tournament.getSeedingMethod() != Tournament.SeedingMethod.RATING_BASED) {
            throw new IllegalArgumentException("Tournament seeding method must be RATING_BASED");
        }
        
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Participants list cannot be null or empty");
        }
        
        if (participants.size() < 2) {
            throw new IllegalArgumentException("Cannot seed tournament with less than 2 participants");
        }
        
        // Verify all participants have valid player IDs
        for (TournamentPlayer participant : participants) {
            if (participant.getPlayerId() == null) {
                throw new IllegalArgumentException("All participants must have valid player IDs for ELO-based seeding");
            }
        }
        
        log.debug("Seeding requirements validated for {} participants", participants.size());
    }
    
    /**
     * Get ELO ratings for all participants based on tournament game type
     */
    private Map<UUID, Integer> getPlayerRatings(List<TournamentPlayer> participants, Tournament tournament) {
        Map<UUID, Integer> ratings = new HashMap<>();
        
        for (TournamentPlayer participant : participants) {
            try {
                var player = playerService.findPlayerById(participant.getPlayerId());
                if (player != null) {
                    int rating = getRatingForGameType(player, tournament);
                    ratings.put(participant.getPlayerId(), rating);
                } else {
                    log.warn("Could not find player with ID: {}, using default rating", 
                            participant.getPlayerId());
                    ratings.put(participant.getPlayerId(), 1200); // Default rating
                }
            } catch (Exception e) {
                log.warn("Error retrieving rating for player {}: {}, using default rating", 
                        participant.getPlayerId(), e.getMessage());
                ratings.put(participant.getPlayerId(), 1200); // Default rating
            }
        }
        
        return ratings;
    }
    
    /**
     * Get the appropriate ELO rating based on tournament game type
     */
    private int getRatingForGameType(com.example.javapingpongelo.models.Player player, Tournament tournament) {
        // Determine which rating to use based on tournament configuration
        if (tournament.getGameType() == Tournament.GameType.SINGLES) {
            // For now, assuming we want ranked rating for tournaments
            // Could be configurable in the future
            return player.getSinglesRankedRating();
        } else {
            // For doubles, we might want to use doubles rating if available
            // For now, fall back to singles rating
            return player.getSinglesRankedRating();
        }
    }
    
    /**
     * Utility method to generate balanced bracket seeding for power-of-2 tournaments
     * This creates the classic "bracket seeding" where 1 plays 16, 2 plays 15, etc.
     */
    public List<TournamentPlayer> generateBracketSeeding(List<TournamentPlayer> seededParticipants) {
        if (seededParticipants.size() < 4) {
            return seededParticipants; // No special seeding needed for very small tournaments
        }
        
        // For now, return standard seeding
        // In the future, we could implement more sophisticated bracket balancing
        return seededParticipants;
    }
    
    /**
     * Analyze seeding quality by looking at rating differences
     */
    public void analyzeSeedingQuality(List<TournamentPlayer> seededParticipants, Tournament tournament) {
        if (seededParticipants.size() < 2) return;
        
        Map<UUID, Integer> ratings = getPlayerRatings(seededParticipants, tournament);
        
        int highestRating = ratings.getOrDefault(seededParticipants.get(0).getPlayerId(), 1200);
        int lowestRating = ratings.getOrDefault(seededParticipants.get(seededParticipants.size() - 1).getPlayerId(), 1200);
        
        int ratingSpread = highestRating - lowestRating;
        double averageRating = ratings.values().stream().mapToInt(Integer::intValue).average().orElse(1200.0);
        
        log.info("Seeding Quality Analysis for tournament {}:", tournament.getName());
        log.info("  - Rating spread: {} points ({} to {})", ratingSpread, highestRating, lowestRating);
        log.info("  - Average rating: {:.1f}", averageRating);
        log.info("  - Participants: {}", seededParticipants.size());
        
        // Check for potential rating imbalances
        if (ratingSpread > 500) {
            log.warn("Large rating spread detected - tournament may be unbalanced");
        }
        
        if (ratingSpread < 50) {
            log.info("Small rating spread - very competitive tournament expected");
        }
    }
}