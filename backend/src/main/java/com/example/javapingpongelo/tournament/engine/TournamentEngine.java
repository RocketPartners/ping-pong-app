package com.example.javapingpongelo.tournament.engine;

import com.example.javapingpongelo.models.Tournament;
import com.example.javapingpongelo.models.TournamentMatch;
import com.example.javapingpongelo.models.TournamentPlayer;
import com.example.javapingpongelo.models.TournamentRound;

import java.util.List;
import java.util.UUID;

/**
 * Main tournament engine that orchestrates the tournament framework
 * This class coordinates between the rules engine, seeding engine, and match generation
 */
public interface TournamentEngine {
    
    /**
     * Initialize a tournament with initial bracket generation
     * @param tournament The tournament to initialize
     * @return The initialized tournament with first round ready
     */
    Tournament initializeTournament(Tournament tournament);
    
    /**
     * Start the next round of the tournament
     * @param tournamentId The tournament ID
     * @return Updated tournament with new round started
     */
    Tournament startNextRound(UUID tournamentId);
    
    /**
     * Process a match result and update tournament state
     * @param matchId The completed match ID
     * @param winnerTeamIds List of winner player IDs
     * @param loserTeamIds List of loser player IDs
     * @param team1Score Score for team 1
     * @param team2Score Score for team 2
     * @return Updated tournament state
     */
    Tournament processMatchResult(
        UUID matchId,
        List<UUID> winnerTeamIds,
        List<UUID> loserTeamIds,
        Integer team1Score,
        Integer team2Score
    );
    
    /**
     * Handle a participant dropping out of the tournament
     * @param tournamentId The tournament ID
     * @param participantId The participant who is dropping out
     * @return Updated tournament state
     */
    Tournament handleParticipantDropout(UUID tournamentId, UUID participantId);
    
    /**
     * Get the current standings/rankings for the tournament
     * @param tournamentId The tournament ID
     * @return Ordered list of participants by current standing
     */
    List<TournamentPlayer> getCurrentStandings(UUID tournamentId);
    
    /**
     * Get all upcoming matches that are ready to be played
     * @param tournamentId The tournament ID
     * @return List of playable matches
     */
    List<TournamentMatch> getUpcomingMatches(UUID tournamentId);
    
    /**
     * Get all matches for a specific round
     * @param tournamentId The tournament ID
     * @param roundNumber The round number
     * @return List of matches in that round
     */
    List<TournamentMatch> getMatchesForRound(UUID tournamentId, int roundNumber);
    
    /**
     * Check if a round is complete and ready for advancement
     * @param tournamentId The tournament ID
     * @param roundNumber The round number to check
     * @return true if round is complete and next round can be generated
     */
    boolean isRoundComplete(UUID tournamentId, int roundNumber);
    
    /**
     * Generate bracket visualization data
     * @param tournamentId The tournament ID
     * @return Bracket structure suitable for frontend display
     */
    TournamentBracketData generateBracketData(UUID tournamentId);
    
    // Inner class for bracket visualization
    class TournamentBracketData {
        private final List<TournamentRound> winnerRounds;
        private final List<TournamentRound> loserRounds;
        private final List<TournamentMatch> finalMatches;
        
        public TournamentBracketData(List<TournamentRound> winnerRounds, 
                                   List<TournamentRound> loserRounds,
                                   List<TournamentMatch> finalMatches) {
            this.winnerRounds = winnerRounds;
            this.loserRounds = loserRounds;
            this.finalMatches = finalMatches;
        }
        
        // Getters
        public List<TournamentRound> getWinnerRounds() { return winnerRounds; }
        public List<TournamentRound> getLoserRounds() { return loserRounds; }
        public List<TournamentMatch> getFinalMatches() { return finalMatches; }
    }
}