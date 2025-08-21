package com.example.javapingpongelo.tournament.engine.impl;

import com.example.javapingpongelo.models.Tournament;
import com.example.javapingpongelo.models.TournamentMatch;
import com.example.javapingpongelo.models.TournamentPlayer;
import com.example.javapingpongelo.models.TournamentRound;
import com.example.javapingpongelo.tournament.engine.TournamentRulesEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.example.javapingpongelo.repositories.TournamentRoundRepository;
import com.example.javapingpongelo.repositories.TournamentMatchRepository;

import java.util.*;

/**
 * Tournament rules engine for Single Elimination tournaments
 * 
 * Single elimination logic:
 * - Participants are eliminated after one loss
 * - Tournament size = n participants → log2(n) rounds (rounded up)
 * - Byes are given in first round if n is not a power of 2
 * - Higher seeds get byes when possible
 */
@Component
@Slf4j
public class SingleEliminationRules implements TournamentRulesEngine {
    
    @Autowired
    private TournamentRoundRepository roundRepository;
    
    @Autowired
    private TournamentMatchRepository matchRepository;
    
    @Override
    public Tournament.TournamentType getSupportedTournamentType() {
        return Tournament.TournamentType.SINGLE_ELIMINATION;
    }
    
    @Override
    public int calculateTotalRounds(int participantCount) {
        if (participantCount < 2) {
            throw new IllegalArgumentException("Tournament must have at least 2 participants");
        }
        // log2(n) rounded up gives us the number of rounds needed
        return (int) Math.ceil(Math.log(participantCount) / Math.log(2));
    }
    
    @Override
    public List<TournamentRound> generateInitialBracket(Tournament tournament, List<TournamentPlayer> participants) {
        validateTournamentConfiguration(tournament);
        
        if (participants.size() < 2) {
            throw new IllegalArgumentException("Cannot create bracket with less than 2 participants");
        }
        
        log.info("Generating initial single elimination bracket for {} participants", participants.size());
        
        List<TournamentRound> rounds = new ArrayList<>();
        
        // Create first round
        TournamentRound firstRound = createFirstRound(tournament, participants);
        rounds.add(firstRound);
        
        return rounds;
    }
    
    private TournamentRound createFirstRound(Tournament tournament, List<TournamentPlayer> participants) {
        TournamentRound round = TournamentRound.builder()
                .tournament(tournament)
                .roundNumber(1)
                .bracketType(TournamentMatch.BracketType.WINNER)
                .name(getRoundDisplayName(1, TournamentMatch.BracketType.WINNER, participants.size()))
                .status(TournamentRound.RoundStatus.READY)
                .matches(new ArrayList<>())
                .build();
        
        // Calculate byes needed
        int nextPowerOf2 = getNextPowerOf2(participants.size());
        int byesNeeded = nextPowerOf2 - participants.size();
        
        log.info("Creating first round: {} participants, {} byes needed", participants.size(), byesNeeded);
        
        List<TournamentMatch> matches = new ArrayList<>();
        int matchPosition = 1;
        int participantIndex = 0;
        
        // Give byes to top seeds first
        for (int i = 0; i < byesNeeded; i++) {
            TournamentPlayer player = participants.get(participantIndex++);
            TournamentMatch byeMatch = createByeMatch(round, matchPosition++, player, 1);
            matches.add(byeMatch);
        }
        
        // Create regular matches for remaining participants
        while (participantIndex < participants.size()) {
            TournamentPlayer player1 = participants.get(participantIndex++);
            TournamentPlayer player2 = participantIndex < participants.size() ? 
                participants.get(participantIndex++) : null;
            
            if (player2 != null) {
                TournamentMatch match = createRegularMatch(round, matchPosition++, player1, player2);
                matches.add(match);
            }
        }
        
        round.setMatches(matches);
        return round;
    }
    
    @Override
    public List<TournamentRound> generateNextRound(Tournament tournament, TournamentRound completedRound, 
                                                  List<TournamentPlayer> participants) {
        
        // Get winners from completed round
        List<TournamentPlayer> winners = getWinnersFromRound(completedRound, participants);
        
        if (winners.size() <= 1) {
            // Tournament is complete
            return new ArrayList<>();
        }
        
        log.info("Generating next round: {} winners advance", winners.size());
        
        int nextRoundNumber = completedRound.getRoundNumber() + 1;
        boolean isFinalRound = winners.size() == 2;
        
        TournamentRound nextRound = TournamentRound.builder()
                .tournament(tournament)
                .roundNumber(nextRoundNumber)
                .bracketType(isFinalRound ? TournamentMatch.BracketType.FINAL : TournamentMatch.BracketType.WINNER)
                .name(getRoundDisplayName(nextRoundNumber, 
                    isFinalRound ? TournamentMatch.BracketType.FINAL : TournamentMatch.BracketType.WINNER, 
                    tournament.getNumberOfPlayers()))
                .status(TournamentRound.RoundStatus.READY)
                .matches(new ArrayList<>())
                .build();
        
        // Pair up winners for next round
        List<TournamentMatch> matches = new ArrayList<>();
        int matchPosition = 1;
        
        for (int i = 0; i < winners.size(); i += 2) {
            TournamentPlayer player1 = winners.get(i);
            TournamentPlayer player2 = i + 1 < winners.size() ? winners.get(i + 1) : null;
            
            if (player2 != null) {
                TournamentMatch match = createRegularMatch(nextRound, matchPosition++, player1, player2);
                matches.add(match);
            } else {
                // Odd number of winners - give bye to last player
                TournamentMatch byeMatch = createByeMatch(nextRound, matchPosition++, player1, 1);
                matches.add(byeMatch);
            }
        }
        
        nextRound.setMatches(matches);
        return new ArrayList<>(Arrays.asList(nextRound));
    }
    
    @Override
    public List<TournamentMatch> handleParticipantDropout(Tournament tournament, UUID droppedParticipantId) {
        // For single elimination, dropping out means automatic loss
        // Implementation would mark future matches as byes for opponents
        // This is a complex operation that would need to traverse the bracket
        log.warn("Participant dropout handling not yet implemented for single elimination");
        return new ArrayList<>();
    }
    
    @Override
    public boolean isTournamentComplete(Tournament tournament) {
        // Tournament is complete when we have a final match that's been played
        List<TournamentRound> rounds = roundRepository.findByTournamentOrderByRoundNumber(tournament);
        
        if (rounds.isEmpty()) {
            return false;
        }
        
        // Find the final round (highest round number)
        TournamentRound finalRound = rounds.get(rounds.size() - 1);
        
        // Check if final round has a completed match with a winner
        return finalRound.getBracketType() == TournamentMatch.BracketType.FINAL && 
               finalRound.getMatches().stream()
                   .anyMatch(match -> match.isCompleted() && 
                           match.getWinnerIds() != null && 
                           !match.getWinnerIds().isEmpty());
    }
    
    @Override
    public List<UUID> getTournamentWinners(Tournament tournament) {
        if (!isTournamentComplete(tournament)) {
            return new ArrayList<>();
        }
        
        List<TournamentRound> rounds = roundRepository.findByTournamentOrderByRoundNumber(tournament);
        TournamentRound finalRound = rounds.get(rounds.size() - 1);
        
        // Find the completed final match and return its winners
        return finalRound.getMatches().stream()
            .filter(match -> match.isCompleted() && match.getBracketType() == TournamentMatch.BracketType.FINAL)
            .findFirst()
            .map(TournamentMatch::getWinnerIds)
            .orElse(new ArrayList<>());
    }
    
    @Override
    public List<UUID> getTournamentRunnersUp(Tournament tournament) {
        if (!isTournamentComplete(tournament)) {
            return new ArrayList<>();
        }
        
        List<TournamentRound> rounds = roundRepository.findByTournamentOrderByRoundNumber(tournament);
        TournamentRound finalRound = rounds.get(rounds.size() - 1);
        
        // Find the completed final match and return its losers
        return finalRound.getMatches().stream()
            .filter(match -> match.isCompleted() && match.getBracketType() == TournamentMatch.BracketType.FINAL)
            .findFirst()
            .map(TournamentMatch::getLoserIds)
            .orElse(new ArrayList<>());
    }
    
    @Override
    public void validateTournamentConfiguration(Tournament tournament) {
        if (tournament.getTournamentType() != Tournament.TournamentType.SINGLE_ELIMINATION) {
            throw new IllegalArgumentException("Tournament type must be SINGLE_ELIMINATION");
        }
        
        if (tournament.getNumberOfPlayers() < 2) {
            throw new IllegalArgumentException("Single elimination tournament must have at least 2 players");
        }
        
        if (tournament.getNumberOfPlayers() > 16) {
            throw new IllegalArgumentException("Maximum 16 players supported in current implementation");
        }
    }
    
    @Override
    public String getRoundDisplayName(int roundNumber, TournamentMatch.BracketType bracketType, int participantCount) {
        if (bracketType == TournamentMatch.BracketType.FINAL) {
            return "Finals";
        }
        
        int totalRounds = calculateTotalRounds(participantCount);
        
        // Work backwards from finals to determine round names
        if (roundNumber == totalRounds - 1) {
            return "Semifinals";
        } else if (roundNumber == totalRounds - 2) {
            return "Quarterfinals";
        } else {
            return "Round " + roundNumber;
        }
    }
    
    // Helper methods
    
    private int getNextPowerOf2(int n) {
        return (int) Math.pow(2, Math.ceil(Math.log(n) / Math.log(2)));
    }
    
    private TournamentMatch createByeMatch(TournamentRound round, int position, TournamentPlayer player, int team) {
        TournamentMatch match = TournamentMatch.builder()
                .displayId(generateMatchDisplayId(round, position))
                .tournament(round.getTournament())
                .round(round.getRoundNumber())
                .positionInRound(position)
                .bracketType(round.getBracketType())
                .isBye(true)
                .byeTeam(team)
                .completed(true) // Byes are automatically completed
                .build();
        
        if (team == 1) {
            match.setTeam1Ids(new ArrayList<>(Arrays.asList(player.getPlayerId())));
            match.setWinnerIds(new ArrayList<>(Arrays.asList(player.getPlayerId())));
        } else {
            match.setTeam2Ids(new ArrayList<>(Arrays.asList(player.getPlayerId())));
            match.setWinnerIds(new ArrayList<>(Arrays.asList(player.getPlayerId())));
        }
        
        return match;
    }
    
    private TournamentMatch createRegularMatch(TournamentRound round, int position, 
                                             TournamentPlayer player1, TournamentPlayer player2) {
        return TournamentMatch.builder()
                .displayId(generateMatchDisplayId(round, position))
                .tournament(round.getTournament())
                .round(round.getRoundNumber())
                .positionInRound(position)
                .bracketType(round.getBracketType())
                .team1Ids(new ArrayList<>(Arrays.asList(player1.getPlayerId())))
                .team2Ids(new ArrayList<>(Arrays.asList(player2.getPlayerId())))
                .team1Seed(player1.getSeed())
                .team2Seed(player2.getSeed())
                .isBye(false)
                .completed(false)
                .build();
    }
    
    private String generateMatchDisplayId(TournamentRound round, int position) {
        return String.format("R%d-M%d", round.getRoundNumber(), position);
    }
    
    private List<TournamentPlayer> getWinnersFromRound(TournamentRound round, List<TournamentPlayer> allParticipants) {
        List<TournamentPlayer> winners = new ArrayList<>();
        
        for (TournamentMatch match : round.getMatches()) {
            if (match.isCompleted() && match.getWinnerIds() != null && !match.getWinnerIds().isEmpty()) {
                // Find the tournament players that correspond to the winner IDs
                for (UUID winnerId : match.getWinnerIds()) {
                    allParticipants.stream()
                        .filter(p -> p.getPlayerId().equals(winnerId))
                        .findFirst()
                        .ifPresent(winners::add);
                }
            }
        }
        
        log.info("Extracted {} winners from round {}", winners.size(), round.getRoundNumber());
        return winners;
    }
}