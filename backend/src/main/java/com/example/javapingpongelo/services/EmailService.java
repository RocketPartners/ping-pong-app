package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.GameConfirmation;
import com.example.javapingpongelo.models.Player;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine emailTemplateEngine;

    @Autowired
    private IPlayerService playerService;

    @Value("${app.email.from:noreply@example.com}")
    private String fromEmail;

    @Value("${FRONTEND_URL:http://localhost:4200}")
    private String frontendUrl;

    /**
     * Send a password reset email
     */
    public void sendPasswordResetEmail(String to, String token) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("Password Reset Request");

        String emailContent = getEmailContent(token);

        helper.setText(emailContent, true);

        mailSender.send(message);
        log.info("Password reset email sent to: {}", to);
    }

    private String getEmailContent(String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        return "<div>" +
                "<h2>Password Reset Request</h2>" +
                "<p>You have requested to reset your password. Click the link below to reset it:</p>" +
                "<a href=\"" + resetUrl + "\">Reset Password</a>" +
                "<p>This link will expire in 30 minutes.</p>" +
                "<p>If you did not request this, please ignore this email.</p>" +
                "</div>";
    }

    /**
     * Send a game confirmation email for multiple games
     */
    public void sendGameConfirmationEmail(Player player, List<Game> games, List<GameConfirmation> confirmations)
            throws MessagingException {
        log.info("Sending game confirmation email to {}", player.getEmail());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(player.getEmail());
        helper.setSubject("Game Results Confirmation - Action Required");

        Context context = new Context();
        context.setVariable("player", player);
        context.setVariable("games", games);
        context.setVariable("confirmations", confirmations);
        context.setVariable("baseUrl", frontendUrl);

        // Add game mappings for easier access in the template
        Map<String, Game> gameMap = new HashMap<>();
        Map<String, String> gameTypeMap = new HashMap<>();
        Map<UUID, PlayerInfo> playerInfoMap = new HashMap<>();
        Map<String, Integer> eloDiffMap = new HashMap<>();

        for (Game game : games) {
            String gameId = game.getGameId().toString();
            gameMap.put(gameId, game);
            gameTypeMap.put(gameId, getGameTypeString(game));

            // Look up player information for the challenger
            if (game.getChallengerId() != null) {
                try {
                    UUID challengerId = game.getChallengerId();
                    Player challenger = playerService.findPlayerById(challengerId);
                    if (challenger != null) {
                        playerInfoMap.put(game.getChallengerId(),
                                          new PlayerInfo(challenger.getFullName(), challenger.getUsername()));
                    }
                }
                catch (Exception e) {
                    log.warn("Failed to get challenger info: {}", e.getMessage());
                }
            }

            // Look up player information for the opponent
            if (game.getOpponentId() != null) {
                try {
                    UUID opponentId = game.getOpponentId();
                    Player opponent = playerService.findPlayerById(opponentId);
                    if (opponent != null) {
                        playerInfoMap.put(game.getOpponentId(),
                                          new PlayerInfo(opponent.getFullName(), opponent.getUsername()));
                    }
                }
                catch (Exception e) {
                    log.warn("Failed to get opponent info: {}", e.getMessage());
                }
            }

            // Load player info for doubles teams
            if (game.isDoublesGame()) {
                // Process challenger team
                if (game.getChallengerTeam() != null) {
                    for (UUID teamPlayerId : game.getChallengerTeam()) {
                        try {
                            Player teamPlayer = playerService.findPlayerById(teamPlayerId);
                            if (teamPlayer != null) {
                                playerInfoMap.put(teamPlayerId,
                                                  new PlayerInfo(teamPlayer.getFullName(), teamPlayer.getUsername()));
                            }
                        }
                        catch (Exception e) {
                            log.warn("Failed to get challenger team player info: {}", e.getMessage());
                        }
                    }
                }

                // Process opponent team
                if (game.getOpponentTeam() != null) {
                    for (UUID teamPlayerId : game.getOpponentTeam()) {
                        try {
                            Player teamPlayer = playerService.findPlayerById(teamPlayerId);
                            if (teamPlayer != null) {
                                playerInfoMap.put(teamPlayerId,
                                                  new PlayerInfo(teamPlayer.getFullName(), teamPlayer.getUsername()));
                            }
                        }
                        catch (Exception e) {
                            log.warn("Failed to get opponent team player info: {}", e.getMessage());
                        }
                    }
                }
            }
        }

        // Calculate ELO differences explicitly for each confirmation
        for (GameConfirmation confirmation : confirmations) {
            int eloDiff = confirmation.getNewElo() - confirmation.getOriginalElo();
            eloDiffMap.put(confirmation.getConfirmationToken(), eloDiff);
        }

        context.setVariable("gameMap", gameMap);
        context.setVariable("gameTypeMap", gameTypeMap);
        context.setVariable("playerInfoMap", playerInfoMap);
        context.setVariable("eloDiffMap", eloDiffMap);

        // Process the template
        String emailContent = emailTemplateEngine.process("game-confirmation-email", context);

        helper.setText(emailContent, true);

        mailSender.send(message);
        log.info("Game confirmation email sent to: {}", player.getEmail());
    }

    /**
     * Get a readable game type string
     */
    private String getGameTypeString(Game game) {
        if (game.isSinglesGame()) {
            return game.isRatedGame() ? "Singles Ranked" : "Singles Normal";
        }
        else {
            return game.isRatedGame() ? "Doubles Ranked" : "Doubles Normal";
        }
    }

    /**
     * Send a notification when a game is rejected
     */
    public void sendGameRejectionNotification(Player player, Game game, Player rejectingPlayer)
            throws MessagingException {
        log.info("Sending game rejection notification to {}", player.getEmail());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(player.getEmail());
        helper.setSubject("Game Result Rejected");

        Context context = new Context();
        context.setVariable("player", player);
        context.setVariable("game", game);
        context.setVariable("rejectingPlayer", rejectingPlayer);
        context.setVariable("gameType", getGameTypeString(game));

        // Add player info for challenger and opponent
        Map<UUID, PlayerInfo> playerInfoMap = new HashMap<>();

        // Look up player information for the challenger
        if (game.getChallengerId() != null) {
            try {
                UUID challengerId = game.getChallengerId();
                Player challenger = playerService.findPlayerById(challengerId);
                if (challenger != null) {
                    playerInfoMap.put(game.getChallengerId(),
                                      new PlayerInfo(challenger.getFullName(), challenger.getUsername()));
                }
            }
            catch (Exception e) {
                log.warn("Failed to get challenger info: {}", e.getMessage());
            }
        }

        // Look up player information for the opponent
        if (game.getOpponentId() != null) {
            try {
                UUID opponentId = game.getOpponentId();
                Player opponent = playerService.findPlayerById(opponentId);
                if (opponent != null) {
                    playerInfoMap.put(game.getOpponentId(),
                                      new PlayerInfo(opponent.getFullName(), opponent.getUsername()));
                }
            }
            catch (Exception e) {
                log.warn("Failed to get opponent info: {}", e.getMessage());
            }
        }

        // Load player info for doubles teams
        if (game.isDoublesGame()) {
            // Process challenger team
            if (game.getChallengerTeam() != null) {
                for (UUID teamPlayerId : game.getChallengerTeam()) {
                    try {
                        Player teamPlayer = playerService.findPlayerById(teamPlayerId);
                        if (teamPlayer != null) {
                            playerInfoMap.put(teamPlayerId,
                                              new PlayerInfo(teamPlayer.getFullName(), teamPlayer.getUsername()));
                        }
                    }
                    catch (Exception e) {
                        log.warn("Failed to get challenger team player info: {}", e.getMessage());
                    }
                }
            }

            // Process opponent team
            if (game.getOpponentTeam() != null) {
                for (UUID teamPlayerId : game.getOpponentTeam()) {
                    try {
                        Player teamPlayer = playerService.findPlayerById(teamPlayerId);
                        if (teamPlayer != null) {
                            playerInfoMap.put(teamPlayerId,
                                              new PlayerInfo(teamPlayer.getFullName(), teamPlayer.getUsername()));
                        }
                    }
                    catch (Exception e) {
                        log.warn("Failed to get opponent team player info: {}", e.getMessage());
                    }
                }
            }
        }

        context.setVariable("playerInfoMap", playerInfoMap);

        // Process the template
        String emailContent = emailTemplateEngine.process("game-rejection-notification", context);

        helper.setText(emailContent, true);

        mailSender.send(message);
        log.info("Game rejection notification sent to: {}", player.getEmail());
    }

    /**
     * Helper class to store player name and username
     */
    public record PlayerInfo(String fullName, String username) {
    }
    
}