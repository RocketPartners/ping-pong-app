package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.PlayerStyle;
import com.example.javapingpongelo.models.PlayerStyleReview;
import com.example.javapingpongelo.models.dto.PlayerStyleReviewDTO;
import com.example.javapingpongelo.models.exceptions.ResourceNotFoundException;
import com.example.javapingpongelo.repositories.PlayerStyleReviewRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Transactional
@Slf4j
public class PlayerStyleReviewService {

    // Base adjustment amount that gets scaled
    private static final int BASE_ADJUSTMENT = 5;

    // Power factor for the diminishing returns curve - lower values = less steep curve
    // 2.0 = quadratic/steep curve, 1.0 = linear, 1.5 = moderate curve
    private static final double CURVE_POWER = 1.3;

    // Minimum adjustment at extreme values (ensures progress is always possible)
    private static final int MIN_ADJUSTMENT = 1;

    @Autowired
    private PlayerStyleReviewRepository reviewRepository;

    @Autowired
    private IPlayerService playerService;

    /**
     * Creates a review and updates player ratings (original method preserved for backward compatibility)
     */
    public PlayerStyleReview createReview(PlayerStyleReviewDTO reviewDTO) {
        return createReview(reviewDTO, false);
    }

    /**
     * Creates a new review and updates player ratings if needed
     *
     * @param reviewDTO        the review data
     * @param skipRatingUpdate if true, player ratings won't be updated (used for dismissing reviews)
     * @return the created review
     */
    public PlayerStyleReview createReview(PlayerStyleReviewDTO reviewDTO, boolean skipRatingUpdate) {
        log.debug("Creating player style review for player: {} (skipRatingUpdate: {})",
                  reviewDTO.getPlayerId(), skipRatingUpdate);

        // Create entity from DTO
        PlayerStyleReview review = PlayerStyleReview.builder()
                                                    .reviewerId(reviewDTO.getReviewerId())
                                                    .playerId(reviewDTO.getPlayerId())
                                                    .gameIds(reviewDTO.getGameIds())
                                                    .strengths(reviewDTO.getStrengths() != null ? reviewDTO.getStrengths() : new ArrayList<>())
                                                    .improvements(reviewDTO.getImprovements() != null ? reviewDTO.getImprovements() : new ArrayList<>())
                                                    .reviewDate(reviewDTO.getReviewDate() != null ? reviewDTO.getReviewDate() : new Date())
                                                    .response(reviewDTO.isResponse())
                                                    .parentReviewId(reviewDTO.getParentReviewId())
                                                    .reviewerUsername(reviewDTO.getReviewerUsername())
                                                    .reviewerFirstName(reviewDTO.getReviewerFirstName())
                                                    .reviewerLastName(reviewDTO.getReviewerLastName())
                                                    .build();

        // If reviewer details are not provided in the DTO, fetch them from the database
        if (review.getReviewerUsername() == null || review.getReviewerFirstName() == null || review.getReviewerLastName() == null) {
            populateReviewerDetails(review);
        }

        // Save the review
        PlayerStyleReview savedReview = reviewRepository.save(review);

        // Update player style ratings based on the review (unless we're skipping this step)
        if (!skipRatingUpdate) {
            updatePlayerStyleRatings(review);
        }
        else {
            log.debug("Skipping player style rating update for dismiss operation");
        }

        // If this is a response to another review, mark the parent review as acknowledged
        if (review.isResponse() && review.getParentReviewId() != null) {
            try {
                acknowledgeReview(review.getParentReviewId());
            }
            catch (Exception e) {
                log.warn("Could not acknowledge parent review: {}", review.getParentReviewId(), e);
            }
        }

        return savedReview;
    }

    /**
     * Populate reviewer details for a single review
     */
    private void populateReviewerDetails(PlayerStyleReview review) {
        try {
            Player reviewer = playerService.findPlayerById(review.getReviewerId());
            if (reviewer != null) {
                review.setReviewerUsername(reviewer.getUsername());
                review.setReviewerFirstName(reviewer.getFirstName());
                review.setReviewerLastName(reviewer.getLastName());
            }
        }
        catch (Exception e) {
            log.warn("Could not populate reviewer details for review: {}", review.getId(), e);
        }
    }

    /**
     * Updates player style ratings based on review feedback with diminishing returns
     * As ratings approach extremes (0 or 100), the adjustment amount decreases
     * Uses a configurable power curve to control how steep the diminishing returns are
     */
    private void updatePlayerStyleRatings(PlayerStyleReview review) {
        log.debug("Updating style ratings for player: {} based on review", review.getPlayerId());

        try {
            // Get the player
            Player player = playerService.findPlayerById(review.getPlayerId());
            if (player == null) {
                throw new ResourceNotFoundException("Player not found with id: " + review.getPlayerId());
            }

            // Increase ratings for strengths with diminishing returns as approach 100
            if (review.getStrengths() != null) {
                for (PlayerStyle style : review.getStrengths()) {
                    int currentRating = player.getStyleRating(style);

                    // Calculate scaled adjustment (smaller as it approaches 100)
                    // Formula: BASE_ADJUSTMENT * (1 - currentRating/100)^CURVE_POWER
                    double scaleFactor = Math.pow(1 - (currentRating / 100.0), CURVE_POWER);
                    int adjustment = (int) Math.ceil(BASE_ADJUSTMENT * scaleFactor);

                    // Ensure at least a minimum adjustment if not already at max
                    adjustment = currentRating < 100 ? Math.max(MIN_ADJUSTMENT, adjustment) : 0;

                    player.setStyleRating(style, currentRating + adjustment);
                    log.debug("Increased {} rating for player {} from {} to {} (adjustment: {})",
                              style, player.getUsername(), currentRating, player.getStyleRating(style), adjustment);
                }
            }

            // Decrease ratings for improvements with diminishing returns as approach 0
            if (review.getImprovements() != null) {
                for (PlayerStyle style : review.getImprovements()) {
                    int currentRating = player.getStyleRating(style);

                    // Calculate scaled adjustment (smaller as it approaches 0)
                    // Formula: BASE_ADJUSTMENT * (currentRating/100)^CURVE_POWER
                    double scaleFactor = Math.pow(currentRating / 100.0, CURVE_POWER);
                    int adjustment = (int) Math.ceil(BASE_ADJUSTMENT * scaleFactor);

                    // Ensure at least a minimum adjustment if not already at min
                    adjustment = currentRating > 0 ? Math.max(MIN_ADJUSTMENT, adjustment) : 0;

                    player.setStyleRating(style, currentRating - adjustment);
                    log.debug("Decreased {} rating for player {} from {} to {} (adjustment: {})",
                              style, player.getUsername(), currentRating, player.getStyleRating(style), adjustment);
                }
            }

            // Save the updated player
            playerService.savePlayer(player);
            log.debug("Successfully updated style ratings for player: {}", player.getUsername());
        }
        catch (Exception e) {
            log.error("Error updating player style ratings: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Mark a review as acknowledged
     */
    public PlayerStyleReview acknowledgeReview(UUID reviewId) {
        log.debug("Acknowledging review: {}", reviewId);

        // Find the review by ID
        PlayerStyleReview review = reviewRepository.findById(reviewId)
                                                   .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        // Mark as acknowledged
        review.setAcknowledged(true);
        review.setAcknowledgedDate(new Date());

        // Save and return
        return reviewRepository.save(review);
    }

    public List<PlayerStyleReview> findReviewsByPlayerId(UUID playerId) {
        List<PlayerStyleReview> reviews = reviewRepository.findByPlayerId(playerId);
        return populateReviewerDetails(reviews);
    }

    /**
     * Populate reviewer details for a list of reviews
     */
    private List<PlayerStyleReview> populateReviewerDetails(List<PlayerStyleReview> reviews) {
        reviews.forEach(this::populateReviewerDetails);
        return reviews;
    }

    public List<PlayerStyleReview> findReviewsByReviewerId(UUID reviewerId) {
        List<PlayerStyleReview> reviews = reviewRepository.findByReviewerId(reviewerId);
        return populateReviewerDetails(reviews);
    }

    public List<PlayerStyleReview> findReviewsByGameId(UUID gameId) {
        List<PlayerStyleReview> reviews = reviewRepository.findByGameIdsContaining(gameId);
        return populateReviewerDetails(reviews);
    }

    /**
     * Find recent unacknowledged initial reviews for a player within the specified days
     * This only returns reviews that are initial reviews (not responses) and haven't been acknowledged
     */
    public List<PlayerStyleReview> findRecentUnacknowledgedReviewsForPlayer(UUID playerId, int days) {
        log.debug("Finding recent unacknowledged reviews for player: {} in last {} days", playerId, days);

        // Calculate date threshold for N days ago
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -days);
        Date cutoffDate = calendar.getTime();

        List<PlayerStyleReview> reviews = reviewRepository.findByPlayerIdAndAcknowledgedFalseAndResponseFalseAndReviewDateAfter(playerId, cutoffDate);
        return populateReviewerDetails(reviews);
    }

    /**
     * Find a review by ID
     */
    public PlayerStyleReview findById(UUID reviewId) {
        PlayerStyleReview review = reviewRepository.findById(reviewId)
                                                   .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        populateReviewerDetails(review);
        return review;
    }
}