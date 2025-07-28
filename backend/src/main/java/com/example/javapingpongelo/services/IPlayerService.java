package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.PlayerStyle;
import com.example.javapingpongelo.models.dto.PlayerStyleAverageDTO;
import com.example.javapingpongelo.models.dto.PlayerStyleTopDTO;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interface for player-related operations
 */
public interface IPlayerService {

    /**
     * Register a new player account
     */
    Player registerNewUserAccount(Player player) throws Exception;

    /**
     * Find a player by first and last name
     */
    Player findPlayerByName(String firstName, String lastName);

    /**
     * Find all players
     */
    List<Player> findAllPlayers();

    /**
     * Create a new player
     */
    Player createPlayer(Player player);

    /**
     * Find a player by username
     */
    Player findPlayerByUsername(String username);

    /**
     * Find a player by email
     */
    Player findPlayerByEmail(String email);

    /**
     * Find a player by ID
     */
    Player findPlayerById(UUID id);

    /**
     * Update player's singles ranked Elo rating
     */
    void updatePlayerSinglesRankedElo(Player player, double eloUpdate, Game game);

    /**
     * Update player's doubles ranked Elo rating
     */
    void updatePlayerDoublesRankedElo(Player player, Double eloUpdate, boolean isWin, Game game);

    /**
     * Update player's doubles normal Elo rating
     */
    void updatePlayerDoublesNormalElo(Player player, Double eloUpdate, boolean isWin, Game game);

    /**
     * Update player's singles normal Elo rating
     */
    void updatePlayerSinglesNormalElo(Player player, double eloUpdate, Game game);

    /**
     * Update a player's password
     */
    void updatePassword(Player player, String newPassword);

    /**
     * Find all player usernames
     */
    List<String> findAllPlayerUsernames();

    /**
     * Save a player
     */
    void savePlayer(Player player);

    /**
     * Delete a player by ID
     */
    void deletePlayer(UUID id);

    /**
     * Authenticate a player with username and password
     */
    Player authenticatePlayer(String username, String password);

    /**
     * Update an existing player
     */
    Player updatePlayer(Player player);

    /**
     * Update a player's style ratings
     */
    Player updatePlayerStyleRatings(UUID playerId, Map<PlayerStyle, Integer> styleRatings);

    /**
     * Get a player's style ratings
     */
    Map<PlayerStyle, Integer> getPlayerStyleRatings(UUID playerId);

    /**
     * Get average values for each player style among all players
     */
    List<PlayerStyleAverageDTO> getAverageStyleRatings();

    /**
     * Get the highest value for each player style and who holds it
     */
    List<PlayerStyleTopDTO> getHighestStyleRatings();

    /**
     * Promote a player to admin role by email
     */
    void promotePlayerToAdmin(String email);

    /**
     * Demote a player from admin role by email
     */
    void demotePlayerFromAdmin(String email);
}