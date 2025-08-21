package com.example.javapingpongelo.tournament.utils;

import com.example.javapingpongelo.models.Tournament;
import com.example.javapingpongelo.models.TournamentMatch;
import com.example.javapingpongelo.models.TournamentPlayer;
import com.example.javapingpongelo.models.TournamentRound;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tournament logic visualizer - focuses on testing framework logic rather than ASCII art
 */
@Slf4j
public class TournamentBracketVisualizer {
    
    /**
     * Visualize tournament structure as structured text
     */
    public static String visualizeTournament(Tournament tournament, List<TournamentRound> rounds, 
                                           Map<UUID, String> playerNames) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("════════════════════════════════════════════════════════════════════════\n");
        sb.append(String.format("Tournament: %s (%d Players, %s)\n", 
            tournament.getName(), 
            tournament.getNumberOfPlayers(), 
            tournament.getTournamentType()));
        sb.append("════════════════════════════════════════════════════════════════════════\n\n");
        
        if (tournament.getTournamentType() == Tournament.TournamentType.SINGLE_ELIMINATION) {
            return sb.toString() + visualizeSingleElimination(rounds, playerNames);
        } else if (tournament.getTournamentType() == Tournament.TournamentType.DOUBLE_ELIMINATION) {
            return sb.toString() + visualizeDoubleElimination(rounds, playerNames);
        } else {
            return sb.toString() + "Visualization not yet supported for " + tournament.getTournamentType();
        }
    }
    
    /**
     * Visualize single elimination tournament logic
     */
    public static String visualizeSingleElimination(List<TournamentRound> rounds, Map<UUID, String> playerNames) {
        if (rounds.isEmpty()) {
            return "No rounds found in tournament.\n";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Sort rounds by round number
        List<TournamentRound> sortedRounds = rounds.stream()
            .sorted(Comparator.comparing(TournamentRound::getRoundNumber))
            .collect(Collectors.toList());
        
        // Show tournament progression round by round
        for (TournamentRound round : sortedRounds) {
            sb.append(visualizeRound(round, playerNames));
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Visualize double elimination tournament logic
     */
    public static String visualizeDoubleElimination(List<TournamentRound> rounds, Map<UUID, String> playerNames) {
        if (rounds.isEmpty()) {
            return "No rounds found in tournament.\n";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Sort rounds by round number
        List<TournamentRound> sortedRounds = rounds.stream()
            .sorted(Comparator.comparing(TournamentRound::getRoundNumber))
            .collect(Collectors.toList());
        
        // Separate winner's bracket, loser's bracket, and grand finals
        List<TournamentRound> winnerRounds = new ArrayList<>();
        List<TournamentRound> loserRounds = new ArrayList<>();
        List<TournamentRound> grandFinals = new ArrayList<>();
        
        for (TournamentRound round : sortedRounds) {
            String name = round.getName().toLowerCase();
            if (name.contains("grand finals")) {
                grandFinals.add(round);
            } else if (name.contains("lb ") || name.contains("loser")) {
                loserRounds.add(round);
            } else if (name.contains("wb ") || name.contains("winner") || 
                       name.contains("quarterfinals") || name.contains("semifinals")) {
                winnerRounds.add(round);
            }
        }
        
        // Display Winner's Bracket
        if (!winnerRounds.isEmpty()) {
            sb.append("┌─ WINNER'S BRACKET ─────────────────────────────────────────────────────┐\n");
            for (TournamentRound round : winnerRounds) {
                sb.append("│ ").append(visualizeRound(round, playerNames).replaceAll("\n", "\n│ "));
                if (!sb.toString().endsWith("│ ")) {
                    sb.append("│");
                }
                sb.append("\n");
            }
            sb.append("└────────────────────────────────────────────────────────────────────────┘\n\n");
        }
        
        // Display Loser's Bracket
        if (!loserRounds.isEmpty()) {
            sb.append("┌─ LOSER'S BRACKET ──────────────────────────────────────────────────────┐\n");
            for (TournamentRound round : loserRounds) {
                sb.append("│ ").append(visualizeRound(round, playerNames).replaceAll("\n", "\n│ "));
                if (!sb.toString().endsWith("│ ")) {
                    sb.append("│");
                }
                sb.append("\n");
            }
            sb.append("└────────────────────────────────────────────────────────────────────────┘\n\n");
        }
        
        // Display Grand Finals
        if (!grandFinals.isEmpty()) {
            sb.append("┌─ GRAND FINALS ─────────────────────────────────────────────────────────┐\n");
            for (TournamentRound round : grandFinals) {
                sb.append("│ ").append(visualizeRound(round, playerNames).replaceAll("\n", "\n│ "));
                if (!sb.toString().endsWith("│ ")) {
                    sb.append("│");
                }
                sb.append("\n");
            }
            sb.append("└────────────────────────────────────────────────────────────────────────┘\n\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Visualize a single round
     */
    private static String visualizeRound(TournamentRound round, Map<UUID, String> playerNames) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(String.format("%s [%s]:\n", 
            round.getName(), 
            round.getStatus()));
        
        List<TournamentMatch> matches = round.getMatches().stream()
            .sorted(Comparator.comparing(TournamentMatch::getPositionInRound))
            .collect(Collectors.toList());
        
        for (TournamentMatch match : matches) {
            sb.append("  ").append(visualizeMatch(match, playerNames)).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Visualize a single match
     */
    private static String visualizeMatch(TournamentMatch match, Map<UUID, String> playerNames) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(match.getDisplayId()).append(": ");
        
        if (match.isBye()) {
            String player = formatPlayer(match.getTeam1Ids(), playerNames, match.getTeam1Seed());
            sb.append(player).append(" (BYE)");
        } else {
            String team1 = formatPlayer(match.getTeam1Ids(), playerNames, match.getTeam1Seed());
            String team2 = formatPlayer(match.getTeam2Ids(), playerNames, match.getTeam2Seed());
            
            sb.append(team1).append(" vs ").append(team2);
            
            if (match.isCompleted()) {
                String winner = formatWinner(match, playerNames);
                sb.append(" → ").append(winner);
                
                if (match.getTeam1Score() != null && match.getTeam2Score() != null) {
                    sb.append(String.format(" (%d-%d)", match.getTeam1Score(), match.getTeam2Score()));
                }
            } else if (match.hasAllTeams()) {
                sb.append(" (READY)");
            } else {
                sb.append(" (WAITING)");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Format player name with seed
     */
    private static String formatPlayer(List<UUID> playerIds, Map<UUID, String> playerNames, Integer seed) {
        if (playerIds == null || playerIds.isEmpty()) {
            return "TBD";
        }
        
        String name = playerIds.stream()
            .map(id -> playerNames.getOrDefault(id, "Unknown"))
            .collect(Collectors.joining(", "));
        
        if (seed != null) {
            return String.format("[%d]%s", seed, name);
        }
        
        return name;
    }
    
    /**
     * Format winner name
     */
    private static String formatWinner(TournamentMatch match, Map<UUID, String> playerNames) {
        if (!match.isCompleted() || match.getWinnerIds() == null || match.getWinnerIds().isEmpty()) {
            return "TBD";
        }
        
        return match.getWinnerIds().stream()
            .map(id -> playerNames.getOrDefault(id, "Unknown"))
            .collect(Collectors.joining(", "));
    }
    
    /**
     * Generate sample player names for testing
     */
    public static Map<UUID, String> generateSamplePlayerNames(List<UUID> playerIds) {
        String[] names = {
            "Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Henry",
            "Ivy", "Jack", "Kate", "Leo", "Mia", "Noah", "Olivia", "Paul"
        };
        
        Map<UUID, String> playerNames = new HashMap<>();
        for (int i = 0; i < playerIds.size() && i < names.length; i++) {
            playerNames.put(playerIds.get(i), names[i]);
        }
        
        return playerNames;
    }
}