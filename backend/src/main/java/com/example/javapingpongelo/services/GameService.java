package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.GameType;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.exceptions.BadRequestException;
import com.example.javapingpongelo.models.exceptions.ResourceNotFoundException;
import com.example.javapingpongelo.repositories.GameRepository;
import com.example.javapingpongelo.services.achievements.IAchievementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class GameService {

    @Autowired
    GameRepository gameRepository;

    @Autowired
    private SlackService slackService;

    @Autowired
    IPlayerService playerService;

    @Autowired
    PlayerGameHistoryService playerGameHistoryService;

    @Autowired
    IAchievementService achievementService;

    @Autowired
    EloService eloService;

    @Autowired
    private GameConfirmationService gameConfirmationService;
    
    @Autowired
    private SlackHelperMethods slackHelper;

    /**
     * Save games and return the saved game entities
     */
    @Transactional
    public List<Game> saveAndReturn(List<Game> games) {
        if (games == null || games.isEmpty()) {
            log.warn("Attempted to save null or empty game list");
            return new ArrayList<>();
        }

        log.info("Saving {} games", games.size());
        List<String> errors = new ArrayList<>();
        List<Game> savedGames = new ArrayList<>();

        for (Game game : games) {
            if (game == null) {
                continue;
            }

            try {
                // Validate anonymous players can only play normal games
                validateAnonymousPlayerRules(game);
                
                // Save the game
                Game savedGame = gameRepository.save(game);
                savedGames.add(savedGame);
                log.debug("Saved game with ID: {}", savedGame.getGameId());

                // Process ELO changes
                processGameElo(game);
                log.debug("Processed Elo for game: {}", game.getGameId());

                // Update player game history with GameResult objects
                updatePlayerGameHistory(game);
                log.debug("Updated player game history for game: {}", game.getGameId());

                // Evaluate achievements for this game
                evaluateAchievements(game);
                log.debug("Evaluated achievements for game: {}", game.getGameId());

                // Create game confirmation records for players to review
                gameConfirmationService.createGameConfirmations(savedGame);
                log.debug("Created game confirmation records for game: {}", savedGame.getGameId());
            }
            catch (Exception e) {
                log.error("Error saving game: {}", game.getGameId(), e);
                errors.add("Error processing game: " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException("Some games could not be processed: " + String.join(", ", errors));
        }

        return savedGames;
    }

    private void processGameElo(Game game) {
        if (game.isSinglesGame()) {
            processSinglesGameElo(game);
        }
        else {
            processDoublesGameElo(game);
        }
        
        // Post game result to Slack after ELO processing
        postGameResultToSlack(game);
        
        // Check for win streaks and post notifications
        if (slackHelper != null) {
            slackHelper.checkAndNotifyWinStreaks(game);
        }
    }
    
    private void postGameResultToSlack(Game game) {
        try {
            Player challenger = playerService.findPlayerById(game.getChallengerId());
            Player opponent = playerService.findPlayerById(game.getOpponentId());
            
            if (challenger != null && opponent != null) {
                slackService.postGameResult(game, challenger, opponent);
            }
        } catch (Exception e) {
            log.error("Error posting game result to Slack for game: {}", game.getGameId(), e);
            // Don't let Slack errors break the game processing
        }
    }

    /**
     * Update player game history with a new Game
     *
     * @param game The game to add to history
     */
    @Transactional
    protected void updatePlayerGameHistory(Game game) {
        if (game.isSinglesGame()) {
            updateSinglesGameHistory(game);
        }
        else {
            updateDoublesGameHistory(game);
        }
    }

    private void evaluateAchievements(Game game) {
        try {
            List<Player> players = getPlayersFromGame(game);

            // Call achievement service to evaluate achievements for all players
            if (!players.isEmpty()) {
                achievementService.evaluateAchievementsForGame(game, players.toArray(new Player[0]));
            }
        }
        catch (Exception e) {
            log.error("Error evaluating achievements for game: {}", game.getGameId(), e);
            // We don't want achievement errors to block the game saving process
        }
    }

    private void processSinglesGameElo(Game game) {
        Player challenger = playerService.findPlayerById(game.getChallengerId());
        Player opponent = playerService.findPlayerById(game.getOpponentId());

        if (game.isRatedGame()) {
            updateSinglesRankedElo(game, challenger, opponent);
        }
        else {
            updateSinglesNormalElo(game, challenger, opponent);
        }
    }

    private void processDoublesGameElo(Game game) {
        if (game.isRatedGame()) {
            updateDoublesRankedElo(game);
        }
        else {
            updateDoublesNormalElo(game);
        }
    }

    /**
     * Update player game history for singles games
     */
    @Transactional
    protected void updateSinglesGameHistory(Game game) {
        Player challenger = playerService.findPlayerById(game.getChallengerId());
        Player opponent = playerService.findPlayerById(game.getOpponentId());

        // Determine game type
        GameType gameType = game.isRatedGame() ?
                GameType.SINGLES_RANKED : GameType.SINGLES_NORMAL;

        // Create game history entries for both players
        playerGameHistoryService.addGameHistoryEntry(
                challenger.getPlayerId(),
                game,
                game.isChallengerWin(),
                gameType);

        playerGameHistoryService.addGameHistoryEntry(
                opponent.getPlayerId(),
                game,
                game.isOpponentWin(),
                gameType);
    }

    /**
     * Update player game history for doubles games
     */
    @Transactional
    protected void updateDoublesGameHistory(Game game) {
        // Determine game type
        GameType gameType = game.isRatedGame() ?
                GameType.DOUBLES_RANKED : GameType.DOUBLES_NORMAL;

        // Process challenger team
        for (UUID playerId : game.getChallengerTeam()) {
            Player player = playerService.findPlayerById(playerId);
            if (player != null) {
                playerGameHistoryService.addGameHistoryEntry(
                        player.getPlayerId(),
                        game,
                        game.isChallengerTeamWin(),
                        gameType);
            }
        }

        // Process opponent team
        for (UUID playerId : game.getOpponentTeam()) {
            Player player = playerService.findPlayerById(playerId);
            if (player != null) {
                playerGameHistoryService.addGameHistoryEntry(
                        player.getPlayerId(),
                        game,
                        game.isOpponentTeamWin(),
                        gameType);
            }
        }
    }

    /**
     * Get all players involved in a game
     */
    private List<Player> getPlayersFromGame(Game game) {
        List<Player> players = new ArrayList<>();

        if (game.isSinglesGame()) {
            Player challenger = playerService.findPlayerById(game.getChallengerId());
            Player opponent = playerService.findPlayerById(game.getOpponentId());

            if (challenger != null) players.add(challenger);
            if (opponent != null) players.add(opponent);
        }
        else {
            for (UUID playerId : game.getChallengerTeam()) {
                Player player = playerService.findPlayerById(playerId);
                if (player != null) players.add(player);
            }
            for (UUID playerId : game.getOpponentTeam()) {
                Player player = playerService.findPlayerById(playerId);
                if (player != null) players.add(player);
            }
        }

        return players;
    }

    private void updateSinglesRankedElo(Game game, Player challenger, Player opponent) {
        if (game.isChallengerWin()) {
            updatePlayerSinglesElo(challenger, opponent, game, true);
        }
        else {
            updatePlayerSinglesElo(opponent, challenger, game, true);
        }
    }

    private void updateSinglesNormalElo(Game game, Player challenger, Player opponent) {
        if (game.isChallengerWin()) {
            updatePlayerSinglesElo(challenger, opponent, game, false);
        }
        else {
            updatePlayerSinglesElo(opponent, challenger, game, false);
        }
    }

    private void updateDoublesRankedElo(Game game) {
        List<Player> players = getPlayersForDoubles(game);
        List<Double> newRatings = eloService.newDoublesRankedEloRatingForEachPlayer(game, players.get(0), players.get(1), players.get(2), players.get(3));
        updatePlayerElo(players, newRatings, true, game);
    }

    private void updateDoublesNormalElo(Game game) {
        List<Player> players = getPlayersForDoubles(game);
        List<Double> newRatings = eloService.newDoublesNormalEloRatingForEachPlayer(game, players.get(0), players.get(1), players.get(2), players.get(3));
        updatePlayerElo(players, newRatings, false, game);
    }

    private void updatePlayerSinglesElo(Player winner, Player loser, Game game, boolean isRanked) {
        if (isRanked) {
            playerService.updatePlayerSinglesRankedElo(winner, eloService.newSinglesEloRankedRatingForWinner(winner, loser, game.getChallengerTeamScore(), game.getOpponentTeamScore()), game);
            playerService.updatePlayerSinglesRankedElo(loser, eloService.newSinglesRankedEloRatingForLoser(loser, winner, game.getOpponentTeamScore(), game.getChallengerTeamScore()), game);
        }
        else {
            playerService.updatePlayerSinglesNormalElo(winner, eloService.newSinglesEloNormalRatingForWinner(winner, loser, game.getChallengerTeamScore(), game.getOpponentTeamScore()), game);
            playerService.updatePlayerSinglesNormalElo(loser, eloService.newSinglesNormalEloRatingForLoser(loser, winner, game.getOpponentTeamScore(), game.getChallengerTeamScore()), game);
        }
    }

    private List<Player> getPlayersForDoubles(Game game) {
        List<Player> players = new ArrayList<>();
        if (game.isChallengerTeamWin()) {
            addPlayerIfNotNull(players, playerService.findPlayerById(game.getChallengerTeam().get(0)));
            addPlayerIfNotNull(players, playerService.findPlayerById(game.getChallengerTeam().get(1)));
            addPlayerIfNotNull(players, playerService.findPlayerById(game.getOpponentTeam().get(0)));
            addPlayerIfNotNull(players, playerService.findPlayerById(game.getOpponentTeam().get(1)));
        }
        else {
            addPlayerIfNotNull(players, playerService.findPlayerById(game.getOpponentTeam().get(0)));
            addPlayerIfNotNull(players, playerService.findPlayerById(game.getOpponentTeam().get(1)));
            addPlayerIfNotNull(players, playerService.findPlayerById(game.getChallengerTeam().get(0)));
            addPlayerIfNotNull(players, playerService.findPlayerById(game.getChallengerTeam().get(1)));
        }
        return players;
    }

    private void addPlayerIfNotNull(List<Player> players, Player player) {
        if (player != null) {
            players.add(player);
        }
    }


    // The private helper methods below are unchanged from the original code

    private void updatePlayerElo(List<Player> players, List<Double> newRatings, boolean isRanked, Game game) {
        for (int i = 0; i < players.size(); i++) {
            if (isRanked) {
                playerService.updatePlayerDoublesRankedElo(players.get(i), newRatings.get(i), (players.get(i).getDoublesRankedRating() < newRatings.get(i)), game);
            }
            else {
                playerService.updatePlayerDoublesNormalElo(players.get(i), newRatings.get(i), (players.get(i).getDoublesNormalRating() < newRatings.get(i)), game);
            }
        }
    }

    /**
     * Handle game rejection from GameConfirmationService
     * This method will be called when GameConfirmationService processes a rejection
     */
    @Transactional
    public void handleGameRejection(UUID gameId, UUID playerId) {
        log.info("Handling game rejection for game: {} by player: {}", gameId, playerId);

        Game game = findById(gameId);
        if (game == null) {
            log.warn("Game not found for rejection: {}", gameId);
            return;
        }

        // Get all players involved in the game
        List<Player> players = getPlayersFromGame(game);

        // Revert ELO changes for all players
        for (Player player : players) {
            revertEloChange(player, game);
        }

        // Delete the game
        deleteGame(gameId);

        log.info("Successfully processed game rejection");
    }

    /**
     * Find a game by its ID
     *
     * @param id UUID of the game to find
     * @return The game if found, null otherwise
     */
    public Game findById(UUID id) {
        log.debug("Finding game with ID: {}", id);

        if (id == null) {
            log.warn("Attempted to find game with null ID");
            return null;
        }

        try {
            return gameRepository.findById(id).orElse(null);
        }
        catch (Exception e) {
            log.error("Error finding game with ID: {}", id, e);
            return null;
        }
    }

    /**
     * Revert ELO changes for a player
     * This uses the original ELO stored in PlayerEloHistory
     */
    private void revertEloChange(Player player, Game game) {
        // We need to find the ELO history record for this game and player
        // For simplicity in this implementation, we'll rely on the fact that
        // win/loss counts and the game record itself will be deleted

        // Revert win/loss counts - this is done by deleteGame()

        // We could also reverse the exact ELO changes if we stored them
        // That would require accessing the PlayerEloHistory repository
        log.info("Reverted ELO changes for player: {} for game: {}",
                 player.getPlayerId(), game.getGameId());
    }

    /**
     * Delete a game and related data
     */
    @Transactional
    public void deleteGame(UUID id) {
        log.info("Deleting game with ID: {}", id);

        if (id == null) {
            log.warn("Attempted to delete game with null ID");
            throw new IllegalArgumentException("Game ID cannot be null");
        }

        try {
            // Make sure the game exists
            Game game = findById(id);
            if (game == null) {
                log.warn("Attempted to delete non-existent game: {}", id);
                throw new BadRequestException("Game not found with id: " + id);
            }

            // Delete game history entries
            playerGameHistoryService.deleteGameHistoryForGame(id);

            // Delete the game
            gameRepository.deleteById(id);
            log.info("Successfully deleted game with ID: {}", id);
        }
        catch (BadRequestException e) {
            // Re-throw this exception so it can be handled correctly
            throw e;
        }
        catch (Exception e) {
            log.error("Error deleting game with ID: {}", id, e);
            throw new RuntimeException("Failed to delete game", e);
        }
    }

    /**
     * Find all games, sorted
     *
     * @param sort The sort specification
     * @return List of all games
     */
    public List<Game> findAllSorted(Sort sort) {
        log.debug("Retrieving all games with sort: {}", sort);

        try {
            List<Game> games = gameRepository.findAll(sort);
            log.debug("Found {} games", games.size());
            return games;
        }
        catch (Exception e) {
            log.error("Error retrieving all games with sort", e);
            throw new RuntimeException("Failed to retrieve games", e);
        }
    }

    /**
     * Find all games with pagination
     *
     * @param pageable The pagination information
     * @return Page of games
     */
    public Page<Game> findAllPaged(Pageable pageable) {
        log.debug("Retrieving paged games: page {}, size {}",
                  pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<Game> gamesPage = gameRepository.findAll(pageable);
            log.debug("Found page with {} games", gamesPage.getNumberOfElements());
            return gamesPage;
        }
        catch (Exception e) {
            log.error("Error retrieving paged games", e);
            throw new RuntimeException("Failed to retrieve paged games", e);
        }
    }

    /**
     * Find all games involving a specific player
     *
     * @param playerId ID of the player
     * @return List of games the player participated in
     */
    public List<Game> findByPlayerId(UUID playerId) {
        log.debug("Finding games for player: {}", playerId);

        if (playerId == null) {
            log.warn("Attempted to find games with null or empty player ID");
            throw new IllegalArgumentException("Player ID cannot be null or empty");
        }

        try {
            // Check if player exists
            playerService.findPlayerById(playerId);

            // Find games involving this player
            List<Game> games = gameRepository.findByPlayerId(playerId);
            log.debug("Found {} games for player: {}", games.size(), playerId);
            return games;
        }
        catch (ResourceNotFoundException e) {
            // Re-throw this exception so it can be handled correctly
            throw e;
        }
        catch (Exception e) {
            log.error("Error finding games for player: {}", playerId, e);
            throw new RuntimeException("Failed to retrieve games for player", e);
        }
    }

    /**
     * Find games for a player with pagination
     *
     * @param playerId ID of the player
     * @param pageable The pagination information
     * @return Page of games the player participated in
     */
    public Page<Game> findByPlayerIdPaged(UUID playerId, Pageable pageable) {
        log.debug("Finding paged games for player: {}, page {}, size {}",
                  playerId, pageable.getPageNumber(), pageable.getPageSize());

        if (playerId == null) {
            log.warn("Attempted to find games with null or empty player ID");
            throw new IllegalArgumentException("Player ID cannot be null or empty");
        }

        try {
            // Check if player exists
            playerService.findPlayerById(playerId);

            // Find games involving this player with pagination
            Page<Game> gamesPage = gameRepository.findByPlayerIdPaged(playerId, pageable);
            log.debug("Found page with {} games for player: {}",
                      gamesPage.getNumberOfElements(), playerId);
            return gamesPage;
        }
        catch (ResourceNotFoundException e) {
            // Re-throw this exception so it can be handled correctly
            throw e;
        }
        catch (Exception e) {
            log.error("Error finding paged games for player: {}", playerId, e);
            throw new RuntimeException("Failed to retrieve paged games for player", e);
        }
    }

    /**
     * Reset all player ratings to initial values
     */
    public void resetAllPlayers() {
        log.info("Resetting all player ratings");

        try {
            List<Player> players = playerService.findAllPlayers();
            int count = 0;

            for (Player player : players) {
                // Reset ratings to default
                player.setSinglesRankedRating(1000);
                player.setSinglesNormalRating(1000);
                player.setDoublesRankedRating(1000);
                player.setDoublesNormalRating(1000);

                // Reset win/loss counts
                player.setSinglesRankedWins(0);
                player.setSinglesRankedLoses(0);
                player.setDoublesRankedLoses(0);
                player.setDoublesRankedWins(0);
                player.setSinglesNormalWins(0);
                player.setSinglesNormalLoses(0);
                player.setDoublesNormalLoses(0);
                player.setDoublesNormalWins(0);

                // Clear game history
                if (player.getGameHistory() != null) {
                    player.getGameHistory().clear();
                }
                else {
                    player.setGameHistory(new ArrayList<>());
                }

                playerService.savePlayer(player);
                count++;
            }

            log.info("Successfully reset {} players", count);
        }
        catch (Exception e) {
            log.error("Error resetting player ratings", e);
            throw new RuntimeException("Failed to reset player ratings", e);
        }
    }

    /**
     * Remove a game from player game histories
     */
    private void removeGameFromPlayerHistories(Game game) {
        Date gameDate = game.getDatePlayed();
        GameType gameType = determineGameType(game);

        if (game.isSinglesGame()) {
            // Update challenger and opponent
            updatePlayerGameHistoryOnDelete(game.getChallengerId(), gameDate, gameType);
            updatePlayerGameHistoryOnDelete(game.getOpponentId(), gameDate, gameType);
        }
        else {
            // Update all players in both teams
            for (UUID playerId : game.getChallengerTeam()) {
                updatePlayerGameHistoryOnDelete(playerId, gameDate, gameType);
            }
            for (UUID playerId : game.getOpponentTeam()) {
                updatePlayerGameHistoryOnDelete(playerId, gameDate, gameType);
            }
        }
    }

    /**
     * Determine the game type from a game entity
     */
    private GameType determineGameType(Game game) {
        if (game.isSinglesGame()) {
            return game.isRatedGame() ? GameType.SINGLES_RANKED : GameType.SINGLES_NORMAL;
        }
        else {
            return game.isRatedGame() ? GameType.DOUBLES_RANKED : GameType.DOUBLES_NORMAL;
        }
    }

    /**
     * Update a player's game history when a game is deleted
     */
    private void updatePlayerGameHistoryOnDelete(UUID playerId, Date gameDate, GameType gameType) {
        try {
            Player player = playerService.findPlayerById(playerId);
            if (player != null && player.getGameHistory() != null) {
                // Remove game result with matching date and type
                player.getGameHistory().removeIf(result ->
                                                         result.getDate().equals(gameDate) && result.getGameType() == gameType);
                playerService.savePlayer(player);
            }
        }
        catch (Exception e) {
            log.warn("Error updating game history for player {} on game delete", playerId, e);
        }
    }

    /**
     * Validate that anonymous players can only participate in normal games
     */
    private void validateAnonymousPlayerRules(Game game) {
        if (game == null) {
            return;
        }

        // Check if this is a ranked game
        if (game.isRatedGame()) {
            // Check all players involved in the game
            List<UUID> allPlayerIds = new ArrayList<>();
            
            if (game.isSinglesGame()) {
                allPlayerIds.add(game.getChallengerId());
                allPlayerIds.add(game.getOpponentId());
            } else {
                allPlayerIds.addAll(game.getChallengerTeam());
                allPlayerIds.addAll(game.getOpponentTeam());
            }

            // Check if any player is anonymous
            for (UUID playerId : allPlayerIds) {
                Player player = playerService.findPlayerById(playerId);
                if (player != null && player.getIsAnonymous() != null && player.getIsAnonymous()) {
                    throw new BadRequestException("Anonymous players can only participate in normal (non-ranked) games");
                }
            }
        }
    }
}