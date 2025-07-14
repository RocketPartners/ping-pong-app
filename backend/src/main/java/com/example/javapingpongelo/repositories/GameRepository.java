package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {
    /**
     * Find games by player ID
     * This query searches for games where the player is either a challenger, opponent,
     * or part of either team using MEMBER OF operator for collections
     */
    @Query("SELECT g FROM Game g WHERE g.challengerId = :playerId OR g.opponentId = :playerId OR " +
            ":playerId MEMBER OF g.challengerTeam OR :playerId MEMBER OF g.opponentTeam")
    List<Game> findByPlayerId(@Param("playerId") UUID playerId);

    /**
     * Find games by player ID with pagination
     */
    @Query("SELECT g FROM Game g WHERE g.challengerId = :playerId OR g.opponentId = :playerId OR " +
            ":playerId MEMBER OF g.challengerTeam OR :playerId MEMBER OF g.opponentTeam")
    Page<Game> findByPlayerIdPaged(@Param("playerId") UUID playerId, Pageable pageable);

    /**
     * Find games by match ID
     */
    List<Game> findByMatchId(UUID matchId);

    /**
     * Find games where a specific player won
     */
    @Query("SELECT g FROM Game g WHERE " +
            "(g.challengerId = :playerId AND g.challengerWin = true) OR " +
            "(g.opponentId = :playerId AND g.opponentWin = true) OR " +
            "(:playerId MEMBER OF g.challengerTeam AND g.challengerTeamWin = true) OR " +
            "(:playerId MEMBER OF g.opponentTeam AND g.opponentTeamWin = true)")
    List<Game> findWinsByPlayerId(@Param("playerId") String playerId);

    /**
     * Find games where a specific player lost
     */
    @Query("SELECT g FROM Game g WHERE " +
            "(g.challengerId = :playerId AND g.opponentWin = true) OR " +
            "(g.opponentId = :playerId AND g.challengerWin = true) OR " +
            "(:playerId MEMBER OF g.challengerTeam AND g.opponentTeamWin = true) OR " +
            "(:playerId MEMBER OF g.opponentTeam AND g.challengerTeamWin = true)")
    List<Game> findLossesByPlayerId(@Param("playerId") String playerId);

    /**
     * Get count of total games
     */
    @Query("SELECT COUNT(g) FROM Game g")
    long countTotalGames();

    /**
     * Get count of singles ranked games
     */
    @Query("SELECT COUNT(g) FROM Game g WHERE g.singlesGame = true AND g.ratedGame = true")
    long countSinglesRankedGames();

    /**
     * Get count of singles normal games
     */
    @Query("SELECT COUNT(g) FROM Game g WHERE g.singlesGame = true AND g.normalGame = true")
    long countSinglesNormalGames();

    /**
     * Get count of doubles ranked games
     */
    @Query("SELECT COUNT(g) FROM Game g WHERE g.doublesGame = true AND g.ratedGame = true")
    long countDoublesRankedGames();

    /**
     * Get count of doubles normal games
     */
    @Query("SELECT COUNT(g) FROM Game g WHERE g.doublesGame = true AND g.normalGame = true")
    long countDoublesNormalGames();

    /**
     * Get sum of all points scored across all games
     */
    @Query("SELECT SUM(g.challengerTeamScore + g.opponentTeamScore) FROM Game g")
    Integer sumTotalPoints();

    /**
     * Get average points scored per game
     */
    @Query("SELECT AVG(g.challengerTeamScore + g.opponentTeamScore) FROM Game g")
    Double averageTotalScore();

    /**
     * Find all games with pagination support
     */
    Page<Game> findAll(Pageable pageable);
}