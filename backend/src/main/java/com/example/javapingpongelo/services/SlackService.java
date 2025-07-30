package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.Challenge;
import com.example.javapingpongelo.models.ChallengeStatus;
import com.example.javapingpongelo.repositories.GameRepository;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SlackService {
    
    @Autowired
    private IPlayerService playerService;
    
    @Autowired
    private GameRepository gameRepository;
    
    
    private final DecimalFormat eloFormat = new DecimalFormat("+#;-#");
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");
    
    @Value("${slack.bot.token:}")
    private String botToken;
    
    @Value("${slack.channel.results:#ping-pong-results}")
    private String resultsChannel;
    
    @Value("${slack.channel.general:#general}")
    private String generalChannel;
    
    @Value("${slack.channel.challenges:#ping-pong-challenges}")
    private String challengesChannel;
    
    private Slack slack;
    private MethodsClient methods;
    
    @PostConstruct
    public void init() {
        if (botToken != null && !botToken.isEmpty()) {
            this.slack = Slack.getInstance();
            this.methods = slack.methods(botToken);
            log.info("Slack integration initialized for channel: {}", resultsChannel);
        } else {
            log.warn("Slack bot token not configured - Slack integration disabled");
        }
    }
    
    public void postGameResult(Game game, Player challenger, Player opponent) {
        if (methods == null) {
            log.debug("Slack not configured, skipping game result post");
            return;
        }
        
        try {
            if (game.isSinglesGame()) {
                postSinglesGameResult(game, challenger, opponent);
            } else {
                postDoublesGameResult(game);
            }
        } catch (Exception e) {
            log.error("Error posting game result to Slack", e);
        }
    }
    
    private void postSinglesGameResult(Game game, Player challenger, Player opponent) throws Exception {
        // Refresh player data to get latest ELO ratings
        Player refreshedChallenger = playerService.findPlayerById(challenger.getPlayerId());
        Player refreshedOpponent = playerService.findPlayerById(opponent.getPlayerId());
        
        Player winner = game.isChallengerWin() ? refreshedChallenger : refreshedOpponent;
        Player loser = game.isChallengerWin() ? refreshedOpponent : refreshedChallenger;
        int winnerScore = game.isChallengerWin() ? game.getChallengerTeamScore() : game.getOpponentTeamScore();
        int loserScore = game.isChallengerWin() ? game.getOpponentTeamScore() : game.getChallengerTeamScore();
        
        String gameType = game.isSinglesGame() ? "Singles" : "Doubles";
        String ratingType = game.isRatedGame() ? "Ranked" : "Normal";
        
        // Get current ratings
        double winnerRating = game.isRatedGame() ? winner.getSinglesRankedRating() : winner.getSinglesNormalRating();
        double loserRating = game.isRatedGame() ? loser.getSinglesRankedRating() : loser.getSinglesNormalRating();
        
        // Calculate approximate ELO changes (simplified)
        double eloChange = calculateEloChange(winnerRating, loserRating, true);
        
        List<LayoutBlock> blocks = new ArrayList<>();
        
        // Header section
        blocks.add(SectionBlock.builder()
            .text(MarkdownTextObject.builder()
                .text(String.format("🏓 *%s %s Game Complete!*", gameType, ratingType))
                .build())
            .build());
        
        // Game result section
        blocks.add(SectionBlock.builder()
            .text(MarkdownTextObject.builder()
                .text(String.format(
                    "🏆 *%s* defeated *%s*\n" +
                    "📊 Final Score: *%d - %d*\n" +
                    "⏰ Played at %s",
                    winner.getFullName(), loser.getFullName(),
                    winnerScore, loserScore,
                    LocalDateTime.now().format(timeFormat)
                ))
                .build())
            .build());
        
        blocks.add(DividerBlock.builder().build());
        
        // ELO changes section
        blocks.add(SectionBlock.builder()
            .text(MarkdownTextObject.builder()
                .text(String.format(
                    "📈 *ELO Changes*\n" +
                    "🥇 %s: %.0f (%s%.0f)\n" +
                    "🥈 %s: %.0f (%s%.0f)",
                    winner.getFullName(), winnerRating, eloFormat.format(eloChange), eloChange,
                    loser.getFullName(), loserRating, eloFormat.format(-eloChange), -eloChange
                ))
                .build())
            .build());
        
        // Head-to-head record
        String h2hRecord = getHeadToHeadRecord(winner, loser, game.isRatedGame());
        if (h2hRecord != null) {
            blocks.add(SectionBlock.builder()
                .text(MarkdownTextObject.builder()
                    .text("⚔️ *Head-to-Head:* " + h2hRecord)
                    .build())
                .build());
        }
        
        ChatPostMessageResponse response = methods.chatPostMessage(req -> req
            .channel(resultsChannel)
            .text(String.format("%s defeated %s %d-%d", winner.getFullName(), loser.getFullName(), winnerScore, loserScore))
            .blocks(blocks)
        );
        
        if (response.isOk()) {
            log.info("Posted singles game result to Slack: {}", game.getGameId());
        } else {
            log.error("Failed to post to Slack: {}", response.getError());
        }
    }
    
    private void postDoublesGameResult(Game game) throws Exception {
        List<Player> challengerTeam = game.getChallengerTeam().stream()
            .map(playerId -> playerService.findPlayerById(playerId))
            .collect(Collectors.toList());
        List<Player> opponentTeam = game.getOpponentTeam().stream()
            .map(playerId -> playerService.findPlayerById(playerId))
            .collect(Collectors.toList());
        
        List<Player> winningTeam = game.isChallengerTeamWin() ? challengerTeam : opponentTeam;
        List<Player> losingTeam = game.isChallengerTeamWin() ? opponentTeam : challengerTeam;
        int winnerScore = game.isChallengerTeamWin() ? game.getChallengerTeamScore() : game.getOpponentTeamScore();
        int loserScore = game.isChallengerTeamWin() ? game.getOpponentTeamScore() : game.getChallengerTeamScore();
        
        String gameType = "Doubles";
        String ratingType = game.isRatedGame() ? "Ranked" : "Normal";
        
        String winningTeamNames = winningTeam.stream()
            .map(Player::getFullName)
            .collect(Collectors.joining(" & "));
        String losingTeamNames = losingTeam.stream()
            .map(Player::getFullName)
            .collect(Collectors.joining(" & "));
        
        List<LayoutBlock> blocks = new ArrayList<>();
        
        blocks.add(SectionBlock.builder()
            .text(MarkdownTextObject.builder()
                .text(String.format("🏓 *%s %s Game Complete!*", gameType, ratingType))
                .build())
            .build());
        
        blocks.add(SectionBlock.builder()
            .text(MarkdownTextObject.builder()
                .text(String.format(
                    "🏆 *%s* defeated *%s*\n" +
                    "📊 Final Score: *%d - %d*\n" +
                    "⏰ Played at %s",
                    winningTeamNames, losingTeamNames,
                    winnerScore, loserScore,
                    LocalDateTime.now().format(timeFormat)
                ))
                .build())
            .build());
        
        ChatPostMessageResponse response = methods.chatPostMessage(req -> req
            .channel(resultsChannel)
            .text(String.format("%s defeated %s %d-%d", winningTeamNames, losingTeamNames, winnerScore, loserScore))
            .blocks(blocks)
        );
        
        if (response.isOk()) {
            log.info("Posted doubles game result to Slack: {}", game.getGameId());
        } else {
            log.error("Failed to post to Slack: {}", response.getError());
        }
    }
    
    // Helper methods
    private double calculateEloChange(double winnerRating, double loserRating, boolean won) {
        double expectedScore = 1.0 / (1.0 + Math.pow(10, (loserRating - winnerRating) / 400.0));
        double actualScore = won ? 1.0 : 0.0;
        return 32 * (actualScore - expectedScore);
    }
    
    private String getHeadToHeadRecord(Player player1, Player player2, boolean isRanked) {
        try {
            List<Game> player1Games = gameRepository.findByPlayerId(player1.getPlayerId());
            int player1Wins = 0;
            int player2Wins = 0;
            
            for (Game game : player1Games) {
                if (game.isRatedGame() == isRanked && game.isSinglesGame()) {
                    boolean hasPlayer2 = (game.getChallengerId().equals(player2.getPlayerId()) || 
                                         game.getOpponentId().equals(player2.getPlayerId()));
                    if (hasPlayer2) {
                        boolean player1Won = (game.getChallengerId().equals(player1.getPlayerId()) && game.isChallengerWin()) ||
                                           (game.getOpponentId().equals(player1.getPlayerId()) && game.isOpponentWin());
                        if (player1Won) {
                            player1Wins++;
                        } else {
                            player2Wins++;
                        }
                    }
                }
            }
            
            if (player1Wins + player2Wins > 0) {
                return String.format("%s leads %d-%d", 
                    player1Wins > player2Wins ? player1.getFullName() : player2.getFullName(),
                    Math.max(player1Wins, player2Wins), 
                    Math.min(player1Wins, player2Wins));
            }
            return null;
        } catch (Exception e) {
            log.error("Error calculating head-to-head record", e);
            return null;
        }
    }
    
    // Leaderboard functionality
    public void postDailyLeaderboard() {
        if (methods == null) return;
        
        try {
            List<Player> topPlayers = playerService.findAllPlayers().stream()
                .sorted((p1, p2) -> Double.compare(p2.getSinglesRankedRating(), p1.getSinglesRankedRating()))
                .limit(10)
                .collect(Collectors.toList());
            
            List<LayoutBlock> blocks = new ArrayList<>();
            
            blocks.add(SectionBlock.builder()
                .text(MarkdownTextObject.builder()
                    .text("🏆 *Daily Leaderboard - Top 10 Players*")
                    .build())
                .build());
            
            StringBuilder leaderboard = new StringBuilder();
            for (int i = 0; i < topPlayers.size(); i++) {
                Player player = topPlayers.get(i);
                String medal = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : String.format("%d.", i + 1);
                leaderboard.append(String.format("%s %s - %.0f ELO\n", 
                    medal, player.getFullName(), player.getSinglesRankedRating()));
            }
            
            blocks.add(SectionBlock.builder()
                .text(MarkdownTextObject.builder()
                    .text(leaderboard.toString())
                    .build())
                .build());
            
            methods.chatPostMessage(req -> req
                .channel(resultsChannel)
                .text("Daily Leaderboard")
                .blocks(blocks)
            );
            
            log.info("Posted daily leaderboard to Slack");
        } catch (Exception e) {
            log.error("Error posting daily leaderboard", e);
        }
    }
    
    // Player achievements
    public void postPlayerAchievement(Player player, String achievementName, String achievementDescription) {
        if (methods == null) return;
        
        try {
            List<LayoutBlock> blocks = new ArrayList<>();
            
            blocks.add(SectionBlock.builder()
                .text(MarkdownTextObject.builder()
                    .text(String.format("🎆 *Achievement Unlocked!*\n" +
                        "🎉 %s has earned: *%s*\n" +
                        "📝 %s",
                        player.getFullName(), 
                        achievementName,
                        achievementDescription))
                    .build())
                .build());
            
            methods.chatPostMessage(req -> req
                .channel(generalChannel)
                .text(String.format("%s unlocked achievement: %s", player.getFullName(), achievementName))
                .blocks(blocks)
            );
            
            log.info("Posted achievement notification for player: {}", player.getPlayerId());
        } catch (Exception e) {
            log.error("Error posting achievement notification", e);
        }
    }
    
    // New player welcome
    public void welcomeNewPlayer(Player player) {
        if (methods == null) return;
        
        try {
            String message = String.format(
                "🎉 *Welcome to Ping Pong ELO!*\n" +
                "Please give a warm welcome to *%s*!\n" +
                "🏓 Ready to serve up some competition? Challenge someone to your first game!",
                player.getFullName());
            
            methods.chatPostMessage(req -> req
                .channel(generalChannel)
                .text(String.format("Welcome %s to Ping Pong ELO!", player.getFullName()))
                .text(message)
            );
            
            log.info("Posted welcome message for new player: {}", player.getPlayerId());
        } catch (Exception e) {
            log.error("Error posting welcome message", e);
        }
    }
    
    // Win streaks
    public void postWinStreak(Player player, int streakLength) {
        if (methods == null || streakLength < 3) return; // Only post for streaks of 3+
        
        try {
            String emoji = streakLength >= 10 ? "🔥🔥🔥" : 
                          streakLength >= 5 ? "🔥🔥" : "🔥";
            
            String message = String.format(
                "%s *%s is on fire!*\n" +
                "🏆 Currently on a *%d game win streak*!\n" +
                "🎯 Who will be brave enough to challenge them?",
                emoji, player.getFullName(), streakLength);
            
            methods.chatPostMessage(req -> req
                .channel(resultsChannel)
                .text(String.format("%s is on a %d game win streak!", player.getFullName(), streakLength))
                .text(message)
            );
            
            log.info("Posted win streak notification for player: {}", player.getPlayerId());
        } catch (Exception e) {
            log.error("Error posting win streak notification", e);
        }
    }
    
    // Scheduled reports
    @Scheduled(cron = "0 0 18 * * MON-FRI") // 6 PM on weekdays
    public void postDailyActivity() {
        postDailyLeaderboard();
    }
    
    @Scheduled(cron = "0 0 9 * * MON") // 9 AM on Mondays
    public void postWeeklyRecap() {
        if (methods == null) return;
        
        try {
            String message = "📅 *Weekly Ping Pong Recap*\n" +
                           "🏓 New week, new opportunities to climb the leaderboard!\n" +
                           "💪 Who's ready to dominate this week?";
            
            methods.chatPostMessage(req -> req
                .channel(generalChannel)
                .text("Weekly Ping Pong Recap")
                .text(message)
            );
            
            log.info("Posted weekly recap to Slack");
        } catch (Exception e) {
            log.error("Error posting weekly recap", e);
        }
    }
    
    // Challenge system methods
    public void postChallengeNotification(Challenge challenge, Player challenger, Player challenged) {
        if (methods == null) return;
        
        try {
            List<LayoutBlock> blocks = new ArrayList<>();
            
            blocks.add(SectionBlock.builder()
                .text(MarkdownTextObject.builder()
                    .text(String.format("⚔️ *New Challenge!*\n" +
                        "🏓 *%s* has challenged *%s* to a %s game!\n" +
                        "💬 \"%s\"",
                        challenger.getFullName(),
                        challenged.getFullName(),
                        challenge.isRanked() ? "ranked" : "casual",
                        challenge.getMessage() != null ? challenge.getMessage() : "Let's play!"
                    ))
                    .build())
                .build());
            
            ChatPostMessageResponse response = methods.chatPostMessage(req -> req
                .channel(challengesChannel)
                .text(String.format("%s challenged %s to a game", challenger.getFullName(), challenged.getFullName()))
                .blocks(blocks)
            );
            
            if (response.isOk()) {
                log.info("Posted challenge notification to Slack: {}", challenge.getChallengeId());
            } else {
                log.error("Failed to post challenge to Slack: {}", response.getError());
            }
        } catch (Exception e) {
            log.error("Error posting challenge notification", e);
        }
    }
    
    public void updateChallengeMessage(Challenge challenge, Player challenger, Player challenged, String status) {
        if (methods == null) return;
        
        try {
            String emoji = switch (status.toLowerCase()) {
                case "accepted" -> "✅";
                case "declined" -> "❌";
                case "expired" -> "⏰";
                case "completed" -> "🏆";
                default -> "📝";
            };
            
            String message = String.format("%s Challenge %s: %s vs %s",
                emoji,
                status,
                challenger.getFullName(),
                challenged.getFullName()
            );
            
            methods.chatPostMessage(req -> req
                .channel(challengesChannel)
                .text(message)
            );
            
            log.info("Posted challenge update to Slack: {} - {}", challenge.getChallengeId(), status);
        } catch (Exception e) {
            log.error("Error posting challenge update", e);
        }
    }
    
    public void postChallengeReminder(Challenge challenge, Player challenger, Player challenged) {
        if (methods == null) return;
        
        try {
            String message = String.format("⏰ *Challenge Reminder*\n" +
                "🏓 %s, you have a pending challenge from *%s*!\n" +
                "⚡️ This challenge will expire soon - respond now!",
                challenged.getFullName(),
                challenger.getFullName()
            );
            
            methods.chatPostMessage(req -> req
                .channel(challengesChannel)
                .text(String.format("Challenge reminder: %s vs %s", challenger.getFullName(), challenged.getFullName()))
                .text(message)
            );
            
            log.info("Posted challenge reminder to Slack: {}", challenge.getChallengeId());
        } catch (Exception e) {
            log.error("Error posting challenge reminder", e);
        }
    }
}