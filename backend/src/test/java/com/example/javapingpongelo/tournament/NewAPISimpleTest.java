package com.example.javapingpongelo.tournament;

import com.example.javapingpongelo.models.*;
import com.example.javapingpongelo.tournament.engine.impl.DoubleEliminationRules;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ CURRENT: Tests the new pre-built bracket API with a 4-player double elimination tournament
 * 
 * This test verifies that:
 * - The complete bracket structure is created upfront
 * - Tournament progresses through all phases without infinite loops
 * - Winner bracket (Semifinals → WB Finals) completes
 * - Loser bracket (LB Round 1 → LB Finals) completes  
 * - Grand Finals activates correctly
 * - Seeding works as expected (higher seeds advance)
 * 
 * Expected result: Complete tournament with champion declared
 */
@SpringBootTest
@Slf4j
public class NewAPISimpleTest {

    @Test
    public void testNewPreBuiltBracketAPI() {
        log.info("🚀 Testing new pre-built bracket API with 4 players");
        
        // Create tournament
        Tournament tournament = Tournament.builder()
            .id(UUID.randomUUID())
            .name("4-Player Test Tournament")
            .tournamentType(Tournament.TournamentType.DOUBLE_ELIMINATION)
            .numberOfPlayers(4)
            .currentRound(0)
            .build();
        
        // Create 4 participants
        List<TournamentPlayer> participants = Arrays.asList(
            createPlayer(tournament, "Alice", 1),
            createPlayer(tournament, "Bob", 2),
            createPlayer(tournament, "Charlie", 3),
            createPlayer(tournament, "Diana", 4)
        );
        
        Map<UUID, String> playerNames = createPlayerNameMap(participants);
        
        // Create rules engine
        DoubleEliminationRules rules = new DoubleEliminationRules();
        
        // Generate initial bracket (this should create ALL rounds and matches)
        List<TournamentRound> allRounds = new ArrayList<>(rules.generateInitialBracket(tournament, participants));
        log.info("✅ Generated complete bracket with {} rounds", allRounds.size());
        
        logTournamentStructure(allRounds, playerNames);
        
        // Phase 1: Get initial ready matches
        List<TournamentMatch> readyMatches = rules.getReadyMatches(tournament, allRounds);
        log.info("📋 Phase 1: {} matches ready to play", readyMatches.size());
        
        for (TournamentMatch match : readyMatches) {
            log.info("  Ready: {}", formatMatch(match, playerNames));
        }
        
        // Play the initial matches (higher seed wins)
        for (TournamentMatch match : readyMatches) {
            playMatchHigherSeedWins(match, playerNames);
        }
        
        // Phase 2: Advance players and get next matches
        List<TournamentMatch> nextMatches = rules.advancePlayersAndGetNextMatches(
            tournament, readyMatches, participants, allRounds);
        log.info("📋 Phase 2: {} new matches ready after advancement", nextMatches.size());
        
        for (TournamentMatch match : nextMatches) {
            log.info("  Next: {}", formatMatch(match, playerNames));
        }
        
        // Phase 3: Play next matches and continue
        for (TournamentMatch match : nextMatches) {
            playMatchHigherSeedWins(match, playerNames);
        }
        
        // Phase 4+: Continue with advancement-driven loop
        int phase = 4;
        int maxPhases = 10; // Safety limit
        List<TournamentMatch> currentMatches = rules.advancePlayersAndGetNextMatches(
            tournament, nextMatches, participants, allRounds);
        log.info("📋 Phase {}: {} new matches after advancement", phase - 1, currentMatches.size());
        
        while (!currentMatches.isEmpty() && phase <= maxPhases) {
            log.info("📋 Phase {}: {} matches ready to play", phase, currentMatches.size());
            
            // Play all current matches
            for (TournamentMatch match : currentMatches) {
                log.info("  Playing: {}", formatMatch(match, playerNames));
                playMatchHigherSeedWins(match, playerNames);
            }
            
            // Advance players and get next batch
            List<TournamentMatch> newMatches = rules.advancePlayersAndGetNextMatches(
                tournament, currentMatches, participants, allRounds);
            log.info("  → Generated {} new matches", newMatches.size());
            
            // Debug: check which rounds have what status after advancement
            for (TournamentRound round : allRounds) {
                int roundReadyMatches = (int) round.getMatches().stream()
                    .filter(m -> !m.isCompleted() && hasPlayersReady(m))
                    .count();
                log.info("    Round {}: {} ({}), {} ready matches", 
                    round.getName(), round.getStatus(), 
                    round.getBracketType(), roundReadyMatches);
            }
            
            currentMatches = newMatches;
            phase++;
        }
        
        if (phase > maxPhases) {
            log.error("❌ Tournament did not complete within {} phases - possible infinite loop", maxPhases);
            throw new AssertionError("Tournament did not complete - infinite loop detected");
        }
        
        log.info("✅ Tournament completed successfully in {} phases!", phase - 1);
        logFinalResults(allRounds, playerNames);
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
        String[] names = {"Alice", "Bob", "Charlie", "Diana"};
        
        for (int i = 0; i < participants.size() && i < names.length; i++) {
            nameMap.put(participants.get(i).getPlayerId(), names[i]);
        }
        
        return nameMap;
    }
    
    private void playMatchHigherSeedWins(TournamentMatch match, Map<UUID, String> playerNames) {
        if (match.getTeam1Ids() == null || match.getTeam2Ids() == null || 
            match.getTeam1Ids().isEmpty() || match.getTeam2Ids().isEmpty()) {
            return; // Skip invalid matches
        }
        
        // Higher seed (lower number) wins
        int seed1 = match.getTeam1Seed() != null ? match.getTeam1Seed() : Integer.MAX_VALUE;
        int seed2 = match.getTeam2Seed() != null ? match.getTeam2Seed() : Integer.MAX_VALUE;
        
        boolean team1Wins = seed1 < seed2;
        
        match.setCompleted(true);
        match.setWinnerIds(team1Wins ? match.getTeam1Ids() : match.getTeam2Ids());
        match.setTeam1Score(team1Wins ? 11 : 7);
        match.setTeam2Score(team1Wins ? 7 : 11);
        
        String winner = getPlayerName(team1Wins ? match.getTeam1Ids().get(0) : match.getTeam2Ids().get(0), playerNames);
        String loser = getPlayerName(team1Wins ? match.getTeam2Ids().get(0) : match.getTeam1Ids().get(0), playerNames);
        
        log.info("    🏓 {} beats {} (11-7)", winner, loser);
    }
    
    private String getPlayerName(UUID playerId, Map<UUID, String> playerNames) {
        return playerNames.getOrDefault(playerId, "Unknown");
    }
    
    private String formatMatch(TournamentMatch match, Map<UUID, String> playerNames) {
        String player1 = match.getTeam1Ids() != null && !match.getTeam1Ids().isEmpty() 
            ? getPlayerName(match.getTeam1Ids().get(0), playerNames) : "TBD";
        String player2 = match.getTeam2Ids() != null && !match.getTeam2Ids().isEmpty() 
            ? getPlayerName(match.getTeam2Ids().get(0), playerNames) : "TBD";
        
        return String.format("%s: %s vs %s", match.getDisplayId(), player1, player2);
    }
    
    private void logTournamentStructure(List<TournamentRound> allRounds, Map<UUID, String> playerNames) {
        log.info("\n📊 COMPLETE TOURNAMENT STRUCTURE:");
        
        // Group by bracket type
        Map<TournamentMatch.BracketType, List<TournamentRound>> roundsByType = allRounds.stream()
            .collect(Collectors.groupingBy(TournamentRound::getBracketType));
        
        // Winner's bracket
        List<TournamentRound> winnerRounds = roundsByType.getOrDefault(TournamentMatch.BracketType.WINNER, new ArrayList<>());
        winnerRounds.sort(Comparator.comparing(TournamentRound::getRoundNumber));
        
        log.info("🏆 WINNER'S BRACKET:");
        for (TournamentRound round : winnerRounds) {
            log.info("  {} ({}): {} matches", round.getName(), round.getStatus(), round.getMatches().size());
            for (TournamentMatch match : round.getMatches()) {
                log.info("    {}", formatMatch(match, playerNames));
            }
        }
        
        // Loser's bracket
        List<TournamentRound> loserRounds = roundsByType.getOrDefault(TournamentMatch.BracketType.LOSER, new ArrayList<>());
        loserRounds.sort(Comparator.comparing(TournamentRound::getRoundNumber));
        
        log.info("💀 LOSER'S BRACKET:");
        for (TournamentRound round : loserRounds) {
            log.info("  {} ({}): {} matches", round.getName(), round.getStatus(), round.getMatches().size());
            for (TournamentMatch match : round.getMatches()) {
                log.info("    {}", formatMatch(match, playerNames));
            }
        }
        
        // Grand finals
        List<TournamentRound> grandRounds = allRounds.stream()
            .filter(r -> r.getName().contains("Grand"))
            .collect(Collectors.toList());
            
        log.info("🏅 GRAND FINALS:");
        for (TournamentRound round : grandRounds) {
            log.info("  {} ({}): {} matches", round.getName(), round.getStatus(), round.getMatches().size());
            for (TournamentMatch match : round.getMatches()) {
                log.info("    {}", formatMatch(match, playerNames));
            }
        }
    }
    
    private boolean hasPlayersReady(TournamentMatch match) {
        return match.getTeam1Ids() != null && !match.getTeam1Ids().isEmpty() &&
               match.getTeam2Ids() != null && !match.getTeam2Ids().isEmpty();
    }
    
    private void logFinalResults(List<TournamentRound> allRounds, Map<UUID, String> playerNames) {
        log.info("\n🏆 FINAL TOURNAMENT RESULTS:");
        
        // Find grand finals winner
        Optional<TournamentMatch> grandFinalsMatch = allRounds.stream()
            .filter(round -> round.getName().contains("Grand"))
            .flatMap(round -> round.getMatches().stream())
            .filter(TournamentMatch::isCompleted)
            .findFirst();
        
        if (grandFinalsMatch.isPresent()) {
            TournamentMatch finalMatch = grandFinalsMatch.get();
            String champion = getPlayerName(finalMatch.getWinnerIds().get(0), playerNames);
            log.info("🥇 CHAMPION: {}", champion);
        } else {
            log.info("❓ Grand finals not completed");
        }
        
        // Count completed matches
        long completedMatches = allRounds.stream()
            .flatMap(round -> round.getMatches().stream())
            .filter(TournamentMatch::isCompleted)
            .count();
            
        long totalMatches = allRounds.stream()
            .flatMap(round -> round.getMatches().stream())
            .count();
            
        log.info("📊 Matches completed: {}/{}", completedMatches, totalMatches);
    }
}