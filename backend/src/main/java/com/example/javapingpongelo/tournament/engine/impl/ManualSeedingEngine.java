package com.example.javapingpongelo.tournament.engine.impl;

import com.example.javapingpongelo.models.Tournament;
import com.example.javapingpongelo.models.TournamentPlayer;
import com.example.javapingpongelo.tournament.engine.SeedingEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Manual seeding engine that uses pre-assigned seed values from TournamentPlayer entities
 * Seeds should be set manually before tournament initialization
 */
@Component
@Slf4j
public class ManualSeedingEngine implements SeedingEngine {
    
    @Override
    public Tournament.SeedingMethod getSupportedSeedingMethod() {
        return Tournament.SeedingMethod.MANUAL;
    }
    
    @Override
    public List<TournamentPlayer> seedParticipants(Tournament tournament, List<TournamentPlayer> participants) {
        validateSeedingRequirements(tournament, participants);
        
        log.info("Using manual seeding for {} participants in tournament {}", 
                participants.size(), tournament.getName());
        
        // Sort participants by their manually assigned seed values
        List<TournamentPlayer> seededParticipants = new ArrayList<>(participants);
        seededParticipants.sort((p1, p2) -> {
            Integer seed1 = p1.getSeed() != null ? p1.getSeed() : Integer.MAX_VALUE;
            Integer seed2 = p2.getSeed() != null ? p2.getSeed() : Integer.MAX_VALUE;
            return Integer.compare(seed1, seed2);
        });
        
        // Ensure all participants have valid seed numbers (1, 2, 3, ...)
        for (int i = 0; i < seededParticipants.size(); i++) {
            TournamentPlayer player = seededParticipants.get(i);
            int expectedSeed = i + 1;
            
            if (player.getSeed() == null || player.getSeed() != expectedSeed) {
                log.debug("Adjusting seed for player {} from {} to {}", 
                        player.getPlayerId(), player.getSeed(), expectedSeed);
                player.setSeed(expectedSeed);
            }
            
            log.debug("Manual seed {} assigned to player {}", 
                    player.getSeed(), player.getPlayerId());
        }
        
        log.info("Completed manual seeding for tournament {}", tournament.getName());
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
        
        log.info("Manually re-seeding {} remaining participants after round {} for tournament {}", 
                remainingParticipants.size(), currentRound, tournament.getName());
        
        // For manual seeding, we maintain original seed order relationships
        // Sort by original seed values to preserve manual ordering
        return seedParticipants(tournament, remainingParticipants);
    }
    
    @Override
    public List<List<TournamentPlayer>> generateOptimalPairings(Tournament tournament,
                                                               List<TournamentPlayer> participants,
                                                               int currentRound) {
        log.info("Generating manual seed-based pairings for {} participants in round {}", 
                participants.size(), currentRound);
        
        if (participants.size() % 2 != 0) {
            throw new IllegalArgumentException("Cannot pair odd number of participants without bye handling");
        }
        
        List<List<TournamentPlayer>> pairings = new ArrayList<>();
        
        // Use standard tournament seeding pairings: 1 vs n, 2 vs (n-1), etc.
        // This respects the manual seeding order while creating balanced matches
        
        int n = participants.size();
        for (int i = 0; i < n / 2; i++) {
            List<TournamentPlayer> pair = new ArrayList<>(Arrays.asList(
                participants.get(i),           // Higher seed
                participants.get(n - 1 - i)   // Lower seed
            ));
            pairings.add(pair);
            
            log.debug("Manually paired seed {} vs seed {} for round {}", 
                    participants.get(i).getSeed(), 
                    participants.get(n - 1 - i).getSeed(), 
                    currentRound);
        }
        
        return pairings;
    }
    
    @Override
    public void validateSeedingRequirements(Tournament tournament, List<TournamentPlayer> participants) {
        if (tournament.getSeedingMethod() != Tournament.SeedingMethod.MANUAL) {
            throw new IllegalArgumentException("Tournament seeding method must be MANUAL");
        }
        
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Participants list cannot be null or empty");
        }
        
        if (participants.size() < 2) {
            throw new IllegalArgumentException("Cannot seed tournament with less than 2 participants");
        }
        
        // Check for duplicate seeds (optional validation)
        Set<Integer> usedSeeds = new HashSet<>();
        for (TournamentPlayer participant : participants) {
            if (participant.getSeed() != null) {
                if (usedSeeds.contains(participant.getSeed())) {
                    log.warn("Duplicate seed {} found in manual seeding - will be corrected", 
                            participant.getSeed());
                }
                usedSeeds.add(participant.getSeed());
            }
        }
        
        log.debug("Manual seeding requirements validated for {} participants", participants.size());
    }
    
    /**
     * Assign specific seeds to participants manually
     * This method can be used by controllers to set up manual seeding
     */
    public void assignManualSeeds(List<TournamentPlayer> participants, Map<UUID, Integer> seedAssignments) {
        log.info("Assigning manual seeds to {} participants", participants.size());
        
        for (TournamentPlayer participant : participants) {
            UUID playerId = participant.getPlayerId();
            if (seedAssignments.containsKey(playerId)) {
                int assignedSeed = seedAssignments.get(playerId);
                participant.setSeed(assignedSeed);
                
                log.debug("Manually assigned seed {} to player {}", assignedSeed, playerId);
            }
        }
    }
    
    /**
     * Auto-assign sequential seeds based on current order
     * Useful when no specific seeds are provided
     */
    public void autoAssignSequentialSeeds(List<TournamentPlayer> participants) {
        log.info("Auto-assigning sequential seeds to {} participants", participants.size());
        
        for (int i = 0; i < participants.size(); i++) {
            TournamentPlayer participant = participants.get(i);
            participant.setSeed(i + 1);
            
            log.debug("Auto-assigned seed {} to player {}", i + 1, participant.getPlayerId());
        }
    }
}