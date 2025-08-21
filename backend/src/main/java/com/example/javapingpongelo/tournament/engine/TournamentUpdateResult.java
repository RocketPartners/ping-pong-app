package com.example.javapingpongelo.tournament.engine;

import com.example.javapingpongelo.models.TournamentRound;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Result of completing a match batch, indicating what new matches/rounds were created
 */
@Data
@Builder
public class TournamentUpdateResult {
    
    /**
     * New rounds that were created as a result of completing the batch
     */
    private List<TournamentRound> newRounds;
    
    /**
     * Whether the tournament is now complete
     */
    private boolean tournamentComplete;
    
    /**
     * If tournament is complete, the winner(s)
     */
    private List<String> winners;
    
    /**
     * Description of what happened (for logging/debugging)
     */
    private String description;
}