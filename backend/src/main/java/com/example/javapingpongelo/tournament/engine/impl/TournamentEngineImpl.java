package com.example.javapingpongelo.tournament.engine.impl;

import com.example.javapingpongelo.models.Tournament;
import com.example.javapingpongelo.models.TournamentMatch;
import com.example.javapingpongelo.models.TournamentPlayer;
import com.example.javapingpongelo.models.TournamentRound;
import com.example.javapingpongelo.repositories.TournamentMatchRepository;
import com.example.javapingpongelo.repositories.TournamentPlayerRepository;
import com.example.javapingpongelo.repositories.TournamentRepository;
import com.example.javapingpongelo.repositories.TournamentRoundRepository;
import com.example.javapingpongelo.tournament.engine.SeedingEngine;
import com.example.javapingpongelo.tournament.engine.TournamentEngine;
import com.example.javapingpongelo.tournament.engine.TournamentRulesEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main tournament engine implementation that orchestrates the tournament framework
 * Coordinates between rules engines, seeding engines, and match generation
 */
@Service
@Slf4j
public class TournamentEngineImpl implements TournamentEngine {
    
    @Autowired
    private TournamentRepository tournamentRepository;
    
    @Autowired
    private TournamentRoundRepository roundRepository;
    
    @Autowired
    private TournamentMatchRepository matchRepository;
    
    @Autowired
    private TournamentPlayerRepository playerRepository;
    
    // Injected rule engines - Spring will find all implementations
    @Autowired
    private List<TournamentRulesEngine> rulesEngines;
    
    // Injected seeding engines - Spring will find all implementations  
    @Autowired
    private List<SeedingEngine> seedingEngines;
    
    @Override
    @Transactional
    public Tournament initializeTournament(Tournament tournament) {
        log.info("Initializing tournament: {} with {} players", 
                tournament.getName(), tournament.getNumberOfPlayers());
        
        // Get the appropriate rules engine
        TournamentRulesEngine rulesEngine = getRulesEngine(tournament.getTournamentType());
        
        // Get the appropriate seeding engine
        SeedingEngine seedingEngine = getSeedingEngine(tournament.getSeedingMethod());
        
        // Validate tournament configuration
        rulesEngine.validateTournamentConfiguration(tournament);
        
        // Calculate total rounds needed
        int totalRounds = rulesEngine.calculateTotalRounds(tournament.getNumberOfPlayers());
        tournament.setTotalRounds(totalRounds);
        
        // Get tournament participants
        List<TournamentPlayer> participants = playerRepository.findByTournament(tournament);
        if (participants.size() != tournament.getNumberOfPlayers()) {
            throw new IllegalStateException(
                String.format("Expected %d participants but found %d", 
                    tournament.getNumberOfPlayers(), participants.size()));
        }
        
        // Seed the participants
        List<TournamentPlayer> seededParticipants = seedingEngine.seedParticipants(tournament, participants);
        
        // Save seeded participants
        playerRepository.saveAll(seededParticipants);
        
        // Generate initial bracket
        List<TournamentRound> initialRounds = rulesEngine.generateInitialBracket(tournament, seededParticipants);
        
        // Save rounds and matches
        for (TournamentRound round : initialRounds) {
            TournamentRound savedRound = roundRepository.save(round);
            for (TournamentMatch match : round.getMatches()) {
                match.setTournament(tournament);
                match.setTournamentRound(savedRound);  // Fix: Set the round relationship
                matchRepository.save(match);
            }
        }
        
        // Update tournament status - only set to READY_TO_START if not already IN_PROGRESS
        if (tournament.getStatus() != Tournament.TournamentStatus.IN_PROGRESS) {
            tournament.setStatus(Tournament.TournamentStatus.READY_TO_START);
        }
        tournament.setCurrentRound(1);
        tournament.setRoundReady(true);
        
        Tournament savedTournament = tournamentRepository.save(tournament);
        
        log.info("Successfully initialized tournament {} with {} rounds", 
                savedTournament.getName(), totalRounds);
        
        return savedTournament;
    }
    
    @Override
    @Transactional
    public Tournament startNextRound(UUID tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
            .orElseThrow(() -> new IllegalArgumentException("Tournament not found: " + tournamentId));
        
        log.info("Starting next round for tournament: {}", tournament.getName());
        
        // Check if current round is complete
        if (!isRoundComplete(tournamentId, tournament.getCurrentRound())) {
            throw new IllegalStateException("Current round is not yet complete");
        }
        
        // Get current round
        List<TournamentRound> currentRounds = roundRepository.findByTournamentAndStatus(
            tournament, TournamentRound.RoundStatus.READY);
        
        if (currentRounds.isEmpty()) {
            throw new IllegalStateException("No rounds ready to start");
        }
        
        // Start all ready rounds (may be multiple in double elimination)
        for (TournamentRound round : currentRounds) {
            round.setStatus(TournamentRound.RoundStatus.ACTIVE);
            round.setStartedAt(new Date());
            roundRepository.save(round);
        }
        
        // Update tournament status
        tournament.setStatus(Tournament.TournamentStatus.IN_PROGRESS);
        tournament.setRoundReady(false);
        
        Tournament savedTournament = tournamentRepository.save(tournament);
        
        log.info("Started round {} for tournament {}", tournament.getCurrentRound(), tournament.getName());
        
        return savedTournament;
    }
    
    @Override
    @Transactional
    public Tournament processMatchResult(UUID matchId, List<UUID> winnerTeamIds, List<UUID> loserTeamIds,
                                       Integer team1Score, Integer team2Score) {
        TournamentMatch match = matchRepository.findById(matchId)
            .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));
        
        Tournament tournament = match.getTournament();
        
        log.info("Processing match result for match {} in tournament {}", 
                match.getDisplayId(), tournament.getName());
        
        // Update match with results
        match.setWinnerIds(winnerTeamIds);
        match.setLoserIds(loserTeamIds);
        match.setTeam1Score(team1Score);
        match.setTeam2Score(team2Score);
        match.setCompleted(true);
        
        TournamentMatch savedMatch = matchRepository.save(match);
        log.info("Match {} saved with completed status: {}", savedMatch.getDisplayId(), savedMatch.isCompleted());
        
        // Check if this completes the current round
        boolean roundComplete = isRoundComplete(tournament.getId(), match.getRound());
        log.info("Is round {} complete? {}", match.getRound(), roundComplete);
        
        if (roundComplete) {
            log.info("Round {} is complete, handling round completion", match.getRound());
            return handleRoundCompletion(tournament, match.getRound());
        } else {
            log.info("Round {} is not complete yet, tournament remains in current state", match.getRound());
        }
        
        return tournament;
    }
    
    @Override
    @Transactional
    public Tournament handleParticipantDropout(UUID tournamentId, UUID participantId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
            .orElseThrow(() -> new IllegalArgumentException("Tournament not found: " + tournamentId));
        
        log.warn("Handling participant dropout: {} from tournament: {}", 
                participantId, tournament.getName());
        
        TournamentRulesEngine rulesEngine = getRulesEngine(tournament.getTournamentType());
        
        // Let the rules engine handle the dropout logic
        List<TournamentMatch> updatedMatches = rulesEngine.handleParticipantDropout(tournament, participantId);
        
        // Save updated matches
        matchRepository.saveAll(updatedMatches);
        
        // Mark participant as eliminated
        List<TournamentPlayer> participants = playerRepository.findByTournament(tournament);
        participants.stream()
            .filter(p -> p.getPlayerId().equals(participantId))
            .findFirst()
            .ifPresent(p -> {
                p.setEliminated(true);
                p.setEliminatedInRound(tournament.getCurrentRound());
                playerRepository.save(p);
            });
        
        log.info("Processed dropout for participant {} in tournament {}", 
                participantId, tournament.getName());
        
        return tournament;
    }
    
    @Override
    public List<TournamentPlayer> getCurrentStandings(UUID tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
            .orElseThrow(() -> new IllegalArgumentException("Tournament not found: " + tournamentId));
        
        List<TournamentPlayer> participants = playerRepository.findByTournament(tournament);
        
        // Sort by elimination status and round, then by original seed
        return participants.stream()
            .sorted((p1, p2) -> {
                // Non-eliminated players first
                if (p1.isEliminated() != p2.isEliminated()) {
                    return Boolean.compare(p1.isEliminated(), p2.isEliminated());
                }
                
                // If both eliminated, sort by elimination round (later is better)
                if (p1.isEliminated() && p2.isEliminated()) {
                    int roundCompare = Integer.compare(
                        p2.getEliminatedInRound() != null ? p2.getEliminatedInRound() : 0,
                        p1.getEliminatedInRound() != null ? p1.getEliminatedInRound() : 0
                    );
                    if (roundCompare != 0) return roundCompare;
                }
                
                // Finally sort by original seed
                return Integer.compare(p1.getSeed(), p2.getSeed());
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public List<TournamentMatch> getUpcomingMatches(UUID tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
            .orElseThrow(() -> new IllegalArgumentException("Tournament not found: " + tournamentId));
        
        // Get active rounds
        List<TournamentRound> activeRounds = roundRepository.findActiveRounds(tournament);
        
        // Get all playable matches from active rounds
        return activeRounds.stream()
            .flatMap(round -> round.getMatches().stream())
            .filter(TournamentMatch::isPlayable)
            .filter(match -> !match.isCompleted())
            .sorted((m1, m2) -> {
                int roundCompare = Integer.compare(m1.getRound(), m2.getRound());
                if (roundCompare != 0) return roundCompare;
                return Integer.compare(m1.getPositionInRound(), m2.getPositionInRound());
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public List<TournamentMatch> getMatchesForRound(UUID tournamentId, int roundNumber) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
            .orElseThrow(() -> new IllegalArgumentException("Tournament not found: " + tournamentId));
        
        return matchRepository.findByTournamentAndRoundOrderByPositionInRound(tournament, roundNumber);
    }
    
    @Override
    public boolean isRoundComplete(UUID tournamentId, int roundNumber) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
            .orElseThrow(() -> new IllegalArgumentException("Tournament not found: " + tournamentId));
        
        List<TournamentMatch> roundMatches = getMatchesForRound(tournamentId, roundNumber);
        
        log.info("Checking round {} completion: found {} matches", roundNumber, roundMatches.size());
        for (TournamentMatch match : roundMatches) {
            log.info("  Match {}: completed={}, isBye={}, hasAllTeams={}", 
                match.getDisplayId(), match.isCompleted(), match.isBye(), match.hasAllTeams());
        }
        
        // Only consider matches that either:
        // 1. Are bye matches (automatically completed)
        // 2. Are regular matches that have been completed 
        // But if a match has both teams assigned, it should not be considered a bye regardless of the flag
        boolean allPlayableMatchesCompleted = roundMatches.stream().allMatch(match -> {
            // If match has both teams, it's a real match and must be completed
            if (match.hasAllTeams()) {
                return match.isCompleted();
            }
            // If it's a true bye (only one team or no teams), it should be marked as completed
            return match.isBye() && match.isCompleted();
        });
        
        log.info("All playable matches completed: {}", allPlayableMatchesCompleted);
        return allPlayableMatchesCompleted;
    }
    
    @Override
    public TournamentBracketData generateBracketData(UUID tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
            .orElseThrow(() -> new IllegalArgumentException("Tournament not found: " + tournamentId));
        
        List<TournamentRound> allRounds = roundRepository.findByTournamentOrderByRoundNumber(tournament);
        
        List<TournamentRound> winnerRounds = allRounds.stream()
            .filter(r -> r.getBracketType() == TournamentMatch.BracketType.WINNER)
            .collect(Collectors.toList());
            
        List<TournamentRound> loserRounds = allRounds.stream()
            .filter(r -> r.getBracketType() == TournamentMatch.BracketType.LOSER)
            .collect(Collectors.toList());
            
        List<TournamentMatch> finalMatches = allRounds.stream()
            .filter(r -> r.getBracketType() == TournamentMatch.BracketType.FINAL ||
                        r.getBracketType() == TournamentMatch.BracketType.GRAND_FINAL ||
                        r.getBracketType() == TournamentMatch.BracketType.GRAND_FINAL_RESET)
            .flatMap(r -> r.getMatches().stream())
            .collect(Collectors.toList());
        
        return new TournamentBracketData(winnerRounds, loserRounds, finalMatches);
    }
    
    // Private helper methods
    
    private TournamentRulesEngine getRulesEngine(Tournament.TournamentType tournamentType) {
        return rulesEngines.stream()
            .filter(engine -> engine.getSupportedTournamentType() == tournamentType)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No rules engine found for tournament type: " + tournamentType));
    }
    
    private SeedingEngine getSeedingEngine(Tournament.SeedingMethod seedingMethod) {
        return seedingEngines.stream()
            .filter(engine -> engine.getSupportedSeedingMethod() == seedingMethod)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No seeding engine found for seeding method: " + seedingMethod));
    }
    
    private Tournament handleRoundCompletion(Tournament tournament, int completedRoundNumber) {
        log.info("Round {} completed for tournament {}", completedRoundNumber, tournament.getName());
        
        // Mark completed rounds as complete
        List<TournamentRound> completedRounds = roundRepository.findByTournamentAndRoundNumberAndStatus(
            tournament, completedRoundNumber, TournamentRound.RoundStatus.ACTIVE);
        
        for (TournamentRound round : completedRounds) {
            round.setStatus(TournamentRound.RoundStatus.COMPLETED);
            round.setCompletedAt(new Date());
            roundRepository.save(round);
        }
        
        // Check if tournament is complete
        TournamentRulesEngine rulesEngine = getRulesEngine(tournament.getTournamentType());
        
        if (rulesEngine.isTournamentComplete(tournament)) {
            return completeTournament(tournament, rulesEngine);
        }
        
        // Generate next round
        return generateNextRound(tournament, completedRounds.get(0));
    }
    
    private Tournament generateNextRound(Tournament tournament, TournamentRound completedRound) {
        TournamentRulesEngine rulesEngine = getRulesEngine(tournament.getTournamentType());
        SeedingEngine seedingEngine = getSeedingEngine(tournament.getSeedingMethod());
        
        // Get remaining participants
        List<TournamentPlayer> remainingParticipants = playerRepository.findByTournamentAndEliminated(
            tournament, false);
        
        // Re-seed if enabled
        if (tournament.isEnableReseeding()) {
            remainingParticipants = seedingEngine.reseedParticipants(
                tournament, remainingParticipants, completedRound.getRoundNumber());
            playerRepository.saveAll(remainingParticipants);
        }
        
        // Generate next round
        List<TournamentRound> nextRounds = rulesEngine.generateNextRound(
            tournament, completedRound, remainingParticipants);
        
        // Save new rounds and matches
        for (TournamentRound round : nextRounds) {
            TournamentRound savedRound = roundRepository.save(round);
            for (TournamentMatch match : round.getMatches()) {
                match.setTournament(tournament);
                match.setTournamentRound(savedRound);  // Fix: Set the round relationship
                matchRepository.save(match);
            }
        }
        
        // Update tournament state
        tournament.setCurrentRound(tournament.getCurrentRound() + 1);
        tournament.setStatus(Tournament.TournamentStatus.ROUND_COMPLETE);
        tournament.setRoundReady(true);
        
        return tournamentRepository.save(tournament);
    }
    
    private Tournament completeTournament(Tournament tournament, TournamentRulesEngine rulesEngine) {
        log.info("Tournament {} is complete!", tournament.getName());
        
        // Get winners and runners-up
        List<UUID> winners = rulesEngine.getTournamentWinners(tournament);
        List<UUID> runnersUp = rulesEngine.getTournamentRunnersUp(tournament);
        
        // Update tournament with results
        tournament.setStatus(Tournament.TournamentStatus.COMPLETED);
        tournament.setEndDate(new Date());
        
        if (!winners.isEmpty()) {
            tournament.setChampionId(winners.get(0));
        }
        
        if (!runnersUp.isEmpty()) {
            tournament.setRunnerUpId(runnersUp.get(0));
        }
        
        return tournamentRepository.save(tournament);
    }
}