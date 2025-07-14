package com.example.javapingpongelo.controllers;

import com.example.javapingpongelo.models.ApiResponse;
import com.example.javapingpongelo.models.Tournament;
import com.example.javapingpongelo.models.TournamentMatch;
import com.example.javapingpongelo.models.TournamentPlayer;
import com.example.javapingpongelo.models.dto.TournamentRequestDTO;
import com.example.javapingpongelo.services.TournamentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for tournament-related operations
 */
@RestController
@RequestMapping("/api/tournaments")
@Slf4j
public class TournamentController {

    @Autowired
    private TournamentService tournamentService;

    /**
     * Create a new tournament
     *
     * @param tournamentDTO Tournament creation request
     * @return Created tournament
     */
    @PostMapping
    public ResponseEntity<Tournament> createTournament(@Valid @RequestBody TournamentRequestDTO tournamentDTO) {
        log.info("Request to create tournament: {}", tournamentDTO.getName());
        Tournament tournament = tournamentService.createTournament(tournamentDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(tournament);
    }

    /**
     * Get a tournament by ID
     *
     * @param id Tournament ID
     * @return Tournament if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Tournament> getTournament(@PathVariable("id") UUID id) {
        log.debug("Request to get tournament: {}", id);
        Tournament tournament = tournamentService.getTournament(id);
        return ResponseEntity.ok(tournament);
    }

    /**
     * Get all tournaments
     *
     * @return List of all tournaments
     */
    @GetMapping
    public ResponseEntity<List<Tournament>> getAllTournaments() {
        log.debug("Request to get all tournaments");
        List<Tournament> tournaments = tournamentService.getAllTournaments();
        return ResponseEntity.ok(tournaments);
    }

    /**
     * Update a tournament
     *
     * @param id         Tournament ID
     * @param tournament Tournament details to update
     * @return Updated tournament
     */
    @PutMapping("/{id}")
    public ResponseEntity<Tournament> updateTournament(
            @PathVariable("id") UUID id,
            @Valid @RequestBody Tournament tournament) {

        log.debug("Request to update tournament: {}", id);
        Tournament updatedTournament = tournamentService.updateTournament(id, tournament);
        return ResponseEntity.ok(updatedTournament);
    }

    /**
     * Delete a tournament
     *
     * @param id Tournament ID
     * @return Success/failure response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteTournament(@PathVariable("id") UUID id) {
        log.debug("Request to delete tournament: {}", id);
        tournamentService.deleteTournament(id);
        return ResponseEntity.ok(new ApiResponse(true, "Tournament deleted successfully"));
    }

    /**
     * Get tournaments by organizer
     *
     * @param organizerId Organizer ID
     * @return List of tournaments
     */
    @GetMapping("/organizer/{organizerId}")
    public ResponseEntity<List<Tournament>> getTournamentsByOrganizer(@PathVariable("organizerId") UUID organizerId) {
        log.debug("Request to get tournaments by organizer: {}", organizerId);
        List<Tournament> tournaments = tournamentService.getTournamentsByOrganizer(organizerId);
        return ResponseEntity.ok(tournaments);
    }

    /**
     * Get tournaments by player
     *
     * @param playerId Player ID
     * @return List of tournaments
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<Tournament>> getTournamentsByPlayer(@PathVariable("playerId") UUID playerId) {
        log.debug("Request to get tournaments by player: {}", playerId);
        List<Tournament> tournaments = tournamentService.getTournamentsByPlayer(playerId);
        return ResponseEntity.ok(tournaments);
    }

    /**
     * Get tournaments by status
     *
     * @param status Tournament status
     * @return List of tournaments
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Tournament>> getTournamentsByStatus(
            @PathVariable("status") Tournament.TournamentStatus status) {

        log.debug("Request to get tournaments by status: {}", status);
        List<Tournament> tournaments = tournamentService.getTournamentsByStatus(status);
        return ResponseEntity.ok(tournaments);
    }

    /**
     * Start a tournament
     *
     * @param id Tournament ID
     * @return Updated tournament
     */
    @PatchMapping("/{id}/start")
    public ResponseEntity<Tournament> startTournament(@PathVariable("id") UUID id) {
        log.info("Request to start tournament: {}", id);
        Tournament tournament = tournamentService.startTournament(id);
        return ResponseEntity.ok(tournament);
    }

    /**
     * Complete a tournament
     *
     * @param id Tournament ID
     * @return Updated tournament
     */
    @PatchMapping("/{id}/complete")
    public ResponseEntity<Tournament> completeTournament(@PathVariable("id") UUID id) {
        log.info("Request to complete tournament: {}", id);
        Tournament tournament = tournamentService.completeTournament(id);
        return ResponseEntity.ok(tournament);
    }

    /**
     * Get matches for a tournament
     *
     * @param id Tournament ID
     * @return List of matches
     */
    @GetMapping("/{id}/matches")
    public ResponseEntity<List<TournamentMatch>> getTournamentMatches(@PathVariable("id") UUID id) {
        log.debug("Request to get matches for tournament: {}", id);
        List<TournamentMatch> matches = tournamentService.getTournamentMatches(id);
        return ResponseEntity.ok(matches);
    }

    /**
     * Get matches by bracket type
     *
     * @param id          Tournament ID
     * @param bracketType Bracket type
     * @return List of matches
     */
    @GetMapping("/{id}/matches/bracket/{bracketType}")
    public ResponseEntity<List<TournamentMatch>> getTournamentMatchesByBracket(
            @PathVariable("id") UUID id,
            @PathVariable("bracketType") TournamentMatch.BracketType bracketType) {

        log.debug("Request to get {} bracket matches for tournament: {}", bracketType, id);
        List<TournamentMatch> matches = tournamentService.getTournamentMatchesByBracket(id, bracketType);
        return ResponseEntity.ok(matches);
    }

    /**
     * Update a match result
     *
     * @param tournamentId Tournament ID
     * @param matchId      Match ID
     * @param match        Match data with result
     * @return Updated match
     */
    @PatchMapping("/{tournamentId}/matches/{matchId}")
    public ResponseEntity<TournamentMatch> updateMatchResult(
            @PathVariable("tournamentId") UUID tournamentId,
            @PathVariable("matchId") UUID matchId,
            @Valid @RequestBody TournamentMatch match) {

        log.info("Request to update match: {} in tournament: {}", matchId, tournamentId);

        TournamentMatch updatedMatch = tournamentService.updateMatchResult(tournamentId, matchId, match);
        return ResponseEntity.ok(updatedMatch);
    }

    /**
     * Get players in a tournament
     *
     * @param id Tournament ID
     * @return List of tournament players
     */
    @GetMapping("/{id}/players")
    public ResponseEntity<List<TournamentPlayer>> getTournamentPlayers(@PathVariable("id") UUID id) {
        log.debug("Request to get players for tournament: {}", id);
        List<TournamentPlayer> players = tournamentService.getTournamentPlayers(id);
        return ResponseEntity.ok(players);
    }
}