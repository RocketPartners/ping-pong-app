package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Match;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.exceptions.BadRequestException;
import com.example.javapingpongelo.models.exceptions.ResourceNotFoundException;
import com.example.javapingpongelo.repositories.GameRepository;
import com.example.javapingpongelo.repositories.MatchRepository;
import com.example.javapingpongelo.services.achievements.IAchievementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class MatchService {

    @Autowired
    MatchRepository matchRepository;

    @Autowired
    IPlayerService playerService;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    EloService eloService;

    @Autowired
    GameService gameService;

    @Autowired
    IAchievementService achievementService;

    public Match save(Match match) {
        if (match != null) {
            try {
                matchRepository.save(match);
            }
            catch (Exception e) {
                log.error("Error saving match: {}", match.getMatchId(), e);
            }
        }
        return match;
    }

    @Transactional
    public void updateMatchAndPlayers(Match match) {
        log.info("Updating match and player ratings: {}", match.getMatchId());

        try {
            // Validate that this is a properly concluded match
            validateMatchConclusion(match);
            // Update match history
            Player challenger = playerService.findPlayerById(match.getChallengerId());
            Player opponent = playerService.findPlayerById(match.getOpponentId());

            updatePlayerMatchHistory(challenger, match);
            updatePlayerMatchHistory(opponent, match);

            // For doubles matches, update partners' histories too
            if (!match.isSinglesGame()) {
                for (UUID playerId : match.getChallengerTeam()) {
                    if (!playerId.equals(match.getChallengerId())) {
                        Player partner = playerService.findPlayerById(playerId);
                        if (partner != null) {
                            updatePlayerMatchHistory(partner, match);
                        }
                    }
                }

                for (UUID playerId : match.getOpponentTeam()) {
                    if (!playerId.equals(match.getOpponentId())) {
                        Player partner = playerService.findPlayerById(playerId);
                        if (partner != null) {
                            updatePlayerMatchHistory(partner, match);
                        }
                    }
                }
            }

            // Save the match first
            matchRepository.save(match);

            // Process games associated with this match if any
            processMatchGames(match);

            // Update player ratings based on match result
            updatePlayerRatings(match);

            log.info("Successfully updated match and player ratings: {}", match.getMatchId());
        }
        catch (Exception e) {
            log.error("Error updating match and player ratings: {}", match.getMatchId(), e);
            throw new RuntimeException("Failed to update match and player ratings", e);
        }
    }

    /**
     * Validate that a match is properly concluded
     */
    private void validateMatchConclusion(Match match) {
        // Check that either challenger or opponent has won
        if (!match.isChallengerWin() && !match.isOpponentWin()) {
            throw new BadRequestException("Match must have a winner");
        }

        // Check that scores are consistent with the result
        if (match.isChallengerWin() && match.getChallengerTeamGameScore() <= match.getOpponentTeamGameScore()) {
            throw new BadRequestException("Match result inconsistent with scores");
        }

        if (match.isOpponentWin() && match.getOpponentTeamGameScore() <= match.getChallengerTeamGameScore()) {
            throw new BadRequestException("Match result inconsistent with scores");
        }

        // Additional validations as needed
    }

    /**
     * Update a player's match history with a new match
     */
    @Transactional
    protected void updatePlayerMatchHistory(Player player, Match match) {
        if (player.getMatchHistory() == null) {
            player.setMatchHistory(new ArrayList<>());
        }

        // Instead of adding just the UUID, add the Match entity to the relationship
        if (!player.getMatchHistory().contains(match)) {
            player.getMatchHistory().add(match);
            playerService.savePlayer(player);
        }
    }

    /**
     * Process games associated with the match
     */
    private void processMatchGames(Match match) {
        if (match.getGameIds() != null && !match.getGameIds().isEmpty()) {
            // Process each game in the match
            for (UUID gameId : match.getGameIds()) {
                try {
                    Game game = gameService.findById(gameId);

                    if (game != null) {
                        // Update game with match results if needed
                        game.setMatchId(match.getMatchId());
                        gameRepository.save(game);
                    }
                }
                catch (Exception e) {
                    log.warn("Error processing game {} for match {}", gameId, match.getMatchId(), e);
                    // Continue processing other games
                }
            }
        }
    }

    /**
     * Update player ratings based on match result
     */
    private void updatePlayerRatings(Match match) {
        // Fetch players
        Player challenger = playerService.findPlayerById(match.getChallengerId());
        Player opponent = playerService.findPlayerById(match.getOpponentId());

        if (challenger == null || opponent == null) {
            throw new ResourceNotFoundException("One or both players not found");
        }

        // Calculate new ratings based on match type and result
        if (match.isSinglesGame()) {
            updateSinglesRatings(match, challenger, opponent);
        }
        else if (match.isDoublesGame()) {
            updateDoublesRatings(match);
        }
    }

    /**
     * Update ratings for singles matches
     */
    private void updateSinglesRatings(Match match, Player challenger, Player opponent) {
        // Calculate rating changes
        double challengerNewRating, opponentNewRating;

        if (match.isRatedGame()) {
            // Rated game - update ranked ratings
            if (match.isChallengerWin()) {
                challengerNewRating = eloService.newSinglesEloRankedRatingForWinner(
                        challenger, opponent, match.getChallengerTeamTotalPoints(), match.getOpponentTeamTotalPoints());
                opponentNewRating = eloService.newSinglesRankedEloRatingForLoser(
                        opponent, challenger, match.getOpponentTeamTotalPoints(), match.getChallengerTeamTotalPoints());

                // Update wins/losses
                challenger.setSinglesRankedWins(challenger.getSinglesRankedWins() + 1);
                opponent.setSinglesRankedLoses(opponent.getSinglesRankedLoses() + 1);
            }
            else {
                challengerNewRating = eloService.newSinglesRankedEloRatingForLoser(
                        challenger, opponent, match.getChallengerTeamTotalPoints(), match.getOpponentTeamTotalPoints());
                opponentNewRating = eloService.newSinglesEloRankedRatingForWinner(
                        opponent, challenger, match.getOpponentTeamTotalPoints(), match.getChallengerTeamTotalPoints());

                // Update wins/losses
                challenger.setSinglesRankedLoses(challenger.getSinglesRankedLoses() + 1);
                opponent.setSinglesRankedWins(opponent.getSinglesRankedWins() + 1);
            }

            // Set new ratings
            challenger.setSinglesRankedRating((int) Math.round(challengerNewRating));
            opponent.setSinglesRankedRating((int) Math.round(opponentNewRating));
        }
        else {
            // Normal game - update normal ratings
            if (match.isChallengerWin()) {
                challengerNewRating = eloService.newSinglesEloNormalRatingForWinner(
                        challenger, opponent, match.getChallengerTeamTotalPoints(), match.getOpponentTeamTotalPoints());
                opponentNewRating = eloService.newSinglesNormalEloRatingForLoser(
                        opponent, challenger, match.getOpponentTeamTotalPoints(), match.getChallengerTeamTotalPoints());

                // Update wins/losses
                challenger.setSinglesNormalWins(challenger.getSinglesNormalWins() + 1);
                opponent.setSinglesNormalLoses(opponent.getSinglesNormalLoses() + 1);
            }
            else {
                challengerNewRating = eloService.newSinglesNormalEloRatingForLoser(
                        challenger, opponent, match.getChallengerTeamTotalPoints(), match.getOpponentTeamTotalPoints());
                opponentNewRating = eloService.newSinglesEloNormalRatingForWinner(
                        opponent, challenger, match.getOpponentTeamTotalPoints(), match.getChallengerTeamTotalPoints());

                // Update wins/losses
                challenger.setSinglesNormalLoses(challenger.getSinglesNormalLoses() + 1);
                opponent.setSinglesNormalWins(opponent.getSinglesNormalWins() + 1);
            }

            // Set new ratings
            challenger.setSinglesNormalRating((int) Math.round(challengerNewRating));
            opponent.setSinglesNormalRating((int) Math.round(opponentNewRating));
        }

        // Update match history
        updatePlayerMatchHistory(challenger, match);
        updatePlayerMatchHistory(opponent, match);

        // Save updated players
        playerService.savePlayer(challenger);
        playerService.savePlayer(opponent);
    }

    /**
     * Update ratings for doubles matches
     */
    private void updateDoublesRatings(Match match) {
        // Fetch all players involved
        List<Player> players = new ArrayList<>();

        // Challenger team
        for (UUID playerId : match.getChallengerTeam()) {
            players.add(playerService.findPlayerById(playerId));
        }

        // Opponent team
        for (UUID playerId : match.getOpponentTeam()) {
            players.add(playerService.findPlayerById(playerId));
        }

        // Check that we have 4 players for doubles
        if (players.size() != 4) {
            throw new BadRequestException("Doubles match requires exactly 4 players");
        }

        // Create a game object to use with EloService
        Game game = new Game();
        game.setChallengerTeamScore(match.getChallengerTeamTotalPoints());
        game.setOpponentTeamScore(match.getOpponentTeamTotalPoints());
        game.setChallengerTeamWin(match.isChallengerTeamWin());
        game.setOpponentTeamWin(match.isOpponentTeamWin());

        List<Double> newRatings;

        // Determine which players are on the winning team
        List<Player> winningTeam = new ArrayList<>();
        List<Player> losingTeam = new ArrayList<>();

        if (match.isChallengerTeamWin()) {
            winningTeam.add(players.get(0));
            winningTeam.add(players.get(1));
            losingTeam.add(players.get(2));
            losingTeam.add(players.get(3));
        }
        else {
            winningTeam.add(players.get(2));
            winningTeam.add(players.get(3));
            losingTeam.add(players.get(0));
            losingTeam.add(players.get(1));
        }

        // Calculate new ratings
        if (match.isRatedGame()) {
            // Rated game - update ranked ratings
            newRatings = eloService.newDoublesRankedEloRatingForEachPlayer(
                    game, winningTeam.get(0), winningTeam.get(1), losingTeam.get(0), losingTeam.get(1));

            // Update wins/losses and ratings
            for (int i = 0; i < 2; i++) {
                Player winner = winningTeam.get(i);
                winner.setDoublesRankedWins(winner.getDoublesRankedWins() + 1);
                winner.setDoublesRankedRating((int) Math.round(newRatings.get(i)));

                Player loser = losingTeam.get(i);
                loser.setDoublesRankedLoses(loser.getDoublesRankedLoses() + 1);
                loser.setDoublesRankedRating((int) Math.round(newRatings.get(i + 2)));
            }
        }
        else {
            // Normal game - update normal ratings
            newRatings = eloService.newDoublesNormalEloRatingForEachPlayer(
                    game, winningTeam.get(0), winningTeam.get(1), losingTeam.get(0), losingTeam.get(1));

            // Update wins/losses and ratings
            for (int i = 0; i < 2; i++) {
                Player winner = winningTeam.get(i);
                winner.setDoublesNormalWins(winner.getDoublesNormalWins() + 1);
                winner.setDoublesNormalRating((int) Math.round(newRatings.get(i)));

                Player loser = losingTeam.get(i);
                loser.setDoublesNormalLoses(loser.getDoublesNormalLoses() + 1);
                loser.setDoublesNormalRating((int) Math.round(newRatings.get(i + 2)));
            }
        }

        // Update match history and save all players
        for (Player player : players) {
            updatePlayerMatchHistory(player, match);
            playerService.savePlayer(player);
        }
    }

    /**
     * Delete a match by ID
     *
     * @param id The UUID of the match to delete
     */
    @Transactional
    public void deleteMatch(UUID id) {
        log.info("Deleting match with ID: {}", id);

        if (id == null) {
            log.warn("Attempted to delete match with null ID");
            throw new IllegalArgumentException("Match ID cannot be null");
        }

        try {
            // Find the match
            Match match = findById(id);
            if (match == null) {
                log.warn("Attempted to delete non-existent match: {}", id);
                throw new ResourceNotFoundException("Match not found with id: " + id);
            }

            // Option 1: Delete the match and any associated games
            // This is a simpler approach but may leave player ratings affected by this match

            // Delete any associated games
            if (match.getGameIds() != null) {
                for (UUID gameId : match.getGameIds()) {
                    try {
                        gameService.deleteGame(gameId);
                    }
                    catch (Exception e) {
                        log.warn("Error deleting game {} for match {}", gameId, id, e);
                        // Continue with other games
                    }
                }
            }

            // Option 2: Reverse the effect on player ratings
            // This is more complex but maintains data integrity
            // Implement if needed

            // Remove match from player match histories
            removeMatchFromPlayerHistories(match);

            // Delete the match
            matchRepository.deleteById(id);
            log.info("Successfully deleted match with ID: {}", id);
        }
        catch (ResourceNotFoundException e) {
            // Re-throw this exception so it can be handled correctly
            throw e;
        }
        catch (Exception e) {
            log.error("Error deleting match with ID: {}", id, e);
            throw new RuntimeException("Failed to delete match", e);
        }
    }

    public Match findById(UUID matchId) {
        try {
            return matchRepository.findById(matchId).orElseThrow();
        }
        catch (Exception e) {
            log.error("Error finding match by ID: {}", matchId, e);
        }
        return null;
    }

    /**
     * Remove a match from all players' match histories
     */
    private void removeMatchFromPlayerHistories(Match match) {
        List<UUID> playerIds = new ArrayList<>();

        // Add all players involved
        playerIds.add(match.getChallengerId());
        playerIds.add(match.getOpponentId());

        if (match.getChallengerTeam() != null) {
            playerIds.addAll(match.getChallengerTeam());
        }

        if (match.getOpponentTeam() != null) {
            playerIds.addAll(match.getOpponentTeam());
        }

        // Remove duplicates
        playerIds = new ArrayList<>(new HashSet<>(playerIds));

        // Update each player
        for (UUID playerId : playerIds) {
            try {
                Player player = playerService.findPlayerById(playerId);
                if (player != null && player.getMatchHistory() != null) {
                    player.getMatchHistory().remove(match.getMatchId());
                    playerService.savePlayer(player);
                }
            }
            catch (Exception e) {
                log.warn("Error removing match {} from player {}'s history", match.getMatchId(), playerId, e);
                // Continue with other players
            }
        }
    }

    /**
     * Find all matches involving a specific player
     *
     * @param playerId ID of the player
     * @return List of matches the player participated in
     */
    public List<Match> findByPlayerId(UUID playerId) {
        log.debug("Finding matches for player: {}", playerId);

        if (playerId == null) {
            log.warn("Attempted to find matches with null or empty player ID");
            throw new IllegalArgumentException("Player ID cannot be null or empty");
        }

        try {
            // Check if player exists
            playerService.findPlayerById(playerId);

            // Find matches involving this player
            List<Match> matches = matchRepository.findByPlayerId(playerId);
            log.debug("Found {} matches for player: {}", matches.size(), playerId);
            return matches;
        }
        catch (ResourceNotFoundException e) {
            // Re-throw this exception so it can be handled correctly
            throw e;
        }
        catch (Exception e) {
            log.error("Error finding matches for player: {}", playerId, e);
            throw new RuntimeException("Failed to retrieve matches for player", e);
        }
    }

    /**
     * Retrieve all matches
     *
     * @return List of all matches
     */
    public List<Match> findAll() {
        log.debug("Retrieving all matches");

        try {
            List<Match> matches = matchRepository.findAll();
            log.debug("Found {} matches", matches.size());
            return matches;
        }
        catch (Exception e) {
            log.error("Error retrieving all matches", e);
            throw new RuntimeException("Failed to retrieve matches", e);
        }
    }
}
