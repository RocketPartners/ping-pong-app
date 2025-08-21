package com.example.javapingpongelo.tournament;

import com.example.javapingpongelo.models.*;
import com.example.javapingpongelo.tournament.engine.impl.DoubleEliminationRules;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests validation logic for double elimination tournaments
 */
@SpringBootTest
@Slf4j
public class DoubleEliminationValidationTest {

    @Test
    public void testValidPlayerCounts() {
        DoubleEliminationRules rules = new DoubleEliminationRules();
        
        // Test valid player counts (4-16)
        for (int playerCount = 4; playerCount <= 16; playerCount++) {
            final int finalPlayerCount = playerCount; // Make effectively final
            Tournament tournament = createTournament(playerCount);
            List<TournamentPlayer> participants = createParticipants(tournament, playerCount);
            
            // Should not throw exception
            assertDoesNotThrow(() -> {
                List<TournamentRound> rounds = rules.generateInitialBracket(tournament, participants);
                assertFalse(rounds.isEmpty(), "Should generate rounds for " + finalPlayerCount + " players");
            }, "Should support " + playerCount + " players");
        }
    }
    
    @Test
    public void testInvalidPlayerCountTooFew() {
        DoubleEliminationRules rules = new DoubleEliminationRules();
        
        // Test invalid player counts (1-3)
        for (int playerCount = 1; playerCount <= 3; playerCount++) {
            Tournament tournament = createTournament(playerCount);
            List<TournamentPlayer> participants = createParticipants(tournament, playerCount);
            
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                rules.generateInitialBracket(tournament, participants);
            }, "Should reject " + playerCount + " players");
            
            assertTrue(exception.getMessage().contains("Double elimination tournaments support 4-16 players"),
                "Error message should mention valid range");
            assertTrue(exception.getMessage().contains("Got: " + playerCount),
                "Error message should show actual count");
        }
    }
    
    @Test
    public void testInvalidPlayerCountTooMany() {
        DoubleEliminationRules rules = new DoubleEliminationRules();
        
        // Test invalid player counts (17+)
        for (int playerCount = 17; playerCount <= 20; playerCount++) {
            Tournament tournament = createTournament(playerCount);
            List<TournamentPlayer> participants = createParticipants(tournament, playerCount);
            
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                rules.generateInitialBracket(tournament, participants);
            }, "Should reject " + playerCount + " players");
            
            assertTrue(exception.getMessage().contains("Double elimination tournaments support 4-16 players"),
                "Error message should mention valid range");
            assertTrue(exception.getMessage().contains("Got: " + playerCount),
                "Error message should show actual count");
        }
    }
    
    private Tournament createTournament(int playerCount) {
        return Tournament.builder()
            .id(UUID.randomUUID())
            .name(playerCount + "-Player Test Tournament")
            .tournamentType(Tournament.TournamentType.DOUBLE_ELIMINATION)
            .numberOfPlayers(playerCount)
            .currentRound(0)
            .build();
    }
    
    private List<TournamentPlayer> createParticipants(Tournament tournament, int count) {
        List<TournamentPlayer> participants = new ArrayList<>();
        String[] names = {"Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Henry",
                         "Ivy", "Jack", "Kate", "Liam", "Maya", "Noah", "Olivia", "Paul",
                         "Quinn", "Rachel", "Sam", "Tara"};
        
        for (int i = 0; i < count; i++) {
            String name = i < names.length ? names[i] : "Player" + (i + 1);
            participants.add(TournamentPlayer.builder()
                .id(UUID.randomUUID())
                .tournament(tournament)
                .playerId(UUID.nameUUIDFromBytes(name.getBytes()))
                .seed(i + 1)
                .build());
        }
        
        return participants;
    }
}