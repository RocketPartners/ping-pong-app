package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.Tournament;
import com.example.javapingpongelo.models.TournamentMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, UUID> {

    // No @Query annotation needed
    List<TournamentMatch> findByTournament_Id(UUID tournamentId);

    /**
     * Find matches by tournament ID and bracket type
     *
     * @param tournamentId The ID of the tournament
     * @param bracketType  The bracket type
     * @return List of matches in the tournament with the given bracket type
     */
    @Query(value = "SELECT tm.* FROM tournament_match tm " +
            "JOIN tournament_match_tournament tmt ON tm.match_id = tmt.match_id " +
            "WHERE tmt.tournament_id = :tournamentId " +
            "AND tm.bracket_type = :bracketType",
            nativeQuery = true)
    List<TournamentMatch> findByTournamentIdAndBracketType(
            @Param("tournamentId") UUID tournamentId,
            @Param("bracketType") String bracketType);

    /**
     * Find matches by player ID
     *
     * @param playerId The ID of the player
     * @return List of matches the player is participating in
     */
    @Query(value = "SELECT DISTINCT tm.* FROM tournament_match tm " +
            "JOIN tournament_match_player tmp ON tm.match_id = tmp.match_id " +
            "WHERE tmp.player_id = :playerId",
            nativeQuery = true)
    List<TournamentMatch> findByPlayerId(@Param("playerId") UUID playerId);

    /**
     * Find matches by tournament ID and round
     *
     * @param tournamentId The ID of the tournament
     * @param round        The round number
     * @return List of matches in the given round
     */
    @Query(value = "SELECT tm.* FROM tournament_match tm " +
            "JOIN tournament_match_tournament tmt ON tm.match_id = tmt.match_id " +
            "WHERE tmt.tournament_id = :tournamentId " +
            "AND tm.round = :round",
            nativeQuery = true)
    List<TournamentMatch> findByTournamentIdAndRound(
            @Param("tournamentId") UUID tournamentId,
            @Param("round") int round);

    /**
     * Find completed matches for a tournament
     *
     * @param tournamentId The ID of the tournament
     * @return List of completed matches
     */
    @Query(value = "SELECT tm.* FROM tournament_match tm " +
            "JOIN tournament_match_tournament tmt ON tm.match_id = tmt.match_id " +
            "WHERE tmt.tournament_id = :tournamentId " +
            "AND tm.completed = true",
            nativeQuery = true)
    List<TournamentMatch> findCompletedMatchesByTournamentId(@Param("tournamentId") UUID tournamentId);

    /**
     * Find pending matches for a tournament
     *
     * @param tournamentId The ID of the tournament
     * @return List of pending matches
     */
    @Query(value = "SELECT tm.* FROM tournament_match tm " +
            "JOIN tournament_match_tournament tmt ON tm.match_id = tmt.match_id " +
            "WHERE tmt.tournament_id = :tournamentId " +
            "AND tm.completed = false",
            nativeQuery = true)
    List<TournamentMatch> findPendingMatchesByTournamentId(@Param("tournamentId") UUID tournamentId);

    /**
     * Find matches ready to be played (both teams assigned but not completed)
     *
     * @param tournamentId The ID of the tournament
     * @return List of matches ready to be played
     */
    @Query(value = "SELECT tm.* FROM tournament_match tm " +
            "JOIN tournament_match_tournament tmt ON tm.match_id = tmt.match_id " +
            "JOIN tournament_match_player tmp1 ON tm.match_id = tmp1.match_id AND tmp1.team_number = 1 " +
            "JOIN tournament_match_player tmp2 ON tm.match_id = tmp2.match_id AND tmp2.team_number = 2 " +
            "WHERE tmt.tournament_id = :tournamentId " +
            "AND tm.completed = false " +
            "GROUP BY tm.match_id " +
            "HAVING COUNT(DISTINCT tmp1.player_id) > 0 AND COUNT(DISTINCT tmp2.player_id) > 0",
            nativeQuery = true)
    List<TournamentMatch> findMatchesReadyToPlay(@Param("tournamentId") UUID tournamentId);

    /**
     * Find a match by its display ID in a specific tournament
     *
     * @param tournamentId The ID of the tournament
     * @param displayId    The display ID of the match (e.g., "W1", "L3", etc.)
     * @return The match if found
     */
    @Query(value = "SELECT tm.* FROM tournament_match tm " +
            "JOIN tournament_match_tournament tmt ON tm.match_id = tmt.match_id " +
            "WHERE tmt.tournament_id = :tournamentId " +
            "AND tm.id = :displayId",
            nativeQuery = true)
    TournamentMatch findByTournamentIdAndDisplayId(
            @Param("tournamentId") UUID tournamentId,
            @Param("displayId") String displayId);

    /**
     * Find matches by tournament and round, ordered by position in round
     *
     * @param tournament The tournament
     * @param round The round number
     * @return List of matches in the round ordered by position
     */
    List<TournamentMatch> findByTournamentAndRoundOrderByPositionInRound(Tournament tournament, Integer round);
}