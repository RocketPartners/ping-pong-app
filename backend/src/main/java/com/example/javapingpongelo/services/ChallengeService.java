package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.Challenge;
import com.example.javapingpongelo.models.ChallengeStatus;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.repositories.ChallengeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChallengeService {
    
    @Autowired
    private ChallengeRepository challengeRepository;
    
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
     * Get smart matchmaking suggestions based on ELO
     */
    public List<Player> getMatchmakingSuggestions(UUID playerId, int maxSuggestions) {
        Player player = playerService.findPlayerById(playerId);
        if (player == null) {
            return List.of();
        }
        
        double playerElo = player.getSinglesRankedRating();
        List<Player> allPlayers = playerService.findAllPlayers();
        
        return allPlayers.stream()
            .filter(p -> !p.getPlayerId().equals(playerId)) // Exclude self
            .filter(p -> Math.abs(p.getSinglesRankedRating() - playerElo) <= 200) // Within 200 ELO
            .sorted((p1, p2) -> {
                double diff1 = Math.abs(p1.getSinglesRankedRating() - playerElo);
                double diff2 = Math.abs(p2.getSinglesRankedRating() - playerElo);
                return Double.compare(diff1, diff2);
            })
            .limit(maxSuggestions)
            .collect(Collectors.toList());
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