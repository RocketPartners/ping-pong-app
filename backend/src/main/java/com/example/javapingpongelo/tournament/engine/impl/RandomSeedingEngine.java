package com.example.javapingpongelo.tournament.engine.impl;

import com.example.javapingpongelo.models.Tournament;
import com.example.javapingpongelo.models.TournamentPlayer;
import com.example.javapingpongelo.tournament.engine.SeedingEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Random seeding engine that randomly assigns seeds to participants
 * Useful for casual tournaments or when ELO ratings are not available/desired
 */
@Component
@Slf4j
public class RandomSeedingEngine implements SeedingEngine {
    
    private final Random random = new Random();
    
    @Override
    public Tournament.SeedingMethod getSupportedSeedingMethod() {
        return Tournament.SeedingMethod.RANDOM;
    }
    
    @Override
    public List<TournamentPlayer> seedParticipants(Tournament tournament, List<TournamentPlayer> participants) {
        validateSeedingRequirements(tournament, participants);
        
        log.info("Randomly seeding {} participants for tournament {}", 
                participants.size(), tournament.getName());
        
        // Create a copy to avoid modifying the original list
        List<TournamentPlayer> shuffledParticipants = new ArrayList<>(participants);
        
        // Shuffle the participants randomly
        Collections.shuffle(shuffledParticipants, random);
        
        // Assign seed numbers (1 = first seed, though it's random)
        for (int i = 0; i < shuffledParticipants.size(); i++) {
            TournamentPlayer player = shuffledParticipants.get(i);
            player.setSeed(i + 1);
            
            log.debug("Assigned random seed {} to player {}", 
                    player.getSeed(), player.getPlayerId());
        }
        
        log.info("Completed random seeding for tournament {}", tournament.getName());
        
        return shuffledParticipants;
    }
    
    @Override
    public List<TournamentPlayer> reseedParticipants(Tournament tournament, 
                                                    List<TournamentPlayer> remainingParticipants, 
                                                    int currentRound) {
        if (!tournament.isEnableReseeding()) {
            log.debug("Re-seeding disabled for tournament {}", tournament.getName());
            return remainingParticipants;
        }
        
        log.info("Randomly re-seeding {} remaining participants after round {} for tournament {}", 
                remainingParticipants.size(), currentRound, tournament.getName());
        
        // For random seeding, we just shuffle again
        return seedParticipants(tournament, remainingParticipants);
    }
    
    @Override
    public List<List<TournamentPlayer>> generateOptimalPairings(Tournament tournament,
                                                               List<TournamentPlayer> participants,
                                                               int currentRound) {
        log.info("Generating random pairings for {} participants in round {}", 
                participants.size(), currentRound);
        
        if (participants.size() % 2 != 0) {
            throw new IllegalArgumentException("Cannot pair odd number of participants without bye handling");
        }
        
        List<List<TournamentPlayer>> pairings = new ArrayList<>();
        
        // For random seeding, we just pair sequentially after shuffling
        // Since participants are already randomly ordered, this gives us random matchups
        
        for (int i = 0; i < participants.size(); i += 2) {
            List<TournamentPlayer> pair = new ArrayList<>(Arrays.asList(
                participants.get(i),
                participants.get(i + 1)
            ));
            pairings.add(pair);
            
            log.debug("Randomly paired seed {} vs seed {} for round {}", 
                    participants.get(i).getSeed(), 
                    participants.get(i + 1).getSeed(), 
                    currentRound);
        }
        
        return pairings;
    }
    
    @Override
    public void validateSeedingRequirements(Tournament tournament, List<TournamentPlayer> participants) {
        if (tournament.getSeedingMethod() != Tournament.SeedingMethod.RANDOM) {
            throw new IllegalArgumentException("Tournament seeding method must be RANDOM");
        }
        
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Participants list cannot be null or empty");
        }
        
        if (participants.size() < 2) {
            throw new IllegalArgumentException("Cannot seed tournament with less than 2 participants");
        }
        
        log.debug("Random seeding requirements validated for {} participants", participants.size());
    }
    
    /**
     * Set a custom seed for the random number generator (useful for testing)
     */
    public void setSeed(long seed) {
        random.setSeed(seed);
        log.debug("Random seeding engine seed set to: {}", seed);
    }
    
    /**
     * Generate completely random pairings without any seeding consideration
     * This can create more unpredictable matchups in later rounds
     */
    public List<List<TournamentPlayer>> generateCompletelyRandomPairings(List<TournamentPlayer> participants) {
        log.info("Generating completely random pairings for {} participants", participants.size());
        
        if (participants.size() % 2 != 0) {
            throw new IllegalArgumentException("Cannot pair odd number of participants");
        }
        
        // Shuffle the participants completely
        List<TournamentPlayer> shuffled = new ArrayList<>(participants);
        Collections.shuffle(shuffled, random);
        
        List<List<TournamentPlayer>> pairings = new ArrayList<>();
        
        for (int i = 0; i < shuffled.size(); i += 2) {
            List<TournamentPlayer> pair = new ArrayList<>(Arrays.asList(
                shuffled.get(i),
                shuffled.get(i + 1)
            ));
            pairings.add(pair);
        }
        
        return pairings;
    }
}