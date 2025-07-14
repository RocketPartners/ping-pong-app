package com.example.javapingpongelo.controllers.test;

import com.example.javapingpongelo.models.ApiResponse;
import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.GameConfirmation;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.services.EmailService;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * DEVELOPMENT ONLY - Controller for testing email templates
 * This controller should be disabled or removed in production
 */
@RestController
@RequestMapping("/api/test/email")
@Slf4j
public class EmailTestController {

    @Autowired
    private EmailService emailService;

    /**
     * Test endpoint to send a game confirmation email
     *
     * @param email                The email address to send the test to
     * @param includeMultipleGames Whether to include multiple games in the test
     */
    @GetMapping("/confirmation")
    public ResponseEntity<ApiResponse> testConfirmationEmail(
            @RequestParam String email,
            @RequestParam(defaultValue = "true") boolean includeMultipleGames) {

        try {
            // Create test data
            Player player = createSamplePlayer(email);
            List<Game> games = createSampleGames(includeMultipleGames ? 3 : 1);
            List<GameConfirmation> confirmations = createSampleConfirmations(player.getPlayerId(), games);

            // Send the email
            emailService.sendGameConfirmationEmail(player, games, confirmations);

            return ResponseEntity.ok(new ApiResponse(true,
                                                     "Test confirmation email sent to " + email + " with " + games.size() + " games"));
        }
        catch (MessagingException e) {
            log.error("Error sending test confirmation email", e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    /**
     * Create a sample player for testing
     */
    private Player createSamplePlayer(String email) {
        Player player = new Player();
        player.setPlayerId(UUID.randomUUID());
        player.setFirstName("Test");
        player.setLastName("User");
        player.setUsername("testuser");
        player.setEmail(email);
        player.setSinglesRankedRating(1000);
        player.setDoublesRankedRating(1000);
        player.setSinglesNormalRating(1000);
        player.setDoublesNormalRating(1000);
        return player;
    }

    /**
     * Create sample games for testing
     */
    private List<Game> createSampleGames(int count) {
        List<Game> games = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Game game = new Game();
            game.setGameId(UUID.randomUUID());

            // Alternate between different game types for testing
            if (i % 2 == 0) {
                game.setSinglesGame(true);
                game.setDoublesGame(false);

                if (i % 4 == 0) {
                    game.setRatedGame(true);
                    game.setNormalGame(false);
                }
                else {
                    game.setRatedGame(false);
                    game.setNormalGame(true);
                }

                // Set player IDs and winner
                game.setChallengerId(UUID.randomUUID());
                game.setOpponentId(UUID.randomUUID());

                if (i % 3 == 0) {
                    game.setChallengerWin(true);
                    game.setOpponentWin(false);
                }
                else {
                    game.setChallengerWin(false);
                    game.setOpponentWin(true);
                }

                // Set scores
                game.setChallengerTeamScore(i % 3 == 0 ? 11 : 8);
                game.setOpponentTeamScore(i % 3 == 0 ? 8 : 11);
            }
            else {
                // Doubles game
                game.setSinglesGame(false);
                game.setDoublesGame(true);

                if (i % 4 == 1) {
                    game.setRatedGame(true);
                    game.setNormalGame(false);
                }
                else {
                    game.setRatedGame(false);
                    game.setNormalGame(true);
                }

                // Set team players
                List<UUID> challengerTeam = new ArrayList<>();
                challengerTeam.add(UUID.randomUUID());
                challengerTeam.add(UUID.randomUUID());
                game.setChallengerTeam(challengerTeam);

                List<UUID> opponentTeam = new ArrayList<>();
                opponentTeam.add(UUID.randomUUID());
                opponentTeam.add(UUID.randomUUID());
                game.setOpponentTeam(opponentTeam);

                // Set team winner
                if (i % 3 == 1) {
                    game.setChallengerTeamWin(true);
                    game.setOpponentTeamWin(false);
                }
                else {
                    game.setChallengerTeamWin(false);
                    game.setOpponentTeamWin(true);
                }

                // Set scores
                game.setChallengerTeamScore(i % 3 == 1 ? 21 : 18);
                game.setOpponentTeamScore(i % 3 == 1 ? 18 : 21);
            }

            // Set date
            game.setDatePlayed(new Date());

            games.add(game);
        }

        return games;
    }

    /**
     * Create sample confirmations for testing
     */
    private List<GameConfirmation> createSampleConfirmations(UUID playerId, List<Game> games) {
        List<GameConfirmation> confirmations = new ArrayList<>();

        for (int i = 0; i < games.size(); i++) {
            Game game = games.get(i);

            GameConfirmation confirmation = new GameConfirmation();
            confirmation.setId(UUID.randomUUID());
            confirmation.setGameId(game.getGameId());
            confirmation.setPlayerId(playerId);
            confirmation.setStatus(GameConfirmation.ConfirmationStatus.PENDING);
            confirmation.setCreatedAt(LocalDateTime.now().minusHours(i));
            confirmation.setConfirmationToken(UUID.randomUUID().toString());

            // Set original and new ELO values with a visible difference
            int originalElo = 1000;
            int eloDiff = i % 2 == 0 ? 15 + i * 5 : -12 - i * 3;
            confirmation.setOriginalElo(originalElo);
            confirmation.setNewElo(originalElo + eloDiff);

            // Set expiration date
            confirmation.setExpirationDate(LocalDateTime.now().plusHours(48 - i));

            confirmations.add(confirmation);
        }

        return confirmations;
    }

    /**
     * Test endpoint to send a game rejection notification email
     *
     * @param email The email address to send the test to
     */
    @GetMapping("/rejection")
    public ResponseEntity<ApiResponse> testRejectionEmail(
            @RequestParam String email) {

        try {
            // Create test data
            Player recipient = createSamplePlayer(email);
            Player rejectingPlayer = createSamplePlayer("rejector@example.com");
            rejectingPlayer.setFirstName("John");
            rejectingPlayer.setLastName("Rejector");
            rejectingPlayer.setUsername("johnr");

            Game game = createSampleGames(1).getFirst();

            // Send the email
            emailService.sendGameRejectionNotification(recipient, game, rejectingPlayer);

            return ResponseEntity.ok(new ApiResponse(true,
                                                     "Test rejection notification email sent to " + email));
        }
        catch (MessagingException e) {
            log.error("Error sending test rejection email", e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }
}