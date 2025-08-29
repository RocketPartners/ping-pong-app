package com.example.javapingpongelo.events;

import com.example.javapingpongelo.models.GameType;
import com.example.javapingpongelo.models.Player;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a player's rating is updated.
 * Triggers achievement evaluation for rating-based achievements.
 */
@Getter
public class RatingUpdatedEvent extends ApplicationEvent {
    
    private final Player player;
    private final GameType gameType;
    private final int oldRating;
    private final int newRating;
    private final int ratingChange;

    public RatingUpdatedEvent(Object source, Player player, GameType gameType, 
                             int oldRating, int newRating) {
        super(source);
        this.player = player;
        this.gameType = gameType;
        this.oldRating = oldRating;
        this.newRating = newRating;
        this.ratingChange = newRating - oldRating;
    }

    /**
     * Convenience method to check if rating increased
     */
    public boolean isRatingIncrease() {
        return ratingChange > 0;
    }

    /**
     * Convenience method to check if rating decreased
     */
    public boolean isRatingDecrease() {
        return ratingChange < 0;
    }
}