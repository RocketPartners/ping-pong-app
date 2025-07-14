package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.GameConfirmation;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.exceptions.BadRequestException;
import com.example.javapingpongelo.models.exceptions.ResourceNotFoundException;
import com.example.javapingpongelo.repositories.GameConfirmationRepository;
import com.example.javapingpongelo.repositories.GameRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GameConfirmationService {

    // Map to hold pending game batches by player
    private final Map<UUID, Set<UUID>> pendingGameBatches = new ConcurrentHashMap<>();

    // Map to hold the last time a game was added to a batch for a player
    private final Map<UUID, LocalDateTime> lastBatchUpdateTime = new ConcurrentHashMap<>();

    @Autowired
    private GameConfirmationRepository confirmationRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private IPlayerService playerService;

    @Autowired
    private EmailService emailService;

    @Value("${app.email.batch-timeout-minutes:3}")
    private int batchTimeoutMinutes;

    /**
     * Create confirmation records for a new game
     */
    @Transactional
    public void createGameConfirmations(Game game) {
        log.info("Creating confirmation records for game: {}", game.getGameId());

        List<UUID> playerIds = getPlayerIdsFromGame(game);

        for (UUID playerIdStr : playerIds) {
            try {
                Player player = playerService.findPlayerById(playerIdStr);

                if (player == null) {
                    log.warn("Player not found with ID: {}", playerIdStr);
                    continue;
                }

                // Determine original and new ELO
                int originalElo = getOriginalElo(player, game);
                int newElo = getCurrentElo(player, game);

                // Create confirmation record
                GameConfirmation confirmation = GameConfirmation.builder()
                                                                .gameId(game.getGameId())
                                                                .playerId(playerIdStr)
                                                                .status(GameConfirmation.ConfirmationStatus.PENDING)
                                                                .originalElo(originalElo)
                                                                .newElo(newElo)
                                                                .confirmationToken(UUID.randomUUID().toString())
                                                                .build();

                confirmationRepository.save(confirmation);

                // Add to batch for this player
                addGameToBatch(playerIdStr, game.getGameId());

            }
            catch (Exception e) {
                log.error("Error creating confirmation for player: {} in game: {}",
                          playerIdStr, game.getGameId(), e);
            }
        }

        // Schedule processing of batches
        checkAndProcessBatches();
    }

    /**
     * Get all player IDs from a game
     */
    private List<UUID> getPlayerIdsFromGame(Game game) {
        return getUuids(game);
    }

    /**
     * Get the original ELO for a player
     */
    private int getOriginalElo(Player player, Game game) {
        if (game.isSinglesGame()) {
            return game.isRatedGame() ? player.getSinglesRankedRating() : player.getSinglesNormalRating();
        }
        else {
            return game.isRatedGame() ? player.getDoublesRankedRating() : player.getDoublesNormalRating();
        }
    }

    /**
     * Get the current ELO for a player
     */
    private int getCurrentElo(Player player, Game game) {
        if (game.isSinglesGame()) {
            if (game.isRatedGame()) {
                return player.getSinglesRankedRating();
            }
            else {
                return player.getSinglesNormalRating();
            }
        }
        else {
            if (game.isRatedGame()) {
                return player.getDoublesRankedRating();
            }
            else {
                return player.getDoublesNormalRating();
            }
        }
    }

    /**
     * Add a game to a player's batch
     */
    private void addGameToBatch(UUID playerId, UUID gameId) {
        pendingGameBatches.computeIfAbsent(playerId, k -> new HashSet<>()).add(gameId);
        lastBatchUpdateTime.put(playerId, LocalDateTime.now());
    }

    /**
     * Process batches that are ready to be sent
     */
    @Scheduled(fixedDelay = 60000) // Run every minute
    public void checkAndProcessBatches() {
        log.debug("Checking for game batches to process...");
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(batchTimeoutMinutes);

        Set<UUID> playersToProcess = lastBatchUpdateTime.entrySet().stream()
                                                        .filter(entry -> entry.getValue().isBefore(cutoffTime))
                                                        .map(Map.Entry::getKey)
                                                        .collect(Collectors.toSet());

        for (UUID playerId : playersToProcess) {
            Set<UUID> gameIds = pendingGameBatches.remove(playerId);
            lastBatchUpdateTime.remove(playerId);

            if (gameIds != null && !gameIds.isEmpty()) {
                sendBatchConfirmationEmail(playerId, gameIds);
            }
        }
    }

    static List<UUID> getUuids(Game game) {
        List<UUID> playerIds = new ArrayList<>();

        if (game.isSinglesGame()) {
            if (game.getChallengerId() != null) playerIds.add(game.getChallengerId());
            if (game.getOpponentId() != null) playerIds.add(game.getOpponentId());
        }
        else {
            if (game.getChallengerTeam() != null) playerIds.addAll(game.getChallengerTeam());
            if (game.getOpponentTeam() != null) playerIds.addAll(game.getOpponentTeam());
        }

        return playerIds;
    }

    /**
     * Send a batch confirmation email for multiple games
     */
    private void sendBatchConfirmationEmail(UUID playerId, Set<UUID> gameIds) {
        try {
            Player player = playerService.findPlayerById(playerId);
            if (player == null) {
                log.warn("Cannot send batch email - player not found: {}", playerId);
                return;
            }

            List<Game> games = new ArrayList<>();
            List<GameConfirmation> confirmations = new ArrayList<>();

            for (UUID gameId : gameIds) {
                Game game = gameRepository.findById(gameId).orElse(null);
                if (game == null) continue;

                games.add(game);

                confirmationRepository
                        .findByGameIdAndPlayerId(gameId, playerId).ifPresent(confirmations::add);

            }

            if (!games.isEmpty() && !confirmations.isEmpty()) {
                emailService.sendGameConfirmationEmail(player, games, confirmations);
                log.info("Sent batch confirmation email to player: {} for {} games",
                         player.getUsername(), games.size());
            }

        }
        catch (Exception e) {
            log.error("Error sending batch confirmation email to player: {}", playerId, e);
        }
    }

    /**
     * Handle a game rejection
     */
    @Transactional
    public void rejectGame(String token) {
        GameConfirmation confirmation = confirmationRepository.findByConfirmationToken(token)
                                                              .orElseThrow(() -> new ResourceNotFoundException("Invalid confirmation token"));

        if (confirmation.isExpired()) {
            throw new BadRequestException("Confirmation window has expired");
        }

        if (confirmation.getStatus() != GameConfirmation.ConfirmationStatus.PENDING) {
            throw new BadRequestException("Game already confirmed or rejected");
        }

        // Mark as rejected
        confirmation.setStatus(GameConfirmation.ConfirmationStatus.REJECTED);
        confirmation.setRespondedAt(LocalDateTime.now());
        confirmationRepository.save(confirmation);

        // Delete the game - GameService will handle this
        Game game = gameRepository.findById(confirmation.getGameId())
                                  .orElseThrow(() -> new ResourceNotFoundException("Game not found"));

        // Notify players about the rejection
        notifyPlayersAboutRejection(game, confirmation.getPlayerId());
    }

    /**
     * Notify other players about a game rejection
     */
    private void notifyPlayersAboutRejection(Game game, UUID rejectingPlayerId) {
        Player rejectingPlayer = playerService.findPlayerById(rejectingPlayerId);
        if (rejectingPlayer == null) {
            log.warn("Rejecting player not found: {}", rejectingPlayerId);
            return;
        }

        List<UUID> playerIds = getPlayerIdsFromGame(game);

        for (UUID playerIdStr : playerIds) {
            try {

                // Skip the player who rejected
                if (playerIdStr.equals(rejectingPlayerId)) {
                    continue;
                }

                Player player = playerService.findPlayerById(playerIdStr);
                if (player == null) continue;

                emailService.sendGameRejectionNotification(player, game, rejectingPlayer);

            }
            catch (Exception e) {
                log.error("Error notifying player about rejection", e);
            }
        }
    }

    /**
     * Auto-confirm expired pending confirmations
     */
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    @Transactional
    public void confirmExpiredPendingConfirmations() {
        log.info("Processing expired game confirmations");

        List<GameConfirmation> expiredConfirmations =
                confirmationRepository.findExpiredPendingConfirmations(LocalDateTime.now());

        for (GameConfirmation confirmation : expiredConfirmations) {
            confirmation.setStatus(GameConfirmation.ConfirmationStatus.CONFIRMED);
            confirmation.setRespondedAt(LocalDateTime.now());
            confirmationRepository.save(confirmation);

            log.debug("Auto-confirmed expired game: {} for player: {}",
                      confirmation.getGameId(), confirmation.getPlayerId());
        }

        log.info("Auto-confirmed {} expired confirmations", expiredConfirmations.size());
    }

    /**
     * Get a GameConfirmation by token
     */
    public GameConfirmation getConfirmationByToken(String token) {
        return confirmationRepository.findByConfirmationToken(token).orElse(null);
    }
}