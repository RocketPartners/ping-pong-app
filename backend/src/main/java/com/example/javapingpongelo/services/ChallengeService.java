package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.Challenge;
import com.example.javapingpongelo.models.ChallengeStatus;
import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.repositories.ChallengeRepository;
import com.example.javapingpongelo.repositories.GameRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChallengeService {
    
    @Autowired
    private ChallengeRepository challengeRepository;
    
    @Autowired
    private GameRepository gameRepository;
    
    @Autowired
    private IPlayerService playerService;
    
    @Autowired
    private SlackService slackService;
    
    private static final int MAX_CHALLENGES_PER_DAY = 10;
    private static final int CHALLENGE_EXPIRY_HOURS = 24;
    
    /**
     * Create a new challenge
     */
    @Transactional
    public Challenge createChallenge(UUID challengerId, UUID challengedId, String message, 
                                   boolean isRanked, boolean isSingles, String slackChannelId,
                                   String challengerSlackId, String challengedSlackId) {
        
        // Validation
        if (challengerId.equals(challengedId)) {
            throw new IllegalArgumentException("Cannot challenge yourself!");
        }
        
        // Check daily limit
        long todaysChallenges = challengeRepository.countChallengesSentToday(challengerId, 
                                                                           LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));
        if (todaysChallenges >= MAX_CHALLENGES_PER_DAY) {
            throw new IllegalArgumentException("Daily challenge limit reached! You can send " + MAX_CHALLENGES_PER_DAY + " challenges per day.");
        }
        
        // Check for recent duplicate challenges
        List<Challenge> recentChallenges = challengeRepository.findRecentChallengesBetweenPlayers(
            challengerId, challengedId, LocalDateTime.now().minusHours(1));
        
        boolean hasPendingChallenge = recentChallenges.stream()
            .anyMatch(c -> c.getStatus() == ChallengeStatus.PENDING);
            
        if (hasPendingChallenge) {
            throw new IllegalArgumentException("You already have a pending challenge with this player!");
        }
        
        // Create challenge
        Challenge challenge = Challenge.builder()
            .challengerId(challengerId)
            .challengedId(challengedId)
            .message(message)
            .isRanked(isRanked)
            .isSingles(isSingles)
            .status(ChallengeStatus.PENDING)
            .slackChannelId(slackChannelId)
            .challengerSlackId(challengerSlackId)
            .challengedSlackId(challengedSlackId)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(CHALLENGE_EXPIRY_HOURS))
            .build();
        
        Challenge savedChallenge = challengeRepository.save(challenge);
        log.info("Created challenge: {} -> {}", challengerId, challengedId);
        
        // Post to Slack
        try {
            Player challenger = playerService.findPlayerById(challengerId);
            Player challenged = playerService.findPlayerById(challengedId);
            slackService.postChallengeNotification(savedChallenge, challenger, challenged);
        } catch (Exception e) {
            log.error("Error posting challenge to Slack: {}", savedChallenge.getChallengeId(), e);
        }
        
        return savedChallenge;
    }
    
    /**
     * Accept a challenge
     */
    @Transactional
    public Challenge acceptChallenge(UUID challengeId, UUID playerId) {
        Challenge challenge = challengeRepository.findById(challengeId)
            .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));
        
        if (!challenge.getChallengedId().equals(playerId)) {
            throw new IllegalArgumentException("You can only accept challenges sent to you");
        }
        
        if (!challenge.canBeAccepted()) {
            throw new IllegalArgumentException("Challenge cannot be accepted (expired or already responded)");
        }
        
        challenge.setStatus(ChallengeStatus.ACCEPTED);
        challenge.setRespondedAt(LocalDateTime.now());
        
        Challenge savedChallenge = challengeRepository.save(challenge);
        log.info("Challenge accepted: {}", challengeId);
        
        // Update Slack message
        try {
            Player challenger = playerService.findPlayerById(challenge.getChallengerId());
            Player challenged = playerService.findPlayerById(challenge.getChallengedId());
            slackService.updateChallengeMessage(savedChallenge, challenger, challenged, "accepted");
        } catch (Exception e) {
            log.error("Error updating Slack message for accepted challenge: {}", challengeId, e);
        }
        
        return savedChallenge;
    }
    
    /**
     * Decline a challenge
     */
    @Transactional
    public Challenge declineChallenge(UUID challengeId, UUID playerId, String reason) {
        Challenge challenge = challengeRepository.findById(challengeId)
            .orElseThrow(() -> new IllegalArgumentException("Challenge not found"));
        
        if (!challenge.getChallengedId().equals(playerId)) {
            throw new IllegalArgumentException("You can only decline challenges sent to you");
        }
        
        if (challenge.getStatus() != ChallengeStatus.PENDING) {
            throw new IllegalArgumentException("Challenge has already been responded to");
        }
        
        challenge.setStatus(ChallengeStatus.DECLINED);
        challenge.setRespondedAt(LocalDateTime.now());
        challenge.setDeclineReason(reason);
        
        Challenge savedChallenge = challengeRepository.save(challenge);
        log.info("Challenge declined: {}", challengeId);
        
        // Update Slack message
        try {
            Player challenger = playerService.findPlayerById(challenge.getChallengerId());
            Player challenged = playerService.findPlayerById(challenge.getChallengedId());
            slackService.updateChallengeMessage(savedChallenge, challenger, challenged, "declined");
        } catch (Exception e) {
            log.error("Error updating Slack message for declined challenge: {}", challengeId, e);
        }
        
        return savedChallenge;
    }
    
    /**
     * Mark challenge as completed when game is logged
     */
    @Transactional
    public void markChallengeCompleted(UUID challengeId, UUID gameId) {
        Optional<Challenge> challengeOpt = challengeRepository.findById(challengeId);
        if (challengeOpt.isPresent()) {
            Challenge challenge = challengeOpt.get();
            challenge.setStatus(ChallengeStatus.COMPLETED);
            challenge.setCompletedGameId(gameId);
            challengeRepository.save(challenge);
            log.info("Marked challenge as completed: {} -> game: {}", challengeId, gameId);
        }
    }
    
    /**
     * Get smart matchmaking suggestions based on ELO (default: singles ranked)
     */
    public List<Player> getMatchmakingSuggestions(UUID playerId, int maxSuggestions) {
        return getMatchmakingSuggestions(playerId, maxSuggestions, "singles-ranked");
    }
    
    /**
     * Get smart matchmaking suggestions based on game type
     */
    public List<Player> getMatchmakingSuggestions(UUID playerId, int maxSuggestions, String gameType) {
        Player player = playerService.findPlayerById(playerId);
        if (player == null) {
            return List.of();
        }
        
        GameTypeConfig config = parseGameType(gameType);
        double playerElo = config.getRating(player);
        List<Player> allPlayers = playerService.findAllPlayers();
        
        // For doubles, we want to suggest both teammates and opponents
        if (config.isDoubles()) {
            return getDoublesMatchmakingSuggestions(player, allPlayers, config, maxSuggestions);
        } else {
            return getSinglesMatchmakingSuggestions(player, allPlayers, config, maxSuggestions);
        }
    }
    
    private List<Player> getSinglesMatchmakingSuggestions(Player player, List<Player> allPlayers, GameTypeConfig config, int maxSuggestions) {
        double playerElo = config.getRating(player);
        
        return allPlayers.stream()
            .filter(p -> !p.getPlayerId().equals(player.getPlayerId())) // Exclude self
            .filter(p -> Math.abs(config.getRating(p) - playerElo) <= 200) // Within 200 ELO
            .sorted((p1, p2) -> {
                double diff1 = Math.abs(config.getRating(p1) - playerElo);
                double diff2 = Math.abs(config.getRating(p2) - playerElo);
                return Double.compare(diff1, diff2);
            })
            .limit(maxSuggestions)
            .collect(Collectors.toList());
    }
    
    private List<Player> getDoublesMatchmakingSuggestions(Player player, List<Player> allPlayers, GameTypeConfig config, int maxSuggestions) {
        // For doubles, suggest good teammates first, then opponents
        List<Player> teammates = getTeammateSuggestions(player, allPlayers, config, maxSuggestions / 2);
        List<Player> opponents = getOpponentSuggestions(player, allPlayers, config, maxSuggestions / 2);
        
        List<Player> suggestions = new ArrayList<>();
        suggestions.addAll(teammates);
        suggestions.addAll(opponents);
        
        return suggestions.stream()
            .distinct()
            .limit(maxSuggestions)
            .collect(Collectors.toList());
    }
    
    private List<Player> getTeammateSuggestions(Player player, List<Player> allPlayers, GameTypeConfig config, int maxSuggestions) {
        // Get players who have been teammates before, prioritized by success rate
        Map<UUID, TeammateStats> teammateHistory = getTeammateHistory(player.getPlayerId(), config);
        
        return allPlayers.stream()
            .filter(p -> !p.getPlayerId().equals(player.getPlayerId()))
            .filter(p -> Math.abs(config.getRating(p) - config.getRating(player)) <= 300) // Wider range for teammates
            .sorted((p1, p2) -> {
                TeammateStats stats1 = teammateHistory.get(p1.getPlayerId());
                TeammateStats stats2 = teammateHistory.get(p2.getPlayerId());
                
                // Prioritize by: 1. Past success rate, 2. Games played together, 3. ELO similarity
                if (stats1 != null && stats2 != null) {
                    int successComparison = Double.compare(stats2.getWinRate(), stats1.getWinRate());
                    if (successComparison != 0) return successComparison;
                    
                    int gamesComparison = Integer.compare(stats2.getGamesPlayed(), stats1.getGamesPlayed());
                    if (gamesComparison != 0) return gamesComparison;
                }
                
                // If one has history and other doesn't, prioritize the one with history
                if (stats1 != null && stats2 == null) return -1;
                if (stats1 == null && stats2 != null) return 1;
                
                // Fall back to ELO similarity
                double eloDiff1 = Math.abs(config.getRating(p1) - config.getRating(player));
                double eloDiff2 = Math.abs(config.getRating(p2) - config.getRating(player));
                return Double.compare(eloDiff1, eloDiff2);
            })
            .limit(maxSuggestions)
            .collect(Collectors.toList());
    }
    
    private List<Player> getOpponentSuggestions(Player player, List<Player> allPlayers, GameTypeConfig config, int maxSuggestions) {
        // Similar ELO players who haven't been teammates
        Map<UUID, TeammateStats> teammateHistory = getTeammateHistory(player.getPlayerId(), config);
        double playerElo = config.getRating(player);
        
        return allPlayers.stream()
            .filter(p -> !p.getPlayerId().equals(player.getPlayerId()))
            .filter(p -> !teammateHistory.containsKey(p.getPlayerId())) // Prefer non-teammates as opponents
            .filter(p -> Math.abs(config.getRating(p) - playerElo) <= 200)
            .sorted((p1, p2) -> {
                double diff1 = Math.abs(config.getRating(p1) - playerElo);
                double diff2 = Math.abs(config.getRating(p2) - playerElo);
                return Double.compare(diff1, diff2);
            })
            .limit(maxSuggestions)
            .collect(Collectors.toList());
    }
    
    private Map<UUID, TeammateStats> getTeammateHistory(UUID playerId, GameTypeConfig config) {
        List<Game> playerGames = gameRepository.findByPlayerIdAndGameType(playerId, config.isSingles(), config.isRanked());
        Map<UUID, TeammateStats> teammateStats = new HashMap<>();
        
        for (Game game : playerGames) {
            if (!game.isDoublesGame()) continue;
            
            // Determine which team the player was on
            List<UUID> playerTeam = null;
            if (game.getChallengerTeam().contains(playerId)) {
                playerTeam = game.getChallengerTeam();
            } else if (game.getOpponentTeam().contains(playerId)) {
                playerTeam = game.getOpponentTeam();
            }
            
            if (playerTeam == null) continue;
            
            // Find teammate(s)
            for (UUID teammateId : playerTeam) {
                if (!teammateId.equals(playerId)) {
                    TeammateStats stats = teammateStats.computeIfAbsent(teammateId, k -> new TeammateStats());
                    stats.addGame(didPlayerWin(game, playerId));
                }
            }
        }
        
        return teammateStats;
    }
    
    private boolean didPlayerWin(Game game, UUID playerId) {
        if (game.getChallengerTeam().contains(playerId)) {
            return !game.isOpponentWin();
        } else {
            return game.isOpponentWin();
        }
    }
    
    private GameTypeConfig parseGameType(String gameType) {
        if (gameType == null) return GameTypeConfig.SINGLES_RANKED;
        
        switch (gameType.toLowerCase().trim()) {
            case "singles-normal":
            case "singles-casual":
            case "sn":
                return GameTypeConfig.SINGLES_NORMAL;
            case "doubles-ranked":
            case "doubles":
            case "dr":
                return GameTypeConfig.DOUBLES_RANKED;
            case "doubles-normal":
            case "doubles-casual":
            case "dn":
                return GameTypeConfig.DOUBLES_NORMAL;
            default:
                return GameTypeConfig.SINGLES_RANKED;
        }
    }
    
    private enum GameTypeConfig {
        SINGLES_RANKED("Singles Ranked", true, true, Player::getSinglesRankedRating),
        SINGLES_NORMAL("Singles Normal", true, false, Player::getSinglesNormalRating),
        DOUBLES_RANKED("Doubles Ranked", false, true, Player::getDoublesRankedRating),
        DOUBLES_NORMAL("Doubles Normal", false, false, Player::getDoublesNormalRating);
        
        private final String displayName;
        private final boolean singles;
        private final boolean ranked;
        private final Function<Player, Integer> ratingExtractor;
        
        GameTypeConfig(String displayName, boolean singles, boolean ranked, Function<Player, Integer> ratingExtractor) {
            this.displayName = displayName;
            this.singles = singles;
            this.ranked = ranked;
            this.ratingExtractor = ratingExtractor;
        }
        
        public String getDisplayName() { return displayName; }
        public boolean isSingles() { return singles; }
        public boolean isDoubles() { return !singles; }
        public boolean isRanked() { return ranked; }
        public int getRating(Player player) { return ratingExtractor.apply(player); }
    }
    
    private static class TeammateStats {
        private int gamesPlayed = 0;
        private int gamesWon = 0;
        
        public void addGame(boolean won) {
            gamesPlayed++;
            if (won) gamesWon++;
        }
        
        public int getGamesPlayed() { return gamesPlayed; }
        public double getWinRate() { 
            return gamesPlayed > 0 ? (double) gamesWon / gamesPlayed : 0.0; 
        }
    }
    
    /**
     * Get pending challenges for a player
     */
    public List<Challenge> getPendingChallenges(UUID playerId) {
        return challengeRepository.findPendingChallengesForPlayer(playerId, LocalDateTime.now());
    }
    
    /**
     * Get challenge history for a player
     */
    public Page<Challenge> getChallengeHistory(UUID playerId, Pageable pageable) {
        return challengeRepository.findChallengesForPlayer(playerId, pageable);
    }
    
    /**
     * Find challenge by Slack message
     */
    public Optional<Challenge> findBySlackMessage(String channelId, String messageTs) {
        return challengeRepository.findBySlackChannelIdAndSlackMessageTs(channelId, messageTs);
    }
    
    /**
     * Scheduled task to expire old challenges
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void expireOldChallenges() {
        List<Challenge> expiredChallenges = challengeRepository.findExpiredChallenges(LocalDateTime.now());
        
        for (Challenge challenge : expiredChallenges) {
            challenge.setStatus(ChallengeStatus.EXPIRED);
            challengeRepository.save(challenge);
            
            try {
                Player challenger = playerService.findPlayerById(challenge.getChallengerId());
                Player challenged = playerService.findPlayerById(challenge.getChallengedId());
                slackService.updateChallengeMessage(challenge, challenger, challenged, "expired");
            } catch (Exception e) {
                log.error("Error updating expired challenge message: {}", challenge.getChallengeId(), e);
            }
        }
        
        if (!expiredChallenges.isEmpty()) {
            log.info("Expired {} old challenges", expiredChallenges.size());
        }
    }
    
    /**
     * Scheduled task to remind about accepted challenges
     */
    @Scheduled(cron = "0 0 10,16 * * *") // 10 AM and 4 PM daily
    public void remindAboutAcceptedChallenges() {
        LocalDateTime reminderTime = LocalDateTime.now().minusHours(4);
        List<Challenge> challengesNeedingReminder = challengeRepository.findAcceptedChallengesNeedingReminder(reminderTime);
        
        for (Challenge challenge : challengesNeedingReminder) {
            try {
                Player challenger = playerService.findPlayerById(challenge.getChallengerId());
                Player challenged = playerService.findPlayerById(challenge.getChallengedId());
                slackService.postChallengeReminder(challenge, challenger, challenged);
            } catch (Exception e) {
                log.error("Error posting challenge reminder: {}", challenge.getChallengeId(), e);
            }
        }
        
        if (!challengesNeedingReminder.isEmpty()) {
            log.info("Sent {} challenge reminders", challengesNeedingReminder.size());
        }
    }
}