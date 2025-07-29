package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.Challenge;
import com.example.javapingpongelo.models.ChallengeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, UUID> {
    
    // Find pending challenges for a player
    @Query("SELECT c FROM Challenge c WHERE c.challengedId = :playerId AND c.status = 'PENDING' AND c.expiresAt > :now ORDER BY c.createdAt DESC")
    List<Challenge> findPendingChallengesForPlayer(@Param("playerId") UUID playerId, @Param("now") LocalDateTime now);
    
    // Find all challenges involving a player
    @Query("SELECT c FROM Challenge c WHERE (c.challengerId = :playerId OR c.challengedId = :playerId) ORDER BY c.createdAt DESC")
    Page<Challenge> findChallengesForPlayer(@Param("playerId") UUID playerId, Pageable pageable);
    
    // Find recent challenges between two players
    @Query("SELECT c FROM Challenge c WHERE " +
           "((c.challengerId = :player1 AND c.challengedId = :player2) OR " +
           " (c.challengerId = :player2 AND c.challengedId = :player1)) " +
           "AND c.createdAt > :since ORDER BY c.createdAt DESC")
    List<Challenge> findRecentChallengesBetweenPlayers(@Param("player1") UUID player1, 
                                                       @Param("player2") UUID player2, 
                                                       @Param("since") LocalDateTime since);
    
    // Find expired challenges that need cleanup
    @Query("SELECT c FROM Challenge c WHERE c.status = 'PENDING' AND c.expiresAt < :now")
    List<Challenge> findExpiredChallenges(@Param("now") LocalDateTime now);
    
    // Find active challenges by Slack message
    Optional<Challenge> findBySlackChannelIdAndSlackMessageTs(String channelId, String messageTs);
    
    // Count challenges sent by player today
    @Query("SELECT COUNT(c) FROM Challenge c WHERE c.challengerId = :playerId AND c.createdAt > :today")
    long countChallengesSentToday(@Param("playerId") UUID playerId, @Param("today") LocalDateTime today);
    
    // Find challenges by status
    List<Challenge> findByStatusOrderByCreatedAtDesc(ChallengeStatus status);
    
    // Find challenges that need game reminders
    @Query("SELECT c FROM Challenge c WHERE c.status = 'ACCEPTED' AND c.respondedAt < :reminderTime")
    List<Challenge> findAcceptedChallengesNeedingReminder(@Param("reminderTime") LocalDateTime reminderTime);
}