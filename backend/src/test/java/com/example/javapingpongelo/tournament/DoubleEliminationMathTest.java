package com.example.javapingpongelo.tournament;

import com.example.javapingpongelo.tournament.engine.DoubleEliminationMath;
import com.example.javapingpongelo.tournament.engine.DoubleEliminationMath.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 🧮 UTILITY: Tests the mathematical foundation for double elimination tournament calculations
 * 
 * This test verifies:
 * - Bracket size calculations (next power of 2)
 * - Total match count formulas (2n-2 matches)
 * - Winner bracket structure (rounds, matches, byes)
 * - Loser bracket structure (complex interleaving patterns)
 * 
 * Status: Core utility test - should be maintained
 */
@SpringBootTest
public class DoubleEliminationMathTest {
    
    @Test
    public void testTotalMatchCalculation() {
        // Known double elimination match counts (formula: 2n - 2)
        assertEquals(2, DoubleEliminationMath.calculateTotalMatches(2));   // 2 players = 2 matches max
        assertEquals(4, DoubleEliminationMath.calculateTotalMatches(3));   // 3 players = 4 matches max  
        assertEquals(6, DoubleEliminationMath.calculateTotalMatches(4));   // 4 players = 6 matches max
        assertEquals(14, DoubleEliminationMath.calculateTotalMatches(8));  // 8 players = 14 matches max
        
        System.out.println("Total match calculations verified ✓");
    }
    
    @Test
    public void testWinnerBracketStructure() {
        // Test 8-player tournament (perfect power of 2)
        WinnerBracketStructure eightPlayer = DoubleEliminationMath.calculateWinnerBracket(8);
        assertEquals(8, eightPlayer.getBracketSize());
        assertEquals(3, eightPlayer.getTotalRounds());      // 8→4→2→1 = 3 rounds
        assertEquals(4, eightPlayer.getFirstRoundMatches()); // 8 players = 4 first round matches
        assertEquals(0, eightPlayer.getByeCount());         // No byes needed
        
        // Test 5-player tournament (non-power of 2)
        WinnerBracketStructure fivePlayer = DoubleEliminationMath.calculateWinnerBracket(5);
        assertEquals(8, fivePlayer.getBracketSize());       // Next power of 2
        assertEquals(3, fivePlayer.getTotalRounds());
        assertEquals(4, fivePlayer.getFirstRoundMatches());
        assertEquals(3, fivePlayer.getByeCount());          // 3 byes needed (8-5=3)
        
        System.out.println("Winner bracket structure calculations verified ✓");
        System.out.println("5-player tournament: " + fivePlayer);
        System.out.println("8-player tournament: " + eightPlayer);
    }
    
    @Test
    public void testLoserBracketStructure() {
        // Test 8-player loser bracket
        LoserBracketStructure eightPlayerLoser = DoubleEliminationMath.calculateLoserBracket(8);
        assertEquals(4, eightPlayerLoser.getTotalRounds());     // (3-1)*2 = 4 loser rounds
        assertEquals(2, eightPlayerLoser.getFirstRoundMatches()); // 4 losers from winner R1 = 2 matches
        
        // Test 5-player loser bracket  
        LoserBracketStructure fivePlayerLoser = DoubleEliminationMath.calculateLoserBracket(5);
        assertEquals(4, fivePlayerLoser.getTotalRounds());
        assertEquals(0, fivePlayerLoser.getFirstRoundMatches()); // Only 2 players play R1, 1 loser = 0 matches
        
        System.out.println("Loser bracket structure calculations verified ✓");
        System.out.println("5-player loser bracket: " + fivePlayerLoser);
        System.out.println("8-player loser bracket: " + eightPlayerLoser);
    }
    
    @Test
    public void testBracketSideCalculation() {
        // Test bracket side calculation (for opposite placement)
        assertEquals(0, DoubleEliminationMath.calculateBracketSide(0, 8)); // Left side
        assertEquals(0, DoubleEliminationMath.calculateBracketSide(3, 8)); // Left side
        assertEquals(1, DoubleEliminationMath.calculateBracketSide(4, 8)); // Right side
        assertEquals(1, DoubleEliminationMath.calculateBracketSide(7, 8)); // Right side
        
        System.out.println("Bracket side calculations verified ✓");
    }
    
    @Test
    public void testPowerOf2Calculation() {
        assertEquals(2, DoubleEliminationMath.getNextPowerOf2(2));
        assertEquals(4, DoubleEliminationMath.getNextPowerOf2(3));
        assertEquals(4, DoubleEliminationMath.getNextPowerOf2(4));
        assertEquals(8, DoubleEliminationMath.getNextPowerOf2(5));
        assertEquals(8, DoubleEliminationMath.getNextPowerOf2(8));
        assertEquals(16, DoubleEliminationMath.getNextPowerOf2(9));
        
        System.out.println("Power of 2 calculations verified ✓");
    }
    
    @Test
    public void testOppositeLoserPlacement() {
        // Test the "opposite side" placement algorithm
        LoserBracketStructure loserStructure = DoubleEliminationMath.calculateLoserBracket(8);
        
        // Player from left side of winner bracket should go to right side of loser bracket
        BracketPosition leftWinnerPos = BracketPosition.builder()
            .position(0)    // Left side
            .round(1)
            .side(0)
            .totalInRound(4)
            .build();
            
        BracketPosition loserPos = DoubleEliminationMath.calculateOppositeLoserPosition(
            leftWinnerPos, 1, loserStructure);
            
        assertEquals(1, loserPos.getSide()); // Should be right side (opposite of 0)
        assertEquals(1, loserPos.getRound()); // Winner R1 → Loser R1
        
        System.out.println("Opposite loser placement verified ✓");
        System.out.println("Winner pos " + leftWinnerPos + " → Loser pos " + loserPos);
    }
}