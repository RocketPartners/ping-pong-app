package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {
    @Query("SELECT m FROM Match m WHERE m.challengerId = :playerId OR m.opponentId = :playerId OR " +
            ":playerId MEMBER OF m.challengerTeam OR :playerId MEMBER OF m.opponentTeam")
    List<Match> findByPlayerId(@Param("playerId") UUID playerId);
}
