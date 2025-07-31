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
import java.util.stream.Collectors;

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
            
            // Find challenged player and extract Slack user ID if possible
            PlayerLookupResult lookupResult = findPlayerBySlackMentionWithId(commandData.getTargetUser());
            if (lookupResult.player == null) {
                return ResponseEntity.ok(createEphemeralResponse("❌ Could not find player: " + commandData.getTargetUser() + 
                    "\\n💡 Try: `/challenge @username`, `/challenge First Last`, or `/challenge <@U123|username>`"));
            }
            
            // Create the challenge
            Challenge challenge = challengeService.createChallenge(
                challenger.getPlayerId(),
                lookupResult.player.getPlayerId(),
                commandData.getMessage(),
                commandData.isRanked(),
                commandData.isSingles(),
                channelId,
                userId,
                lookupResult.slackUserId
            );
            
            return ResponseEntity.ok(createResponse("🏓 Challenge sent to " + lookupResult.player.getFullName() + "!"));
            
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
     * Usage: /matchmaking [type]  
     * Types: singles-ranked, singles-normal, doubles-ranked, doubles-normal
     */
    @PostMapping("/command/matchmaking")
    public ResponseEntity<Map<String, Object>> handleMatchmakingCommand(@RequestParam Map<String, String> params) {
        try {
            String userId = params.get("user_id");
            String userName = params.get("user_name");
            String text = params.get("text");
            
            log.info("Processing /matchmaking command from user: {} with text: '{}'", userName, text);
            
            Player player = findPlayerBySlackIdentifier(userId, userName);
            if (player == null) {
                return ResponseEntity.ok(createEphemeralResponse("❌ You need to be registered in the ping pong system first!"));
            }
            
            // Parse the game type from command text
            String gameType = "singles-ranked"; // default
            if (text != null && !text.trim().isEmpty()) {
                gameType = text.trim().toLowerCase();
            }
            
            List<Player> suggestions = challengeService.getMatchmakingSuggestions(player.getPlayerId(), 8, gameType);
            
            if (suggestions.isEmpty()) {
                return ResponseEntity.ok(createEphemeralResponse("🤔 No suitable players found for " + getMatchmakingGameTypeDisplay(gameType)));
            }
            
            String gameTypeDisplay = getMatchmakingGameTypeDisplay(gameType);
            int playerRating = getPlayerRatingForType(player, gameType);
            
            StringBuilder response = new StringBuilder();
            
            if (gameType.contains("doubles")) {
                response.append(String.format("🎯 **%s Suggestions** (Your ELO: %d)\\n\\n", gameTypeDisplay, playerRating));
                response.append("👥 **Good Teammates:**\\n");
                
                int teammateCount = 0;
                for (int i = 0; i < suggestions.size() && teammateCount < 4; i++) {
                    Player suggested = suggestions.get(i);
                    int suggestedRating = getPlayerRatingForType(suggested, gameType);
                    double eloDiff = Math.abs(suggestedRating - playerRating);
                    
                    response.append(String.format("%d. **%s** (%d ELO, ±%.0f)\\n",
                        teammateCount + 1, suggested.getFullName(), suggestedRating, eloDiff));
                    teammateCount++;
                }
                
                response.append("\\n⚔️ **Potential Opponents:**\\n");
                int opponentCount = 0;
                for (int i = teammateCount; i < suggestions.size() && opponentCount < 4; i++) {
                    Player suggested = suggestions.get(i);
                    int suggestedRating = getPlayerRatingForType(suggested, gameType);
                    double eloDiff = Math.abs(suggestedRating - playerRating);
                    
                    response.append(String.format("%d. **%s** (%d ELO, ±%.0f)\\n",
                        opponentCount + 1, suggested.getFullName(), suggestedRating, eloDiff));
                    opponentCount++;
                }
            } else {
                response.append(String.format("🎯 **%s Opponents** (Your ELO: %d)\\n\\n", gameTypeDisplay, playerRating));
                
                for (int i = 0; i < suggestions.size(); i++) {
                    Player suggested = suggestions.get(i);
                    int suggestedRating = getPlayerRatingForType(suggested, gameType);
                    double eloDiff = Math.abs(suggestedRating - playerRating);
                    
                    response.append(String.format("%d. **%s** (%d ELO, ±%.0f)\\n",
                        i + 1, suggested.getFullName(), suggestedRating, eloDiff));
                }
            }
            
            response.append("\\n💡 Use `/challenge @player` to challenge someone!");
            
            return ResponseEntity.ok(createEphemeralResponse(response.toString()));
            
        } catch (Exception e) {
            log.error("Error processing matchmaking command", e);
            return ResponseEntity.ok(createEphemeralResponse("❌ Error getting suggestions: " + e.getMessage()));
        }
    }
    
    private String getMatchmakingGameTypeDisplay(String type) {
        switch (type.toLowerCase().trim()) {
            case "singles-normal": case "singles-casual": case "sn": return "Singles Normal";
            case "doubles-ranked": case "doubles": case "dr": return "Doubles Ranked";
            case "doubles-normal": case "doubles-casual": case "dn": return "Doubles Normal";
            default: return "Singles Ranked";
        }
    }
    
    private int getPlayerRatingForType(Player player, String gameType) {
        switch (gameType.toLowerCase().trim()) {
            case "singles-normal": case "singles-casual": case "sn": 
                return player.getSinglesNormalRating();
            case "doubles-ranked": case "doubles": case "dr": 
                return player.getDoublesRankedRating();
            case "doubles-normal": case "doubles-casual": case "dn": 
                return player.getDoublesNormalRating();
            default: 
                return player.getSinglesRankedRating();
        }
    }
    
    /**
     * Handle interactive button clicks
     */
    @PostMapping("/interactive")
    public ResponseEntity<Map<String, Object>> handleInteractiveAction(@RequestBody String payload) {
        try {
            log.info("Interactive action received: {}", payload);
            
            // Parse Slack interactive payload (URL-encoded JSON)
            String decodedPayload = java.net.URLDecoder.decode(payload.substring(8), "UTF-8"); // Remove "payload="
            log.info("Decoded payload: {}", decodedPayload);
            
            // Parse JSON payload
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode payloadJson = mapper.readTree(decodedPayload);
            
            String actionId = payloadJson.get("actions").get(0).get("action_id").asText();
            String challengeId = payloadJson.get("actions").get(0).get("value").asText();
            String userId = payloadJson.get("user").get("id").asText();
            String userName = payloadJson.get("user").get("name").asText();
            String channelId = payloadJson.get("channel").get("id").asText();
            String messageTs = payloadJson.get("message").get("ts").asText();
            
            log.info("Button clicked: actionId={}, challengeId={}, userId={}, userName={}", actionId, challengeId, userId, userName);
            
            // Find the player who clicked the button
            Player player = findPlayerBySlackIdentifier(userId, userName);
            if (player == null) {
                log.warn("Could not find player for user: {} ({})", userName, userId);
                return ResponseEntity.ok(createSlackResponse("❌ You need to be registered in the ping pong system first!"));
            }
            
            // Find the challenge
            UUID challengeUuid = UUID.fromString(challengeId);
            Challenge challenge = challengeService.findById(challengeUuid);
            if (challenge == null) {
                log.warn("Challenge not found: {}", challengeId);
                return ResponseEntity.ok(createSlackResponse("❌ Challenge not found or expired"));
            }
            
            // Verify the player is the challenged player
            if (!challenge.getChallengedId().equals(player.getPlayerId())) {
                log.warn("Player {} attempted to respond to challenge {} but is not the challenged player", player.getPlayerId(), challengeId);
                return ResponseEntity.ok(createSlackResponse("❌ You can only respond to challenges directed at you"));
            }
            
            // Handle the action
            if ("challenge_accept".equals(actionId)) {
                challengeService.acceptChallenge(challengeUuid, player.getPlayerId());
                slackService.postChallengeResponse(challenge, player, "accepted", channelId, messageTs);
                return ResponseEntity.ok(createSlackResponse("✅ Challenge accepted! Good luck!"));
            } else if ("challenge_decline".equals(actionId)) {
                challengeService.declineChallenge(challengeUuid, player.getPlayerId(), "Declined via Slack");
                slackService.postChallengeResponse(challenge, player, "declined", channelId, messageTs);
                return ResponseEntity.ok(createSlackResponse("❌ Challenge declined"));
            }
            
            return ResponseEntity.ok(createSlackResponse("❓ Unknown action"));
            
        } catch (Exception e) {
            log.error("Error processing interactive action", e);
            return ResponseEntity.ok(createSlackResponse("❌ Error processing your response: " + e.getMessage()));
        }
    }
    
    private Map<String, Object> createSlackResponse(String text) {
        Map<String, Object> response = new HashMap<>();
        response.put("text", text);
        response.put("response_type", "ephemeral");
        return response;
    }
    
    // Helper methods
    
    private Player findPlayerBySlackIdentifier(String slackUserId, String slackUserName) {
        List<Player> allPlayers = playerService.findAllPlayers();
        
        if (slackUserName == null || slackUserName.trim().isEmpty()) {
            log.warn("Empty Slack username provided");
            return null;
        }
        
        String normalizedSlackName = normalizeString(slackUserName);
        log.info("Attempting to match Slack user '{}' (normalized: '{}') to registered players", slackUserName, normalizedSlackName);
        
        // Strategy 1: Exact matches (case insensitive)
        Player exactMatch = allPlayers.stream()
            .filter(p -> {
                String firstName = normalizeString(p.getFirstName());
                String lastName = normalizeString(p.getLastName());
                String fullName = normalizeString(p.getFullName());
                
                return normalizedSlackName.equals(firstName) ||
                       normalizedSlackName.equals(lastName) ||
                       normalizedSlackName.equals(fullName);
            })
            .findFirst()
            .orElse(null);
        
        if (exactMatch != null) {
            log.info("Found exact match: {} for Slack user {}", exactMatch.getFullName(), slackUserName);
            return exactMatch;
        }
        
        // Strategy 2: Partial matches - Slack name contains player name or vice versa
        Player partialMatch = allPlayers.stream()
            .filter(p -> {
                String firstName = normalizeString(p.getFirstName());
                String lastName = normalizeString(p.getLastName());
                String fullName = normalizeString(p.getFullName());
                
                return normalizedSlackName.contains(firstName) ||
                       normalizedSlackName.contains(lastName) ||
                       firstName.contains(normalizedSlackName) ||
                       lastName.contains(normalizedSlackName) ||
                       fullName.contains(normalizedSlackName) ||
                       normalizedSlackName.contains(fullName);
            })
            .findFirst()
            .orElse(null);
        
        if (partialMatch != null) {
            log.info("Found partial match: {} for Slack user {}", partialMatch.getFullName(), slackUserName);
            return partialMatch;
        }
        
        // Strategy 3: Handle common username patterns (first.last, firstlast, etc.)
        Player patternMatch = findByUsernamePatterns(allPlayers, normalizedSlackName);
        if (patternMatch != null) {
            log.info("Found pattern match: {} for Slack user {}", patternMatch.getFullName(), slackUserName);
            return patternMatch;
        }
        
        // Strategy 4: Fuzzy matching - similar sounding names
        Player fuzzyMatch = findBySimilarity(allPlayers, normalizedSlackName);
        if (fuzzyMatch != null) {
            log.info("Found fuzzy match: {} for Slack user {}", fuzzyMatch.getFullName(), slackUserName);
            return fuzzyMatch;
        }
        
        log.warn("No match found for Slack user '{}'. Available players: {}", 
            slackUserName, 
            allPlayers.stream().map(Player::getFullName).collect(Collectors.joining(", ")));
        return null;
    }
    
    private String normalizeString(String input) {
        if (input == null) return "";
        return input.toLowerCase()
                   .replaceAll("[^a-z0-9]", "") // Remove all non-alphanumeric characters
                   .trim();
    }
    
    private Player findByUsernamePatterns(List<Player> players, String normalizedSlackName) {
        return players.stream()
            .filter(p -> {
                String firstName = normalizeString(p.getFirstName());
                String lastName = normalizeString(p.getLastName());
                
                // Check patterns like: first.last, first_last, firstlast, lastfirst, etc.
                String[] commonPatterns = {
                    firstName + lastName,           // johnsmith
                    lastName + firstName,           // smithjohn  
                    firstName + "." + lastName,     // john.smith (normalized removes dots)
                    firstName + "_" + lastName,     // john_smith (normalized removes underscores)
                    lastName + "." + firstName,     // smith.john
                    lastName + "_" + firstName,     // smith_john
                    firstName.substring(0, Math.min(firstName.length(), 3)) + lastName, // johsmith
                    firstName + lastName.substring(0, Math.min(lastName.length(), 3))  // johnsmi
                };
                
                for (String pattern : commonPatterns) {
                    if (normalizedSlackName.equals(normalizeString(pattern))) {
                        return true;
                    }
                }
                return false;
            })
            .findFirst()
            .orElse(null);
    }
    
    private Player findBySimilarity(List<Player> players, String normalizedSlackName) {
        // Simple similarity check - if slack name and player name share significant characters
        return players.stream()
            .filter(p -> {
                String firstName = normalizeString(p.getFirstName());
                String lastName = normalizeString(p.getLastName());
                
                // Check if they share at least 3 characters and slack name is at least 60% similar
                return (calculateSimilarity(normalizedSlackName, firstName) > 0.6 && firstName.length() >= 3) ||
                       (calculateSimilarity(normalizedSlackName, lastName) > 0.6 && lastName.length() >= 3) ||
                       (calculateSimilarity(normalizedSlackName, firstName + lastName) > 0.7);
            })
            .findFirst()
            .orElse(null);
    }
    
    private double calculateSimilarity(String s1, String s2) {
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;
        
        int maxLength = Math.max(s1.length(), s2.length());
        int commonChars = 0;
        
        // Count common characters
        for (char c : s1.toCharArray()) {
            if (s2.indexOf(c) >= 0) {
                commonChars++;
            }
        }
        
        return (double) commonChars / maxLength;
    }
    
    private Player findPlayerBySlackMention(String mention) {
        if (mention == null || mention.trim().isEmpty()) {
            return null;
        }
        
        // Strategy 1: Handle Slack mention format <@U123456|username>
        Pattern mentionPattern = Pattern.compile("<@([UW][A-Z0-9]+)\\|?([^>]*)>");
        Matcher matcher = mentionPattern.matcher(mention);
        
        if (matcher.find()) {
            String userId = matcher.group(1);
            String username = matcher.group(2);
            
            log.info("Found Slack mention format: userId={}, username={}", userId, username);
            return findPlayerBySlackIdentifier(userId, username);
        }
        
        // Strategy 2: Handle @username format (without brackets)
        if (mention.startsWith("@")) {
            String username = mention.substring(1); // Remove @ prefix
            log.info("Found @username format: {}", username);
            return findPlayerBySlackIdentifier(null, username);
        }
        
        // Strategy 3: Handle plain text name (display name or username)
        log.info("Treating as plain text name: {}", mention);
        return findPlayerBySlackIdentifier(null, mention);
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
    
    // Helper class for player lookup with Slack ID
    private static class PlayerLookupResult {
        public final Player player;
        public final String slackUserId;
        
        public PlayerLookupResult(Player player, String slackUserId) {
            this.player = player;
            this.slackUserId = slackUserId;
        }
    }
    
    private PlayerLookupResult findPlayerBySlackMentionWithId(String mention) {
        if (mention == null || mention.trim().isEmpty()) {
            return new PlayerLookupResult(null, null);
        }
        
        // Strategy 1: Handle Slack mention format <@U123456|username>
        Pattern mentionPattern = Pattern.compile("<@([UW][A-Z0-9]+)\\|?([^>]*)>");
        Matcher matcher = mentionPattern.matcher(mention);
        
        if (matcher.find()) {
            String userId = matcher.group(1);
            String username = matcher.group(2);
            
            log.info("Found Slack mention format: userId={}, username={}", userId, username);
            Player player = findPlayerBySlackIdentifier(userId, username);
            return new PlayerLookupResult(player, userId);
        }
        
        // Strategy 2 & 3: Handle @username format or plain text (no user ID available)
        Player player = findPlayerBySlackMention(mention);
        return new PlayerLookupResult(player, null);
    }
}