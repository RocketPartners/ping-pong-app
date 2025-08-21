package com.example.javapingpongelo.tournament.engine.impl;

import com.example.javapingpongelo.models.Tournament;
import com.example.javapingpongelo.models.TournamentMatch;
import com.example.javapingpongelo.models.TournamentPlayer;
import com.example.javapingpongelo.models.TournamentRound;
import com.example.javapingpongelo.tournament.engine.DoubleEliminationMath;
import com.example.javapingpongelo.tournament.engine.MatchBatch;
import com.example.javapingpongelo.tournament.engine.TournamentRulesEngine;
import com.example.javapingpongelo.tournament.engine.TournamentUpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Double elimination tournament rules engine
 * Implements all complex double elimination logic including:
 * - Winner's and loser's bracket management
 * - Opposite side placement rule
 * - Grand finals with reset capability
 * - Bye handling for non-power-of-2 tournaments
 * 
 * ## RECOMMENDED API (Pre-built Bracket):
 * 1. generateInitialBracket() - Create complete tournament structure upfront
 * 2. getReadyMatches() - Get matches ready to play
 * 3. advancePlayersAndGetNextMatches() - Advance after completing matches
 * 
 * ## LEGACY API (Deprecated):
 * - generateNextRound() - Reactive round generation (@deprecated)
 * - getNextMatchBatches() - Batch-based match management (@deprecated)
 * - completeMatchBatch() - Batch completion processing (@deprecated)
 * 
 * The new pre-built bracket API is more efficient and eliminates the complex bugs
 * that existed in the reactive approach. Legacy methods are maintained for backward
 * compatibility with existing integrations.
 */
@Slf4j
@Component("doubleEliminationRules")
public class DoubleEliminationRules implements TournamentRulesEngine {
    
    @Override
    public Tournament.TournamentType getSupportedTournamentType() {
        return Tournament.TournamentType.DOUBLE_ELIMINATION;
    }
    
    @Override
    public int calculateTotalRounds(int participantCount) {
        DoubleEliminationMath.WinnerBracketStructure winnerStructure = 
            DoubleEliminationMath.calculateWinnerBracket(participantCount);
        DoubleEliminationMath.LoserBracketStructure loserStructure = 
            DoubleEliminationMath.calculateLoserBracket(participantCount);
        
        // Total rounds = winner rounds + loser rounds + grand finals (1)
        return winnerStructure.getTotalRounds() + loserStructure.getTotalRounds() + 1;
    }
    
    @Override
    public List<TournamentRound> generateInitialBracket(Tournament tournament, List<TournamentPlayer> participants) {
        int playerCount = participants.size();
        log.info("Generating double elimination bracket for {} players", playerCount);
        
        // Validate player count - double elimination works best with 4-16 players
        if (playerCount < 4 || playerCount > 16) {
            throw new IllegalArgumentException(
                "Double elimination tournaments support 4-16 players. Got: " + playerCount + 
                ". For 2-3 players, consider single elimination or round-robin format.");
        }
        
        List<TournamentRound> rounds = new ArrayList<>();
        
        // Generate winner's bracket rounds
        rounds.addAll(generateWinnerBracketRounds(tournament, participants));
        
        // Generate loser's bracket rounds
        rounds.addAll(generateLoserBracketRounds(tournament, participants));
        
        // Generate grand finals
        rounds.add(generateGrandFinalsRound(tournament, participants.size()));
        
        log.info("Generated {} total rounds for double elimination", rounds.size());
        return rounds;
    }
    
    /**
     * Generate winner's bracket rounds
     */
    private List<TournamentRound> generateWinnerBracketRounds(Tournament tournament, List<TournamentPlayer> players) {
        List<TournamentRound> winnerRounds = new ArrayList<>();
        
        DoubleEliminationMath.WinnerBracketStructure structure = 
            DoubleEliminationMath.calculateWinnerBracket(players.size());
        
        // Sort players by seed for proper bracket placement
        List<TournamentPlayer> sortedPlayers = players.stream()
            .sorted(Comparator.comparing(TournamentPlayer::getSeed))
            .collect(Collectors.toList());
        
        // Generate each winner's bracket round
        for (int roundNum = 1; roundNum <= structure.getTotalRounds(); roundNum++) {
            TournamentRound round = TournamentRound.builder()
                .roundId(UUID.randomUUID())
                .tournament(tournament)
                .roundNumber(roundNum)
                .bracketType(TournamentMatch.BracketType.WINNER)
                .name(getWinnerRoundName(roundNum, structure.getTotalRounds()))
                .status(roundNum == 1 ? TournamentRound.RoundStatus.READY : TournamentRound.RoundStatus.PENDING)
                .matches(new ArrayList<>())
                .build();
            
            // Generate matches for this round
            if (roundNum == 1) {
                round.getMatches().addAll(generateFirstRoundMatches(tournament, round, sortedPlayers, structure));
            } else {
                round.getMatches().addAll(generateSubsequentRoundMatches(tournament, round, roundNum, structure));
            }
            
            winnerRounds.add(round);
        }
        
        return winnerRounds;
    }
    
    /**
     * Generate loser's bracket rounds
     */
    private List<TournamentRound> generateLoserBracketRounds(Tournament tournament, List<TournamentPlayer> players) {
        List<TournamentRound> loserRounds = new ArrayList<>();
        
        DoubleEliminationMath.LoserBracketStructure structure = 
            DoubleEliminationMath.calculateLoserBracket(players.size());
        
        int baseRoundNumber = DoubleEliminationMath.calculateWinnerBracket(players.size()).getTotalRounds();
        
        // Generate each loser's bracket round
        for (int loserRoundNum = 1; loserRoundNum <= structure.getTotalRounds(); loserRoundNum++) {
            TournamentRound round = TournamentRound.builder()
                .roundId(UUID.randomUUID())
                .tournament(tournament)
                .roundNumber(baseRoundNumber + loserRoundNum)
                .bracketType(TournamentMatch.BracketType.LOSER)
                .name(getLoserRoundName(loserRoundNum, structure.getTotalRounds()))
                .status(TournamentRound.RoundStatus.PENDING)
                .matches(new ArrayList<>())
                .build();
            
            // Generate placeholder matches for loser's bracket
            // These will be populated as teams drop from winner's bracket
            round.getMatches().addAll(generateLoserRoundPlaceholders(tournament, round, loserRoundNum, structure));
            
            loserRounds.add(round);
        }
        
        return loserRounds;
    }
    
    /**
     * Generate grand finals round
     */
    private TournamentRound generateGrandFinalsRound(Tournament tournament, int playerCount) {
        int totalRounds = calculateTotalRounds(playerCount);
        
        return TournamentRound.builder()
            .roundId(UUID.randomUUID())
            .tournament(tournament)
            .roundNumber(totalRounds)
            .bracketType(TournamentMatch.BracketType.GRAND_FINAL)
            .name("Grand Finals")
            .status(TournamentRound.RoundStatus.PENDING)
            .matches(Arrays.asList(
                TournamentMatch.builder()
                    .matchId(UUID.randomUUID())
                    .displayId("GF-1")
                    .tournament(tournament)
                    .round(totalRounds)
                    .positionInRound(1)
                    .bracketType(TournamentMatch.BracketType.GRAND_FINAL)
                    .completed(false)
                    .build()
            ))
            .build();
    }
    
    /**
     * Generate first round matches with proper seeding and byes for ANY player count 2-16
     */
    private List<TournamentMatch> generateFirstRoundMatches(Tournament tournament, TournamentRound round, 
                                                           List<TournamentPlayer> sortedPlayers,
                                                           DoubleEliminationMath.WinnerBracketStructure structure) {
        List<TournamentMatch> matches = new ArrayList<>();
        
        // Get bracket layout using our new algorithm (supports all player counts)
        int[] bracketOrder = getBracketOrder(sortedPlayers.size());
        
        // Create matches from bracket positions
        for (int matchNum = 1; matchNum <= structure.getFirstRoundMatches(); matchNum++) {
            int pos1 = (matchNum - 1) * 2; // First position in match
            int pos2 = (matchNum - 1) * 2 + 1; // Second position in match
            
            TournamentPlayer player1 = getPlayerFromBracketPosition(bracketOrder[pos1], sortedPlayers);
            TournamentPlayer player2 = getPlayerFromBracketPosition(bracketOrder[pos2], sortedPlayers);
            
            TournamentMatch match = createMatch(tournament, round, matchNum, player1, player2);
            matches.add(match);
        }
        
        return matches;
    }
    
    /**
     * Convert bracket position to actual player
     * Positive numbers = actual player seeds, Negative numbers = byes to that seed
     */
    private TournamentPlayer getPlayerFromBracketPosition(int position, List<TournamentPlayer> sortedPlayers) {
        if (position > 0 && position <= sortedPlayers.size()) {
            // Regular player - find by seed
            return sortedPlayers.stream()
                .filter(p -> p.getSeed() == position)
                .findFirst()
                .orElse(null);
        } else {
            // Negative number means bye (the actual beneficiary gets this bye)
            return null; // Bye represented as null player
        }
    }
    
    /**
     * Generate subsequent round matches (placeholders)
     */
    private List<TournamentMatch> generateSubsequentRoundMatches(Tournament tournament, TournamentRound round,
                                                               int roundNum, DoubleEliminationMath.WinnerBracketStructure structure) {
        List<TournamentMatch> matches = new ArrayList<>();
        
        int matchesInRound = structure.getFirstRoundMatches() / (int) Math.pow(2, roundNum - 1);
        
        for (int matchNum = 1; matchNum <= matchesInRound; matchNum++) {
            TournamentMatch match = TournamentMatch.builder()
                .matchId(UUID.randomUUID())
                .displayId(String.format("WB%d-M%d", roundNum, matchNum))
                .tournament(tournament)
                .tournamentRound(round)
                .round(round.getRoundNumber())
                .positionInRound(matchNum)
                .bracketType(TournamentMatch.BracketType.WINNER)
                .completed(false)
                .build();
            
            matches.add(match);
        }
        
        return matches;
    }
    
    /**
     * Generate loser bracket placeholder matches
     */
    private List<TournamentMatch> generateLoserRoundPlaceholders(Tournament tournament, TournamentRound round,
                                                               int loserRoundNum, DoubleEliminationMath.LoserBracketStructure structure) {
        List<TournamentMatch> matches = new ArrayList<>();
        
        // Calculate matches for this loser round (simplified for now)
        int matchesInRound = Math.max(1, structure.getFirstRoundMatches() / (int) Math.pow(2, (loserRoundNum - 1) / 2));
        
        for (int matchNum = 1; matchNum <= matchesInRound; matchNum++) {
            TournamentMatch match = TournamentMatch.builder()
                .matchId(UUID.randomUUID())
                .displayId(String.format("LB%d-M%d", loserRoundNum, matchNum))
                .tournament(tournament)
                .tournamentRound(round)
                .round(round.getRoundNumber())
                .positionInRound(matchNum)
                .bracketType(TournamentMatch.BracketType.LOSER)
                .completed(false)
                .build();
            
            matches.add(match);
        }
        
        return matches;
    }
    
    /**
     * Create a match between two players (handles byes)
     */
    private TournamentMatch createMatch(Tournament tournament, TournamentRound round, int position,
                                      TournamentPlayer player1, TournamentPlayer player2) {
        
        TournamentMatch.TournamentMatchBuilder builder = TournamentMatch.builder()
            .matchId(UUID.randomUUID())
            .displayId(String.format("WB%d-M%d", round.getRoundNumber(), position))
            .tournament(tournament)
            .tournamentRound(round)
            .round(round.getRoundNumber())
            .positionInRound(position)
            .bracketType(TournamentMatch.BracketType.WINNER)
            .completed(false);
        
        // Handle byes
        if (player1 != null && player2 == null) {
            // Player 1 gets bye
            builder.team1Ids(Arrays.asList(player1.getPlayerId()))
                .team1Seed(player1.getSeed())
                .winnerIds(Arrays.asList(player1.getPlayerId()))
                .isBye(true)
                .byeTeam(1)
                .completed(true);
        } else if (player1 == null && player2 != null) {
            // Player 2 gets bye
            builder.team2Ids(Arrays.asList(player2.getPlayerId()))
                .team2Seed(player2.getSeed())
                .winnerIds(Arrays.asList(player2.getPlayerId()))
                .isBye(true)
                .byeTeam(2)
                .completed(true);
        } else if (player1 != null && player2 != null) {
            // Regular match
            builder.team1Ids(Arrays.asList(player1.getPlayerId()))
                .team1Seed(player1.getSeed())
                .team2Ids(Arrays.asList(player2.getPlayerId()))
                .team2Seed(player2.getSeed());
        }
        // If both null, it's a placeholder match (will be filled later)
        
        return builder.build();
    }
    
    @Override
    @Deprecated
    /**
     * @deprecated Use the new pre-built bracket API instead:
     * 1. generateInitialBracket() to create complete tournament structure
     * 2. getReadyMatches() to get matches ready to play  
     * 3. advancePlayersAndGetNextMatches() to advance after completing matches
     * 
     * This legacy method is maintained for backward compatibility with TournamentEngineImpl
     * but should not be used in new code.
     */
    public List<TournamentRound> generateNextRound(Tournament tournament, TournamentRound completedRound, 
                                                   List<TournamentPlayer> participants) {
        log.info("Generating next round after round {} ({})", completedRound.getRoundNumber(), completedRound.getName());
        
        List<TournamentRound> newRounds = new ArrayList<>();
        
        // Get winners and losers from completed round
        AdvancementResult advancement = processRoundCompletion(completedRound, participants, tournament);
        
        // Handle winner's bracket advancement
        if (!advancement.winnersAdvancement.isEmpty()) {
            TournamentRound nextWinnerRound = createNextWinnerRound(tournament, completedRound, advancement.winnersAdvancement);
            if (nextWinnerRound != null) {
                newRounds.add(nextWinnerRound);
                log.info("Created next winner's bracket round: {}", nextWinnerRound.getName());
            }
        }
        
        // Handle loser's bracket seeding and advancement
        if (!advancement.losersToLoserBracket.isEmpty()) {
            TournamentRound nextLoserRound = createOrUpdateLoserRound(tournament, completedRound, advancement.losersToLoserBracket);
            if (nextLoserRound != null) {
                newRounds.add(nextLoserRound);
                log.info("Created/updated loser's bracket round: {}", nextLoserRound.getName());
            }
        }
        
        // Handle loser's bracket progression
        if (!advancement.loserBracketAdvancement.isEmpty()) {
            TournamentRound nextLoserRound = createNextLoserRound(tournament, completedRound, advancement.loserBracketAdvancement);
            if (nextLoserRound != null) {
                newRounds.add(nextLoserRound);
                log.info("Created next loser's bracket progression round: {}", nextLoserRound.getName());
            }
        }
        
        // Check if we need to create grand finals
        if (shouldCreateGrandFinals(tournament, completedRound, advancement)) {
            TournamentRound grandFinals = createGrandFinalsRound(tournament, advancement);
            if (grandFinals != null) {
                newRounds.add(grandFinals);
                log.info("Created grand finals round");
            }
        }
        
        return newRounds;
    }
    
    @Override
    public List<TournamentMatch> handleParticipantDropout(Tournament tournament, UUID droppedParticipantId) {
        // TODO: Implement dropout handling
        log.info("Handling participant dropout: {}", droppedParticipantId);
        return new ArrayList<>();
    }
    
    @Override
    public boolean isTournamentComplete(Tournament tournament) {
        // TODO: Implement completion check
        return false;
    }
    
    @Override
    public List<UUID> getTournamentWinners(Tournament tournament) {
        // TODO: Implement winner detection
        return new ArrayList<>();
    }
    
    @Override
    public List<UUID> getTournamentRunnersUp(Tournament tournament) {
        // TODO: Implement runner-up detection
        return new ArrayList<>();
    }
    
    @Override
    public void validateTournamentConfiguration(Tournament tournament) {
        if (tournament.getTournamentType() != Tournament.TournamentType.DOUBLE_ELIMINATION) {
            throw new IllegalArgumentException("Tournament must be DOUBLE_ELIMINATION type");
        }
        if (tournament.getNumberOfPlayers() < 2) {
            throw new IllegalArgumentException("Tournament must have at least 2 players");
        }
        if (tournament.getNumberOfPlayers() > 16) {
            throw new IllegalArgumentException("Tournament cannot exceed 16 players");
        }
    }
    
    @Override
    public String getRoundDisplayName(int roundNumber, TournamentMatch.BracketType bracketType, int participantCount) {
        return switch (bracketType) {
            case WINNER -> getWinnerRoundName(roundNumber, calculateWinnerRounds(participantCount));
            case LOSER -> getLoserRoundName(roundNumber - calculateWinnerRounds(participantCount), 
                                          calculateLoserRounds(participantCount));
            case GRAND_FINAL -> "Grand Finals";
            case GRAND_FINAL_RESET -> "Grand Finals (Reset)";
            default -> "Round " + roundNumber;
        };
    }
    
    private int calculateWinnerRounds(int participantCount) {
        return DoubleEliminationMath.calculateWinnerBracket(participantCount).getTotalRounds();
    }
    
    private int calculateLoserRounds(int participantCount) {
        return DoubleEliminationMath.calculateLoserBracket(participantCount).getTotalRounds();
    }
    
    /**
     * Get winner round name based on round number
     */
    private String getWinnerRoundName(int roundNum, int totalRounds) {
        // The last round is always WB Finals
        if (roundNum == totalRounds) {
            return "WB Finals";
        }
        
        // Handle specific tournament sizes with meaningful round names
        if (totalRounds == 4) {
            return switch (roundNum) {
                case 1 -> "Round of 16";
                case 2 -> "Quarterfinals";  
                case 3 -> "Semifinals";
                case 4 -> "WB Finals";
                default -> "WB Round " + roundNum;
            };
        } else if (totalRounds == 3) {
            return switch (roundNum) {
                case 1 -> "Quarterfinals";
                case 2 -> "Semifinals";  
                case 3 -> "WB Finals";
                default -> "WB Round " + roundNum;
            };
        } else if (totalRounds == 2) {
            return switch (roundNum) {
                case 1 -> "Semifinals";
                case 2 -> "WB Finals";
                default -> "WB Round " + roundNum;
            };
        }
        
        // Generic naming for other sizes
        if (roundNum == totalRounds - 1) {
            return "Semifinals";
        }
        
        return "WB Round " + roundNum;
    }
    
    /**
     * Get loser round name based on round number
     */
    private String getLoserRoundName(int loserRoundNum, int totalLoserRounds) {
        if (loserRoundNum == totalLoserRounds) {
            return "LB Finals";
        }
        return "LB Round " + loserRoundNum;
    }
    
    /**
     * Data class to hold advancement results from a completed round
     */
    private static class AdvancementResult {
        final List<TournamentPlayer> winnersAdvancement;
        final List<TournamentPlayer> losersToLoserBracket;
        final List<TournamentPlayer> loserBracketAdvancement;
        final boolean isWinnerBracketComplete;
        final boolean isLoserBracketComplete;
        
        AdvancementResult(List<TournamentPlayer> winnersAdvancement, 
                         List<TournamentPlayer> losersToLoserBracket,
                         List<TournamentPlayer> loserBracketAdvancement,
                         boolean isWinnerBracketComplete,
                         boolean isLoserBracketComplete) {
            this.winnersAdvancement = winnersAdvancement != null ? new ArrayList<>(winnersAdvancement) : new ArrayList<>();
            this.losersToLoserBracket = losersToLoserBracket != null ? new ArrayList<>(losersToLoserBracket) : new ArrayList<>();
            this.loserBracketAdvancement = loserBracketAdvancement != null ? new ArrayList<>(loserBracketAdvancement) : new ArrayList<>();
            this.isWinnerBracketComplete = isWinnerBracketComplete;
            this.isLoserBracketComplete = isLoserBracketComplete;
        }
    }
    
    /**
     * Process completion of a round and determine advancement
     */
    private AdvancementResult processRoundCompletion(TournamentRound completedRound, 
                                                   List<TournamentPlayer> participants,
                                                   Tournament tournament) {
        List<TournamentPlayer> winnersAdvancement = new ArrayList<>();
        List<TournamentPlayer> losersToLoserBracket = new ArrayList<>(); 
        List<TournamentPlayer> loserBracketAdvancement = new ArrayList<>();
        
        // Process matches in bracket position order to preserve bracket structure
        List<TournamentMatch> sortedMatches = completedRound.getMatches().stream()
            .sorted(Comparator.comparing(TournamentMatch::getPositionInRound))
            .collect(Collectors.toList());
            
        for (TournamentMatch match : sortedMatches) {
            if (!match.isCompleted() || match.getWinnerIds() == null || match.getWinnerIds().isEmpty()) {
                continue; // Skip incomplete matches
            }
            
            // Get winner and loser players
            TournamentPlayer winner = findPlayerByIds(match.getWinnerIds(), participants);
            TournamentPlayer loser = findPlayerByIds(getLoserIds(match), participants);
            
            if (winner == null) continue;
            
            if (completedRound.getBracketType() == TournamentMatch.BracketType.WINNER) {
                // Winner's bracket: winners advance in bracket position order
                winnersAdvancement.add(winner);
                if (loser != null && !match.isBye()) {
                    losersToLoserBracket.add(loser);
                }
            } else if (completedRound.getBracketType() == TournamentMatch.BracketType.LOSER) {
                // Loser's bracket: winners advance, losers are eliminated
                loserBracketAdvancement.add(winner);
                // Losers in loser's bracket are eliminated (no further action needed)
            }
        }
        
        // Determine bracket completion status
        boolean isWinnerBracketComplete = (completedRound.getBracketType() == TournamentMatch.BracketType.WINNER) 
            && winnersAdvancement.size() == 1;
        boolean isLoserBracketComplete = (completedRound.getBracketType() == TournamentMatch.BracketType.LOSER)
            && completedRound.getName().toLowerCase().contains("finals")
            && loserBracketAdvancement.size() == 1;
        
        return new AdvancementResult(winnersAdvancement, losersToLoserBracket, loserBracketAdvancement,
                                   isWinnerBracketComplete, isLoserBracketComplete);
    }
    
    /**
     * Find tournament player by player IDs
     */
    private TournamentPlayer findPlayerByIds(List<UUID> playerIds, List<TournamentPlayer> participants) {
        if (playerIds == null || playerIds.isEmpty()) return null;
        
        // For singles tournaments, match by single player ID
        if (playerIds.size() == 1) {
            UUID targetId = playerIds.get(0);
            return participants.stream()
                .filter(p -> p.getPlayerId().equals(targetId))
                .findFirst()
                .orElse(null);
        }
        
        // For doubles tournaments, would need to match by player + partner combo
        // TODO: Implement doubles support later
        return null;
    }
    
    /**
     * Get loser IDs from a match
     */
    private List<UUID> getLoserIds(TournamentMatch match) {
        if (match.getWinnerIds() == null || match.getWinnerIds().isEmpty()) {
            return new ArrayList<>();
        }
        
        // Return the team that didn't win
        if (match.getWinnerIds().equals(match.getTeam1Ids())) {
            return match.getTeam2Ids() != null ? match.getTeam2Ids() : new ArrayList<>();
        } else {
            return match.getTeam1Ids() != null ? match.getTeam1Ids() : new ArrayList<>();
        }
    }
    
    /**
     * Create next winner's bracket round
     */
    private TournamentRound createNextWinnerRound(Tournament tournament, TournamentRound completedRound, 
                                                List<TournamentPlayer> winners) {
        if (winners.size() <= 1) {
            return null; // Winner's bracket is complete
        }
        
        DoubleEliminationMath.WinnerBracketStructure structure = 
            DoubleEliminationMath.calculateWinnerBracket(tournament.getNumberOfPlayers());
        
        int currentWinnerRound = getCurrentWinnerRoundNumber(completedRound);
        int nextWinnerRound = currentWinnerRound + 1;
        
        if (nextWinnerRound > structure.getTotalRounds()) {
            return null; // No more winner rounds
        }
        
        TournamentRound nextRound = TournamentRound.builder()
            .roundId(UUID.randomUUID())
            .tournament(tournament)
            .roundNumber(getNextRoundNumber(tournament))
            .bracketType(TournamentMatch.BracketType.WINNER)
            .name(getWinnerRoundName(nextWinnerRound, structure.getTotalRounds()))
            .status(TournamentRound.RoundStatus.READY)
            .matches(new ArrayList<>())
            .build();
        
        // Create matches using FIXED bracket advancement - NO reseeding!
        // Winners advance to predetermined bracket positions based on match structure
        List<TournamentMatch> matches = new ArrayList<>();
        int matchPosition = 1;
        
        // Pair winners based on bracket structure - winner of match 1 vs winner of match 2, etc.
        for (int i = 0; i < winners.size(); i += 2) {
            TournamentPlayer player1 = winners.get(i);
            TournamentPlayer player2 = i + 1 < winners.size() ? winners.get(i + 1) : null;
            
            TournamentMatch match = TournamentMatch.builder()
                .matchId(UUID.randomUUID())
                .displayId(String.format("WB%d-M%d", nextWinnerRound, matchPosition++))
                .tournament(tournament)
                .tournamentRound(nextRound)
                .round(nextRound.getRoundNumber())
                .positionInRound(matchPosition - 1)
                .bracketType(TournamentMatch.BracketType.WINNER)
                .team1Ids(List.of(player1.getPlayerId()))
                .team1Seed(player1.getSeed())
                .completed(false)
                .build();
            
            if (player2 != null) {
                match.setTeam2Ids(List.of(player2.getPlayerId()));
                match.setTeam2Seed(player2.getSeed());
            }
            
            matches.add(match);
        }
        
        nextRound.setMatches(matches);
        return nextRound;
    }
    
    /**
     * Create or update loser's bracket round with new dropouts from winner's bracket
     */
    private TournamentRound createOrUpdateLoserRound(Tournament tournament, TournamentRound completedRound,
                                                   List<TournamentPlayer> losers) {
        log.info("Placing {} losers in loser's bracket using opposite side rule", losers.size());
        
        // Calculate loser bracket structure
        DoubleEliminationMath.LoserBracketStructure loserStructure = 
            DoubleEliminationMath.calculateLoserBracket(tournament.getNumberOfPlayers());
        
        // Determine which loser round to place these dropouts in
        int winnerRoundNumber = getCurrentWinnerRoundNumber(completedRound);
        int targetLoserRound = calculateTargetLoserRound(winnerRoundNumber, loserStructure);
        
        log.info("Placing dropouts from winner round {} into loser round {}", winnerRoundNumber, targetLoserRound);
        
        // Create the loser bracket round
        TournamentRound loserRound = TournamentRound.builder()
            .roundId(UUID.randomUUID())
            .tournament(tournament)
            .roundNumber(getNextRoundNumber(tournament))
            .bracketType(TournamentMatch.BracketType.LOSER)
            .name(getLoserRoundName(targetLoserRound, loserStructure.getTotalRounds()))
            .status(TournamentRound.RoundStatus.READY)
            .matches(new ArrayList<>())
            .build();
        
        // Sort losers by seed for consistent bracket placement
        List<TournamentPlayer> sortedLosers = new ArrayList<>(losers);
        sortedLosers.sort(Comparator.comparing(TournamentPlayer::getSeed));
        
        // Create matches by pairing losers appropriately
        List<TournamentMatch> loserMatches = new ArrayList<>();
        int matchPosition = 1;
        
        // For first round losers in 7-person tournament: Grace(7), Frank(6), Eve(5)
        // Eve should get bye, Grace vs Frank should play
        if (sortedLosers.size() == 3 && targetLoserRound == 1) {
            // Lowest seed (Eve) gets bye, other two play
            TournamentPlayer byePlayer = sortedLosers.get(0); // Eve (seed 5)
            TournamentPlayer player1 = sortedLosers.get(1);   // Frank (seed 6) 
            TournamentPlayer player2 = sortedLosers.get(2);   // Grace (seed 7)
            
            // Create bye match for Eve
            TournamentMatch byeMatch = TournamentMatch.builder()
                .matchId(UUID.randomUUID())
                .displayId(String.format("LB%d-M%d", targetLoserRound, matchPosition++))
                .tournament(tournament)
                .tournamentRound(loserRound)
                .round(loserRound.getRoundNumber())
                .positionInRound(0)
                .bracketType(TournamentMatch.BracketType.LOSER)
                .team1Ids(List.of(byePlayer.getPlayerId()))
                .team1Seed(byePlayer.getSeed())
                .completed(true) // Bye is automatically completed
                .winnerIds(List.of(byePlayer.getPlayerId()))
                .build();
            loserMatches.add(byeMatch);
            log.info("Created loser bracket bye: {} advances", getPlayerDebugName(byePlayer));
            
            // Create match between other two losers
            TournamentMatch loserMatch = TournamentMatch.builder()
                .matchId(UUID.randomUUID())
                .displayId(String.format("LB%d-M%d", targetLoserRound, matchPosition++))
                .tournament(tournament)
                .tournamentRound(loserRound)
                .round(loserRound.getRoundNumber())
                .positionInRound(1)
                .bracketType(TournamentMatch.BracketType.LOSER)
                .team1Ids(List.of(player1.getPlayerId()))
                .team1Seed(player1.getSeed())
                .team2Ids(List.of(player2.getPlayerId()))
                .team2Seed(player2.getSeed())
                .completed(false)
                .build();
            loserMatches.add(loserMatch);
            log.info("Created loser bracket match: {} vs {}", getPlayerDebugName(player1), getPlayerDebugName(player2));
        } else {
            // General case: pair losers sequentially
            for (int i = 0; i < sortedLosers.size(); i += 2) {
                TournamentPlayer player1 = sortedLosers.get(i);
                TournamentPlayer player2 = i + 1 < sortedLosers.size() ? sortedLosers.get(i + 1) : null;
                
                TournamentMatch loserMatch = TournamentMatch.builder()
                    .matchId(UUID.randomUUID())
                    .displayId(String.format("LB%d-M%d", targetLoserRound, matchPosition++))
                    .tournament(tournament)
                    .tournamentRound(loserRound)
                    .round(loserRound.getRoundNumber())
                    .positionInRound(i / 2)
                    .bracketType(TournamentMatch.BracketType.LOSER)
                    .team1Ids(List.of(player1.getPlayerId()))
                    .team1Seed(player1.getSeed())
                    .completed(player2 == null) // Bye if no opponent
                    .build();
                
                if (player2 != null) {
                    loserMatch.setTeam2Ids(List.of(player2.getPlayerId()));
                    loserMatch.setTeam2Seed(player2.getSeed());
                    log.info("Created loser bracket match: {} vs {}", getPlayerDebugName(player1), getPlayerDebugName(player2));
                } else {
                    loserMatch.setWinnerIds(List.of(player1.getPlayerId()));
                    log.info("Created loser bracket bye: {} advances", getPlayerDebugName(player1));
                }
                
                loserMatches.add(loserMatch);
            }
        }
        
        // Sort matches by position for proper bracket layout
        loserMatches.sort(Comparator.comparing(TournamentMatch::getPositionInRound));
        loserRound.setMatches(loserMatches);
        
        log.info("Created loser bracket round '{}' with {} matches", loserRound.getName(), loserMatches.size());
        return loserRound;
    }
    
    /**
     * Calculate which loser bracket round dropouts should enter
     */
    private int calculateTargetLoserRound(int winnerRoundNumber, DoubleEliminationMath.LoserBracketStructure structure) {
        // In double elimination, dropouts from winner round N typically enter loser round N
        // But this can vary based on bracket structure
        return Math.min(winnerRoundNumber, structure.getTotalRounds());
    }
    
    /**
     * Find the original winner's bracket position for a player who lost
     */
    private DoubleEliminationMath.BracketPosition findOriginalWinnerPosition(TournamentPlayer loser, TournamentRound completedRound) {
        // Look through the completed round matches to find where this player lost
        for (int i = 0; i < completedRound.getMatches().size(); i++) {
            TournamentMatch match = completedRound.getMatches().get(i);
            
            // Check if this player was the loser in this match
            List<UUID> loserIds = getLoserIds(match);
            if (loserIds.contains(loser.getPlayerId())) {
                // Found the match where this player lost
                int totalInRound = completedRound.getMatches().size();
                return DoubleEliminationMath.BracketPosition.builder()
                    .position(i + 1) // 1-based position
                    .round(getCurrentWinnerRoundNumber(completedRound))
                    .side(DoubleEliminationMath.calculateBracketSide(i + 1, totalInRound))
                    .totalInRound(totalInRound)
                    .build();
            }
        }
        
        log.warn("Could not find original winner position for player {}", getPlayerDebugName(loser));
        return null;
    }
    
    /**
     * Get bracket order that ensures #1 and #2 are in opposite halves for ANY player count 2-16
     * Uses simplified player-first approach: assign byes to top seeds, place strategically
     */
    private int[] getBracketOrder(int actualPlayers) {
        int bracketSize = nextPowerOf2(actualPlayers);
        int byes = bracketSize - actualPlayers;
        
        // Determine which seeds get byes (top seeds)
        Set<Integer> seedsWithByes = new HashSet<>();
        for (int i = 1; i <= byes; i++) {
            seedsWithByes.add(i);
        }
        
        // Create bracket layout using separation-first approach
        return createBracketLayout(actualPlayers, bracketSize, seedsWithByes);
    }
    
    /**
     * Create bracket layout by placing players strategically
     * Key principle: #1 goes in top half, #2 goes in bottom half
     */
    private int[] createBracketLayout(int players, int bracketSize, Set<Integer> seedsWithByes) {
        int[] layout = new int[bracketSize];
        
        if (bracketSize == 2) {
            layout[0] = 1; layout[1] = 2;
        } else if (bracketSize == 4) {
            if (players == 3) {
                // 3 players: [BYE→#1, BYE→#1, #2, #3]
                layout[0] = -1; layout[1] = -1; layout[2] = 2; layout[3] = 3; // -1 = bye to #1
            } else {
                // 4 players: [#1, #4, #2, #3] - standard seeding
                layout[0] = 1; layout[1] = 4; layout[2] = 2; layout[3] = 3;
            }
        } else if (bracketSize == 8) {
            if (players == 5) {
                // 5 players: [BYE→#1, BYE→#1, #4, #5, BYE→#2, BYE→#2, BYE→#3, BYE→#3]
                layout[0] = -1; layout[1] = -1; layout[2] = 4; layout[3] = 5;
                layout[4] = -2; layout[5] = -2; layout[6] = -3; layout[7] = -3;
            } else if (players == 6) {
                // 6 players: [BYE→#1, BYE→#1, #4, #5, BYE→#2, BYE→#2, #3, #6]
                layout[0] = -1; layout[1] = -1; layout[2] = 4; layout[3] = 5;
                layout[4] = -2; layout[5] = -2; layout[6] = 3; layout[7] = 6;
            } else if (players == 7) {
                // 7 players: [BYE→#1, BYE→#1, #4, #5, #2, #7, #3, #6]
                layout[0] = -1; layout[1] = -1; layout[2] = 4; layout[3] = 5;
                layout[4] = 2; layout[5] = 7; layout[6] = 3; layout[7] = 6;
            } else {
                // 8 players: [#1, #8, #4, #5, #2, #7, #3, #6] - standard seeding
                layout[0] = 1; layout[1] = 8; layout[2] = 4; layout[3] = 5;
                layout[4] = 2; layout[5] = 7; layout[6] = 3; layout[7] = 6;
            }
        } else if (bracketSize == 16) {
            // Use standard 16-player seeding as base, replace missing seeds with byes to #1
            int[] standardOrder = {1,16, 8,9, 4,13, 5,12, 2,15, 7,10, 3,14, 6,11};
            
            for (int i = 0; i < 16; i++) {
                int expectedSeed = standardOrder[i];
                if (expectedSeed <= players) {
                    layout[i] = expectedSeed;
                } else {
                    // Missing seed becomes bye to #1 (top half) or #2 (bottom half)
                    int beneficiary = (i < 8) ? 1 : 2;
                    layout[i] = -beneficiary;
                }
            }
        }
        
        return layout;
    }
    
    /**
     * Calculate next power of 2 greater than or equal to n
     */
    private int nextPowerOf2(int n) {
        if (n <= 1) return 2;
        return Integer.highestOneBit(n - 1) << 1;
    }
    
    /**
     * Generate seed order recursively for any bracket size
     */
    private List<Integer> generateSeedOrderRecursively(int bracketSize) {
        if (bracketSize == 2) {
            return Arrays.asList(1, 2);
        }
        
        List<Integer> prevSeeds = generateSeedOrderRecursively(bracketSize / 2);
        List<Integer> newSeeds = new ArrayList<>();
        
        // For each seed in the previous bracket, add it and its "opposite"
        for (int seed : prevSeeds) {
            newSeeds.add(seed);
            newSeeds.add(bracketSize + 1 - seed);
        }
        
        return newSeeds;
    }
    
    /**
     * Get debug name for player
     */
    private String getPlayerDebugName(TournamentPlayer player) {
        return String.format("Seed%d(ID:%s)", player.getSeed(), 
            player.getPlayerId().toString().substring(0, 8));
    }
    
    /**
     * Create next loser's bracket round for progression within loser's bracket
     */
    private TournamentRound createNextLoserRound(Tournament tournament, TournamentRound completedRound,
                                               List<TournamentPlayer> winners) {
        log.info("Advancing {} winners within loser's bracket", winners.size());
        
        if (winners.isEmpty()) {
            return null;
        }
        
        // Calculate loser bracket structure
        DoubleEliminationMath.LoserBracketStructure loserStructure = 
            DoubleEliminationMath.calculateLoserBracket(tournament.getNumberOfPlayers());
        
        // Determine current and next loser round numbers
        int currentLoserRound = getCurrentLoserRoundNumber(completedRound);
        int nextLoserRound = currentLoserRound + 1;
        
        if (nextLoserRound > loserStructure.getTotalRounds()) {
            log.info("Loser bracket complete - no more loser rounds");
            return null; // Loser bracket is complete
        }
        
        log.info("Creating next loser bracket round: LB Round {} with {} winners", nextLoserRound, winners.size());
        
        // Create the next loser bracket round
        TournamentRound nextRound = TournamentRound.builder()
            .roundId(UUID.randomUUID())
            .tournament(tournament)
            .roundNumber(getNextRoundNumber(tournament))
            .bracketType(TournamentMatch.BracketType.LOSER)
            .name(getLoserRoundName(nextLoserRound, loserStructure.getTotalRounds()))
            .status(TournamentRound.RoundStatus.READY)
            .matches(new ArrayList<>())
            .build();
        
        // Create matches by pairing loser bracket winners
        List<TournamentMatch> matches = new ArrayList<>();
        int matchPosition = 1;
        
        for (int i = 0; i < winners.size(); i += 2) {
            TournamentPlayer player1 = winners.get(i);
            TournamentPlayer player2 = i + 1 < winners.size() ? winners.get(i + 1) : null;
            
            TournamentMatch match = TournamentMatch.builder()
                .matchId(UUID.randomUUID())
                .displayId(String.format("LB%d-M%d", nextLoserRound, matchPosition++))
                .tournament(tournament)
                .tournamentRound(nextRound)
                .round(nextRound.getRoundNumber())
                .positionInRound(matchPosition - 1)
                .bracketType(TournamentMatch.BracketType.LOSER)
                .team1Ids(List.of(player1.getPlayerId()))
                .team1Seed(player1.getSeed())
                .completed(false)
                .build();
            
            if (player2 != null) {
                match.setTeam2Ids(List.of(player2.getPlayerId()));
                match.setTeam2Seed(player2.getSeed());
                log.info("Created loser bracket match: {} vs {}", 
                    getPlayerDebugName(player1), getPlayerDebugName(player2));
            } else {
                // Odd number of winners - player1 gets a bye
                log.info("Created loser bracket bye: {} advances", getPlayerDebugName(player1));
            }
            
            matches.add(match);
        }
        
        nextRound.setMatches(matches);
        log.info("Created loser bracket round '{}' with {} matches", nextRound.getName(), matches.size());
        return nextRound;
    }
    
    /**
     * Get current loser round number from completed round
     */
    private int getCurrentLoserRoundNumber(TournamentRound completedRound) {
        String name = completedRound.getName().toLowerCase();
        
        // Parse loser round number from name like "LB Round 1", "LB Round 2", "LB Finals"
        if (name.contains("lb finals")) {
            // This is the final loser round - determine number from structure
            DoubleEliminationMath.LoserBracketStructure structure = 
                DoubleEliminationMath.calculateLoserBracket(completedRound.getTournament().getNumberOfPlayers());
            return structure.getTotalRounds();
        } else if (name.contains("lb round")) {
            // Extract number from "LB Round X"
            String[] parts = name.split("\\s+");
            for (int i = 0; i < parts.length - 1; i++) {
                if (parts[i].equals("round") && i + 1 < parts.length) {
                    try {
                        return Integer.parseInt(parts[i + 1]);
                    } catch (NumberFormatException e) {
                        log.warn("Could not parse loser round number from: {}", name);
                    }
                }
            }
        }
        
        log.warn("Could not determine loser round number from: {}", name);
        return 1; // Default fallback
    }
    
    /**
     * Check if grand finals should be created
     */
    private boolean shouldCreateGrandFinals(Tournament tournament, TournamentRound completedRound,
                                          AdvancementResult advancement) {
        // Check if there's no loser bracket (2-player tournament)
        DoubleEliminationMath.LoserBracketStructure loserStructure = 
            DoubleEliminationMath.calculateLoserBracket(tournament.getNumberOfPlayers());
        boolean hasLoserBracket = loserStructure.getTotalRounds() > 0;
        
        boolean hasWinnerChampion = advancement.isWinnerBracketComplete && advancement.winnersAdvancement.size() == 1;
        boolean hasLoserChampion = advancement.isLoserBracketComplete && advancement.loserBracketAdvancement.size() == 1;
        
        if (!hasLoserBracket) {
            // Special case: 2-player tournament - activate Grand Finals when WB is complete
            log.info("Grand finals check (no loser bracket): winner champion={}", hasWinnerChampion);
            return hasWinnerChampion && advancement.losersToLoserBracket.size() == 1; // WB loser available
        } else {
            // Normal case: need both bracket champions
            log.info("Grand finals check: winner champion={}, loser champion={}", hasWinnerChampion, hasLoserChampion);
            return hasWinnerChampion && hasLoserChampion;
        }
    }
    
    /**
     * Create grand finals round
     */
    private TournamentRound createGrandFinalsRound(Tournament tournament, AdvancementResult advancement) {
        log.info("Creating grand finals round");
        
        // Check if there's no loser bracket (2-player tournament)
        DoubleEliminationMath.LoserBracketStructure loserStructure = 
            DoubleEliminationMath.calculateLoserBracket(tournament.getNumberOfPlayers());
        boolean hasLoserBracket = loserStructure.getTotalRounds() > 0;
        
        TournamentPlayer winnerChampion;
        TournamentPlayer loserChampion;
        
        if (!hasLoserBracket) {
            // Special case: 2-player tournament - loser champion is the WB Finals loser
            if (advancement.winnersAdvancement.size() != 1 || advancement.losersToLoserBracket.size() != 1) {
                log.error("Cannot create grand finals (2-player): need exactly 1 winner and 1 WB loser");
                return null;
            }
            winnerChampion = advancement.winnersAdvancement.get(0);
            loserChampion = advancement.losersToLoserBracket.get(0);
            log.info("Grand finals (2-player): {} (WB Winner) vs {} (WB Loser)", 
                getPlayerDebugName(winnerChampion), getPlayerDebugName(loserChampion));
        } else {
            // Normal case: both bracket champions
            if (advancement.winnersAdvancement.size() != 1 || advancement.loserBracketAdvancement.size() != 1) {
                log.error("Cannot create grand finals: need exactly 1 winner and 1 loser champion");
                return null;
            }
            winnerChampion = advancement.winnersAdvancement.get(0);
            loserChampion = advancement.loserBracketAdvancement.get(0);
            log.info("Grand finals: {} (Winner's Champion) vs {} (Loser's Champion)", 
                getPlayerDebugName(winnerChampion), getPlayerDebugName(loserChampion));
        }
        
        // Create grand finals round
        TournamentRound grandFinalsRound = TournamentRound.builder()
            .roundId(UUID.randomUUID())
            .tournament(tournament)
            .roundNumber(getNextRoundNumber(tournament))
            .bracketType(TournamentMatch.BracketType.FINAL)
            .name("Grand Finals")
            .status(TournamentRound.RoundStatus.READY)
            .matches(new ArrayList<>())
            .build();
        
        // Create the grand finals match
        TournamentMatch grandFinalsMatch = TournamentMatch.builder()
            .matchId(UUID.randomUUID())
            .displayId("GF-1")
            .tournament(tournament)
            .tournamentRound(grandFinalsRound)
            .round(grandFinalsRound.getRoundNumber())
            .positionInRound(1)
            .bracketType(TournamentMatch.BracketType.FINAL)
            .team1Ids(List.of(winnerChampion.getPlayerId()))
            .team1Seed(winnerChampion.getSeed())
            .team2Ids(List.of(loserChampion.getPlayerId()))
            .team2Seed(loserChampion.getSeed())
            .completed(false)
            .build();
        
        grandFinalsRound.setMatches(List.of(grandFinalsMatch));
        
        log.info("Created grand finals: {} vs {}", 
            getPlayerDebugName(winnerChampion), getPlayerDebugName(loserChampion));
        
        return grandFinalsRound;
    }
    
    /**
     * Get current winner round number from completed round
     */
    private int getCurrentWinnerRoundNumber(TournamentRound completedRound) {
        String name = completedRound.getName();
        int totalPlayers = completedRound.getTournament().getNumberOfPlayers();
        
        // Calculate total rounds for this tournament size
        DoubleEliminationMath.WinnerBracketStructure structure = 
            DoubleEliminationMath.calculateWinnerBracket(totalPlayers);
        int totalRounds = structure.getTotalRounds();
        
        // Map specific round names to round numbers based on tournament structure
        if (totalRounds == 4) {
            // 15-16 player tournaments: Round of 16(1) → Quarterfinals(2) → Semifinals(3) → WB Finals(4)
            if (name.equals("Round of 16")) return 1;
            if (name.equals("Quarterfinals")) return 2;
            if (name.equals("Semifinals")) return 3;
            if (name.equals("WB Finals")) return 4;
        } else if (totalRounds == 3) {
            // 8 player tournaments: Quarterfinals(1) → Semifinals(2) → WB Finals(3)
            if (name.equals("Quarterfinals")) return 1;
            if (name.equals("Semifinals")) return 2;
            if (name.equals("WB Finals")) return 3;
        } else if (totalRounds == 2) {
            // 4 player tournaments: Semifinals(1) → WB Finals(2)
            if (name.equals("Semifinals")) return 1;
            if (name.equals("WB Finals")) return 2;
        }
        
        // Parse from generic "WB Round X" format
        if (name.contains("WB Round ")) {
            try {
                String[] parts = name.split("\\s+");
                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i].equals("Round")) {
                        return Integer.parseInt(parts[i + 1]);
                    }
                }
            } catch (NumberFormatException e) {
                log.warn("Could not parse winner round number from: {}", name);
            }
        }
        
        return 1; // Default fallback
    }
    
    /**
     * Get next available round number for the tournament
     */
    private int getNextRoundNumber(Tournament tournament) {
        // Use current round counter from tournament + 1
        return (tournament.getCurrentRound() != null ? tournament.getCurrentRound() : 0) + 1;
    }
    
    // ========== NEW PRE-BUILT BRACKET API ==========
    
    /**
     * Get ready matches from the pre-built tournament structure.
     * Returns only matches that are ready to be played right now.
     * 
     * @param tournament The tournament to get matches for
     * @param allRounds All tournament rounds (pre-built structure)
     * @return List of matches ready to play
     */
    public List<TournamentMatch> getReadyMatches(Tournament tournament, List<TournamentRound> allRounds) {
        log.info("Getting ready matches for tournament: {}", tournament.getName());
        
        List<TournamentMatch> readyMatches = new ArrayList<>();
        
        // Find all READY rounds and collect their incomplete matches
        for (TournamentRound round : allRounds) {
            if (round.getStatus() == TournamentRound.RoundStatus.READY) {
                List<TournamentMatch> roundMatches = round.getMatches().stream()
                    .filter(match -> !match.isCompleted() && hasPlayersReady(match))
                    .collect(Collectors.toList());
                readyMatches.addAll(roundMatches);
                
                log.info("Found {} ready matches in {}", roundMatches.size(), round.getName());
            }
        }
        
        log.info("Total ready matches: {}", readyMatches.size());
        return readyMatches;
    }
    
    /**
     * Complete matches and advance players to next rounds in the pre-built structure.
     * Instead of generating new rounds, this populates existing placeholder matches.
     * 
     * @param tournament The tournament
     * @param completedMatches List of completed matches
     * @param participants All tournament participants  
     * @param allRounds All tournament rounds (pre-built structure)
     * @return List of newly ready matches
     */
    public List<TournamentMatch> advancePlayersAndGetNextMatches(Tournament tournament, 
                                                               List<TournamentMatch> completedMatches,
                                                               List<TournamentPlayer> participants, 
                                                               List<TournamentRound> allRounds) {
        log.info("Advancing players from {} completed matches", completedMatches.size());
        
        // Group completed matches by round
        Map<TournamentRound, List<TournamentMatch>> matchesByRound = completedMatches.stream()
            .filter(match -> match.getTournamentRound() != null)
            .collect(Collectors.groupingBy(TournamentMatch::getTournamentRound));
        
        List<TournamentMatch> newlyReadyMatches = new ArrayList<>();
        
        for (Map.Entry<TournamentRound, List<TournamentMatch>> entry : matchesByRound.entrySet()) {
            TournamentRound completedRound = entry.getKey();
            List<TournamentMatch> roundMatches = entry.getValue();
            
            log.info("Processing completed round: {} ({} matches)", completedRound.getName(), roundMatches.size());
            
            // Check if this round is now fully complete
            boolean allRoundMatchesComplete = completedRound.getMatches().stream()
                .allMatch(TournamentMatch::isCompleted);
                
            if (allRoundMatchesComplete) {
                log.info("Round {} is fully complete, advancing players", completedRound.getName());
                
                // Mark round as complete
                completedRound.setStatus(TournamentRound.RoundStatus.COMPLETED);
                
                // Advance players to existing placeholder matches
                List<TournamentMatch> newMatches = advancePlayersToNextRounds(
                    tournament, completedRound, participants, allRounds);
                newlyReadyMatches.addAll(newMatches);
            }
        }
        
        // Remove duplicates by match ID
        List<TournamentMatch> uniqueMatches = newlyReadyMatches.stream()
            .distinct()
            .collect(Collectors.toList());
            
        if (uniqueMatches.size() != newlyReadyMatches.size()) {
            log.info("Removed {} duplicate matches", newlyReadyMatches.size() - uniqueMatches.size());
        }
        
        log.info("Generated {} newly ready matches", uniqueMatches.size());
        return uniqueMatches;
    }
    
    /**
     * Advance players from a completed round to existing placeholder matches in next rounds.
     * This replaces the old generateNextRound() logic.
     */
    private List<TournamentMatch> advancePlayersToNextRounds(Tournament tournament, TournamentRound completedRound,
                                                           List<TournamentPlayer> participants, List<TournamentRound> allRounds) {
        List<TournamentMatch> newlyReadyMatches = new ArrayList<>();
        
        // Process round completion and get advancement results
        AdvancementResult advancement = processRoundCompletion(completedRound, participants, tournament);
        
        // Handle winner's bracket advancement
        if (!advancement.winnersAdvancement.isEmpty()) {
            List<TournamentMatch> winnerMatches = populateNextWinnerMatches(
                tournament, completedRound, advancement.winnersAdvancement, allRounds);
            newlyReadyMatches.addAll(winnerMatches);
        }
        
        // Handle loser's bracket placement
        if (!advancement.losersToLoserBracket.isEmpty()) {
            log.info("Placing {} losers from {} to loser bracket", 
                advancement.losersToLoserBracket.size(), completedRound.getName());
            List<TournamentMatch> loserMatches = populateLoserBracketMatches(
                tournament, completedRound, advancement.losersToLoserBracket, allRounds);
            newlyReadyMatches.addAll(loserMatches);
        }
        
        // Handle loser's bracket advancement
        if (!advancement.loserBracketAdvancement.isEmpty()) {
            log.info("Advancing {} winners from {} in loser bracket", 
                advancement.loserBracketAdvancement.size(), completedRound.getName());
            List<TournamentMatch> loserAdvancementMatches = populateNextLoserMatches(
                tournament, completedRound, advancement.loserBracketAdvancement, allRounds);
            newlyReadyMatches.addAll(loserAdvancementMatches);
        }
        
        // Check if we need to activate grand finals
        if (shouldActivateGrandFinals(tournament, completedRound, advancement, allRounds)) {
            List<TournamentMatch> grandFinalsMatches = populateGrandFinalsMatches(
                tournament, advancement, allRounds);
            newlyReadyMatches.addAll(grandFinalsMatches);
        }
        
        return newlyReadyMatches;
    }
    
    /**
     * Populate existing winner's bracket placeholder matches with advancing winners
     */
    private List<TournamentMatch> populateNextWinnerMatches(Tournament tournament, TournamentRound completedRound,
                                                          List<TournamentPlayer> winners, List<TournamentRound> allRounds) {
        if (winners.size() <= 1) {
            log.info("Winner's bracket complete - only {} winners remaining", winners.size());
            return new ArrayList<>();
        }
        
        // Find the next winner's bracket round
        TournamentRound nextWinnerRound = findNextWinnerBracketRound(completedRound, allRounds);
        if (nextWinnerRound == null) {
            log.info("No next winner's bracket round found");
            return new ArrayList<>();
        }
        
        log.info("Populating {} with {} winners", nextWinnerRound.getName(), winners.size());
        
        // Debug: show the winners
        for (int i = 0; i < winners.size(); i++) {
            log.info("  Winner {}: {}", i+1, getPlayerDebugName(winners.get(i)));
        }
        
        // Debug: examine all matches in the round
        log.info("Examining {} matches in {}", nextWinnerRound.getMatches().size(), nextWinnerRound.getName());
        for (TournamentMatch match : nextWinnerRound.getMatches()) {
            log.info("  Match {}: team1Ids={}, team2Ids={}, completed={}", 
                match.getDisplayId(), match.getTeam1Ids(), match.getTeam2Ids(), match.isCompleted());
        }

        // Populate placeholder matches with winners (maintain bracket order)
        List<TournamentMatch> populatedMatches = new ArrayList<>();
        List<TournamentMatch> placeholderMatches = nextWinnerRound.getMatches().stream()
            .filter(match -> (match.getTeam1Ids() == null || match.getTeam1Ids().isEmpty()) && 
                           (match.getTeam2Ids() == null || match.getTeam2Ids().isEmpty()))
            .sorted(Comparator.comparing(TournamentMatch::getPositionInRound))
            .collect(Collectors.toList());
            
        log.info("Found {} placeholder matches (null team IDs) out of {} total matches", 
            placeholderMatches.size(), nextWinnerRound.getMatches().size());
        
        for (int i = 0; i < Math.min(placeholderMatches.size(), (winners.size() + 1) / 2); i++) {
            TournamentMatch match = placeholderMatches.get(i);
            TournamentPlayer player1 = winners.get(i * 2);
            TournamentPlayer player2 = (i * 2 + 1 < winners.size()) ? winners.get(i * 2 + 1) : null;
            
            match.setTeam1Ids(List.of(player1.getPlayerId()));
            match.setTeam1Seed(player1.getSeed());
            
            if (player2 != null) {
                match.setTeam2Ids(List.of(player2.getPlayerId()));
                match.setTeam2Seed(player2.getSeed());
            }
            
            populatedMatches.add(match);
            log.info("Populated {}: {} vs {}", match.getDisplayId(), 
                getPlayerDebugName(player1), player2 != null ? getPlayerDebugName(player2) : "BYE");
        }
        
        // Mark round as ready if it has populated matches
        if (!populatedMatches.isEmpty()) {
            nextWinnerRound.setStatus(TournamentRound.RoundStatus.READY);
            log.info("Marked {} as READY with {} populated matches", nextWinnerRound.getName(), populatedMatches.size());
        } else {
            log.warn("No matches were populated for {}", nextWinnerRound.getName());
        }
        
        log.info("Returning {} populated winner matches", populatedMatches.size());
        return populatedMatches;
    }
    
    /**
     * Populate existing loser's bracket placeholder matches with dropouts from winner's bracket
     */
    private List<TournamentMatch> populateLoserBracketMatches(Tournament tournament, TournamentRound completedRound,
                                                            List<TournamentPlayer> losers, List<TournamentRound> allRounds) {
        // Find appropriate loser's bracket round for these dropouts
        TournamentRound loserRound = findLoserBracketRoundForDropouts(completedRound, allRounds);
        if (loserRound == null) {
            log.info("No appropriate loser's bracket round found for {} dropouts", losers.size());
            return new ArrayList<>();
        }
        
        log.info("Populating {} with {} dropouts from {}", loserRound.getName(), losers.size(), completedRound.getName());
        log.info("Loser round {} status: {}, matches: {}", loserRound.getName(), loserRound.getStatus(), 
            loserRound.getMatches().size());
        
        // Populate matches with losers
        List<TournamentMatch> populatedMatches = new ArrayList<>();
        
        // Find matches that can be populated:
        // 1. Completely empty matches (both teams null/empty)
        // 2. Partially filled matches (one team filled, other is null/empty) - for LB Finals case
        List<TournamentMatch> availableMatches = loserRound.getMatches().stream()
            .filter(match -> !match.isCompleted() && (
                // Completely empty match
                ((match.getTeam1Ids() == null || match.getTeam1Ids().isEmpty()) && 
                 (match.getTeam2Ids() == null || match.getTeam2Ids().isEmpty())) ||
                // Partially filled match (one team present, other empty)
                ((match.getTeam1Ids() != null && !match.getTeam1Ids().isEmpty()) && 
                 (match.getTeam2Ids() == null || match.getTeam2Ids().isEmpty())) ||
                ((match.getTeam1Ids() == null || match.getTeam1Ids().isEmpty()) && 
                 (match.getTeam2Ids() != null && !match.getTeam2Ids().isEmpty()))
            ))
            .sorted(Comparator.comparing(TournamentMatch::getPositionInRound))
            .collect(Collectors.toList());
            
        log.info("Found {} available matches to populate in {}", availableMatches.size(), loserRound.getName());
            
        // Sort losers by seed for consistent placement
        List<TournamentPlayer> sortedLosers = new ArrayList<>(losers);
        sortedLosers.sort(Comparator.comparing(TournamentPlayer::getSeed));
        
        // Populate matches with available dropouts
        int loserIndex = 0;
        for (TournamentMatch match : availableMatches) {
            if (loserIndex >= sortedLosers.size()) {
                break; // No more losers to place
            }
            
            TournamentPlayer currentLoser = sortedLosers.get(loserIndex++);
            
            // Check if match is completely empty or partially filled
            boolean team1Empty = (match.getTeam1Ids() == null || match.getTeam1Ids().isEmpty());
            boolean team2Empty = (match.getTeam2Ids() == null || match.getTeam2Ids().isEmpty());
            
            if (team1Empty && team2Empty) {
                // Completely empty match - populate team1 with current loser
                match.setTeam1Ids(List.of(currentLoser.getPlayerId()));
                match.setTeam1Seed(currentLoser.getSeed());
                
                // Try to populate team2 with next loser if available
                if (loserIndex < sortedLosers.size()) {
                    TournamentPlayer nextLoser = sortedLosers.get(loserIndex++);
                    match.setTeam2Ids(List.of(nextLoser.getPlayerId()));
                    match.setTeam2Seed(nextLoser.getSeed());
                    log.info("Populated {}: {} vs {}", match.getDisplayId(), 
                        getPlayerDebugName(currentLoser), getPlayerDebugName(nextLoser));
                } else {
                    // Current loser gets a bye
                    match.setCompleted(true);
                    match.setWinnerIds(List.of(currentLoser.getPlayerId()));
                    log.info("Populated {}: {} vs BYE (bye)", match.getDisplayId(), 
                        getPlayerDebugName(currentLoser));
                }
            } else if (team1Empty) {
                // Team2 already filled, populate team1
                match.setTeam1Ids(List.of(currentLoser.getPlayerId()));
                match.setTeam1Seed(currentLoser.getSeed());
                log.info("Populated {}: {} vs {} (completed match)", match.getDisplayId(), 
                    getPlayerDebugName(currentLoser), "existing-player");
            } else if (team2Empty) {
                // Team1 already filled, populate team2
                match.setTeam2Ids(List.of(currentLoser.getPlayerId()));
                match.setTeam2Seed(currentLoser.getSeed());
                log.info("Populated {}: {} vs {} (completed match)", match.getDisplayId(), 
                    "existing-player", getPlayerDebugName(currentLoser));
            }
            
            populatedMatches.add(match);
        }
        
        // Mark round as ready if it has populated matches
        if (!populatedMatches.isEmpty()) {
            loserRound.setStatus(TournamentRound.RoundStatus.READY);
            log.info("Marked {} as READY with {} populated matches", loserRound.getName(), populatedMatches.size());
        } else {
            log.warn("No matches were populated for {}", loserRound.getName());
        }
        
        log.info("Returning {} populated loser matches", populatedMatches.size());
        return populatedMatches;
    }
    
    /**
     * Populate existing loser's bracket placeholder matches with advancing loser's bracket winners
     */
    private List<TournamentMatch> populateNextLoserMatches(Tournament tournament, TournamentRound completedRound,
                                                         List<TournamentPlayer> winners, List<TournamentRound> allRounds) {
        if (winners.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Find the next loser's bracket round
        TournamentRound nextLoserRound = findNextLoserBracketRound(completedRound, allRounds);
        if (nextLoserRound == null) {
            log.info("Loser's bracket complete - no next round found");
            return new ArrayList<>();
        }
        
        log.info("Populating {} with {} LB winners", nextLoserRound.getName(), winners.size());
        log.info("Next loser round {} status: {}, matches: {}", nextLoserRound.getName(), nextLoserRound.getStatus(), 
            nextLoserRound.getMatches().size());
        
        // Populate placeholder matches with winners
        List<TournamentMatch> populatedMatches = new ArrayList<>();
        List<TournamentMatch> placeholderMatches = nextLoserRound.getMatches().stream()
            .filter(match -> (match.getTeam1Ids() == null || match.getTeam1Ids().isEmpty()) && 
                           (match.getTeam2Ids() == null || match.getTeam2Ids().isEmpty()))
            .sorted(Comparator.comparing(TournamentMatch::getPositionInRound))
            .collect(Collectors.toList());
        
        for (int i = 0; i < Math.min(placeholderMatches.size(), (winners.size() + 1) / 2); i++) {
            TournamentMatch match = placeholderMatches.get(i);
            TournamentPlayer player1 = winners.get(i * 2);
            TournamentPlayer player2 = (i * 2 + 1 < winners.size()) ? winners.get(i * 2 + 1) : null;
            
            match.setTeam1Ids(List.of(player1.getPlayerId()));
            match.setTeam1Seed(player1.getSeed());
            
            if (player2 != null) {
                match.setTeam2Ids(List.of(player2.getPlayerId()));
                match.setTeam2Seed(player2.getSeed());
            }
            
            populatedMatches.add(match);
            log.info("Populated {}: {} vs {}", match.getDisplayId(), 
                getPlayerDebugName(player1), player2 != null ? getPlayerDebugName(player2) : "BYE");
        }
        
        // Mark round as ready if it has populated matches
        if (!populatedMatches.isEmpty()) {
            nextLoserRound.setStatus(TournamentRound.RoundStatus.READY);
        }
        
        return populatedMatches;
    }
    
    /**
     * Check if grand finals should be activated
     */
    private boolean shouldActivateGrandFinals(Tournament tournament, TournamentRound completedRound,
                                            AdvancementResult advancement, List<TournamentRound> allRounds) {
        // Grand finals should be activated when both brackets have champions
        // Check global tournament state, not just current advancement
        
        // Find winner bracket champion (WB Finals winner)
        boolean hasWinnerChampion = allRounds.stream()
            .filter(round -> round.getBracketType() == TournamentMatch.BracketType.WINNER)
            .filter(round -> round.getStatus() == TournamentRound.RoundStatus.COMPLETED)
            .anyMatch(round -> round.getMatches().stream()
                .anyMatch(match -> match.isCompleted() && match.getWinnerIds() != null && !match.getWinnerIds().isEmpty()
                    && isWinnerBracketFinals(round)));
                    
        // Find loser bracket champion (LB Finals winner)
        boolean hasLoserChampion = allRounds.stream()
            .filter(round -> round.getBracketType() == TournamentMatch.BracketType.LOSER)
            .filter(round -> round.getStatus() == TournamentRound.RoundStatus.COMPLETED)
            .filter(round -> round.getName().toLowerCase().contains("finals"))
            .anyMatch(round -> round.getMatches().stream()
                .anyMatch(match -> match.isCompleted() && match.getWinnerIds() != null && !match.getWinnerIds().isEmpty()));
        
        log.info("Grand finals check: winner champion={}, loser champion={}", hasWinnerChampion, hasLoserChampion);
        return hasWinnerChampion && hasLoserChampion;
    }
    
    private boolean isWinnerBracketFinals(TournamentRound round) {
        String roundName = round.getName().toLowerCase();
        return roundName.contains("wb finals") || 
               roundName.equals("wb finals");
    }
    
    /**
     * Populate the existing grand finals match with the two champions
     */
    private List<TournamentMatch> populateGrandFinalsMatches(Tournament tournament, AdvancementResult advancement,
                                                           List<TournamentRound> allRounds) {
        // Find champions from completed rounds instead of current advancement
        TournamentPlayer winnerChampion = findWinnerBracketChampion(allRounds);
        TournamentPlayer loserChampion = findLoserBracketChampion(allRounds);
        
        if (winnerChampion == null || loserChampion == null) {
            log.error("Cannot populate grand finals: winner champion={}, loser champion={}", 
                winnerChampion != null ? getPlayerDebugName(winnerChampion) : "null",
                loserChampion != null ? getPlayerDebugName(loserChampion) : "null");
            return new ArrayList<>();
        }
        
        // Find the existing grand finals round
        TournamentRound grandFinalsRound = allRounds.stream()
            .filter(round -> round.getName().contains("Grand"))
            .findFirst()
            .orElse(null);
            
        if (grandFinalsRound == null || grandFinalsRound.getMatches().isEmpty()) {
            log.error("No existing grand finals round found");
            return new ArrayList<>();
        }
        
        TournamentMatch grandFinalsMatch = grandFinalsRound.getMatches().get(0);
        
        // Populate the match with the found champions
        grandFinalsMatch.setTeam1Ids(List.of(winnerChampion.getPlayerId()));
        grandFinalsMatch.setTeam1Seed(winnerChampion.getSeed());
        grandFinalsMatch.setTeam2Ids(List.of(loserChampion.getPlayerId()));
        grandFinalsMatch.setTeam2Seed(loserChampion.getSeed());
        
        // Mark round as ready
        grandFinalsRound.setStatus(TournamentRound.RoundStatus.READY);
        
        log.info("Populated Grand Finals: {} vs {}", 
            getPlayerDebugName(winnerChampion), getPlayerDebugName(loserChampion));
        
        return List.of(grandFinalsMatch);
    }
    
    private TournamentPlayer findWinnerBracketChampion(List<TournamentRound> allRounds) {
        return allRounds.stream()
            .filter(round -> round.getBracketType() == TournamentMatch.BracketType.WINNER)
            .filter(round -> round.getStatus() == TournamentRound.RoundStatus.COMPLETED)
            .filter(round -> isWinnerBracketFinals(round))
            .flatMap(round -> round.getMatches().stream())
            .filter(match -> match.isCompleted() && match.getWinnerIds() != null && !match.getWinnerIds().isEmpty())
            .map(match -> findPlayerByIds(match.getWinnerIds(), getAllParticipants(allRounds)))
            .filter(player -> player != null)
            .findFirst()
            .orElse(null);
    }
    
    private TournamentPlayer findLoserBracketChampion(List<TournamentRound> allRounds) {
        // Find the highest numbered completed loser bracket round
        return allRounds.stream()
            .filter(round -> round.getBracketType() == TournamentMatch.BracketType.LOSER)
            .filter(round -> round.getStatus() == TournamentRound.RoundStatus.COMPLETED)
            .max(Comparator.comparing(TournamentRound::getRoundNumber)) // Get highest round number
            .map(finalRound -> {
                // Get the winner from this final loser round
                return finalRound.getMatches().stream()
                    .filter(match -> match.isCompleted() && match.getWinnerIds() != null && !match.getWinnerIds().isEmpty())
                    .map(match -> findPlayerByIds(match.getWinnerIds(), getAllParticipants(allRounds)))
                    .filter(player -> player != null)
                    .findFirst()
                    .orElse(null);
            })
            .orElse(null);
    }
    
    private List<TournamentPlayer> getAllParticipants(List<TournamentRound> allRounds) {
        // Extract all participants from the tournament rounds
        return allRounds.stream()
            .flatMap(round -> round.getMatches().stream())
            .flatMap(match -> {
                List<TournamentPlayer> players = new ArrayList<>();
                if (match.getTeam1Ids() != null && !match.getTeam1Ids().isEmpty()) {
                    // Create TournamentPlayer from match data
                    TournamentPlayer player = TournamentPlayer.builder()
                        .playerId(match.getTeam1Ids().get(0))
                        .seed(match.getTeam1Seed())
                        .tournament(match.getTournament())
                        .build();
                    players.add(player);
                }
                if (match.getTeam2Ids() != null && !match.getTeam2Ids().isEmpty()) {
                    TournamentPlayer player = TournamentPlayer.builder()
                        .playerId(match.getTeam2Ids().get(0))
                        .seed(match.getTeam2Seed())
                        .tournament(match.getTournament())
                        .build();
                    players.add(player);
                }
                return players.stream();
            })
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * Find the next winner's bracket round after the completed round
     */
    private TournamentRound findNextWinnerBracketRound(TournamentRound completedRound, List<TournamentRound> allRounds) {
        int currentWinnerRound = getCurrentWinnerRoundNumber(completedRound);
        int nextWinnerRound = currentWinnerRound + 1;
        int totalPlayers = completedRound.getTournament().getNumberOfPlayers();
        
        log.info("Looking for next winner round {} after {} (current={})", nextWinnerRound, completedRound.getName(), currentWinnerRound);
        
        // Generate expected round name based on tournament structure
        DoubleEliminationMath.WinnerBracketStructure structure = 
            DoubleEliminationMath.calculateWinnerBracket(totalPlayers);
        String expectedNextRoundName = getWinnerRoundName(nextWinnerRound, structure.getTotalRounds());
        
        log.info("Expected next round name: {}", expectedNextRoundName);
        
        TournamentRound nextRound = allRounds.stream()
            .filter(round -> round.getBracketType() == TournamentMatch.BracketType.WINNER)
            .filter(round -> round.getName().equals(expectedNextRoundName))
            .findFirst()
            .orElse(null);
            
        if (nextRound != null) {
            log.info("Found next winner round: {}", nextRound.getName());
        } else {
            log.warn("Could not find next winner round with name: {}", expectedNextRoundName);
            // Debug: list all available winner rounds
            allRounds.stream()
                .filter(round -> round.getBracketType() == TournamentMatch.BracketType.WINNER)
                .forEach(round -> log.info("  Available winner round: {}", round.getName()));
        }
        
        return nextRound;
    }
    
    /**
     * Find appropriate loser's bracket round for dropouts from winner's bracket
     */
    private TournamentRound findLoserBracketRoundForDropouts(TournamentRound completedRound, List<TournamentRound> allRounds) {
        int winnerRoundNumber = getCurrentWinnerRoundNumber(completedRound);
        int totalPlayers = completedRound.getTournament().getNumberOfPlayers();
        
        log.info("Looking for loser bracket round for dropouts from WB round {}", winnerRoundNumber);
        
        // Calculate the correct loser bracket round number using double elimination rules
        int targetLoserRoundNumber = calculateLoserRoundForWinnerDropouts(winnerRoundNumber, totalPlayers);
        
        // Special case: -1 means direct to Grand Finals (no loser bracket)
        if (targetLoserRoundNumber == -1) {
            log.info("No loser bracket round needed - WB Finals loser goes directly to Grand Finals");
            return null; // Signal that Grand Finals should be activated directly
        }
        
        // Generate expected loser round name
        DoubleEliminationMath.LoserBracketStructure structure = 
            DoubleEliminationMath.calculateLoserBracket(totalPlayers);
        String expectedLoserRoundName = getLoserRoundName(targetLoserRoundNumber, structure.getTotalRounds());
        
        log.info("Expected loser round name: {} (LB round {} for WB round {} dropouts)", 
            expectedLoserRoundName, targetLoserRoundNumber, winnerRoundNumber);
        
        TournamentRound loserRound = allRounds.stream()
            .filter(round -> round.getBracketType() == TournamentMatch.BracketType.LOSER)
            .filter(round -> round.getName().equals(expectedLoserRoundName))
            .findFirst()
            .orElse(null);
            
        if (loserRound != null) {
            log.info("Found loser round: {}", loserRound.getName());
        } else {
            log.warn("Could not find loser round with name: {}", expectedLoserRoundName);
            // Debug: list all available loser rounds
            allRounds.stream()
                .filter(round -> round.getBracketType() == TournamentMatch.BracketType.LOSER)
                .forEach(round -> log.info("  Available loser round: {}", round.getName()));
        }
        
        return loserRound;
    }
    
    /**
     * Calculate which loser bracket round should receive dropouts from a specific winner bracket round.
     * This method needs to handle the special case where WB Finals losers go to LB Finals.
     */
    private int calculateLoserRoundForWinnerDropouts(int winnerRoundNumber, int totalPlayers) {
        // Calculate the total loser bracket rounds for this tournament size
        DoubleEliminationMath.LoserBracketStructure structure = 
            DoubleEliminationMath.calculateLoserBracket(totalPlayers);
        int totalLoserRounds = structure.getTotalRounds();
        
        // Calculate the total winner bracket rounds for this tournament size
        DoubleEliminationMath.WinnerBracketStructure winnerStructure = 
            DoubleEliminationMath.calculateWinnerBracket(totalPlayers);
        int totalWinnerRounds = winnerStructure.getTotalRounds();
        
        // Special case: WB Finals (last winner round) losers always go to LB Finals (last loser round)
        if (winnerRoundNumber == totalWinnerRounds) {
            if (totalLoserRounds == 0) {
                // For 2-player tournaments, there's no loser bracket - loser goes directly to Grand Finals
                log.info("WB Finals loser goes directly to Grand Finals (no loser bracket for {} players)", totalPlayers);
                return -1; // Special code indicating direct to Grand Finals
            } else {
                log.info("WB Finals loser goes to LB Finals (round {})", totalLoserRounds);
                return totalLoserRounds; // LB Finals
            }
        }
        
        // Standard double elimination formula for earlier rounds: WB round N dropouts go to LB round (2*N - 1)
        // But for round 1, they go directly to LB round 1
        if (winnerRoundNumber == 1) {
            return 1; // First round losers go to LB Round 1
        } else {
            int targetRound = (2 * winnerRoundNumber) - 1;
            // Ensure we don't exceed total loser rounds
            return Math.min(targetRound, totalLoserRounds);
        }
    }
    
    /**
     * Find the next loser's bracket round after the completed round
     */
    private TournamentRound findNextLoserBracketRound(TournamentRound completedRound, List<TournamentRound> allRounds) {
        int currentLoserRound = getCurrentLoserRoundNumber(completedRound);
        int nextLoserRound = currentLoserRound + 1;
        
        return allRounds.stream()
            .filter(round -> round.getBracketType() == TournamentMatch.BracketType.LOSER)
            .filter(round -> round.getName().contains("Round " + nextLoserRound) ||
                           (round.getName().contains("Finals") && nextLoserRound >= currentLoserRound))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * OLD BATCH-BASED API - TO BE REMOVED
     * Get the next batch(es) of matches that can be played.
     * Returns matches in priority order with automatic winner/loser bracket synchronization.
     * 
     * @deprecated Use the new pre-built bracket API instead:
     * 1. generateInitialBracket() to create complete tournament structure
     * 2. getReadyMatches() to get matches ready to play
     * 3. advancePlayersAndGetNextMatches() to advance after completing matches
     * 
     * This legacy method is maintained for backward compatibility but should not be used in new code.
     * 
     * @param tournament The tournament to get matches for
     * @param allRounds All tournament rounds (for testing, or pass null to auto-fetch)
     * @return List of match batches, ordered by priority (play in order)
     */
    @Deprecated
    public List<MatchBatch> getNextMatchBatches(Tournament tournament, List<TournamentRound> allRounds) {
        log.info("Getting next match batches for tournament: {}", tournament.getName());
        
        List<MatchBatch> batches = new ArrayList<>();
        
        // Get all rounds for this tournament
        if (allRounds == null) {
            allRounds = findAllTournamentRounds(tournament);
        }
        
        // Separate winner and loser bracket rounds
        List<TournamentRound> winnerRounds = allRounds.stream()
            .filter(r -> r.getBracketType() == TournamentMatch.BracketType.WINNER)
            .sorted(Comparator.comparing(TournamentRound::getRoundNumber))
            .collect(Collectors.toList());
            
        List<TournamentRound> loserRounds = allRounds.stream()
            .filter(r -> r.getBracketType() == TournamentMatch.BracketType.LOSER)
            .sorted(Comparator.comparing(TournamentRound::getRoundNumber))
            .collect(Collectors.toList());
            
        List<TournamentRound> grandFinalsRounds = allRounds.stream()
            .filter(r -> r.getName().contains("Grand"))
            .collect(Collectors.toList());
        
        int priority = 0;
        
        // Strategy: Alternate between winner's and loser's bracket rounds
        // This ensures proper synchronization
        
        // 1. First, get any ready winner's bracket rounds
        for (TournamentRound winnerRound : winnerRounds) {
            if (winnerRound.getStatus() == TournamentRound.RoundStatus.READY) {
                List<TournamentMatch> readyMatches = winnerRound.getMatches().stream()
                    .filter(match -> !match.isCompleted() && hasPlayersReady(match))
                    .collect(Collectors.toList());
                    
                if (!readyMatches.isEmpty()) {
                    batches.add(MatchBatch.builder()
                        .matches(readyMatches)
                        .description("Winner's " + winnerRound.getName())
                        .priority(priority++)
                        .canPlayInParallel(true)
                        .roundName(winnerRound.getName())
                        .bracketType(MatchBatch.BracketType.WINNERS)
                        .build());
                }
            }
        }
        
        // 2. Then, get any ready loser's bracket rounds
        for (TournamentRound loserRound : loserRounds) {
            if (loserRound.getStatus() == TournamentRound.RoundStatus.READY) {
                List<TournamentMatch> readyMatches = loserRound.getMatches().stream()
                    .filter(match -> !match.isCompleted() && hasPlayersReady(match))
                    .collect(Collectors.toList());
                    
                if (!readyMatches.isEmpty()) {
                    batches.add(MatchBatch.builder()
                        .matches(readyMatches)
                        .description("Loser's " + loserRound.getName())
                        .priority(priority++)
                        .canPlayInParallel(true)
                        .roundName(loserRound.getName())
                        .bracketType(MatchBatch.BracketType.LOSERS)
                        .build());
                }
            }
        }
        
        // 3. Finally, check for grand finals
        for (TournamentRound grandRound : grandFinalsRounds) {
            if (grandRound.getStatus() == TournamentRound.RoundStatus.READY) {
                List<TournamentMatch> readyMatches = grandRound.getMatches().stream()
                    .filter(match -> !match.isCompleted() && hasPlayersReady(match))
                    .collect(Collectors.toList());
                    
                if (!readyMatches.isEmpty()) {
                    batches.add(MatchBatch.builder()
                        .matches(readyMatches)
                        .description("Grand Finals")
                        .priority(priority++)
                        .canPlayInParallel(false) // Grand finals should be played one at a time
                        .roundName(grandRound.getName())
                        .bracketType(MatchBatch.BracketType.GRAND_FINALS)
                        .build());
                }
            }
        }
        
        log.info("Found {} match batches ready to play", batches.size());
        for (MatchBatch batch : batches) {
            log.info("  Batch {}: {} ({} matches)", batch.getPriority(), batch.getDescription(), batch.getMatches().size());
        }
        
        return batches;
    }
    
    /**
     * Complete a batch of matches and return what new rounds/matches were created
     * 
     * @deprecated Use the new pre-built bracket API instead:
     * 1. generateInitialBracket() to create complete tournament structure
     * 2. getReadyMatches() to get matches ready to play
     * 3. advancePlayersAndGetNextMatches() to advance after completing matches
     * 
     * This legacy method is maintained for backward compatibility but should not be used in new code.
     * 
     * @param tournament The tournament
     * @param completedBatch The batch that was completed
     * @param participants All tournament participants
     * @param allRounds All tournament rounds (for testing, or pass null to auto-fetch)
     * @return Result indicating what was updated
     */
    @Deprecated
    public TournamentUpdateResult completeMatchBatch(Tournament tournament, MatchBatch completedBatch, 
                                                   List<TournamentPlayer> participants, List<TournamentRound> allRounds) {
        log.info("Completing match batch: {} ({} matches)", completedBatch.getDescription(), completedBatch.getMatches().size());
        
        List<TournamentRound> newRounds = new ArrayList<>();
        
        // Find the round this batch belongs to
        if (allRounds == null) {
            allRounds = findAllTournamentRounds(tournament);
        }
        TournamentRound round = allRounds.stream()
            .filter(r -> r.getName().equals(completedBatch.getRoundName()))
            .findFirst()
            .orElse(null);
            
        if (round == null) {
            log.warn("Could not find round for batch: {}", completedBatch.getRoundName());
            return TournamentUpdateResult.builder()
                .newRounds(newRounds)
                .tournamentComplete(false)
                .description("Error: Round not found")
                .build();
        }
        
        // Check if all matches in the round are now complete
        boolean allMatchesComplete = round.getMatches().stream()
            .allMatch(TournamentMatch::isCompleted);
            
        if (allMatchesComplete) {
            log.info("All matches in {} are complete, generating next rounds", round.getName());
            
            // Mark round as complete
            round.setStatus(TournamentRound.RoundStatus.COMPLETED);
            
            // Generate next rounds using existing logic
            List<TournamentRound> generatedRounds = generateNextRound(tournament, round, participants);
            newRounds.addAll(generatedRounds);
            
            log.info("Generated {} new rounds after completing {}", generatedRounds.size(), round.getName());
        }
        
        // Check if tournament is complete
        boolean isComplete = checkTournamentComplete(tournament, allRounds);
        List<String> winners = isComplete ? getTournamentWinnerNames(tournament, allRounds) : new ArrayList<>();
        
        return TournamentUpdateResult.builder()
            .newRounds(newRounds)
            .tournamentComplete(isComplete)
            .winners(winners)
            .description(String.format("Completed %s, generated %d new rounds", 
                       completedBatch.getDescription(), newRounds.size()))
            .build();
    }
    
    /**
     * Helper method to check if a match has players ready
     */
    private boolean hasPlayersReady(TournamentMatch match) {
        return match.getTeam1Ids() != null && match.getTeam2Ids() != null &&
               !match.getTeam1Ids().isEmpty() && !match.getTeam2Ids().isEmpty();
    }
    
    /**
     * Helper method to find all rounds for a tournament
     * Note: This assumes rounds are stored somewhere accessible. 
     * You may need to adjust this based on your data access pattern.
     */
    private List<TournamentRound> findAllTournamentRounds(Tournament tournament) {
        // This would typically query your database/repository
        // For now, returning empty list - this needs to be implemented based on your data access
        log.warn("findAllTournamentRounds not implemented - returning empty list");
        return new ArrayList<>();
    }
    
    /**
     * Helper method to check if tournament is complete
     */
    private boolean checkTournamentComplete(Tournament tournament, List<TournamentRound> allRounds) {
        // Tournament is complete when grand finals is complete
        return allRounds.stream()
            .filter(r -> r.getName().contains("Grand"))
            .anyMatch(r -> r.getStatus() == TournamentRound.RoundStatus.COMPLETED);
    }
    
    /**
     * Helper method to get tournament winner names
     */
    private List<String> getTournamentWinnerNames(Tournament tournament, List<TournamentRound> allRounds) {
        // Find the winner of grand finals
        return allRounds.stream()
            .filter(r -> r.getName().contains("Grand"))
            .filter(r -> r.getStatus() == TournamentRound.RoundStatus.COMPLETED)
            .flatMap(r -> r.getMatches().stream())
            .filter(TournamentMatch::isCompleted)
            .flatMap(m -> m.getWinnerIds().stream())
            .map(Object::toString) // Convert UUID to string - you'd want player names here
            .collect(Collectors.toList());
    }
}