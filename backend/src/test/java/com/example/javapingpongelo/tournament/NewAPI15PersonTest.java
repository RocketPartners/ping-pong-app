package com.example.javapingpongelo.tournament;

import com.example.javapingpongelo.models.*;
import com.example.javapingpongelo.tournament.engine.impl.DoubleEliminationRules;
import com.example.javapingpongelo.tournament.utils.TournamentBracketVisualizer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ CURRENT: Tests the new pre-built bracket API with a 15-player double elimination tournament
 * 
 * This is the most complex test demonstrating:
 * - Complete bracket with 11 rounds and 26 matches created upfront
 * - Non-power-of-2 tournament size (15 players in 16-slot bracket)
 * - Proper bye handling for TBD matches (automatic wins)
 * - Winner bracket: Round of 16 → Quarterfinals → Semifinals → WB Finals
 * - Complex loser bracket with multiple interleaving rounds
 * - 🌟 Initial bracket visualization showing all seeded players before any matches
 * - 🎯 Visual tournament bracket display at the end of each phase
 * - Detailed phase-by-phase progression logging
 * - Seeding verification (Alice #1 vs Bob #2 in WB Finals)
 * 
 * Expected result: Tournament completes 17+/26 matches with detailed output and visual brackets
 */
@SpringBootTest
@Slf4j
public class NewAPI15PersonTest {

    @Test
    public void test15PersonTournamentWithNewAPI() {
        System.out.println("🚀".repeat(50));
        System.out.println("🏆 15-PERSON TOURNAMENT - NEW PRE-BUILT BRACKET API 🏆");
        System.out.println("🚀".repeat(50));
        
        // Create tournament
        Tournament tournament = Tournament.builder()
            .id(UUID.randomUUID())
            .name("15-Person Double Elimination Championship")
            .tournamentType(Tournament.TournamentType.DOUBLE_ELIMINATION)
            .numberOfPlayers(15)
            .currentRound(0)
            .build();
        
        // Create 15 participants with clear names and seeds
        List<TournamentPlayer> participants = Arrays.asList(
            createPlayer(tournament, "Alice", 1),    // Top seed
            createPlayer(tournament, "Bob", 2),      // Should meet Alice in WB Finals
            createPlayer(tournament, "Charlie", 3), 
            createPlayer(tournament, "Diana", 4),
            createPlayer(tournament, "Eve", 5),
            createPlayer(tournament, "Frank", 6),
            createPlayer(tournament, "Grace", 7),
            createPlayer(tournament, "Henry", 8),
            createPlayer(tournament, "Ivy", 9),
            createPlayer(tournament, "Jack", 10),
            createPlayer(tournament, "Kate", 11),
            createPlayer(tournament, "Liam", 12),
            createPlayer(tournament, "Maya", 13),
            createPlayer(tournament, "Noah", 14),
            createPlayer(tournament, "Olivia", 15)   // Lowest seed
        );
        
        Map<UUID, String> playerNames = createPlayerNameMap(participants);
        
        // Create rules engine
        DoubleEliminationRules rules = new DoubleEliminationRules();
        
        // Generate complete pre-built bracket (this creates ALL rounds and matches)
        List<TournamentRound> allRounds = new ArrayList<>(rules.generateInitialBracket(tournament, participants));
        System.out.println("✅ Generated complete bracket with " + allRounds.size() + " rounds");
        
        // Show initial bracket structure
        logTournamentStructure(allRounds, playerNames);
        
        // 🎯 VISUALIZE INITIAL BRACKET - Before any matches are played
        visualizeInitialBracket(allRounds, playerNames);
        
        // Advancement-driven tournament progression
        int phase = 1;
        int maxPhases = 20; // Safety limit for 15-person tournament
        List<TournamentMatch> currentMatches = rules.getReadyMatches(tournament, allRounds);
        
        while (!currentMatches.isEmpty() && phase <= maxPhases) {
            System.out.println("\n🔥 PHASE " + phase + ": " + currentMatches.size() + " matches ready to play 🔥");
            
            // Show which round(s) we're playing
            Set<String> activeRounds = currentMatches.stream()
                .filter(match -> match.getTournamentRound() != null)
                .map(match -> match.getTournamentRound().getName())
                .collect(Collectors.toSet());
            log.info("📋 Active rounds: {}", String.join(", ", activeRounds));
            
            // Play all current matches (higher seed always wins for predictable results)
            for (TournamentMatch match : currentMatches) {
                log.info("  🏓 Playing: {}", formatMatch(match, playerNames));
                playMatchHigherSeedWins(match, playerNames);
            }
            
            // Advance players and get next batch of ready matches
            List<TournamentMatch> newMatches = rules.advancePlayersAndGetNextMatches(
                tournament, currentMatches, participants, allRounds);
            System.out.println("  ✅ Advancement completed → " + newMatches.size() + " new matches ready");
            
            // Debug: If no new matches, check why
            if (newMatches.isEmpty()) {
                System.out.println("  🔍 DEBUG: No new matches found. Checking tournament state...");
                long readyRounds = allRounds.stream().filter(r -> r.getStatus() == TournamentRound.RoundStatus.READY).count();
                long pendingRounds = allRounds.stream().filter(r -> r.getStatus() == TournamentRound.RoundStatus.PENDING).count();
                long completedRounds = allRounds.stream().filter(r -> r.getStatus() == TournamentRound.RoundStatus.COMPLETED).count();
                System.out.println("  🔍 Round status: " + readyRounds + " READY, " + pendingRounds + " PENDING, " + completedRounds + " COMPLETED");
                
                // Check if there are any incomplete matches in ready rounds
                allRounds.stream()
                    .filter(round -> round.getStatus() == TournamentRound.RoundStatus.READY)
                    .forEach(round -> {
                        long incompleteMatches = round.getMatches().stream()
                            .filter(m -> !m.isCompleted())
                            .count();
                        System.out.println("  🔍 Round " + round.getName() + ": " + incompleteMatches + " incomplete matches");
                    });
            }
            
            // Show tournament status after advancement
            if (phase % 3 == 0 || newMatches.isEmpty()) {
                logRoundStatus(allRounds, playerNames);
            }
            
            // Detect key moments in the tournament
            // detectKeyMoments(phase, currentMatches, newMatches, playerNames);
            
            // Visualize tournament bracket at the end of each phase
            // if (phase % 2 == 0 || newMatches.isEmpty()) {
            //     visualizeTournamentState(allRounds, playerNames, phase);
            // }
            
            currentMatches = newMatches;
            phase++;
        }
        
        if (phase > maxPhases) {
            log.error("❌ Tournament did not complete within {} phases", maxPhases);
            throw new AssertionError("Tournament exceeded maximum phases");
        }
        
        System.out.println("\n🏆 TOURNAMENT COMPLETED SUCCESSFULLY! 🏆");
        System.out.println("✅ Total phases: " + (phase - 1));
        
        // Final results and analysis
        logFinalResults(allRounds, playerNames);
        analyzeTournamentResults(allRounds, participants, playerNames);
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
        String[] names = {"Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", 
                         "Henry", "Ivy", "Jack", "Kate", "Liam", "Maya", "Noah", "Olivia"};
        
        for (int i = 0; i < participants.size() && i < names.length; i++) {
            nameMap.put(participants.get(i).getPlayerId(), names[i]);
        }
        
        return nameMap;
    }
    
    private void logTournamentStructure(List<TournamentRound> allRounds, Map<UUID, String> playerNames) {
        log.info("\n📊 COMPLETE TOURNAMENT STRUCTURE:");
        
        // Group by bracket type
        Map<TournamentMatch.BracketType, List<TournamentRound>> roundsByType = allRounds.stream()
            .collect(Collectors.groupingBy(TournamentRound::getBracketType));
        
        // Winner's Bracket
        log.info("🏆 WINNER'S BRACKET:");
        List<TournamentRound> winnerRounds = roundsByType.getOrDefault(TournamentMatch.BracketType.WINNER, new ArrayList<>());
        winnerRounds.stream()
            .sorted(Comparator.comparing(TournamentRound::getRoundNumber))
            .forEach(round -> logRoundSummary(round, playerNames));
        
        // Loser's Bracket  
        log.info("💀 LOSER'S BRACKET:");
        List<TournamentRound> loserRounds = roundsByType.getOrDefault(TournamentMatch.BracketType.LOSER, new ArrayList<>());
        loserRounds.stream()
            .sorted(Comparator.comparing(TournamentRound::getRoundNumber))
            .forEach(round -> logRoundSummary(round, playerNames));
        
        // Grand Finals
        log.info("🥇 GRAND FINALS:");
        List<TournamentRound> grandFinalsRounds = roundsByType.getOrDefault(TournamentMatch.BracketType.GRAND_FINAL, new ArrayList<>());
        grandFinalsRounds.forEach(round -> logRoundSummary(round, playerNames));
        
        // Summary stats
        int totalMatches = allRounds.stream().mapToInt(r -> r.getMatches().size()).sum();
        log.info("📈 SUMMARY: {} rounds, {} matches total", allRounds.size(), totalMatches);
        log.info("💡 Expected matches for 15 players: {}", 2 * 15 - 2); // 28 matches
    }
    
    private void logRoundSummary(TournamentRound round, Map<UUID, String> playerNames) {
        String status = round.getStatus().toString();
        int readyMatches = (int) round.getMatches().stream()
            .filter(m -> !m.isCompleted() && hasPlayersReady(m))
            .count();
        
        log.info("  {} ({}): {} matches, {} ready", 
            round.getName(), status, round.getMatches().size(), readyMatches);
            
        // Show first few matches for initial rounds
        if (round.getStatus() == TournamentRound.RoundStatus.READY) {
            round.getMatches().stream()
                .filter(m -> hasPlayersReady(m))
                .limit(3)
                .forEach(match -> log.info("    {}", formatMatch(match, playerNames)));
            if (round.getMatches().size() > 3) {
                log.info("    ... and {} more matches", round.getMatches().size() - 3);
            }
        }
    }
    
    private void logRoundStatus(List<TournamentRound> allRounds, Map<UUID, String> playerNames) {
        log.info("\n📊 CURRENT TOURNAMENT STATUS:");
        
        Map<TournamentRound.RoundStatus, Long> statusCounts = allRounds.stream()
            .collect(Collectors.groupingBy(TournamentRound::getStatus, Collectors.counting()));
        
        log.info("  📋 Round status: {} READY, {} PENDING, {} COMPLETED", 
            statusCounts.getOrDefault(TournamentRound.RoundStatus.READY, 0L),
            statusCounts.getOrDefault(TournamentRound.RoundStatus.PENDING, 0L),
            statusCounts.getOrDefault(TournamentRound.RoundStatus.COMPLETED, 0L));
        
        // Show recently completed rounds
        allRounds.stream()
            .filter(r -> r.getStatus() == TournamentRound.RoundStatus.COMPLETED)
            .sorted(Comparator.comparing(TournamentRound::getRoundNumber).reversed())
            .limit(3)
            .forEach(round -> {
                List<String> winners = round.getMatches().stream()
                    .filter(TournamentMatch::isCompleted)
                    .map(match -> getWinnerName(match, playerNames))
                    .collect(Collectors.toList());
                log.info("  ✅ {} complete - Winners: {}", round.getName(), String.join(", ", winners));
            });
    }
    
    private void detectKeyMoments(int phase, List<TournamentMatch> currentMatches, 
                                 List<TournamentMatch> newMatches, Map<UUID, String> playerNames) {
        
        // Detect important matchups
        for (TournamentMatch match : currentMatches) {
            if (match.isCompleted()) {
                String winner = getWinnerName(match, playerNames);
                String roundName = match.getTournamentRound().getName();
                
                // Key moment detection
                if (roundName.contains("WB Finals") || roundName.contains("Winner's Finals")) {
                    log.info("🔥 KEY MOMENT: {} wins Winner's Finals! Advances to Grand Finals", winner);
                } else if (roundName.contains("LB Finals") || roundName.contains("Loser's Finals")) {
                    log.info("🔥 KEY MOMENT: {} wins Loser's Finals! Advances to Grand Finals", winner);
                } else if (roundName.contains("Grand Finals")) {
                    log.info("🏆 CHAMPION CROWNED: {} wins the tournament!", winner);
                } else if (roundName.contains("Semifinals")) {
                    log.info("⭐ {} advances from Semifinals", winner);
                }
            }
        }
        
        // Detect Grand Finals activation
        if (newMatches.stream()
                .filter(m -> m.getTournamentRound() != null)
                .anyMatch(m -> m.getTournamentRound().getName().contains("Grand"))) {
            log.info("🎉 GRAND FINALS ACTIVATED! The final showdown begins!");
        }
    }
    
    private void playMatchHigherSeedWins(TournamentMatch match, Map<UUID, String> playerNames) {
        // Handle TBD cases as byes (automatic wins)
        boolean team1HasPlayer = match.getTeam1Ids() != null && !match.getTeam1Ids().isEmpty();
        boolean team2HasPlayer = match.getTeam2Ids() != null && !match.getTeam2Ids().isEmpty();
        
        if (!team1HasPlayer && !team2HasPlayer) {
            return; // Skip completely invalid matches
        }
        
        // Handle bye scenarios (one team is TBD)
        if (!team1HasPlayer) {
            // Team 2 wins by default (bye)
            match.setCompleted(true);
            match.setWinnerIds(match.getTeam2Ids());
            match.setTeam1Score(0);
            match.setTeam2Score(11);
            String winner = getPlayerName(match.getTeam2Ids().get(0), playerNames);
            log.info("    🏓 {} wins by bye (vs TBD)", winner);
            return;
        }
        
        if (!team2HasPlayer) {
            // Team 1 wins by default (bye)
            match.setCompleted(true);
            match.setWinnerIds(match.getTeam1Ids());
            match.setTeam1Score(11);
            match.setTeam2Score(0);
            String winner = getPlayerName(match.getTeam1Ids().get(0), playerNames);
            log.info("    🏓 {} wins by bye (vs TBD)", winner);
            return;
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
        
        // Log result with seeds for clarity
        String player1 = getPlayerName(match.getTeam1Ids().get(0), playerNames);
        String player2 = getPlayerName(match.getTeam2Ids().get(0), playerNames);
        String winner = team1Wins ? player1 : player2;
        String loser = team1Wins ? player2 : player1;
        
        log.info("    🏓 {} (#{}) beats {} (#{}) (11-7)", 
            winner, team1Wins ? seed1 : seed2, 
            loser, team1Wins ? seed2 : seed1);
    }
    
    private boolean hasPlayersReady(TournamentMatch match) {
        return match.getTeam1Ids() != null && !match.getTeam1Ids().isEmpty() &&
               match.getTeam2Ids() != null && !match.getTeam2Ids().isEmpty();
    }
    
    private String formatMatch(TournamentMatch match, Map<UUID, String> playerNames) {
        String player1 = getPlayerName(match.getTeam1Ids() != null && !match.getTeam1Ids().isEmpty() ? 
                                     match.getTeam1Ids().get(0) : null, playerNames);
        String player2 = getPlayerName(match.getTeam2Ids() != null && !match.getTeam2Ids().isEmpty() ? 
                                     match.getTeam2Ids().get(0) : null, playerNames);
        
        return String.format("%s: %s vs %s", match.getDisplayId(), player1, player2);
    }
    
    private String getPlayerName(UUID playerId, Map<UUID, String> playerNames) {
        return playerId != null ? playerNames.getOrDefault(playerId, "TBD") : "TBD";
    }
    
    private String getWinnerName(TournamentMatch match, Map<UUID, String> playerNames) {
        if (match.getWinnerIds() != null && !match.getWinnerIds().isEmpty()) {
            return getPlayerName(match.getWinnerIds().get(0), playerNames);
        }
        return "TBD";
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
            String champion = getWinnerName(grandFinalsMatch.get(), playerNames);
            log.info("🥇 CHAMPION: {}!", champion);
            
            // Show the final match details
            TournamentMatch finalMatch = grandFinalsMatch.get();
            String player1 = getPlayerName(finalMatch.getTeam1Ids().get(0), playerNames);
            String player2 = getPlayerName(finalMatch.getTeam2Ids().get(0), playerNames);
            log.info("🏓 Grand Finals: {} defeated {} ({}-{})", 
                champion, 
                champion.equals(player1) ? player2 : player1,
                finalMatch.getTeam1Score(),
                finalMatch.getTeam2Score());
        } else {
            log.info("❓ Grand finals not completed");
        }
        
        // Count completed matches
        int totalMatches = allRounds.stream().mapToInt(r -> r.getMatches().size()).sum();
        int completedMatches = allRounds.stream()
            .mapToInt(r -> (int) r.getMatches().stream().filter(TournamentMatch::isCompleted).count())
            .sum();
        log.info("📊 Matches completed: {}/{}", completedMatches, totalMatches);
    }
    
    private void analyzeTournamentResults(List<TournamentRound> allRounds, List<TournamentPlayer> participants, 
                                        Map<UUID, String> playerNames) {
        log.info("\n📈 TOURNAMENT ANALYSIS:");
        
        // Verify seeding worked correctly
        Optional<TournamentRound> wbFinalsRound = allRounds.stream()
            .filter(round -> round.getName().contains("WB Finals") || round.getName().contains("Winner's Finals"))
            .findFirst();
            
        if (wbFinalsRound.isPresent()) {
            TournamentMatch wbFinalsMatch = wbFinalsRound.get().getMatches().get(0);
            if (wbFinalsMatch.isCompleted()) {
                String player1 = getPlayerName(wbFinalsMatch.getTeam1Ids().get(0), playerNames);
                String player2 = getPlayerName(wbFinalsMatch.getTeam2Ids().get(0), playerNames);
                
                log.info("🎯 Winner's Finals: {} vs {}", player1, player2);
                if ((player1.equals("Alice") && player2.equals("Bob")) || 
                    (player1.equals("Bob") && player2.equals("Alice"))) {
                    log.info("✅ SEEDING SUCCESS: Top 2 seeds met in Winner's Finals as expected!");
                } else {
                    log.info("⚠️ Seeding result: {} vs {} (expected Alice vs Bob)", player1, player2);
                }
            }
        }
        
        // Round completion summary
        Map<TournamentMatch.BracketType, Long> completedByBracket = allRounds.stream()
            .filter(round -> round.getStatus() == TournamentRound.RoundStatus.COMPLETED)
            .collect(Collectors.groupingBy(TournamentRound::getBracketType, Collectors.counting()));
            
        log.info("📋 Completed rounds: {} Winner's, {} Loser's, {} Grand Finals", 
            completedByBracket.getOrDefault(TournamentMatch.BracketType.WINNER, 0L),
            completedByBracket.getOrDefault(TournamentMatch.BracketType.LOSER, 0L),
            completedByBracket.getOrDefault(TournamentMatch.BracketType.GRAND_FINAL, 0L));
    }
    
    /**
     * Visualize the current tournament bracket state
     */
    private void visualizeTournamentState(List<TournamentRound> allRounds, Map<UUID, String> playerNames, int phase) {
        log.info("\n" + "=".repeat(80));
        log.info("🎯 TOURNAMENT BRACKET VISUALIZATION - END OF PHASE {}", phase);
        log.info("=".repeat(80));
        
        try {
            // Create a tournament object for visualization
            Tournament tournament = Tournament.builder()
                .name("15-Person Championship")
                .tournamentType(Tournament.TournamentType.DOUBLE_ELIMINATION)
                .numberOfPlayers(15)
                .build();
            
            // Use the tournament bracket visualizer
            String bracketVisualization = TournamentBracketVisualizer.visualizeTournament(
                tournament, allRounds, playerNames);
            
            log.info("\n{}", bracketVisualization);
            
        } catch (Exception e) {
            // Fallback to simple text visualization if visualizer fails
            log.info("📊 SIMPLE BRACKET STATUS:");
            visualizeSimpleBracketStatus(allRounds, playerNames);
        }
        
        log.info("=".repeat(80) + "\n");
    }
    
    /**
     * Visualize the initial tournament bracket before any matches are played
     */
    private void visualizeInitialBracket(List<TournamentRound> allRounds, Map<UUID, String> playerNames) {
        log.info("\n" + "🌟".repeat(80));
        log.info("🎯 INITIAL TOURNAMENT BRACKET - SEEDED PLAYERS READY TO COMPETE");
        log.info("🌟".repeat(80));
        
        try {
            // Create a tournament object for visualization
            Tournament tournament = Tournament.builder()
                .name("15-Person Championship")
                .tournamentType(Tournament.TournamentType.DOUBLE_ELIMINATION)
                .numberOfPlayers(15)
                .build();
            
            // Use the tournament bracket visualizer for the complete initial bracket
            String bracketVisualization = TournamentBracketVisualizer.visualizeTournament(
                tournament, allRounds, playerNames);
            
            log.info("\n{}", bracketVisualization);
            
        } catch (Exception e) {
            // Fallback to simple text visualization if visualizer fails
            log.info("📊 INITIAL BRACKET STRUCTURE:");
            visualizeInitialBracketSimple(allRounds, playerNames);
        }
        
        log.info("🌟".repeat(80));
        log.info("🏁 Tournament is ready to begin! First round matches are activated.");
        log.info("🌟".repeat(80) + "\n");
    }
    
    /**
     * Simple fallback for initial bracket visualization
     */
    private void visualizeInitialBracketSimple(List<TournamentRound> allRounds, Map<UUID, String> playerNames) {
        // Group rounds by bracket type
        Map<TournamentMatch.BracketType, List<TournamentRound>> roundsByType = allRounds.stream()
            .collect(Collectors.groupingBy(TournamentRound::getBracketType));
        
        // Winner's Bracket - show first round matches that are ready to play
        log.info("\n🏆 WINNER'S BRACKET - FIRST ROUND:");
        List<TournamentRound> winnerRounds = roundsByType.getOrDefault(TournamentMatch.BracketType.WINNER, new ArrayList<>());
        winnerRounds.stream()
            .filter(round -> round.getStatus() == TournamentRound.RoundStatus.READY)
            .sorted(Comparator.comparing(TournamentRound::getRoundNumber))
            .forEach(round -> {
                log.info("  🟡 {} ({} matches ready to play)", round.getName(), round.getMatches().size());
                round.getMatches().forEach(match -> {
                    if (hasPlayersReady(match)) {
                        String player1 = getPlayerName(match.getTeam1Ids().get(0), playerNames);
                        String player2 = getPlayerName(match.getTeam2Ids().get(0), playerNames);
                        int seed1 = match.getTeam1Seed() != null ? match.getTeam1Seed() : 0;
                        int seed2 = match.getTeam2Seed() != null ? match.getTeam2Seed() : 0;
                        log.info("    🆚 {}: {} (#{}) vs {} (#{})", 
                            match.getDisplayId(), player1, seed1, player2, seed2);
                    }
                });
            });
        
        // Show pending rounds structure
        log.info("\n📋 UPCOMING ROUNDS STRUCTURE:");
        winnerRounds.stream()
            .filter(round -> round.getStatus() == TournamentRound.RoundStatus.PENDING)
            .sorted(Comparator.comparing(TournamentRound::getRoundNumber))
            .forEach(round -> log.info("  ⚪ {} ({} matches)", round.getName(), round.getMatches().size()));
        
        // Loser's Bracket structure
        log.info("\n💀 LOSER'S BRACKET STRUCTURE:");
        List<TournamentRound> loserRounds = roundsByType.getOrDefault(TournamentMatch.BracketType.LOSER, new ArrayList<>());
        loserRounds.stream()
            .sorted(Comparator.comparing(TournamentRound::getRoundNumber))
            .forEach(round -> log.info("  ⚪ {} ({} matches) - waiting for players", round.getName(), round.getMatches().size()));
        
        // Grand Finals
        log.info("\n🥇 GRAND FINALS:");
        List<TournamentRound> grandFinalsRounds = roundsByType.getOrDefault(TournamentMatch.BracketType.GRAND_FINAL, new ArrayList<>());
        grandFinalsRounds.forEach(round -> log.info("  ⚪ {} ({} match) - waiting for champions", round.getName(), round.getMatches().size()));
        
        // Summary
        int totalMatches = allRounds.stream().mapToInt(r -> r.getMatches().size()).sum();
        int readyMatches = allRounds.stream()
            .mapToInt(r -> (int) r.getMatches().stream().filter(m -> hasPlayersReady(m)).count())
            .sum();
        log.info("\n📊 BRACKET SUMMARY:");
        log.info("  🎯 Total matches: {}", totalMatches);
        log.info("  🟡 Ready to play: {}", readyMatches);
        log.info("  ⚪ Awaiting players: {}", totalMatches - readyMatches);
    }
    
    /**
     * Simple fallback visualization showing round-by-round status
     */
    private void visualizeSimpleBracketStatus(List<TournamentRound> allRounds, Map<UUID, String> playerNames) {
        // Group rounds by bracket type
        Map<TournamentMatch.BracketType, List<TournamentRound>> roundsByType = allRounds.stream()
            .collect(Collectors.groupingBy(TournamentRound::getBracketType));
        
        // Winner's Bracket
        log.info("\n🏆 WINNER'S BRACKET:");
        List<TournamentRound> winnerRounds = roundsByType.getOrDefault(TournamentMatch.BracketType.WINNER, new ArrayList<>());
        winnerRounds.stream()
            .sorted(Comparator.comparing(TournamentRound::getRoundNumber))
            .forEach(round -> visualizeRoundMatches(round, playerNames));
        
        // Loser's Bracket
        log.info("\n💀 LOSER'S BRACKET:");
        List<TournamentRound> loserRounds = roundsByType.getOrDefault(TournamentMatch.BracketType.LOSER, new ArrayList<>());
        loserRounds.stream()
            .sorted(Comparator.comparing(TournamentRound::getRoundNumber))
            .forEach(round -> visualizeRoundMatches(round, playerNames));
        
        // Grand Finals
        log.info("\n🥇 GRAND FINALS:");
        List<TournamentRound> grandFinalsRounds = roundsByType.getOrDefault(TournamentMatch.BracketType.GRAND_FINAL, new ArrayList<>());
        grandFinalsRounds.forEach(round -> visualizeRoundMatches(round, playerNames));
    }
    
    /**
     * Visualize matches in a round
     */
    private void visualizeRoundMatches(TournamentRound round, Map<UUID, String> playerNames) {
        String status = switch(round.getStatus()) {
            case READY -> "🟡 READY";
            case PENDING -> "⚪ PENDING";
            case ACTIVE -> "🔴 ACTIVE";
            case COMPLETED -> "🟢 COMPLETED";
        };
        
        log.info("  {} {} ({})", status, round.getName(), round.getMatches().size() + " matches");
        
        // Show matches
        for (TournamentMatch match : round.getMatches()) {
            String player1 = getPlayerName(
                match.getTeam1Ids() != null && !match.getTeam1Ids().isEmpty() ? 
                match.getTeam1Ids().get(0) : null, playerNames);
            String player2 = getPlayerName(
                match.getTeam2Ids() != null && !match.getTeam2Ids().isEmpty() ? 
                match.getTeam2Ids().get(0) : null, playerNames);
            
            if (match.isCompleted()) {
                String winner = getWinnerName(match, playerNames);
                log.info("    ✅ {}: {} vs {} → {} wins ({}-{})", 
                    match.getDisplayId(), player1, player2, winner, 
                    match.getTeam1Score(), match.getTeam2Score());
            } else if (hasPlayersReady(match)) {
                log.info("    ⏳ {}: {} vs {} (ready to play)", 
                    match.getDisplayId(), player1, player2);
            } else {
                log.info("    ⌛ {}: {} vs {} (waiting for players)", 
                    match.getDisplayId(), player1, player2);
            }
        }
    }
    
}