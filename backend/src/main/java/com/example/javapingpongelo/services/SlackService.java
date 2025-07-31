package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.Challenge;
import com.example.javapingpongelo.models.ChallengeStatus;
import com.example.javapingpongelo.repositories.ChallengeRepository;
import com.example.javapingpongelo.repositories.GameRepository;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.ActionsBlock;
import com.slack.api.model.block.element.ButtonElement;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SlackService {
    
    @Autowired
    private IPlayerService playerService;
    
    @Autowired
    private GameRepository gameRepository;
    
    @Autowired
    private ChallengeRepository challengeRepository;
    
    
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
            
            // Check for completed challenges and post results to challenge threads
            checkAndPostChallengeResults(game);
            
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
    public boolean postDailyLeaderboard() {
        return postLeaderboard("singles-ranked");
    }
    
    public boolean postLeaderboard(String type) {
        if (methods == null) {
            log.error("Slack methods not initialized");
            return false;
        }
        
        // Parse leaderboard type
        LeaderboardType leaderboardType = parseLeaderboardType(type);
        if (leaderboardType == null) {
            log.error("Invalid leaderboard type: {}", type);
            return false;
        }
        
        // Handle "all" type specially
        if (leaderboardType == LeaderboardType.ALL) {
            return postAllLeaderboards();
        }
        
        try {
            log.info("Fetching top players for {} leaderboard", type);
            List<Player> topPlayers = playerService.findAllPlayers().stream()
                .sorted(leaderboardType.getComparator())
                .limit(10)
                .collect(Collectors.toList());
            
            if (topPlayers.isEmpty()) {
                log.warn("No players found for leaderboard");
                return false;
            }
            
            log.info("Building {} leaderboard with {} players", type, topPlayers.size());
            List<LayoutBlock> blocks = new ArrayList<>();
            
            blocks.add(SectionBlock.builder()
                .text(MarkdownTextObject.builder()
                    .text(String.format("🏆 *%s Leaderboard - Top 10 Players*", leaderboardType.getDisplayName()))
                    .build())
                .build());
            
            StringBuilder leaderboard = new StringBuilder();
            for (int i = 0; i < topPlayers.size(); i++) {
                Player player = topPlayers.get(i);
                String medal = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : String.format("%d.", i + 1);
                int rating = leaderboardType.getRating(player);
                leaderboard.append(String.format("%s %s - %d ELO\n", 
                    medal, player.getFullName(), rating));
            }
            
            blocks.add(SectionBlock.builder()
                .text(MarkdownTextObject.builder()
                    .text(leaderboard.toString())
                    .build())
                .build());
            
            log.info("Posting {} leaderboard to Slack channel: {}", type, resultsChannel);
            ChatPostMessageResponse response = methods.chatPostMessage(req -> req
                .channel(resultsChannel)
                .text(leaderboardType.getDisplayName() + " Leaderboard")
                .blocks(blocks)
            );
            
            if (response.isOk()) {
                log.info("Successfully posted {} leaderboard to Slack", type);
                return true;
            } else {
                log.error("Failed to post {} leaderboard to Slack: {}", type, response.getError());
                return false;
            }
        } catch (Exception e) {
            log.error("Error posting {} leaderboard", type, e);
            return false;
        }
    }
    
    private LeaderboardType parseLeaderboardType(String type) {
        if (type == null) return LeaderboardType.SINGLES_RANKED;
        
        switch (type.toLowerCase().trim()) {
            case "singles-ranked":
            case "singles":
            case "ranked":
            case "sr":
                return LeaderboardType.SINGLES_RANKED;
            case "singles-normal":
            case "singles-casual":
            case "sn":
                return LeaderboardType.SINGLES_NORMAL;
            case "doubles-ranked":
            case "doubles":
            case "dr":
                return LeaderboardType.DOUBLES_RANKED;
            case "doubles-normal":
            case "doubles-casual":
            case "dn":
                return LeaderboardType.DOUBLES_NORMAL;
            case "all":
                return LeaderboardType.ALL;
            default:
                return null;
        }
    }
    
    public boolean postAllLeaderboards() {
        boolean allSuccess = true;
        allSuccess &= postLeaderboard("singles-ranked");
        allSuccess &= postLeaderboard("singles-normal");
        allSuccess &= postLeaderboard("doubles-ranked");
        allSuccess &= postLeaderboard("doubles-normal");
        return allSuccess;
    }
    
    private enum LeaderboardType {
        SINGLES_RANKED("Singles Ranked", 
            (p1, p2) -> Integer.compare(p2.getSinglesRankedRating(), p1.getSinglesRankedRating()),
            Player::getSinglesRankedRating),
        SINGLES_NORMAL("Singles Normal", 
            (p1, p2) -> Integer.compare(p2.getSinglesNormalRating(), p1.getSinglesNormalRating()),
            Player::getSinglesNormalRating),
        DOUBLES_RANKED("Doubles Ranked", 
            (p1, p2) -> Integer.compare(p2.getDoublesRankedRating(), p1.getDoublesRankedRating()),
            Player::getDoublesRankedRating),
        DOUBLES_NORMAL("Doubles Normal", 
            (p1, p2) -> Integer.compare(p2.getDoublesNormalRating(), p1.getDoublesNormalRating()),
            Player::getDoublesNormalRating),
        ALL("All Categories", null, null);
        
        private final String displayName;
        private final Comparator<Player> comparator;
        private final Function<Player, Integer> ratingExtractor;
        
        LeaderboardType(String displayName, Comparator<Player> comparator, Function<Player, Integer> ratingExtractor) {
            this.displayName = displayName;
            this.comparator = comparator;
            this.ratingExtractor = ratingExtractor;
        }
        
        public String getDisplayName() { return displayName; }
        public Comparator<Player> getComparator() { return comparator; }
        public int getRating(Player player) { return ratingExtractor.apply(player); }
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
        boolean success = postDailyLeaderboard();
        if (!success) {
            log.warn("Scheduled daily leaderboard posting failed");
        }
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
            String gameType = String.format("%s %s", 
                challenge.isSingles() ? "Singles" : "Doubles",
                challenge.isRanked() ? "Ranked" : "Normal");
            
            List<LayoutBlock> blocks = new ArrayList<>();
            
            blocks.add(SectionBlock.builder()
                .text(MarkdownTextObject.builder()
                    .text(String.format("⚔️ *New Challenge!*\n" +
                        "🏓 *%s* has challenged you to a %s game!\n" +
                        "💬 \"%s\"\n\n" +
                        "⏰ Challenge expires in 24 hours",
                        challenger.getFullName(),
                        gameType,
                        challenge.getMessage() != null ? challenge.getMessage() : "Let's play!"
                    ))
                    .build())
                .build());
            
            // Add interactive buttons
            blocks.add(ActionsBlock.builder()
                .elements(Arrays.asList(
                    ButtonElement.builder()
                        .text(PlainTextObject.builder().text("✅ Accept").build())
                        .style("primary")
                        .actionId("challenge_accept")
                        .value(challenge.getChallengeId().toString())
                        .build(),
                    ButtonElement.builder()
                        .text(PlainTextObject.builder().text("❌ Decline").build())
                        .style("danger")
                        .actionId("challenge_decline")
                        .value(challenge.getChallengeId().toString())
                        .build()
                ))
                .build());
            
            // Try to send DM to challenged player first
            boolean dmSent = false;
            if (challenge.getChallengedSlackId() != null) {
                try {
                    ChatPostMessageResponse dmResponse = methods.chatPostMessage(req -> req
                        .channel(challenge.getChallengedSlackId()) // DM to user
                        .text(String.format("%s challenged you to a %s game!", challenger.getFullName(), gameType))
                        .blocks(blocks)
                    );
                    
                    if (dmResponse.isOk()) {
                        log.info("Sent challenge DM to {}: {}", challenged.getFullName(), challenge.getChallengeId());
                        // Store message timestamp for threading replies
                        challenge.setSlackMessageTs(dmResponse.getMessage().getTs());
                        challengeRepository.save(challenge);
                        dmSent = true;
                    } else {
                        log.warn("Failed to send challenge DM: {}", dmResponse.getError());
                    }
                } catch (Exception e) {
                    log.warn("Error sending challenge DM, will fallback to channel", e);
                }
            }
            
            // Fallback: post to the channel where command was used
            if (!dmSent) {
                String targetChannel = challenge.getSlackChannelId() != null ? 
                    challenge.getSlackChannelId() : resultsChannel;
                
                ChatPostMessageResponse response = methods.chatPostMessage(req -> req
                    .channel(targetChannel)
                    .text(String.format("%s challenged %s to a %s game!", 
                        challenger.getFullName(), challenged.getFullName(), gameType))
                    .blocks(blocks)
                );
                
                if (response.isOk()) {
                    log.info("Posted challenge notification to channel {}: {}", targetChannel, challenge.getChallengeId());
                    // Store message timestamp for threading replies
                    challenge.setSlackMessageTs(response.getMessage().getTs());
                    challengeRepository.save(challenge);
                } else {
                    log.error("Failed to post challenge to channel {}: {}", targetChannel, response.getError());
                }
            }
            
        } catch (Exception e) {
            log.error("Error posting challenge notification", e);
        }
    }
    
    /**
     * Post challenge response as threaded reply
     */
    public void postChallengeResponse(Challenge challenge, Player respondingPlayer, String action, String channelId, String messageTs) {
        if (methods == null) return;
        
        try {
            String emoji = "accepted".equals(action) ? "✅" : "❌";
            String message = String.format("%s **%s** %s the challenge!", emoji, respondingPlayer.getFullName(), action);
            
            ChatPostMessageResponse response = methods.chatPostMessage(req -> req
                .channel(channelId)
                .threadTs(messageTs) // This makes it a threaded reply
                .text(message)
            );
            
            if (response.isOk()) {
                log.info("Posted challenge response to thread: {} {} challenge {}", respondingPlayer.getFullName(), action, challenge.getChallengeId());
            } else {
                log.error("Failed to post challenge response: {}", response.getError());
            }
        } catch (Exception e) {
            log.error("Error posting challenge response", e);
        }
    }
    
    /**
     * Check if a game completes any accepted challenges and post results to challenge threads
     */
    private void checkAndPostChallengeResults(Game game) {
        if (methods == null) return;
        
        try {
            log.info("Checking for completed challenges for game: {}", game.getGameId());
            
            // Get all player IDs involved in the game
            List<UUID> gamePlayerIds = new ArrayList<>();
            gamePlayerIds.add(game.getChallengerId());
            gamePlayerIds.add(game.getOpponentId());
            if (game.getChallengerTeam() != null) {
                gamePlayerIds.addAll(game.getChallengerTeam());
            }
            if (game.getOpponentTeam() != null) {
                gamePlayerIds.addAll(game.getOpponentTeam());
            }
            
            // Find accepted challenges between any of these players within the last 24 hours
            LocalDateTime oneDayAgo = LocalDateTime.now().minusHours(24);
            List<Challenge> acceptedChallenges = challengeRepository.findAcceptedChallengesInTimeRange(
                gamePlayerIds, oneDayAgo, LocalDateTime.now());
            
            for (Challenge challenge : acceptedChallenges) {
                // Check if this game involves the challenge participants
                if (isGameForChallenge(game, challenge)) {
                    log.info("Found matching challenge for game: challenge={}, game={}", challenge.getChallengeId(), game.getGameId());
                    postChallengeGameResult(game, challenge);
                    
                    // Mark challenge as completed
                    challenge.setStatus(ChallengeStatus.COMPLETED);
                    challenge.setCompletedGameId(game.getGameId());
                    challengeRepository.save(challenge);
                    
                    log.info("Marked challenge as completed: {}", challenge.getChallengeId());
                }
            }
            
        } catch (Exception e) {
            log.error("Error checking for completed challenges", e);
        }
    }
    
    /**
     * Check if a game involves the players from a specific challenge
     */
    private boolean isGameForChallenge(Game game, Challenge challenge) {
        List<UUID> gamePlayerIds = new ArrayList<>();
        gamePlayerIds.add(game.getChallengerId());
        gamePlayerIds.add(game.getOpponentId());
        if (game.getChallengerTeam() != null) {
            gamePlayerIds.addAll(game.getChallengerTeam());
        }
        if (game.getOpponentTeam() != null) {
            gamePlayerIds.addAll(game.getOpponentTeam());
        }
        
        // Check if both challenge participants are in the game
        boolean challengerInGame = gamePlayerIds.contains(challenge.getChallengerId());
        boolean challengedInGame = gamePlayerIds.contains(challenge.getChallengedId());
        
        // For singles challenges, both players must be in the game
        if (challenge.isSingles()) {
            return challengerInGame && challengedInGame && game.isSinglesGame();
        }
        
        // For doubles challenges, it's more complex - we need to check team composition
        // For now, just check if both players are in the game and it's a doubles game
        return challengerInGame && challengedInGame && game.isDoublesGame();
    }
    
    /**
     * Post game result as threaded reply to challenge message
     */
    private void postChallengeGameResult(Game game, Challenge challenge) {
        if (methods == null || challenge.getSlackMessageTs() == null) return;
        
        try {
            // Get player details
            Player challenger = playerService.findPlayerById(challenge.getChallengerId());
            Player challenged = playerService.findPlayerById(challenge.getChallengedId());
            
            // Determine winner and scores
            String winnerName, loserName;
            int winnerScore, loserScore;
            
            if (game.isSinglesGame()) {
                boolean challengerWon = game.isChallengerWin();
                winnerName = challengerWon ? challenger.getFullName() : challenged.getFullName();
                loserName = challengerWon ? challenged.getFullName() : challenger.getFullName();
                winnerScore = challengerWon ? game.getChallengerTeamScore() : game.getOpponentTeamScore();
                loserScore = challengerWon ? game.getOpponentTeamScore() : game.getChallengerTeamScore();
            } else {
                // For doubles, determine which team each player was on
                boolean challengerOnWinningTeam = isPlayerOnWinningTeam(game, challenge.getChallengerId());
                boolean challengedOnWinningTeam = isPlayerOnWinningTeam(game, challenge.getChallengedId());
                
                if (challengerOnWinningTeam == challengedOnWinningTeam) {
                    // Both players on same team - they were teammates, not opponents
                    String teamResult = challengerOnWinningTeam ? "won" : "lost";
                    winnerName = String.format("%s & %s", challenger.getFullName(), challenged.getFullName());
                    loserName = "their opponents";
                    winnerScore = challengerOnWinningTeam ? game.getChallengerTeamScore() : game.getOpponentTeamScore();
                    loserScore = challengerOnWinningTeam ? game.getOpponentTeamScore() : game.getChallengerTeamScore();
                } else {
                    // Players were on opposing teams
                    winnerName = challengerOnWinningTeam ? challenger.getFullName() : challenged.getFullName();
                    loserName = challengerOnWinningTeam ? challenged.getFullName() : challenger.getFullName();
                    winnerScore = challengerOnWinningTeam ? game.getChallengerTeamScore() : game.getOpponentTeamScore();
                    loserScore = challengerOnWinningTeam ? game.getOpponentTeamScore() : game.getChallengerTeamScore();
                }
            }
            
            String gameType = String.format("%s %s", 
                game.isSinglesGame() ? "Singles" : "Doubles",
                game.isRatedGame() ? "Ranked" : "Normal");
            
            String resultMessage = String.format("🏆 **Game Complete!**\n" +
                "🏓 %s defeated %s **%d-%d** in %s\n" +
                "⏰ Played at %s",
                winnerName, loserName, winnerScore, loserScore, gameType,
                LocalDateTime.now().format(timeFormat));
            
            // Determine which channel to post to
            String channelId = challenge.getSlackChannelId() != null ? 
                challenge.getSlackChannelId() : resultsChannel;
            
            ChatPostMessageResponse response = methods.chatPostMessage(req -> req
                .channel(channelId)
                .threadTs(challenge.getSlackMessageTs()) // Thread to original challenge
                .text(resultMessage)
            );
            
            if (response.isOk()) {
                log.info("Posted challenge game result to thread: challenge={}, game={}", challenge.getChallengeId(), game.getGameId());
            } else {
                log.error("Failed to post challenge game result: {}", response.getError());
            }
            
        } catch (Exception e) {
            log.error("Error posting challenge game result", e);
        }
    }
    
    /**
     * Determine if a player was on the winning team
     */
    private boolean isPlayerOnWinningTeam(Game game, UUID playerId) {
        boolean playerOnChallengerTeam = game.getChallengerId().equals(playerId) || 
            (game.getChallengerTeam() != null && game.getChallengerTeam().contains(playerId));
        
        if (playerOnChallengerTeam) {
            return game.isChallengerWin();
        } else {
            return game.isOpponentWin();
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