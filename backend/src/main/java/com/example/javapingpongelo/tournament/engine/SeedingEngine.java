package com.example.javapingpongelo.tournament.engine;

import com.example.javapingpongelo.models.Tournament;
import com.example.javapingpongelo.models.TournamentPlayer;

import java.util.List;

/**
 * Interface for different seeding strategies
 */
public interface SeedingEngine {
    
    /**
     * Get the seeding method this engine handles
     */
    Tournament.SeedingMethod getSupportedSeedingMethod();
    
    /**
     * Seed participants for initial bracket generation
     * @param tournament The tournament
     * @param participants Unsorted list of participants
     * @return Seeded list of participants (1st seed, 2nd seed, etc.)
     */
    List<TournamentPlayer> seedParticipants(Tournament tournament, List<TournamentPlayer> participants);
    
    /**
     * Re-seed participants for next round (if re-seeding is enabled)
     * @param tournament The tournament
     * @param remainingParticipants Participants still in the tournament
     * @param currentRound The round that was just completed
     * @return Re-seeded list of remaining participants
     */
    List<TournamentPlayer> reseedParticipants(
        Tournament tournament, 
        List<TournamentPlayer> remainingParticipants,
        int currentRound
    );
    
    /**
     * Generate optimal pairings to avoid repeat matchups when possible
     * @param tournament The tournament
     * @param participants Seeded participants for this round
     * @param currentRound The round number being generated
     * @return Optimally paired participants
     */
    List<List<TournamentPlayer>> generateOptimalPairings(
        Tournament tournament,
        List<TournamentPlayer> participants,
        int currentRound
    );
    
    /**
     * Validate that participants can be seeded with this method
     * @param tournament The tournament
     * @param participants List of participants
     * @throws IllegalArgumentException if seeding is not possible
     */
    void validateSeedingRequirements(Tournament tournament, List<TournamentPlayer> participants);
}