package com.example.javapingpongelo.events;

import com.example.javapingpongelo.models.EasterEgg;
import com.example.javapingpongelo.models.Player;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Event fired when a player finds an easter egg.
 * Used to trigger achievement evaluation for egg hunting milestones.
 */
@Data
@AllArgsConstructor
public class EasterEggFoundEvent {
    private final Player player;
    private final EasterEgg easterEgg;
    private final int pointsEarned;
    private final int totalEggsFound;
    private final int totalPointsEarned;
}