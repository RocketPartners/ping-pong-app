package com.example.javapingpongelo.controllers;

import com.example.javapingpongelo.models.ApiResponse;
import com.example.javapingpongelo.models.Match;
import com.example.javapingpongelo.models.exceptions.BadRequestException;
import com.example.javapingpongelo.models.exceptions.ResourceNotFoundException;
import com.example.javapingpongelo.services.MatchService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
@Slf4j
public class MatchController {

    @Autowired
    private MatchService matchService;

    @GetMapping("/{id}")
    public ResponseEntity<Match> getMatchById(@PathVariable("id") UUID id) {
        log.debug("Request to get match: {}", id);

        Match match = matchService.findById(id);
        if (match == null) {
            throw new ResourceNotFoundException("Match not found with id: " + id);
        }

        return ResponseEntity.ok(match);
    }

    @GetMapping
    public ResponseEntity<List<Match>> getAllMatches() {
        log.debug("Request to get all matches");

        List<Match> matches = matchService.findAll();
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<Match>> getMatchesByPlayerId(@PathVariable("playerId") UUID playerId) {
        log.debug("Request to get matches by player ID: {}", playerId);

        List<Match> matches = matchService.findByPlayerId(playerId);
        return ResponseEntity.ok(matches);
    }

    @PostMapping
    public ResponseEntity<Match> createMatch(@Valid @RequestBody Match match) {
        log.debug("Request to create match between challenger: {} and opponent: {}",
                  match.getChallengerId(), match.getOpponentId());

        Match createdMatch = matchService.save(match);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMatch);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Match> updateMatch(
            @PathVariable("id") UUID id,
            @Valid @RequestBody Match match) {

        log.debug("Request to update match: {}", id);

        // Ensure the match exists
        Match existingMatch = matchService.findById(id);
        if (existingMatch == null) {
            throw new ResourceNotFoundException("Match not found with id: " + id);
        }

        // Ensure IDs match
        if (!match.getMatchId().equals(id)) {
            throw new BadRequestException("Match ID in path does not match body");
        }

        Match updatedMatch = matchService.save(match);
        return ResponseEntity.ok(updatedMatch);
    }

    @PostMapping("/{id}/conclude")
    public ResponseEntity<ApiResponse> concludeMatch(
            @PathVariable("id") UUID id,
            @Valid @RequestBody Match match) {

        log.debug("Request to conclude match: {}", id);

        // Ensure the match exists
        Match existingMatch = matchService.findById(id);
        if (existingMatch == null) {
            throw new ResourceNotFoundException("Match not found with id: " + id);
        }

        // Ensure IDs match
        if (!match.getMatchId().equals(id)) {
            throw new BadRequestException("Match ID in path does not match body");
        }

        matchService.updateMatchAndPlayers(match);
        return ResponseEntity.ok(new ApiResponse(true, "Match concluded successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(@PathVariable("id") UUID id) {
        log.debug("Request to delete match: {}", id);

        Match match = matchService.findById(id);
        if (match == null) {
            throw new ResourceNotFoundException("Match not found with id: " + id);
        }

        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }
}