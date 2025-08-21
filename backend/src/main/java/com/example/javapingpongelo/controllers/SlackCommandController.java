package com.example.javapingpongelo.controllers;

import com.example.javapingpongelo.models.Challenge;
import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.repositories.GameRepository;
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
    
    @Autowired
    private GameRepository gameRepository;
    
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
     * Handle interactive button clicks and shortcuts
     */
    @PostMapping("/interactive")
    public ResponseEntity<Map<String, Object>> handleInteractiveAction(@RequestBody String payload) {
        try {
            log.info("Interactive action received: {}", payload);
            
            // Parse Slack interactive payload (URL-encoded JSON)
            String decodedPayload;
            if (payload.startsWith("payload=")) {
                decodedPayload = java.net.URLDecoder.decode(payload.substring(8), "UTF-8");
            } else {
                decodedPayload = payload;
            }
            log.info("Decoded payload: {}", decodedPayload);
            
            // Parse JSON payload
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode payloadJson = mapper.readTree(decodedPayload);
            
            // Check if this is a shortcut or button interaction
            String type = payloadJson.get("type").asText();
            if ("shortcut".equals(type)) {
                return handleShortcutPayload(payloadJson);
            }
            
            // Handle button interactions (existing logic)
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
    
    /**
     * Handle shortcut payloads within the interactive endpoint
     */
    private ResponseEntity<Map<String, Object>> handleShortcutPayload(com.fasterxml.jackson.databind.JsonNode payloadJson) {
        try {
            String callbackId = payloadJson.get("callback_id").asText();
            String userId = payloadJson.get("user").get("id").asText();
            String userName = payloadJson.get("user").get("name").asText();
            String triggerId = payloadJson.get("trigger_id").asText();
            
            log.info("Shortcut: callbackId={}, userId={}, userName={}", callbackId, userId, userName);
            
            // Find the player
            Player player = findPlayerBySlackIdentifier(userId, userName);
            if (player == null && !"player_spotlight".equals(callbackId)) {
                return ResponseEntity.ok(createSlackResponse("❌ You need to be registered in the ping pong system first!"));
            }
            
            // Route to appropriate handler
            switch (callbackId) {
                case "quick_challenge":
                    return handleQuickChallengeShortcut(triggerId, player);
                case "my_stats":
                    return handleMyStatsShortcut(triggerId, player);
                case "leaderboards":
                    return handleLeaderboardsShortcut(triggerId);
                case "find_opponent":
                    return handleFindOpponentShortcut(triggerId, player);
                case "tournament_creator":
                    return handleTournamentCreatorShortcut(triggerId, player);
                case "hot_streaks":
                    return handleHotStreaksShortcut(triggerId);
                case "player_spotlight":
                    return handlePlayerSpotlightShortcut(triggerId);
                case "challenge_person":
                    return handleChallengePersonShortcut(payloadJson, triggerId, player);
                case "check_head_to_head":
                    return handleHeadToHeadShortcut(payloadJson, triggerId, player);
                default:
                    return ResponseEntity.ok(createSlackResponse("❓ Unknown shortcut"));
            }
            
        } catch (Exception e) {
            log.error("Error processing shortcut", e);
            return ResponseEntity.ok(createSlackResponse("❌ Error processing shortcut: " + e.getMessage()));
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
    
    // ==================== SHORTCUT HANDLERS ====================
    
    /**
     * ⚡ Quick Challenge - Modal to challenge someone
     */
    private ResponseEntity<Map<String, Object>> handleQuickChallengeShortcut(String triggerId, Player player) {
        return ResponseEntity.ok(createSlackResponse("⚡ Quick Challenge feature coming soon!"));
    }
    
    /**
     * 📊 My Stats - Show personal statistics
     */
    private ResponseEntity<Map<String, Object>> handleMyStatsShortcut(String triggerId, Player player) {
        try {
            List<Game> playerGames = gameRepository.findByPlayerId(player.getPlayerId());
            
            // Calculate stats
            long totalGames = playerGames.size();
            long wins = playerGames.stream().filter(g -> isPlayerWinner(g, player.getPlayerId())).count();
            long losses = totalGames - wins;
            double winRate = totalGames > 0 ? (double) wins / totalGames * 100 : 0;
            
            StringBuilder stats = new StringBuilder();
            stats.append(String.format("📊 **%s's Statistics**\n\n", player.getFullName()));
            stats.append(String.format("🎯 **Overall Record**: %d-%d (%.1f%% win rate)\n\n", wins, losses, winRate));
            
            // ELO Ratings
            stats.append("🏆 **Current ELO Ratings**\n");
            stats.append(String.format("• Singles Ranked: %d\n", player.getSinglesRankedRating()));
            stats.append(String.format("• Singles Normal: %d\n", player.getSinglesNormalRating()));
            stats.append(String.format("• Doubles Ranked: %d\n", player.getDoublesRankedRating()));
            stats.append(String.format("• Doubles Normal: %d\n", player.getDoublesNormalRating()));
            
            return ResponseEntity.ok(createSlackResponse(stats.toString()));
            
        } catch (Exception e) {
            log.error("Error getting player stats", e);
            return ResponseEntity.ok(createSlackResponse("❌ Error retrieving your stats"));
        }
    }
    
    /**
     * 🏆 Leaderboards - Quick view of all leaderboards
     */
    private ResponseEntity<Map<String, Object>> handleLeaderboardsShortcut(String triggerId) {
        slackService.postAllLeaderboards();
        return ResponseEntity.ok(createSlackResponse("🏆 All leaderboards have been posted to the channel!"));
    }
    
    /**
     * 🎯 Find Opponent - ELO-based matchmaking
     */
    private ResponseEntity<Map<String, Object>> handleFindOpponentShortcut(String triggerId, Player player) {
        try {
            List<Player> suggestions = challengeService.getMatchmakingSuggestions(player.getPlayerId(), 5, "singles-ranked");
            
            if (suggestions.isEmpty()) {
                return ResponseEntity.ok(createSlackResponse("🤔 No suitable opponents found right now"));
            }
            
            StringBuilder response = new StringBuilder();
            response.append(String.format("🎯 **Perfect Opponents for %s** (ELO: %d)\n\n", 
                player.getFullName(), player.getSinglesRankedRating()));
            
            for (int i = 0; i < suggestions.size(); i++) {
                Player suggested = suggestions.get(i);
                int ratingDiff = Math.abs(suggested.getSinglesRankedRating() - player.getSinglesRankedRating());
                response.append(String.format("%d. **%s** (ELO: %d, ±%d)\n", 
                    i + 1, suggested.getFullName(), suggested.getSinglesRankedRating(), ratingDiff));
            }
            
            response.append("\n💡 Use `/challenge @player` to challenge someone!");
            
            return ResponseEntity.ok(createSlackResponse(response.toString()));
            
        } catch (Exception e) {
            log.error("Error getting matchmaking suggestions", e);
            return ResponseEntity.ok(createSlackResponse("❌ Error finding opponents"));
        }
    }
    
    /**
     * 📅 Tournament Creator - Modal to create tournament
     */
    private ResponseEntity<Map<String, Object>> handleTournamentCreatorShortcut(String triggerId, Player player) {
        return ResponseEntity.ok(createSlackResponse("📅 Tournament Creator feature coming soon!"));
    }
    
    /**
     * 🔥 Hot Streaks - Show current win streaks
     */
    private ResponseEntity<Map<String, Object>> handleHotStreaksShortcut(String triggerId) {
        try {
            List<Player> allPlayers = playerService.findAllPlayers();
            
            // Calculate win streaks for all players
            List<PlayerStreak> streaks = allPlayers.stream()
                .map(this::calculateCurrentStreak)
                .filter(streak -> streak.streakLength >= 3) // Only show 3+ streaks
                .sorted((a, b) -> Integer.compare(b.streakLength, a.streakLength))
                .limit(5)
                .collect(Collectors.toList());
            
            if (streaks.isEmpty()) {
                return ResponseEntity.ok(createSlackResponse("😴 No hot streaks right now - who will be the first to get on fire?"));
            }
            
            StringBuilder response = new StringBuilder();
            response.append("🔥 **Players on Fire!**\n\n");
            
            for (int i = 0; i < streaks.size(); i++) {
                PlayerStreak streak = streaks.get(i);
                String fire = streak.streakLength >= 10 ? "🔥🔥🔥" : 
                            streak.streakLength >= 5 ? "🔥🔥" : "🔥";
                response.append(String.format("%d. %s **%s** - %d game win streak!\n", 
                    i + 1, fire, streak.playerName, streak.streakLength));
            }
            
            response.append("\n⚔️ Who dares to challenge them?");
            
            return ResponseEntity.ok(createSlackResponse(response.toString()));
            
        } catch (Exception e) {
            log.error("Error getting hot streaks", e);
            return ResponseEntity.ok(createSlackResponse("❌ Error getting hot streaks"));
        }
    }
    
    /**
     * 🔍 Player Spotlight - Search and analyze any player
     */
    private ResponseEntity<Map<String, Object>> handlePlayerSpotlightShortcut(String triggerId) {
        return ResponseEntity.ok(createSlackResponse("🔍 Player Spotlight feature coming soon!"));
    }
    
    /**
     * Challenge Person (Message Shortcut)
     */
    private ResponseEntity<Map<String, Object>> handleChallengePersonShortcut(com.fasterxml.jackson.databind.JsonNode payload, String triggerId, Player challenger) {
        try {
            // Get the user from the message
            String targetUserId = payload.get("message").get("user").asText();
            Player targetPlayer = findPlayerBySlackIdentifier(targetUserId, null);
            
            if (targetPlayer == null) {
                return ResponseEntity.ok(createSlackResponse("❌ That person is not registered in the ping pong system"));
            }
            
            if (targetPlayer.getPlayerId().equals(challenger.getPlayerId())) {
                return ResponseEntity.ok(createSlackResponse("❌ You can't challenge yourself!"));
            }
            
            // Create quick challenge
            Challenge challenge = challengeService.createChallenge(
                challenger.getPlayerId(),
                targetPlayer.getPlayerId(),
                "Challenged via message shortcut!",
                false, // Normal game
                true,  // Singles
                payload.get("channel").get("id").asText(),
                payload.get("user").get("id").asText(),
                targetUserId
            );
            
            return ResponseEntity.ok(createSlackResponse(String.format("⚔️ Challenge sent to %s!", targetPlayer.getFullName())));
            
        } catch (Exception e) {
            log.error("Error creating challenge from message shortcut", e);
            return ResponseEntity.ok(createSlackResponse("❌ Error creating challenge"));
        }
    }
    
    /**
     * Check Head-to-Head (Message Shortcut)
     */
    private ResponseEntity<Map<String, Object>> handleHeadToHeadShortcut(com.fasterxml.jackson.databind.JsonNode payload, String triggerId, Player player) {
        try {
            // Get the user from the message
            String targetUserId = payload.get("message").get("user").asText();
            Player targetPlayer = findPlayerBySlackIdentifier(targetUserId, null);
            
            if (targetPlayer == null) {
                return ResponseEntity.ok(createSlackResponse("❌ That person is not registered in the ping pong system"));
            }
            
            if (targetPlayer.getPlayerId().equals(player.getPlayerId())) {
                return ResponseEntity.ok(createSlackResponse("❌ You can't check head-to-head against yourself!"));
            }
            
            // Calculate head-to-head record
            String h2hRecord = getDetailedHeadToHeadRecord(player, targetPlayer);
            
            return ResponseEntity.ok(createSlackResponse(h2hRecord));
            
        } catch (Exception e) {
            log.error("Error getting head-to-head record", e);
            return ResponseEntity.ok(createSlackResponse("❌ Error getting head-to-head record"));
        }
    }
    
    // Helper methods for shortcuts
    
    private boolean isPlayerWinner(Game game, UUID playerId) {
        return (game.getChallengerId().equals(playerId) && game.isChallengerWin()) ||
               (game.getOpponentId().equals(playerId) && game.isOpponentWin()) ||
               (game.getChallengerTeam() != null && game.getChallengerTeam().contains(playerId) && game.isChallengerTeamWin()) ||
               (game.getOpponentTeam() != null && game.getOpponentTeam().contains(playerId) && game.isOpponentTeamWin());
    }
    
    private PlayerStreak calculateCurrentStreak(Player player) {
        try {
            List<Game> recentGames = gameRepository.findByPlayerId(player.getPlayerId())
                .stream()
                .sorted((a, b) -> b.getDatePlayed().compareTo(a.getDatePlayed()))
                .collect(Collectors.toList());
            
            int streak = 0;
            for (Game game : recentGames) {
                if (isPlayerWinner(game, player.getPlayerId())) {
                    streak++;
                } else {
                    break; // Streak broken
                }
            }
            
            return new PlayerStreak(player.getFullName(), streak);
        } catch (Exception e) {
            return new PlayerStreak(player.getFullName(), 0);
        }
    }
    
    private String getDetailedHeadToHeadRecord(Player player1, Player player2) {
        try {
            List<Game> player1Games = gameRepository.findByPlayerId(player1.getPlayerId());
            int player1Wins = 0;
            int player2Wins = 0;
            
            for (Game game : player1Games) {
                if (game.isSinglesGame()) { // Focus on singles for now
                    boolean hasPlayer2 = (game.getChallengerId().equals(player2.getPlayerId()) || 
                                         game.getOpponentId().equals(player2.getPlayerId()));
                    if (hasPlayer2) {
                        if (isPlayerWinner(game, player1.getPlayerId())) {
                            player1Wins++;
                        } else {
                            player2Wins++;
                        }
                    }
                }
            }
            
            if (player1Wins + player2Wins == 0) {
                return String.format("⚔️ **%s vs %s**\n\nNo games played yet! Time to settle this on the table! 🏓", 
                    player1.getFullName(), player2.getFullName());
            }
            
            String leader = player1Wins > player2Wins ? player1.getFullName() : player2.getFullName();
            int leaderWins = Math.max(player1Wins, player2Wins);
            int otherWins = Math.min(player1Wins, player2Wins);
            
            return String.format(
                "⚔️ **Head-to-Head: %s vs %s**\n\n" +
                "🏆 %s leads %d-%d\n" +
                "📊 Total games: %d\n" +
                "🎯 %s's win rate: %.1f%%",
                player1.getFullName(), player2.getFullName(),
                leader, leaderWins, otherWins,
                player1Wins + player2Wins,
                player1.getFullName(), 
                (double) player1Wins / (player1Wins + player2Wins) * 100
            );
            
        } catch (Exception e) {
            return "❌ Error calculating head-to-head record";
        }
    }
    
    // Helper class for streaks
    private static class PlayerStreak {
        final String playerName;
        final int streakLength;
        
        PlayerStreak(String playerName, int streakLength) {
            this.playerName = playerName;
            this.streakLength = streakLength;
        }
    }
}