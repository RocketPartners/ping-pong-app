package com.example.javapingpongelo.tournament.engine;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Mathematical foundation for double elimination tournament brackets
 * Provides algorithmic bracket generation for any tournament size
 */
@Slf4j
public class DoubleEliminationMath {
    
    /**
     * Calculate total number of matches needed for double elimination tournament
     * Formula: 2n - 2 for most cases, 2n - 3 if grand final reset happens
     * Let's use maximum possible: 2n - 2 where n = number of players
     * Reasoning: Each player must lose twice except winner (who may lose once)
     */
    public static int calculateTotalMatches(int playerCount) {
        if (playerCount < 2) {
            throw new IllegalArgumentException("Tournament must have at least 2 players");
        }
        return 2 * playerCount - 2;
    }
    
    /**
     * Calculate winner's bracket structure
     */
    public static WinnerBracketStructure calculateWinnerBracket(int playerCount) {
        int bracketSize = getNextPowerOf2(playerCount);
        int totalRounds = (int) Math.ceil(Math.log(bracketSize) / Math.log(2));
        int firstRoundMatches = bracketSize / 2;
        int byeCount = bracketSize - playerCount;
        
        return WinnerBracketStructure.builder()
            .bracketSize(bracketSize)
            .totalRounds(totalRounds)
            .firstRoundMatches(firstRoundMatches)
            .byeCount(byeCount)
            .build();
    }
    
    /**
     * Calculate loser's bracket structure
     * Loser's bracket is more complex - has roughly 2 * (winner_rounds - 1) rounds
     */
    public static LoserBracketStructure calculateLoserBracket(int playerCount) {
        WinnerBracketStructure winnerStructure = calculateWinnerBracket(playerCount);
        
        // Loser's bracket has alternating rounds of different sizes
        int loserRounds = (winnerStructure.totalRounds - 1) * 2;
        
        return LoserBracketStructure.builder()
            .totalRounds(loserRounds)
            .firstRoundMatches(calculateLoserFirstRoundMatches(playerCount))
            .build();
    }
    
    /**
     * Calculate first round matches in loser's bracket
     * Only players who lose in winner's bracket first round enter loser's bracket round 1
     */
    private static int calculateLoserFirstRoundMatches(int playerCount) {
        WinnerBracketStructure winnerStructure = calculateWinnerBracket(playerCount);
        
        // Players who lose in winner's bracket round 1 go to loser's bracket round 1
        int winnerFirstRoundPlayers = playerCount - winnerStructure.byeCount;
        int winnerFirstRoundLosers = winnerFirstRoundPlayers / 2;
        
        // If odd number of losers, one gets bye
        return winnerFirstRoundLosers / 2;
    }
    
    /**
     * Get next power of 2 (for bracket sizing)
     */
    public static int getNextPowerOf2(int n) {
        if (n <= 1) return 2;
        return (int) Math.pow(2, Math.ceil(Math.log(n) / Math.log(2)));
    }
    
    /**
     * Calculate which side of bracket a position is on (for opposite side placement)
     * Returns 0 for left side, 1 for right side
     */
    public static int calculateBracketSide(int position, int totalPositions) {
        return position < (totalPositions / 2) ? 0 : 1;
    }
    
    /**
     * Calculate opposite side position for loser bracket placement
     * This implements the "opposite side" rule to prevent same matchups
     */
    public static BracketPosition calculateOppositeLoserPosition(
            BracketPosition winnerPosition, 
            int winnerRound,
            LoserBracketStructure loserStructure) {
        
        // Flip to opposite side
        int oppositeSide = 1 - calculateBracketSide(winnerPosition.position, winnerPosition.totalInRound);
        
        // Calculate which round in loser's bracket this corresponds to
        int loserRound = calculateLoserRoundFromWinnerRound(winnerRound);
        
        // Calculate position within that round on the opposite side
        int positionInRound = calculateLoserPositionInRound(oppositeSide, loserRound, loserStructure);
        
        return BracketPosition.builder()
            .position(positionInRound)
            .round(loserRound)
            .side(oppositeSide)
            .totalInRound(calculateLoserRoundSize(loserRound, loserStructure))
            .build();
    }
    
    /**
     * Map winner's bracket round to corresponding loser's bracket round
     */
    private static int calculateLoserRoundFromWinnerRound(int winnerRound) {
        // Players losing in winner round 1 → loser round 1
        // Players losing in winner round 2 → loser round 3  
        // Players losing in winner round 3 → loser round 5
        // Pattern: loser_round = (winner_round - 1) * 2 + 1
        return (winnerRound - 1) * 2 + 1;
    }
    
    /**
     * Calculate position within loser bracket round based on side
     */
    private static int calculateLoserPositionInRound(int side, int loserRound, LoserBracketStructure structure) {
        int roundSize = calculateLoserRoundSize(loserRound, structure);
        int sideSize = roundSize / 2;
        
        // For now, assign to first available position on that side
        // In full implementation, this would be more sophisticated
        return side * sideSize;
    }
    
    /**
     * Calculate number of matches in a specific loser bracket round
     */
    private static int calculateLoserRoundSize(int loserRound, LoserBracketStructure structure) {
        // Simplified calculation - in reality this alternates between
        // rounds that receive new dropdowns vs consolidation rounds
        return Math.max(1, structure.firstRoundMatches / (int) Math.pow(2, loserRound / 2));
    }
    
    /**
     * Structure representing winner's bracket dimensions
     */
    @Data
    @lombok.Builder
    public static class WinnerBracketStructure {
        private int bracketSize;        // Next power of 2
        private int totalRounds;        // Number of rounds
        private int firstRoundMatches;  // Matches in round 1
        private int byeCount;          // Number of byes needed
    }
    
    /**
     * Structure representing loser's bracket dimensions
     */
    @Data
    @lombok.Builder
    public static class LoserBracketStructure {
        private int totalRounds;        // Number of rounds in loser bracket
        private int firstRoundMatches;  // Matches in loser bracket round 1
    }
    
    /**
     * Represents a position within a bracket
     */
    @Data
    @lombok.Builder
    public static class BracketPosition {
        private int position;           // Position within round
        private int round;             // Round number
        private int side;              // 0 = left, 1 = right
        private int totalInRound;      // Total positions in this round
    }
}