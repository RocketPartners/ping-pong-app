package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, UUID> {

    /**
     * Find tournaments by their organizer
     *
     * @param organizerId The ID of the organizer
     * @return List of tournaments organized by the given organizer
     */
    List<Tournament> findByOrganizerId(UUID organizerId);

    /**
     * Find tournaments by their status
     *
     * @param status The tournament status
     * @return List of tournaments with the given status
     */
    List<Tournament> findByStatus(Tournament.TournamentStatus status);

    /**
     * Find tournaments by tournament type
     *
     * @param tournamentType The tournament type
     * @return List of tournaments of the given type
     */
    List<Tournament> findByTournamentType(Tournament.TournamentType tournamentType);

    /**
     * Find tournaments by game type
     *
     * @param gameType The game type
     * @return List of tournaments of the given game type
     */
    List<Tournament> findByGameType(Tournament.GameType gameType);

    /**
     * Find tournaments that a player is participating in
     *
     * @param playerId The ID of the player
     * @return List of tournaments the player is participating in
     */
    @Query(value = "SELECT t.* FROM tournament t " +
            "JOIN tournament_player tp ON t.id = tp.tournament_id " +
            "WHERE tp.player_id = :playerId",
            nativeQuery = true)
    List<Tournament> findByPlayerId(@Param("playerId") UUID playerId);
}