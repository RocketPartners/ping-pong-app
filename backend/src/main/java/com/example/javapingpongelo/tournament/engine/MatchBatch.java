package com.example.javapingpongelo.tournament.engine;

import com.example.javapingpongelo.models.TournamentMatch;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Represents a batch of tournament matches that can be played together.
 * Provides automatic synchronization between winner's and loser's brackets.
 */
@Data
@Builder
public class MatchBatch {
    
    /**
     * The matches in this batch that can be played in parallel
     */
    private List<TournamentMatch> matches;
    
    /**
     * Human-readable description of this batch (e.g., "WB Round 1", "LB Round 2")
     */
    private String description;
    
    /**
     * Priority of this batch - lower numbers have higher priority
     * Used to ensure proper tournament flow (e.g., WB Round 1 before LB Round 1)
     */
    private int priority;
    
    /**
     * Whether all matches in this batch can be played simultaneously
     * True for most cases, false for special scenarios like grand finals
     */
    private boolean canPlayInParallel;
    
    /**
     * The round name this batch belongs to
     */
    private String roundName;
    
    /**
     * Type of bracket this batch belongs to
     */
    private BracketType bracketType;
    
    public enum BracketType {
        WINNERS,
        LOSERS,
        GRAND_FINALS
    }
}