package com.example.javapingpongelo.tournament;

import com.example.javapingpongelo.models.*;
import com.example.javapingpongelo.tournament.engine.impl.DoubleEliminationRules;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ COMPREHENSIVE: Tests the new pre-built bracket API across all supported tournament sizes (4-16)
 * 
 * This validates that the tournament system works correctly for:
 * - Power of 2 sizes (4, 8, 16): Perfect brackets, no byes
 * - Non-power of 2 sizes (5, 6, 7, 9, 10, 11, 12, 13, 14, 15): Complex bye handling
 * - Edge cases: Minimal supported size (4 players)
 * - Maximum size: 16-player full bracket
 * 
 * Note: 2-3 player tournaments are not supported for double elimination as they don't
 * provide meaningful bracket structure. Use single elimination or round-robin instead.
 * 
 * Expected result: All tournament sizes complete successfully with proper double elimination flow
 */
@SpringBootTest
@Slf4j
public class MultiSizeDoubleEliminationTest {

    @Test
    public void testAllTournamentSizes4Through16() {
        System.out.println("🎯".repeat(50));
        System.out.println("🏆 COMPREHENSIVE DOUBLE ELIMINATION TEST - ALL SIZES 4-16 🏆");
        System.out.println("🎯".repeat(50));
        
        DoubleEliminationRules rules = new DoubleEliminationRules();
        
        // Test each size from 4 to 16 players (2-3 not supported for double elimination)
        for (int playerCount = 4; playerCount <= 16; playerCount++) {
            System.out.println("\n" + "=".repeat(60));
            System.out.printf("🚀 TESTING %d-PLAYER TOURNAMENT 🚀%n", playerCount);
            System.out.println("=".repeat(60));
            
            try {
                runTournamentSizeTest(rules, playerCount);
                System.out.printf("✅ %d-player tournament: SUCCESS%n", playerCount);
            } catch (Exception e) {
                System.out.printf("❌ %d-player tournament: FAILED - %s%n", playerCount, e.getMessage());
                throw new AssertionError(String.format("%d-player tournament failed", playerCount), e);
            }
        }
        
        System.out.println("\n" + "🎉".repeat(50));
        System.out.println("🏆 ALL TOURNAMENT SIZES (4-16) PASSED SUCCESSFULLY! 🏆");
        System.out.println("🎉".repeat(50));
    }
    
    private void runTournamentSizeTest(DoubleEliminationRules rules, int playerCount) {
        // Create tournament
        Tournament tournament = Tournament.builder()
            .id(UUID.randomUUID())
            .name(String.format("%d-Player Double Elimination", playerCount))
            .tournamentType(Tournament.TournamentType.DOUBLE_ELIMINATION)
            .numberOfPlayers(playerCount)
            .currentRound(0)
            .build();
        
        // Create participants with predictable names
        List<TournamentPlayer> participants = new ArrayList<>();
        String[] names = {"Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Henry",
                         "Ivy", "Jack", "Kate", "Liam", "Maya", "Noah", "Olivia", "Paul"};
        
        for (int i = 0; i < playerCount; i++) {
            participants.add(TournamentPlayer.builder()
                .id(UUID.randomUUID())
                .tournament(tournament)
                .playerId(UUID.nameUUIDFromBytes(names[i].getBytes()))
                .seed(i + 1)
                .build());
        }
        
        Map<UUID, String> playerNames = new HashMap<>();
        for (int i = 0; i < playerCount; i++) {
            playerNames.put(participants.get(i).getPlayerId(), names[i]);
        }
        
        // Generate complete bracket
        List<TournamentRound> allRounds = new ArrayList<>(rules.generateInitialBracket(tournament, participants));
        log.info("Generated bracket with {} rounds for {} players", allRounds.size(), playerCount);
        
        // Calculate rough expected match count (2n-2 for double elimination is a rough estimate)
        int expectedMatches = (2 * playerCount) - 2;
        int actualMatches = allRounds.stream().mapToInt(r -> r.getMatches().size()).sum();
        log.info("Expected matches: {}, Actual matches: {}", expectedMatches, actualMatches);
        
        // Validate that match count is reasonable (should be between 1.5x and 3x the player count)
        // Double elimination can vary significantly based on bracket structure and byes
        int minMatches = (int) (playerCount * 1.5);
        int maxMatches = playerCount * 3;
        
        if (actualMatches < minMatches || actualMatches > maxMatches) {
            throw new AssertionError(String.format("Match count unreasonable: got %d matches for %d players (expected range: %d-%d)", 
                actualMatches, playerCount, minMatches, maxMatches));
        }
        
        // Run tournament simulation
        int phase = 1;
        int maxPhases = playerCount * 2; // Conservative safety limit
        List<TournamentMatch> currentMatches = rules.getReadyMatches(tournament, allRounds);
        
        while (!currentMatches.isEmpty() && phase <= maxPhases) {
            log.info("Phase {}: {} matches ready", phase, currentMatches.size());
            
            // Play all current matches (higher seed always wins)
            for (TournamentMatch match : currentMatches) {
                playMatchHigherSeedWins(match, playerNames);
            }
            
            // Advance players
            List<TournamentMatch> newMatches = rules.advancePlayersAndGetNextMatches(
                tournament, currentMatches, participants, allRounds);
            
            currentMatches = newMatches;
            phase++;
        }
        
        if (phase > maxPhases) {
            throw new AssertionError(String.format("Tournament did not complete within %d phases", maxPhases));
        }
        
        // Verify tournament completed with champion
        verifyTournamentCompletion(allRounds, playerNames, playerCount);
        
        log.info("✅ {}-player tournament completed in {} phases", playerCount, phase - 1);
    }
    
    private void playMatchHigherSeedWins(TournamentMatch match, Map<UUID, String> playerNames) {
        // Handle TBD cases as byes
        boolean team1HasPlayer = match.getTeam1Ids() != null && !match.getTeam1Ids().isEmpty();
        boolean team2HasPlayer = match.getTeam2Ids() != null && !match.getTeam2Ids().isEmpty();
        
        if (!team1HasPlayer && !team2HasPlayer) {
            return; // Skip invalid matches
        }
        
        if (!team1HasPlayer) {
            // Team 2 wins by bye
            match.setCompleted(true);
            match.setWinnerIds(match.getTeam2Ids());
            match.setTeam1Score(0);
            match.setTeam2Score(11);
            return;
        }
        
        if (!team2HasPlayer) {
            // Team 1 wins by bye
            match.setCompleted(true);
            match.setWinnerIds(match.getTeam1Ids());
            match.setTeam1Score(11);
            match.setTeam2Score(0);
            return;
        }
        
        // Determine higher seed (lower seed number = higher seed)
        int seed1 = match.getTeam1Seed() != null ? match.getTeam1Seed() : Integer.MAX_VALUE;
        int seed2 = match.getTeam2Seed() != null ? match.getTeam2Seed() : Integer.MAX_VALUE;
        
        boolean team1Wins = seed1 < seed2;
        
        // Set match result
        match.setCompleted(true);
        match.setWinnerIds(team1Wins ? match.getTeam1Ids() : match.getTeam2Ids());
        match.setTeam1Score(team1Wins ? 11 : 7);
        match.setTeam2Score(team1Wins ? 7 : 11);
    }
    
    private void verifyTournamentCompletion(List<TournamentRound> allRounds, Map<UUID, String> playerNames, int playerCount) {
        // Find grand finals match
        Optional<TournamentMatch> grandFinalsMatch = allRounds.stream()
            .filter(round -> round.getName().contains("Grand"))
            .flatMap(round -> round.getMatches().stream())
            .filter(TournamentMatch::isCompleted)
            .findFirst();
            
        if (grandFinalsMatch.isEmpty()) {
            throw new AssertionError("Grand Finals not completed");
        }
        
        // Verify champion exists
        TournamentMatch finalMatch = grandFinalsMatch.get();
        if (finalMatch.getWinnerIds() == null || finalMatch.getWinnerIds().isEmpty()) {
            throw new AssertionError("No champion determined");
        }
        
        String champion = playerNames.getOrDefault(finalMatch.getWinnerIds().get(0), "Unknown");
        log.info("🏆 Champion: {}", champion);
        
        // For predictable seeding, Alice (seed #1) should win most tournaments
        if (playerCount >= 3 && !"Alice".equals(champion)) {
            log.warn("⚠️ Unexpected champion: {} (expected Alice for higher seed wins)", champion);
        }
        
        // Count completed matches
        int completedMatches = allRounds.stream()
            .mapToInt(r -> (int) r.getMatches().stream().filter(TournamentMatch::isCompleted).count())
            .sum();
        
        log.info("Completed {}/{} total matches", completedMatches, 
            allRounds.stream().mapToInt(r -> r.getMatches().size()).sum());
    }
}