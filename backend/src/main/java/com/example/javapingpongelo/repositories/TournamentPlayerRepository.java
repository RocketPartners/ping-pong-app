package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.TournamentPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TournamentPlayerRepository extends JpaRepository<TournamentPlayer, UUID> {

    /**
     * Find all players in a tournament
     *
     * @param tournamentId The ID of the tournament
     * @return List of TournamentPlayer entries for the tournament
     */
    List<TournamentPlayer> findByTournament_Id(UUID tournamentId);

    /**
     * Find all tournaments a player is participating in
     *
     * @param playerId The ID of the player
     * @return List of TournamentPlayer entries for the player
     */
    List<TournamentPlayer> findByPlayerId(UUID playerId);

    /**
     * Find a specific tournament-player entry
     *
     * @param tournamentId The ID of the tournament
     * @param playerId     The ID of the player
     * @return The TournamentPlayer entry if found
     */
    TournamentPlayer findByTournament_IdAndPlayerId(UUID tournamentId, UUID playerId);

    /**
     * Find doubles partners in a tournament
     *
     * @param tournamentId The ID of the tournament
     * @param playerId     The ID of the player
     * @return The partner's player ID
     */
    @Query("SELECT tp.partnerId FROM TournamentPlayer tp " +
            "WHERE tp.tournament.id = :tournamentId AND tp.playerId = :playerId")
    UUID findPartnerIdByTournamentIdAndPlayerId(
            @Param("tournamentId") UUID tournamentId,
            @Param("playerId") UUID playerId);
}