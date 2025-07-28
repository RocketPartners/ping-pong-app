package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SlackService {
    
    @Value("${slack.bot.token:}")
    private String botToken;
    
    @Value("${slack.channel.results:#ping-pong-results}")
    private String resultsChannel;
    
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
            // Determine winner and loser based on game results
            Player winner = game.isChallengerWin() ? challenger : opponent;
            Player loser = game.isChallengerWin() ? opponent : challenger;
            int winnerScore = game.isChallengerWin() ? game.getChallengerTeamScore() : game.getOpponentTeamScore();
            int loserScore = game.isChallengerWin() ? game.getOpponentTeamScore() : game.getChallengerTeamScore();
            
            String gameType = game.isSinglesGame() ? "Singles" : "Doubles";
            String ratingType = game.isRatedGame() ? "Ranked" : "Normal";
            
            String message = String.format(
                "🏓 *%s %s Game Complete!*\n" +
                "🏆 %s defeated %s\n" +
                "📊 Score: %d - %d\n" +
                "🎯 Game Type: %s %s",
                gameType, ratingType,
                winner.getFullName(), loser.getFullName(),
                winnerScore, loserScore,
                gameType, ratingType
            );
            
            ChatPostMessageResponse response = methods.chatPostMessage(req -> req
                .channel(resultsChannel)
                .text(message)
            );
            
            if (response.isOk()) {
                log.info("Posted game result to Slack: {}", game.getGameId());
            } else {
                log.error("Failed to post to Slack: {}", response.getError());
            }
        } catch (Exception e) {
            log.error("Error posting game result to Slack", e);
        }
    }
}