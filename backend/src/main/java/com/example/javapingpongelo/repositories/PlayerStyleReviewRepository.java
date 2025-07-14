package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.PlayerStyleReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Repository
public interface PlayerStyleReviewRepository extends JpaRepository<PlayerStyleReview, UUID> {
    /**
     * Find all reviews for a specific player
     */
    List<PlayerStyleReview> findByPlayerId(UUID playerId);

    /**
     * Find all reviews submitted by a specific reviewer
     */
    List<PlayerStyleReview> findByReviewerId(UUID reviewerId);

    /**
     * Find all reviews that relate to a game
     */
    List<PlayerStyleReview> findByGameIdsContaining(UUID gameId);

    List<PlayerStyleReview> findByPlayerIdAndAcknowledgedFalseAndResponseFalseAndReviewDateAfter(UUID playerId, Date cutoffDate);
}