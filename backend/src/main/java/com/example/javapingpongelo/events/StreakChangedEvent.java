package com.example.javapingpongelo.events;

import com.example.javapingpongelo.models.Player;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a player's win/loss streak changes.
 * Triggers achievement evaluation for streak-based achievements.
 */
@Getter
public class StreakChangedEvent extends ApplicationEvent {
    
    private final Player player;
    private final int newWinStreak;
    private final int newLossStreak;
    private final int previousWinStreak;
    private final int previousLossStreak;
    private final boolean isNewWinStreakRecord;

    public StreakChangedEvent(Object source, Player player, 
                             int newWinStreak, int newLossStreak,
                             int previousWinStreak, int previousLossStreak,
                             boolean isNewWinStreakRecord) {
        super(source);
        this.player = player;
        this.newWinStreak = newWinStreak;
        this.newLossStreak = newLossStreak;
        this.previousWinStreak = previousWinStreak;
        this.previousLossStreak = previousLossStreak;
        this.isNewWinStreakRecord = isNewWinStreakRecord;
    }

    /**
     * Convenience method to check if win streak started
     */
    public boolean isWinStreakStarted() {
        return previousWinStreak == 0 && newWinStreak > 0;
    }

    /**
     * Convenience method to check if win streak ended
     */
    public boolean isWinStreakEnded() {
        return previousWinStreak > 0 && newWinStreak == 0;
    }

    /**
     * Convenience method to check if loss streak started
     */
    public boolean isLossStreakStarted() {
        return previousLossStreak == 0 && newLossStreak > 0;
    }

    /**
     * Convenience method to check if loss streak ended
     */
    public boolean isLossStreakEnded() {
        return previousLossStreak > 0 && newLossStreak == 0;
    }
}