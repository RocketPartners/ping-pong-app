package com.example.javapingpongelo.tournament;

import com.example.javapingpongelo.models.*;
import com.example.javapingpongelo.tournament.engine.MatchBatch;
import com.example.javapingpongelo.tournament.engine.TournamentUpdateResult;
import com.example.javapingpongelo.tournament.engine.impl.DoubleEliminationRules;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

/**
 * 🔄 LEGACY: Tests the old batch-based match management API for double elimination tournaments
 * 
 * This test uses the original reactive approach where:
 * - Rounds are generated on-demand as matches complete
 * - Match batches are processed sequentially
 * - Winner's and loser's brackets are synchronized automatically
 * 
 * Status: Still functional but superseded by NewAPISimpleTest and NewAPI15PersonTest
 * Note: May be removed in future cleanup as part of old API deprecation
 */
@SpringBootTest
@Slf4j
public class BatchBasedDoubleEliminationTest {

    @Test
    public void testBatchBasedTournamentFlow() {
        log.info("🚀 Testing Batch-Based Double Elimination Tournament Flow");
        
        // Create a small 8-person tournament for easier testing
        Tournament tournament = Tournament.builder()
            .id(UUID.randomUUID())
            .name("8-Person Batch Test Tournament")
            .tournamentType(Tournament.TournamentType.DOUBLE_ELIMINATION)
            .numberOfPlayers(8)
            .currentRound(0)
            .build();
        
        // Create 8 participants
        List<TournamentPlayer> participants = Arrays.asList(
            createPlayer(tournament, "Alice", 1),
            createPlayer(tournament, "Bob", 2),
            createPlayer(tournament, "Charlie", 3),
            createPlayer(tournament, "Diana", 4),
            createPlayer(tournament, "Eve", 5),
            createPlayer(tournament, "Frank", 6),
            createPlayer(tournament, "Grace", 7),
            createPlayer(tournament, "Henry", 8)
        );
        
        Map<UUID, String> playerNames = createPlayerNameMap(participants);
        
        // Create rules engine
        DoubleEliminationRules rules = new DoubleEliminationRules();
        
        // Generate initial bracket
        List<TournamentRound> allRounds = new ArrayList<>(rules.generateInitialBracket(tournament, participants));
        log.info("🎯 Generated initial bracket with {} rounds", allRounds.size());
        
        // Main tournament loop using batch-based API
        int batchCount = 0;
        boolean tournamentComplete = false;
        final int MAX_BATCHES = 20; // Safety limit
        
        while (!tournamentComplete && batchCount < MAX_BATCHES) {
            batchCount++;
            
            // Get next available match batches
            List<MatchBatch> batches = rules.getNextMatchBatches(tournament, allRounds);
            
            if (batches.isEmpty()) {
                log.info("🏁 No more batches available - tournament may be complete");
                break;
            }
            
            log.info("\n🎯 BATCH {}: {} batches available", batchCount, batches.size());
            
            // Process each batch in priority order
            for (MatchBatch batch : batches) {
                log.info("  📋 Processing: {} ({} matches, priority {})", 
                        batch.getDescription(), batch.getMatches().size(), batch.getPriority());
                
                // Play all matches in the batch (higher seed always wins)
                for (TournamentMatch match : batch.getMatches()) {
                    playMatchHigherSeedWins(match, playerNames);
                }
                
                // Complete the batch and get updates
                TournamentUpdateResult result = rules.completeMatchBatch(tournament, batch, participants, allRounds);
                
                // Add any new rounds that were generated
                allRounds.addAll(result.getNewRounds());
                
                log.info("    ✅ Completed: {} - Generated {} new rounds", 
                        result.getDescription(), result.getNewRounds().size());
                
                // Check if tournament is complete
                if (result.isTournamentComplete()) {
                    tournamentComplete = true;
                    log.info("🏆 TOURNAMENT COMPLETE! Winners: {}", result.getWinners());
                    break;
                }
            }
        }
        
        log.info("\n🎯 Tournament processed in {} batches", batchCount);
        
        if (tournamentComplete) {
            log.info("✅ SUCCESS: Tournament completed successfully using batch-based API!");
        } else {
            log.warn("⚠️ Tournament did not complete within {} batches", MAX_BATCHES);
        }
    }
    
    private TournamentPlayer createPlayer(Tournament tournament, String name, int seed) {
        return TournamentPlayer.builder()
            .id(UUID.randomUUID())
            .tournament(tournament)
            .playerId(UUID.nameUUIDFromBytes(name.getBytes()))
            .seed(seed)
            .build();
    }
    
    private Map<UUID, String> createPlayerNameMap(List<TournamentPlayer> participants) {
        Map<UUID, String> nameMap = new HashMap<>();
        String[] names = {"Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Henry"};
        
        for (int i = 0; i < participants.size() && i < names.length; i++) {
            nameMap.put(participants.get(i).getPlayerId(), names[i]);
        }
        
        return nameMap;
    }
    
    /**
     * Play a match with higher seed always winning
     */
    private void playMatchHigherSeedWins(TournamentMatch match, Map<UUID, String> playerNames) {
        if (match.getTeam1Ids() == null || match.getTeam2Ids() == null || 
            match.getTeam1Ids().isEmpty() || match.getTeam2Ids().isEmpty()) {
            return; // Skip invalid matches
        }
        
        // Determine higher seed (lower seed number = higher seed)
        int seed1 = match.getTeam1Seed() != null ? match.getTeam1Seed() : Integer.MAX_VALUE;
        int seed2 = match.getTeam2Seed() != null ? match.getTeam2Seed() : Integer.MAX_VALUE;
        
        boolean team1Wins = seed1 < seed2; // Lower seed number = higher seed = wins
        
        // Set match result
        match.setCompleted(true);
        match.setWinnerIds(team1Wins ? match.getTeam1Ids() : match.getTeam2Ids());
        match.setTeam1Score(team1Wins ? 11 : 7);
        match.setTeam2Score(team1Wins ? 7 : 11);
        
        // Log result
        String player1 = getPlayerName(match.getTeam1Ids().get(0), playerNames);
        String player2 = getPlayerName(match.getTeam2Ids().get(0), playerNames);
        String winner = team1Wins ? player1 : player2;
        String loser = team1Wins ? player2 : player1;
        
        log.info("      🏓 {} beats {} (11-7)", winner, loser);
    }
    
    /**
     * Get player name from ID
     */
    private String getPlayerName(UUID playerId, Map<UUID, String> playerNames) {
        return playerNames.getOrDefault(playerId, "Unknown");
    }
}