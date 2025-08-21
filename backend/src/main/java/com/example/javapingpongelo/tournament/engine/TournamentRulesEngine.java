package com.example.javapingpongelo.tournament.engine;

import com.example.javapingpongelo.models.Tournament;
import com.example.javapingpongelo.models.TournamentMatch;
import com.example.javapingpongelo.models.TournamentPlayer;
import com.example.javapingpongelo.models.TournamentRound;

import java.util.List;
import java.util.UUID;

/**
 * Core interface for tournament rule engines
 * Each tournament type (Single Elimination, Double Elimination, etc.) implements this
 */
public interface TournamentRulesEngine {
    
    /**
     * Get the tournament type this engine handles
     */
    Tournament.TournamentType getSupportedTournamentType();
    
    /**
     * Calculate total number of rounds needed for this tournament
     * @param participantCount Number of participants
     * @return Total rounds needed
     */
    int calculateTotalRounds(int participantCount);
    
    /**
     * Generate the initial bracket structure
     * @param tournament The tournament
     * @param participants Seeded list of participants
     * @return List of initial rounds with matches
     */
    List<TournamentRound> generateInitialBracket(Tournament tournament, List<TournamentPlayer> participants);
    
    /**
     * Generate the next round based on completed matches
     * @param tournament The tournament
     * @param completedRound The round that was just completed
     * @param participants Current participant list (for re-seeding)
     * @return Next round(s) to be played, or empty if tournament is complete
     */
    List<TournamentRound> generateNextRound(
        Tournament tournament, 
        TournamentRound completedRound,
        List<TournamentPlayer> participants
    );
    
    /**
     * Handle a player/team dropping out of the tournament
     * @param tournament The tournament
     * @param droppedParticipantId ID of the participant who dropped out
     * @return Updated matches/rounds to handle the dropout
     */
    List<TournamentMatch> handleParticipantDropout(Tournament tournament, UUID droppedParticipantId);
    
    /**
     * Determine if the tournament is complete
     * @param tournament The tournament
     * @return true if tournament has a winner and no more matches needed
     */
    boolean isTournamentComplete(Tournament tournament);
    
    /**
     * Get the winner(s) of the tournament
     * @param tournament The tournament
     * @return List of winner participant IDs (usually 1, but could be multiple for ties)
     */
    List<UUID> getTournamentWinners(Tournament tournament);
    
    /**
     * Get the runner-up(s) of the tournament
     * @param tournament The tournament
     * @return List of runner-up participant IDs
     */
    List<UUID> getTournamentRunnersUp(Tournament tournament);
    
    /**
     * Validate that a tournament configuration is valid for this rule engine
     * @param tournament The tournament to validate
     * @throws IllegalArgumentException if configuration is invalid
     */
    void validateTournamentConfiguration(Tournament tournament);
    
    /**
     * Get display name for a round (e.g., "Quarterfinals", "Semifinals")
     * @param roundNumber The round number
     * @param bracketType The bracket type
     * @param participantCount Total participants in tournament
     * @return Human-readable round name
     */
    String getRoundDisplayName(int roundNumber, TournamentMatch.BracketType bracketType, int participantCount);
}