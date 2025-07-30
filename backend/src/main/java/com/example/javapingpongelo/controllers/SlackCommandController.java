package com.example.javapingpongelo.controllers;

import com.example.javapingpongelo.models.Challenge;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.services.ChallengeService;
import com.example.javapingpongelo.services.IPlayerService;
import com.example.javapingpongelo.services.SlackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/slack")
@Slf4j
public class SlackCommandController {
    
    @Autowired
    private ChallengeService challengeService;
    
    @Autowired
    private IPlayerService playerService;
    
    @Autowired
    private SlackService slackService;
    
    /**
     * Handle /challenge slash command
     */
    @PostMapping("/command/challenge")
    public ResponseEntity<Map<String, Object>> handleChallengeCommand(@RequestParam Map<String, String> params) {
        try {
            String userId = params.get("user_id");
            String userName = params.get("user_name");
            String channelId = params.get("channel_id");
            String text = params.get("text");
            
            log.info("Challenge command from {}: {}", userName, text);
            
            // Find challenger by Slack ID or username
            Player challenger = findPlayerBySlackIdentifier(userId, userName);
            if (challenger == null) {
                return ResponseEntity.ok(createEphemeralResponse("❌ You need to be registered in the ping pong system first!"));
            }
            
            // Parse the command text
            ChallengeCommandData commandData = parseChallengeCommand(text);
            if (commandData.hasError()) {
                return ResponseEntity.ok(createEphemeralResponse("❌ " + commandData.getError()));
            }
            
            // Find challenged player
            Player challenged = findPlayerBySlackMention(commandData.getTargetUser());
            if (challenged == null) {
                return ResponseEntity.ok(createEphemeralResponse("❌ Could not find player: " + commandData.getTargetUser()));
            }
            
            // Create the challenge
            Challenge challenge = challengeService.createChallenge(
                challenger.getPlayerId(),
                challenged.getPlayerId(),
                commandData.getMessage(),
                commandData.isRanked(),
                commandData.isSingles(),
                channelId,
                userId,
                commandData.getTargetUserId()
            );
            
            return ResponseEntity.ok(createResponse("🏓 Challenge sent to " + challenged.getFullName() + "!"));
            
        } catch (Exception e) {
            log.error("Error processing challenge command", e);
            return ResponseEntity.ok(createEphemeralResponse("❌ Error processing challenge: " + e.getMessage()));
        }
    }
    
    /**
     * Handle /challenges slash command
     */
    @PostMapping("/command/challenges")
    public ResponseEntity<Map<String, Object>> handleChallengesCommand(@RequestParam Map<String, String> params) {
        try {
            String userId = params.get("user_id");
            String userName = params.get("user_name");
            
            Player player = findPlayerBySlackIdentifier(userId, userName);
            if (player == null) {
                return ResponseEntity.ok(createEphemeralResponse("❌ You need to be registered in the ping pong system first!"));
            }
            
            List<Challenge> pendingChallenges = challengeService.getPendingChallenges(player.getPlayerId());
            
            if (pendingChallenges.isEmpty()) {
                return ResponseEntity.ok(createEphemeralResponse("📭 No pending challenges"));
            }
            
            StringBuilder response = new StringBuilder("🏓 **Your Pending Challenges:**\\n\\n");
            for (int i = 0; i < pendingChallenges.size(); i++) {
                Challenge challenge = pendingChallenges.get(i);
                Player challenger = playerService.findPlayerById(challenge.getChallengerId());
                
                response.append(String.format("%d. **%s** challenged you\\n", 
                    i + 1, challenger.getFullName()));
                
                if (challenge.getMessage() != null && !challenge.getMessage().isEmpty()) {
                    response.append("   💬 \"%s\"\\n").append(challenge.getMessage());
                }
                
                response.append(String.format("   🎯 %s %s • ⏰ Expires: %s\\n\\n",
                    challenge.isSingles() ? "Singles" : "Doubles",
                    challenge.isRanked() ? "Ranked" : "Normal",
                    challenge.getExpiresAt().toString()));
            }
            
            return ResponseEntity.ok(createEphemeralResponse(response.toString()));
            
        } catch (Exception e) {
            log.error("Error processing challenges command", e);
            return ResponseEntity.ok(createEphemeralResponse("❌ Error retrieving challenges: " + e.getMessage()));
        }
    }
    
    /**
     * Handle /leaderboard slash command
     * Usage: /leaderboard [type]
     * Types: singles-ranked, singles-normal, doubles-ranked, doubles-normal, all
     */
    @PostMapping("/command/leaderboard")
    public ResponseEntity<Map<String, Object>> handleLeaderboardCommand(@RequestParam Map<String, String> params) {
        try {
            String userName = params.get("user_name");
            String text = params.get("text");
            
            log.info("Processing /leaderboard command from user: {} with text: '{}'", userName, text);
            
            // Parse the leaderboard type from command text
            String leaderboardType = "singles-ranked"; // default
            if (text != null && !text.trim().isEmpty()) {
                leaderboardType = text.trim().toLowerCase();
            }
            
            boolean success = slackService.postLeaderboard(leaderboardType);
            if (success) {
                String typeDisplay = getLeaderboardTypeDisplay(leaderboardType);
                return ResponseEntity.ok(createEphemeralResponse("📊 " + typeDisplay + " leaderboard has been posted to the channel!"));
            } else {
                return ResponseEntity.ok(createEphemeralResponse("❌ Failed to post leaderboard - check logs for details"));
            }
        } catch (Exception e) {
            log.error("Error processing leaderboard command", e);
            return ResponseEntity.ok(createEphemeralResponse("❌ Error posting leaderboard: " + e.getMessage()));
        }
    }
    
    private String getLeaderboardTypeDisplay(String type) {
        switch (type.toLowerCase().trim()) {
            case "singles-normal": case "singles-casual": case "sn": return "Singles Normal";
            case "doubles-ranked": case "doubles": case "dr": return "Doubles Ranked";
            case "doubles-normal": case "doubles-casual": case "dn": return "Doubles Normal";
            case "all": return "All";
            default: return "Singles Ranked";
        }
    }
    
    /**
     * Handle /matchmaking slash command for ELO-based suggestions
     */
    @PostMapping("/command/matchmaking")
    public ResponseEntity<Map<String, Object>> handleMatchmakingCommand(@RequestParam Map<String, String> params) {
        try {
            String userId = params.get("user_id");
            String userName = params.get("user_name");
            
            Player player = findPlayerBySlackIdentifier(userId, userName);
            if (player == null) {
                return ResponseEntity.ok(createEphemeralResponse("❌ You need to be registered in the ping pong system first!"));
            }
            
            List<Player> suggestions = challengeService.getMatchmakingSuggestions(player.getPlayerId(), 5);
            
            if (suggestions.isEmpty()) {
                return ResponseEntity.ok(createEphemeralResponse("🤔 No suitable opponents found near your skill level"));
            }
            
            StringBuilder response = new StringBuilder(String.format(
                "🎯 **Suggested Opponents** (Your ELO: %d)\\n\\n", player.getSinglesRankedRating()));
            
            for (int i = 0; i < suggestions.size(); i++) {
                Player suggested = suggestions.get(i);
                double eloDiff = Math.abs(suggested.getSinglesRankedRating() - player.getSinglesRankedRating());
                
                response.append(String.format("%d. **%s** (%d ELO, ±%.0f)\\n",
                    i + 1, suggested.getFullName(), suggested.getSinglesRankedRating(), eloDiff));
            }
            
            response.append("\\n💡 Use `/challenge @player` to challenge someone!");
            
            return ResponseEntity.ok(createEphemeralResponse(response.toString()));
            
        } catch (Exception e) {
            log.error("Error processing matchmaking command", e);
            return ResponseEntity.ok(createEphemeralResponse("❌ Error getting suggestions: " + e.getMessage()));
        }
    }
    
    /**
     * Handle interactive button clicks
     */
    @PostMapping("/interactive")
    public ResponseEntity<Map<String, Object>> handleInteractiveAction(@RequestBody String payload) {
        try {
            // Parse Slack interactive payload
            // This would contain button click data, user info, etc.
            log.info("Interactive action received: {}", payload);
            
            // TODO: Parse payload and handle accept/decline actions
            // For now, return empty response
            return ResponseEntity.ok(Map.of());
            
        } catch (Exception e) {
            log.error("Error processing interactive action", e);
            return ResponseEntity.ok(Map.of());
        }
    }
    
    // Helper methods
    
    private Player findPlayerBySlackIdentifier(String slackUserId, String slackUserName) {
        // Try to find by Slack user ID first, then by username
        // This would need to be implemented based on how you store Slack user mappings
        List<Player> allPlayers = playerService.findAllPlayers();
        
        // For now, find by username match (case insensitive)
        return allPlayers.stream()
            .filter(p -> p.getFullName().toLowerCase().contains(slackUserName.toLowerCase()) ||
                        p.getFirstName().toLowerCase().equals(slackUserName.toLowerCase()) ||
                        p.getLastName().toLowerCase().equals(slackUserName.toLowerCase()))
            .findFirst()
            .orElse(null);
    }
    
    private Player findPlayerBySlackMention(String mention) {
        // Extract user ID from mention format <@U123456|username>
        Pattern pattern = Pattern.compile("<@([UW][A-Z0-9]+)\\|?([^>]*)>");
        Matcher matcher = pattern.matcher(mention);
        
        if (matcher.find()) {
            String userId = matcher.group(1);
            String username = matcher.group(2);
            
            // Find player by the username part for now
            return findPlayerBySlackIdentifier(userId, username);
        }
        
        return null;
    }
    
    private ChallengeCommandData parseChallengeCommand(String text) {
        ChallengeCommandData data = new ChallengeCommandData();
        
        if (text == null || text.trim().isEmpty()) {
            data.setError("Usage: `/challenge @player [message] [--ranked] [--doubles]`");
            return data;
        }
        
        String[] parts = text.trim().split("\\\\s+");
        if (parts.length == 0) {
            data.setError("Please specify a player to challenge");
            return data;
        }
        
        // First part should be the @mention
        data.setTargetUser(parts[0]);
        
        // Parse options and message
        StringBuilder message = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if ("--ranked".equals(part)) {
                data.setRanked(true);
            } else if ("--doubles".equals(part)) {
                data.setSingles(false);
            } else {
                if (message.length() > 0) message.append(" ");
                message.append(part);
            }
        }
        
        data.setMessage(message.toString());
        return data;
    }
    
    private Map<String, Object> createResponse(String text) {
        Map<String, Object> response = new HashMap<>();
        response.put("text", text);
        return response;
    }
    
    private Map<String, Object> createEphemeralResponse(String text) {
        Map<String, Object> response = new HashMap<>();
        response.put("text", text);
        response.put("response_type", "ephemeral");
        return response;
    }
    
    // Inner class for parsing challenge commands
    private static class ChallengeCommandData {
        private String targetUser;
        private String targetUserId;
        private String message = "";
        private boolean isRanked = false;
        private boolean isSingles = true;
        private String error;
        
        // Getters and setters
        public String getTargetUser() { return targetUser; }
        public void setTargetUser(String targetUser) { this.targetUser = targetUser; }
        
        public String getTargetUserId() { return targetUserId; }
        public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public boolean isRanked() { return isRanked; }
        public void setRanked(boolean ranked) { isRanked = ranked; }
        
        public boolean isSingles() { return isSingles; }
        public void setSingles(boolean singles) { isSingles = singles; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public boolean hasError() { return error != null; }
    }
}