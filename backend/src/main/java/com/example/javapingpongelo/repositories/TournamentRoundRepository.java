package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.Tournament;
import com.example.javapingpongelo.models.TournamentMatch;
import com.example.javapingpongelo.models.TournamentRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TournamentRoundRepository extends JpaRepository<TournamentRound, UUID> {
    
    /**
     * Find all rounds for a tournament, ordered by round number
     */
    List<TournamentRound> findByTournamentOrderByRoundNumber(Tournament tournament);
    
    /**
     * Find rounds by tournament and bracket type
     */
    List<TournamentRound> findByTournamentAndBracketTypeOrderByRoundNumber(
        Tournament tournament, 
        TournamentMatch.BracketType bracketType
    );
    
    /**
     * Find a specific round by tournament, round number, and bracket type
     */
    Optional<TournamentRound> findByTournamentAndRoundNumberAndBracketType(
        Tournament tournament, 
        Integer roundNumber, 
        TournamentMatch.BracketType bracketType
    );
    
    /**
     * Find rounds by status
     */
    List<TournamentRound> findByTournamentAndStatus(Tournament tournament, TournamentRound.RoundStatus status);
    
    /**
     * Find the current active round for a tournament
     */
    @Query("SELECT r FROM TournamentRound r WHERE r.tournament = :tournament AND r.status = 'ACTIVE'")
    List<TournamentRound> findActiveRounds(@Param("tournament") Tournament tournament);
    
    /**
     * Find the latest completed round for a tournament
     */
    @Query("SELECT r FROM TournamentRound r WHERE r.tournament = :tournament AND r.status = 'COMPLETED' ORDER BY r.roundNumber DESC")
    List<TournamentRound> findLatestCompletedRounds(@Param("tournament") Tournament tournament);
    
    /**
     * Count rounds by tournament and bracket type
     */
    long countByTournamentAndBracketType(Tournament tournament, TournamentMatch.BracketType bracketType);
    
    /**
     * Find a round by tournament, round number and status
     */
    List<TournamentRound> findByTournamentAndRoundNumberAndStatus(
        Tournament tournament, 
        Integer roundNumber, 
        TournamentRound.RoundStatus status
    );
}