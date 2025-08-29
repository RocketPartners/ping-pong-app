package com.example.javapingpongelo.events;

import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.Tournament;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * Event published for tournament-related achievements.
 * Triggers achievement evaluation for tournament-based achievements.
 */
@Getter
public class TournamentEvent extends ApplicationEvent {
    
    private final Tournament tournament;
    private final TournamentEventType eventType;
    private final Player player; // For player-specific events
    private final List<Player> players; // For multi-player events
    private final Integer round; // Current round (for progression tracking)
    private final String additionalData; // JSON for complex data

    public TournamentEvent(Object source, Tournament tournament, TournamentEventType eventType, 
                          Player player) {
        this(source, tournament, eventType, player, null, null, null);
    }

    public TournamentEvent(Object source, Tournament tournament, TournamentEventType eventType, 
                          List<Player> players) {
        this(source, tournament, eventType, null, players, null, null);
    }

    public TournamentEvent(Object source, Tournament tournament, TournamentEventType eventType, 
                          Player player, List<Player> players, Integer round, String additionalData) {
        super(source);
        this.tournament = tournament;
        this.eventType = eventType;
        this.player = player;
        this.players = players;
        this.round = round;
        this.additionalData = additionalData;
    }

    /**
     * Types of tournament events
     */
    public enum TournamentEventType {
        PLAYER_REGISTERED,     // Player registered for tournament
        TOURNAMENT_STARTED,    // Tournament started
        ROUND_COMPLETED,       // Player completed a round
        PLAYER_ELIMINATED,     // Player eliminated from tournament
        SEMIFINAL_REACHED,     // Player reached semifinals
        FINAL_REACHED,         // Player reached finals
        TOURNAMENT_WON,        // Player won tournament
        TOURNAMENT_COMPLETED   // Tournament completed
    }
}