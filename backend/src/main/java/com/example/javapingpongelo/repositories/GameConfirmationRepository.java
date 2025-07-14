package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.GameConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameConfirmationRepository extends JpaRepository<GameConfirmation, UUID> {

    /**
     * Find confirmation by game ID and player ID
     */
    Optional<GameConfirmation> findByGameIdAndPlayerId(UUID gameId, UUID playerId);

    /**
     * Find all confirmations for a specific game
     */
    List<GameConfirmation> findByGameId(UUID gameId);

    /**
     * Find all pending confirmations for a player
     */
    List<GameConfirmation> findByPlayerIdAndStatus(UUID playerId, GameConfirmation.ConfirmationStatus status);

    /**
     * Find confirmation by token
     */
    Optional<GameConfirmation> findByConfirmationToken(String token);

    /**
     * Find all pending confirmations that have expired
     */
    @Query("SELECT gc FROM GameConfirmation gc WHERE gc.status = 'PENDING' AND gc.expirationDate < :now")
    List<GameConfirmation> findExpiredPendingConfirmations(@Param("now") LocalDateTime now);

    /**
     * Find all pending confirmations for games between the same players within a specific time window
     */
    @Query("SELECT gc FROM GameConfirmation gc " +
            "WHERE gc.playerId = :playerId " +
            "AND gc.status = 'PENDING' " +
            "AND gc.createdAt > :sinceTime " +
            "AND gc.gameId IN (" +
            "    SELECT g.gameId FROM Game g " +
            "    WHERE (g.challengerId = :opponentId OR g.opponentId = :opponentId OR " +
            "          :opponentId MEMBER OF g.challengerTeam OR :opponentId MEMBER OF g.opponentTeam)" +
            ")")
    List<GameConfirmation> findRecentGamesBetweenPlayers(
            @Param("playerId") UUID playerId,
            @Param("opponentId") String opponentId,
            @Param("sinceTime") LocalDateTime sinceTime);
}