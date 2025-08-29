package com.example.javapingpongelo.events;

import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * Event published when a game is completed.
 * Triggers achievement evaluation for game-related achievements.
 */
@Getter
public class GameCompletedEvent extends ApplicationEvent {
    
    private final Game game;
    private final List<Player> players;
    private final boolean isRatingUpdate; // If this game updated ratings

    public GameCompletedEvent(Object source, Game game, List<Player> players) {
        this(source, game, players, false);
    }

    public GameCompletedEvent(Object source, Game game, List<Player> players, boolean isRatingUpdate) {
        super(source);
        this.game = game;
        this.players = players;
        this.isRatingUpdate = isRatingUpdate;
    }
}