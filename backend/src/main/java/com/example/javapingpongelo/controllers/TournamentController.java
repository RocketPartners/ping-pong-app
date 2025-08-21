package com.example.javapingpongelo.controllers;

import com.example.javapingpongelo.models.Tournament;
import com.example.javapingpongelo.models.TournamentMatch;
import com.example.javapingpongelo.models.TournamentPlayer;
import com.example.javapingpongelo.models.TournamentRound;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.dto.TournamentRequestDTO;
import com.example.javapingpongelo.repositories.TournamentRepository;
import com.example.javapingpongelo.repositories.TournamentPlayerRepository;
import com.example.javapingpongelo.repositories.TournamentMatchRepository;
import com.example.javapingpongelo.repositories.PlayerRepository;
import com.example.javapingpongelo.tournament.engine.TournamentEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tournaments")
@Slf4j
public class TournamentController {

    @Autowired
    private TournamentRepository tournamentRepository;
    
    @Autowired
    private TournamentPlayerRepository tournamentPlayerRepository;
    
    @Autowired
    private TournamentMatchRepository tournamentMatchRepository;
    
    @Autowired
    private PlayerRepository playerRepository;
    
    @Autowired
    private TournamentEngine tournamentEngine;

    /**
     * Create a new tournament
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTournament(
            @RequestBody TournamentRequestDTO request,
            Authentication authentication) {
        
        try {
            log.info("Creating tournament: {}", request.getName());
            
            // Get organizer
            Player organizer = playerRepository.findByUsername(authentication.getName());
            if (organizer == null) {
                throw new RuntimeException("Organizer not found");
            }
            
            // Create tournament
            Tournament tournament = Tournament.builder()
                .name(request.getName())
                .description(request.getDescription())
                .numberOfPlayers(request.getPlayerIds().size())
                .organizerId(organizer.getPlayerId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Tournament.TournamentStatus.CREATED)
                .tournamentType(request.getTournamentType())
                .gameType(request.getGameType())
                .seedingMethod(request.getSeedingMethod())
                .build();
            
            Tournament savedTournament = tournamentRepository.save(tournament);
            
            // Add participants
            for (UUID playerId : request.getPlayerIds()) {
                Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new RuntimeException("Player not found: " + playerId));
                
                TournamentPlayer tournamentPlayer = TournamentPlayer.builder()
                    .tournament(savedTournament)
                    .playerId(player.getPlayerId())
                    .build();
                tournamentPlayerRepository.save(tournamentPlayer);
            }
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedTournament.getId().toString());
            response.put("name", savedTournament.getName());
            response.put("description", savedTournament.getDescription());
            response.put("status", savedTournament.getStatus().name());
            response.put("type", request.getTournamentType().name().toLowerCase());
            response.put("organizerId", organizer.getPlayerId().toString());
            response.put("organizerName", organizer.getFirstName() + " " + organizer.getLastName());
            response.put("maxParticipants", savedTournament.getNumberOfPlayers());
            response.put("currentParticipants", request.getPlayerIds().size());
            response.put("createdAt", savedTournament.getCreated());
            response.put("isPublic", savedTournament.isPublic());
            
            log.info("Tournament created successfully: {}", savedTournament.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error creating tournament", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get tournament list
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getTournaments() {
        try {
            List<Tournament> tournaments = tournamentRepository.findAll()
                .stream()
                .sorted((t1, t2) -> t2.getCreated().compareTo(t1.getCreated()))
                .collect(Collectors.toList());
            
            List<Map<String, Object>> tournamentList = tournaments.stream()
                .map(this::mapTournamentToListItem)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", tournamentList);
            response.put("totalElements", tournaments.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting tournaments", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get tournament details with bracket data
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTournamentDetails(@PathVariable UUID id) {
        try {
            Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
            
            // Get participants with consistent ID mapping
            Map<UUID, Integer> playerIdMap = new HashMap<>();
            List<Map<String, Object>> participants = createParticipantsList(tournament, playerIdMap);
            
            // Prepare response
            Map<String, Object> response = mapTournamentToDetails(tournament);
            response.put("participants", participants);
            
            // Generate bracket data if tournament is started
            Map<String, Object> bracketData = null;
            if (tournament.getStatus() != Tournament.TournamentStatus.CREATED) {
                try {
                    log.info("Generating bracket data for tournament details: {}", id);
                    TournamentEngine.TournamentBracketData engineBracketData = tournamentEngine.generateBracketData(id);
                    bracketData = convertBracketDataForFrontend(tournament, engineBracketData);
                    log.info("Successfully generated bracket data for tournament details: {}", id);
                } catch (Exception e) {
                    log.warn("Failed to generate bracket data for tournament details: {}", id, e);
                    bracketData = null;
                }
            }
            response.put("bracketData", bracketData);
            
            response.put("canEdit", true); 
            response.put("canJoin", false);
            response.put("canStart", Tournament.TournamentStatus.CREATED.equals(tournament.getStatus()) && participants.size() >= 2);
            response.put("canComplete", false);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting tournament details", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Start tournament and generate bracket
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<Map<String, Object>> startTournament(@PathVariable UUID id) {
        try {
            log.info("START TOURNAMENT REQUEST: Tournament ID: {}", id);
            Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
            
            log.info("Found tournament: {} with status: {}", tournament.getName(), tournament.getStatus());
            log.info("Status comparison - CREATED: {}, READY_TO_START: {}", 
                Tournament.TournamentStatus.CREATED.equals(tournament.getStatus()),
                Tournament.TournamentStatus.READY_TO_START.equals(tournament.getStatus()));
            
            // Allow starting from both CREATED and READY_TO_START statuses
            if (!Tournament.TournamentStatus.CREATED.equals(tournament.getStatus()) && 
                !Tournament.TournamentStatus.READY_TO_START.equals(tournament.getStatus())) {
                log.error("Cannot start tournament - invalid status: {} (expected CREATED or READY_TO_START)", tournament.getStatus());
                if (Tournament.TournamentStatus.IN_PROGRESS.equals(tournament.getStatus())) {
                    throw new RuntimeException("Tournament is already in progress");
                } else if (Tournament.TournamentStatus.COMPLETED.equals(tournament.getStatus())) {
                    throw new RuntimeException("Tournament is already completed");
                } else {
                    throw new RuntimeException("Tournament cannot be started in its current state: " + tournament.getStatus());
                }
            }
            
            // Get participants
            List<TournamentPlayer> tournamentPlayers = tournamentPlayerRepository.findByTournament_Id(id);
            if (tournamentPlayers.size() < 2) {
                throw new RuntimeException("Tournament needs at least 2 participants to start");
            }
            
            // Update tournament status
            tournament.setStatus(Tournament.TournamentStatus.IN_PROGRESS);
            tournament = tournamentRepository.save(tournament);
            
            // Initialize tournament bracket using tournament engine
            log.info("Initializing tournament bracket for tournament: {}", id);
            tournament = tournamentEngine.initializeTournament(tournament);
            
            Map<String, Object> response = mapTournamentToDetails(tournament);
            log.info("Tournament started and initialized successfully: {}", id);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error starting tournament", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get bracket data for a tournament
     */
    @GetMapping("/{id}/bracket")
    public ResponseEntity<Map<String, Object>> getBracketData(@PathVariable UUID id) {
        try {
            Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
            
            // Only generate bracket data if tournament is in progress or completed
            if (tournament.getStatus() == Tournament.TournamentStatus.CREATED) {
                log.info("Tournament not started yet, no bracket data available: {}", id);
                return ResponseEntity.ok().body(null);
            }
            
            // Generate full bracket data using tournament engine
            log.info("Requesting bracket data from tournament engine for tournament: {}", id);
            TournamentEngine.TournamentBracketData bracketData = tournamentEngine.generateBracketData(id);
            
            log.info("Bracket data received: {} winner rounds, {} loser rounds, {} final matches",
                bracketData.getWinnerRounds().size(),
                bracketData.getLoserRounds().size(),
                bracketData.getFinalMatches().size());
            
            // Convert engine data to frontend-friendly format
            Map<String, Object> frontendBracketData = convertBracketDataForFrontend(tournament, bracketData);
            
            log.info("Generated bracket data for tournament: {}", id);
            return ResponseEntity.ok(frontendBracketData);
            
        } catch (Exception e) {
            log.error("Error getting bracket data", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update match result and process tournament progression
     */
    @PostMapping("/{tournamentId}/matches/{matchId}/result")
    public ResponseEntity<?> updateMatchResult(
            @PathVariable UUID tournamentId,
            @PathVariable String matchId,  // Changed to String to handle both numeric and UUID formats
            @RequestBody Map<String, Object> request) {
        
        try {
            log.info("Match result update requested: tournament={}, match={}", tournamentId, matchId);
            
            // Validate inputs
            if (!request.containsKey("winnerId")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Winner ID is required"));
            }
            
            // Extract data from request
            Integer winnerId = (Integer) request.get("winnerId");
            Integer winnerScore = request.containsKey("winnerScore") ? (Integer) request.get("winnerScore") : 11;
            Integer loserScore = request.containsKey("loserScore") ? (Integer) request.get("loserScore") : 0;
            
            // Find the tournament
            Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
            
            // Find the match - handle both numeric frontend IDs and UUID backend IDs
            TournamentMatch match = findMatchByFrontendId(tournamentId, matchId);
            log.info("Found match: {} with existing winner IDs: {} and loser IDs: {}", 
                match.getMatchId(), match.getWinnerIds(), match.getLoserIds());
            
            // Get tournament participants for ID mapping  
            Map<UUID, Integer> playerIdMap = new HashMap<>();
            createParticipantsList(tournament, playerIdMap);
            Map<Integer, UUID> idToUuidMap = new HashMap<>();
            playerIdMap.forEach((uuid, id) -> idToUuidMap.put(id, uuid));
            
            // Get winner UUID
            UUID winnerUuid = idToUuidMap.get(winnerId);
            if (winnerUuid == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid winner ID"));
            }
            
            // Update match with winner info
            List<UUID> winnerIds = new ArrayList<>();
            winnerIds.add(winnerUuid);
            
            // Determine loser
            List<UUID> allPlayers = new ArrayList<>();
            if (match.getTeam1Ids() != null) allPlayers.addAll(match.getTeam1Ids());
            if (match.getTeam2Ids() != null) allPlayers.addAll(match.getTeam2Ids());
            allPlayers.remove(winnerUuid);
            
            List<UUID> loserIds = new ArrayList<>(allPlayers);
            
            log.info("Updating match: winner={}, loser={}", winnerUuid, loserIds);
            
            // Update match scores and completion
            match.setCompleted(true);
            match.setTeam1Score(winnerScore);
            match.setTeam2Score(loserScore);
            
            // Save match first
            TournamentMatch updatedMatch = tournamentMatchRepository.save(match);
            
            // Now trigger tournament progression  
            log.info("Triggering tournament progression...");
            tournamentEngine.processMatchResult(
                match.getMatchId(),
                winnerIds,
                loserIds,
                winnerScore,
                loserScore
            );
            
            log.info("Successfully processed match result and tournament progression");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("matchId", matchId);
            response.put("winnerId", winnerId);
            response.put("winnerScore", winnerScore);
            response.put("loserScore", loserScore);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error updating match result", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Helper methods
    
    private Map<String, Object> mapTournamentToListItem(Tournament tournament) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", tournament.getId().toString());
        item.put("name", tournament.getName());
        item.put("description", tournament.getDescription());
        item.put("type", tournament.getTournamentType().name().toLowerCase());
        item.put("status", tournament.getStatus().name());
        item.put("organizerId", tournament.getOrganizerId().toString());
        
        // Get organizer name
        String organizerName = "Unknown";
        try {
            Player organizer = playerRepository.findById(tournament.getOrganizerId()).orElse(null);
            if (organizer != null) {
                organizerName = organizer.getFirstName() + " " + organizer.getLastName();
            }
        } catch (Exception e) {
            log.warn("Could not find organizer for tournament {}", tournament.getId());
        }
        item.put("organizerName", organizerName);
        
        item.put("maxParticipants", tournament.getNumberOfPlayers());
        
        int currentParticipants = tournamentPlayerRepository.findByTournament_Id(tournament.getId()).size();
        item.put("currentParticipants", currentParticipants);
        
        item.put("createdAt", tournament.getCreated());
        item.put("startDate", tournament.getStartDate());
        item.put("isPublic", tournament.isPublic());
        
        return item;
    }
    
    private Map<String, Object> mapTournamentToDetails(Tournament tournament) {
        Map<String, Object> details = new HashMap<>();
        details.put("id", tournament.getId().toString());
        details.put("name", tournament.getName());
        details.put("description", tournament.getDescription());
        details.put("type", tournament.getTournamentType().name().toLowerCase());
        details.put("status", tournament.getStatus().name());
        details.put("organizerId", tournament.getOrganizerId().toString());
        
        // Get organizer name
        String organizerName = "Unknown";
        try {
            Player organizer = playerRepository.findById(tournament.getOrganizerId()).orElse(null);
            if (organizer != null) {
                organizerName = organizer.getFirstName() + " " + organizer.getLastName();
            }
        } catch (Exception e) {
            log.warn("Could not find organizer for tournament {}", tournament.getId());
        }
        details.put("organizerName", organizerName);
        
        details.put("maxParticipants", tournament.getNumberOfPlayers());
        
        int currentParticipants = tournamentPlayerRepository.findByTournament_Id(tournament.getId()).size();
        details.put("currentParticipants", currentParticipants);
        
        details.put("createdAt", tournament.getCreated());
        details.put("startDate", tournament.getStartDate());
        details.put("endDate", tournament.getEndDate());
        details.put("isPublic", tournament.isPublic());
        
        return details;
    }
    
    /**
     * Converts internal tournament engine bracket data to frontend format
     */
    private Map<String, Object> convertBracketDataForFrontend(Tournament tournament, TournamentEngine.TournamentBracketData bracketData) {
        Map<String, Object> frontendBracketData = new HashMap<>();
        
        // Create participant list with consistent mapping between UUID and sequential IDs
        Map<UUID, Integer> playerIdMap = new HashMap<>();
        List<Map<String, Object>> participants = createParticipantsList(tournament, playerIdMap);
        
        // Create stage information
        List<Map<String, Object>> stages = createStages(tournament);
        
        // Convert all tournament rounds and matches to frontend format
        List<Map<String, Object>> matches = createMatches(bracketData, playerIdMap);
        
        // Empty match games for now
        List<Map<String, Object>> matchGames = new ArrayList<>();
        
        // Build final bracket data structure
        frontendBracketData.put("stage", stages);
        frontendBracketData.put("match", matches);
        frontendBracketData.put("match_game", matchGames);
        frontendBracketData.put("participant", participants);
        
        return frontendBracketData;
    }
    
    /**
     * Creates the participant list with consistent mapping from UUID to sequential IDs
     * Always sorts by player ID to ensure consistent ordering across all endpoints
     */
    private List<Map<String, Object>> createParticipantsList(Tournament tournament, Map<UUID, Integer> playerIdMap) {
        List<TournamentPlayer> tournamentPlayers = tournamentPlayerRepository.findByTournament_Id(tournament.getId());
        
        // Sort by player ID to ensure consistent ordering across all API calls
        tournamentPlayers.sort((a, b) -> a.getPlayerId().compareTo(b.getPlayerId()));
        
        List<Map<String, Object>> participants = new ArrayList<>();
        
        for (int i = 0; i < tournamentPlayers.size(); i++) {
            TournamentPlayer tp = tournamentPlayers.get(i);
            Player player = playerRepository.findById(tp.getPlayerId()).orElse(null);
            if (player != null) {
                int sequentialId = i + 1; // Sequential ID for frontend compatibility
                playerIdMap.put(tp.getPlayerId(), sequentialId);
                
                Map<String, Object> participant = new HashMap<>();
                participant.put("id", sequentialId);
                participant.put("tournament_id", tournament.getId().toString());
                participant.put("name", player.getFirstName() + " " + player.getLastName());
                participant.put("playerId", tp.getPlayerId().toString());
                if (tp.getSeed() != null) {
                    participant.put("seed", tp.getSeed());
                }
                participants.add(participant);
            }
        }
        
        return participants;
    }
    
    /**
     * Creates stage information for the tournament
     */
    private List<Map<String, Object>> createStages(Tournament tournament) {
        List<Map<String, Object>> stages = new ArrayList<>();
        Map<String, Object> stage = new HashMap<>();
        
        stage.put("id", 1);
        stage.put("tournament_id", tournament.getId().toString());
        stage.put("name", "Main Stage");
        
        // Convert tournament type to frontend format
        String tournamentType = tournament.getTournamentType().name().toLowerCase();
        if (tournamentType.equals("single_elimination")) {
            stage.put("type", "single_elimination");
        } else if (tournamentType.equals("double_elimination")) {
            stage.put("type", "double_elimination");
        } else {
            stage.put("type", tournamentType);
        }
        
        stage.put("number", 1);
        
        // Add stage settings for bracket display
        Map<String, Object> settings = new HashMap<>();
        settings.put("grandFinal", "simple");
        settings.put("balanceByes", true);
        stage.put("settings", settings);
        
        stages.add(stage);
        return stages;
    }
    
    /**
     * Creates match data for the frontend from tournament engine data
     */
    private List<Map<String, Object>> createMatches(TournamentEngine.TournamentBracketData bracketData, 
                                                Map<UUID, Integer> playerIdMap) {
        List<Map<String, Object>> matches = new ArrayList<>();
        int matchId = 1;
        
        // Process all winner bracket rounds
        for (TournamentRound round : bracketData.getWinnerRounds()) {
            for (TournamentMatch match : round.getMatches()) {
                matches.add(convertMatchToFrontendFormat(match, matchId++, 1, round.getRoundNumber(), playerIdMap));
            }
        }
        
        // Process all loser bracket rounds for double elimination
        int loserRoundOffset = bracketData.getWinnerRounds().size() + 1; // Offset loser rounds
        for (TournamentRound round : bracketData.getLoserRounds()) {
            for (TournamentMatch match : round.getMatches()) {
                matches.add(convertMatchToFrontendFormat(match, matchId++, 1, 
                    loserRoundOffset + round.getRoundNumber(), playerIdMap));
            }
        }
        
        // Process final matches
        int finalRoundId = Math.max(bracketData.getWinnerRounds().size(),
            bracketData.getLoserRounds().isEmpty() ? 0 : loserRoundOffset + bracketData.getLoserRounds().size()) + 1;
        for (TournamentMatch match : bracketData.getFinalMatches()) {
            matches.add(convertMatchToFrontendFormat(match, matchId++, 1, finalRoundId, playerIdMap));
        }
        
        return matches;
    }
    
    /**
     * Converts a backend TournamentMatch to frontend match format
     */
    private Map<String, Object> convertMatchToFrontendFormat(TournamentMatch match, int id, int stageId,
                                                         int roundId, Map<UUID, Integer> playerIdMap) {
        Map<String, Object> matchData = new HashMap<>();
        
        // Basic match properties
        matchData.put("id", id);
        matchData.put("stage_id", stageId);
        matchData.put("group_id", 1); // Default to group 1
        matchData.put("round_id", roundId);
        matchData.put("number", match.getPositionInRound() != null ? match.getPositionInRound() : id);
        
        // Match status
        int status;
        if (match.isCompleted()) {
            status = 4; // COMPLETED
        } else if (match.hasAllTeams()) {
            status = 2; // READY
        } else if (match.isBye()) {
            status = 5; // ARCHIVED/BYE
        } else {
            status = 1; // WAITING
        }
        matchData.put("status", status);
        
        // Opponent 1 data
        Map<String, Object> opponent1 = new HashMap<>();
        if (!match.getTeam1Ids().isEmpty()) {
            UUID playerId = match.getTeam1Ids().get(0);
            Integer sequentialId = playerIdMap.get(playerId);
            opponent1.put("id", sequentialId != null ? sequentialId : null);
            opponent1.put("position", 1);
            
            if (match.isCompleted() && match.getTeam1Score() != null) {
                opponent1.put("score", match.getTeam1Score());
                if (match.getWinnerIds().contains(playerId)) {
                    opponent1.put("result", "win");
                } else if (match.getLoserIds().contains(playerId)) {
                    opponent1.put("result", "loss");
                }
            }
        } else {
            opponent1.put("id", null);
            opponent1.put("position", 1);
        }
        matchData.put("opponent1", opponent1);
        
        // Opponent 2 data
        Map<String, Object> opponent2 = new HashMap<>();
        if (!match.getTeam2Ids().isEmpty()) {
            UUID playerId = match.getTeam2Ids().get(0);
            Integer sequentialId = playerIdMap.get(playerId);
            opponent2.put("id", sequentialId != null ? sequentialId : null);
            opponent2.put("position", 2);
            
            if (match.isCompleted() && match.getTeam2Score() != null) {
                opponent2.put("score", match.getTeam2Score());
                if (match.getWinnerIds().contains(playerId)) {
                    opponent2.put("result", "win");
                } else if (match.getLoserIds().contains(playerId)) {
                    opponent2.put("result", "loss");
                }
            }
        } else {
            opponent2.put("id", null);
            opponent2.put("position", 2);
        }
        matchData.put("opponent2", opponent2);
        
        return matchData;
    }
    
    /**
     * Finds a TournamentMatch by either UUID or frontend numeric ID
     */
    private TournamentMatch findMatchByFrontendId(UUID tournamentId, String matchId) {
        try {
            // First, try to parse as UUID (for direct backend access)
            UUID matchUuid = UUID.fromString(matchId);
            return tournamentMatchRepository.findById(matchUuid)
                .orElseThrow(() -> new RuntimeException("Match not found with UUID: " + matchId));
        } catch (IllegalArgumentException e) {
            // Not a UUID, treat as frontend numeric ID
            try {
                int frontendMatchId = Integer.parseInt(matchId);
                log.info("Looking for match with frontend ID: {} in tournament: {}", frontendMatchId, tournamentId);
                
                // Get all matches for the tournament and sort them consistently
                List<TournamentMatch> tournamentMatches = tournamentMatchRepository.findByTournament_Id(tournamentId);
                log.info("Found {} matches in tournament", tournamentMatches.size());
                
                // Create a mutable copy of the list to avoid UnsupportedOperationException
                List<TournamentMatch> allMatches = new ArrayList<>(tournamentMatches);
                
                // Sort matches consistently (same way we generate frontend IDs)
                // First by round, then by position in round
                allMatches.sort((a, b) -> {
                    int roundCompare = Integer.compare(a.getRound() != null ? a.getRound() : 0, 
                                                     b.getRound() != null ? b.getRound() : 0);
                    if (roundCompare != 0) return roundCompare;
                    return Integer.compare(a.getPositionInRound() != null ? a.getPositionInRound() : 0,
                                         b.getPositionInRound() != null ? b.getPositionInRound() : 0);
                });
                
                // The frontend match ID is 1-based, so adjust
                if (frontendMatchId > 0 && frontendMatchId <= allMatches.size()) {
                    TournamentMatch foundMatch = allMatches.get(frontendMatchId - 1);
                    log.info("Found match: {} for frontend ID: {}", foundMatch.getMatchId(), frontendMatchId);
                    log.info("Match details: displayId={}, isBye={}, team1Ids={}, team2Ids={}", 
                        foundMatch.getDisplayId(), foundMatch.isBye(), 
                        foundMatch.getTeam1Ids(), foundMatch.getTeam2Ids());
                    return foundMatch;
                } else {
                    throw new RuntimeException("Frontend match ID out of range: " + frontendMatchId);
                }
            } catch (NumberFormatException ex) {
                throw new RuntimeException("Invalid match ID format: " + matchId);
            }
        }
    }
}