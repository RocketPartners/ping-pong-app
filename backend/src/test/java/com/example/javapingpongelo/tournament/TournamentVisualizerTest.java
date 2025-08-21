package com.example.javapingpongelo.tournament;

import com.example.javapingpongelo.models.Tournament;
import com.example.javapingpongelo.models.TournamentMatch;
import com.example.javapingpongelo.models.TournamentPlayer;
import com.example.javapingpongelo.models.TournamentRound;
import com.example.javapingpongelo.tournament.engine.impl.SingleEliminationRules;
import com.example.javapingpongelo.tournament.utils.TournamentBracketVisualizer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

/**
 * 📊 UTILITY: Tests the tournament bracket visualization utilities
 * 
 * This test demonstrates:
 * - ASCII bracket visualization for different tournament sizes
 * - Single elimination and double elimination layouts
 * - Bracket formatting and display utilities
 * 
 * Status: Useful for debugging bracket structures - should be maintained
 * Run these tests to see ASCII representations of tournament brackets
 */
@SpringBootTest
public class TournamentVisualizerTest {
    
    @Test
    public void testSingleEliminationBracket4Players() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SINGLE ELIMINATION - 4 PLAYERS");
        System.out.println("=".repeat(80));
        
        Tournament tournament = createSampleTournament("4-Player Single Elim", 4, 
            Tournament.TournamentType.SINGLE_ELIMINATION);
        
        List<TournamentPlayer> players = createSamplePlayers(4);
        List<TournamentRound> rounds = createSample4PlayerSingleElimRounds(tournament, players);
        
        Map<UUID, String> playerNames = TournamentBracketVisualizer.generateSamplePlayerNames(
            players.stream().map(TournamentPlayer::getPlayerId).toList());
        
        String visualization = TournamentBracketVisualizer.visualizeTournament(tournament, rounds, playerNames);
        System.out.println(visualization);
    }
    
    @Test
    public void testSingleEliminationBracket8Players() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SINGLE ELIMINATION - 8 PLAYERS");
        System.out.println("=".repeat(80));
        
        Tournament tournament = createSampleTournament("8-Player Single Elim", 8, 
            Tournament.TournamentType.SINGLE_ELIMINATION);
        
        List<TournamentPlayer> players = createSamplePlayers(8);
        List<TournamentRound> rounds = createSample8PlayerSingleElimRounds(tournament, players);
        
        Map<UUID, String> playerNames = TournamentBracketVisualizer.generateSamplePlayerNames(
            players.stream().map(TournamentPlayer::getPlayerId).toList());
        
        String visualization = TournamentBracketVisualizer.visualizeTournament(tournament, rounds, playerNames);
        System.out.println(visualization);
    }
    
    @Test
    public void testSingleEliminationBracket5Players() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SINGLE ELIMINATION - 5 PLAYERS (with byes)");
        System.out.println("=".repeat(80));
        
        Tournament tournament = createSampleTournament("5-Player Single Elim", 5, 
            Tournament.TournamentType.SINGLE_ELIMINATION);
        
        List<TournamentPlayer> players = createSamplePlayers(5);
        List<TournamentRound> rounds = createSample5PlayerSingleElimRounds(tournament, players);
        
        Map<UUID, String> playerNames = TournamentBracketVisualizer.generateSamplePlayerNames(
            players.stream().map(TournamentPlayer::getPlayerId).toList());
        
        String visualization = TournamentBracketVisualizer.visualizeTournament(tournament, rounds, playerNames);
        System.out.println(visualization);
    }
    
    
    // Helper methods to create sample data
    
    private Tournament createSampleTournament(String name, int playerCount, Tournament.TournamentType type) {
        return Tournament.builder()
            .id(UUID.randomUUID())
            .name(name)
            .tournamentType(type)
            .gameType(Tournament.GameType.SINGLES)
            .seedingMethod(Tournament.SeedingMethod.RATING_BASED)
            .numberOfPlayers(playerCount)
            .status(Tournament.TournamentStatus.IN_PROGRESS)
            .organizerId(UUID.randomUUID())
            .startDate(new Date())
            .currentRound(1)
            .enableReseeding(true)
            .build();
    }
    
    private List<TournamentPlayer> createSamplePlayers(int count) {
        List<TournamentPlayer> players = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            players.add(TournamentPlayer.builder()
                .playerId(UUID.randomUUID())
                .seed(i)
                .eliminated(false)
                .build());
        }
        return players;
    }
    
    private List<TournamentRound> createSample4PlayerSingleElimRounds(Tournament tournament, List<TournamentPlayer> players) {
        List<TournamentRound> rounds = new ArrayList<>();
        
        // Round 1 (Semifinals)
        TournamentRound round1 = TournamentRound.builder()
            .roundId(UUID.randomUUID())
            .tournament(tournament)
            .roundNumber(1)
            .bracketType(TournamentMatch.BracketType.WINNER)
            .name("Semifinals")
            .status(TournamentRound.RoundStatus.COMPLETED)
            .matches(new ArrayList<>())
            .build();
        
        // Match 1: Player 1 vs Player 4
        TournamentMatch match1 = createMatch(round1, "R1-M1", 1, players.get(0), players.get(3), true, players.get(0));
        // Match 2: Player 2 vs Player 3  
        TournamentMatch match2 = createMatch(round1, "R1-M2", 2, players.get(1), players.get(2), true, players.get(1));
        
        round1.getMatches().add(match1);
        round1.getMatches().add(match2);
        rounds.add(round1);
        
        // Round 2 (Finals)
        TournamentRound round2 = TournamentRound.builder()
            .roundId(UUID.randomUUID())
            .tournament(tournament)
            .roundNumber(2)
            .bracketType(TournamentMatch.BracketType.FINAL)
            .name("Finals")
            .status(TournamentRound.RoundStatus.ACTIVE)
            .matches(new ArrayList<>())
            .build();
        
        // Finals: Winner of Match 1 vs Winner of Match 2
        TournamentMatch finals = createMatch(round2, "R2-M1", 1, players.get(0), players.get(1), false, null);
        round2.getMatches().add(finals);
        rounds.add(round2);
        
        return rounds;
    }
    
    private List<TournamentRound> createSample8PlayerSingleElimRounds(Tournament tournament, List<TournamentPlayer> players) {
        List<TournamentRound> rounds = new ArrayList<>();
        
        // Round 1 (Quarterfinals) - 4 matches
        TournamentRound round1 = TournamentRound.builder()
            .roundId(UUID.randomUUID())
            .tournament(tournament)
            .roundNumber(1)
            .bracketType(TournamentMatch.BracketType.WINNER)
            .name("Quarterfinals")
            .status(TournamentRound.RoundStatus.COMPLETED)
            .matches(new ArrayList<>())
            .build();
        
        round1.getMatches().add(createMatch(round1, "R1-M1", 1, players.get(0), players.get(7), true, players.get(0)));
        round1.getMatches().add(createMatch(round1, "R1-M2", 2, players.get(1), players.get(6), true, players.get(1)));
        round1.getMatches().add(createMatch(round1, "R1-M3", 3, players.get(2), players.get(5), true, players.get(2)));
        round1.getMatches().add(createMatch(round1, "R1-M4", 4, players.get(3), players.get(4), true, players.get(3)));
        rounds.add(round1);
        
        // Round 2 (Semifinals) - 2 matches
        TournamentRound round2 = TournamentRound.builder()
            .roundId(UUID.randomUUID())
            .tournament(tournament)
            .roundNumber(2)
            .bracketType(TournamentMatch.BracketType.WINNER)
            .name("Semifinals")
            .status(TournamentRound.RoundStatus.ACTIVE)
            .matches(new ArrayList<>())
            .build();
        
        round2.getMatches().add(createMatch(round2, "R2-M1", 1, players.get(0), players.get(1), false, null));
        round2.getMatches().add(createMatch(round2, "R2-M2", 2, players.get(2), players.get(3), false, null));
        rounds.add(round2);
        
        // Round 3 (Finals) - 1 match
        TournamentRound round3 = TournamentRound.builder()
            .roundId(UUID.randomUUID())
            .tournament(tournament)
            .roundNumber(3)
            .bracketType(TournamentMatch.BracketType.FINAL)
            .name("Finals")
            .status(TournamentRound.RoundStatus.READY)
            .matches(new ArrayList<>())
            .build();
        
        round3.getMatches().add(createMatch(round3, "R3-M1", 1, null, null, false, null));
        rounds.add(round3);
        
        return rounds;
    }
    
    private List<TournamentRound> createSample5PlayerSingleElimRounds(Tournament tournament, List<TournamentPlayer> players) {
        List<TournamentRound> rounds = new ArrayList<>();
        
        // Round 1 (with byes) - Top 3 seeds get byes, bottom 2 play
        TournamentRound round1 = TournamentRound.builder()
            .roundId(UUID.randomUUID())
            .tournament(tournament)
            .roundNumber(1)
            .bracketType(TournamentMatch.BracketType.WINNER)
            .name("Round 1")
            .status(TournamentRound.RoundStatus.COMPLETED)
            .matches(new ArrayList<>())
            .build();
        
        // Byes for top 3 seeds
        round1.getMatches().add(createByeMatch(round1, "R1-M1", 1, players.get(0)));
        round1.getMatches().add(createByeMatch(round1, "R1-M2", 2, players.get(1)));
        round1.getMatches().add(createByeMatch(round1, "R1-M3", 3, players.get(2)));
        // Regular match for bottom 2 seeds
        round1.getMatches().add(createMatch(round1, "R1-M4", 4, players.get(3), players.get(4), true, players.get(3)));
        rounds.add(round1);
        
        // Round 2 (Semifinals) - 2 matches
        TournamentRound round2 = TournamentRound.builder()
            .roundId(UUID.randomUUID())
            .tournament(tournament)
            .roundNumber(2)
            .bracketType(TournamentMatch.BracketType.WINNER)
            .name("Semifinals")
            .status(TournamentRound.RoundStatus.ACTIVE)
            .matches(new ArrayList<>())
            .build();
        
        round2.getMatches().add(createMatch(round2, "R2-M1", 1, players.get(0), players.get(3), false, null));
        round2.getMatches().add(createMatch(round2, "R2-M2", 2, players.get(1), players.get(2), false, null));
        rounds.add(round2);
        
        // Round 3 (Finals)
        TournamentRound round3 = TournamentRound.builder()
            .roundId(UUID.randomUUID())
            .tournament(tournament)
            .roundNumber(3)
            .bracketType(TournamentMatch.BracketType.FINAL)
            .name("Finals")
            .status(TournamentRound.RoundStatus.READY)
            .matches(new ArrayList<>())
            .build();
        
        round3.getMatches().add(createMatch(round3, "R3-M1", 1, null, null, false, null));
        rounds.add(round3);
        
        return rounds;
    }
    
    
    private TournamentMatch createMatch(TournamentRound round, String displayId, int position, 
                                      TournamentPlayer team1, TournamentPlayer team2, 
                                      boolean completed, TournamentPlayer winner) {
        TournamentMatch match = TournamentMatch.builder()
            .matchId(UUID.randomUUID())
            .displayId(displayId)
            .tournament(round.getTournament())
            .round(round.getRoundNumber())
            .positionInRound(position)
            .bracketType(round.getBracketType())
            .completed(completed)
            .build();
        
        if (team1 != null) {
            match.setTeam1Ids(Arrays.asList(team1.getPlayerId()));
            match.setTeam1Seed(team1.getSeed());
        }
        
        if (team2 != null) {
            match.setTeam2Ids(Arrays.asList(team2.getPlayerId()));
            match.setTeam2Seed(team2.getSeed());
        }
        
        if (completed && winner != null) {
            match.setWinnerIds(Arrays.asList(winner.getPlayerId()));
            match.setTeam1Score(winner == team1 ? 21 : 18);
            match.setTeam2Score(winner == team2 ? 21 : 18);
            
            // Set loser IDs
            TournamentPlayer loser = winner == team1 ? team2 : team1;
            if (loser != null) {
                match.setLoserIds(Arrays.asList(loser.getPlayerId()));
            }
        }
        
        return match;
    }
    
    private TournamentMatch createByeMatch(TournamentRound round, String displayId, int position, 
                                         TournamentPlayer player) {
        return TournamentMatch.builder()
            .matchId(UUID.randomUUID())
            .displayId(displayId)
            .tournament(round.getTournament())
            .round(round.getRoundNumber())
            .positionInRound(position)
            .bracketType(round.getBracketType())
            .team1Ids(Arrays.asList(player.getPlayerId()))
            .team1Seed(player.getSeed())
            .winnerIds(Arrays.asList(player.getPlayerId()))
            .isBye(true)
            .byeTeam(1)
            .completed(true)
            .build();
    }
}