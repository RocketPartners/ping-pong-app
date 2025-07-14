package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.Tournament;
import com.example.javapingpongelo.models.TournamentMatch;
import com.example.javapingpongelo.models.TournamentPlayer;
import com.example.javapingpongelo.models.dto.TournamentRequestDTO;
import com.example.javapingpongelo.models.exceptions.BadRequestException;
import com.example.javapingpongelo.models.exceptions.ResourceNotFoundException;
import com.example.javapingpongelo.repositories.TournamentMatchRepository;
import com.example.javapingpongelo.repositories.TournamentPlayerRepository;
import com.example.javapingpongelo.repositories.TournamentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for tournament-related operations
 */
@Service
@Slf4j
public class TournamentService {

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TournamentMatchRepository matchRepository;

    @Autowired
    private TournamentPlayerRepository playerRepository;

    @Autowired
    private IPlayerService playerService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Create a new tournament with given details
     *
     * @param tournamentDTO Tournament details
     * @return Created tournament with matches
     */
    @Transactional
    public Tournament createTournament(TournamentRequestDTO tournamentDTO) {
        log.info("Creating tournament: {}", tournamentDTO.getName());

        // Validate tournament details
        validateTournamentRequest(tournamentDTO);

        // Create tournament entity
        Tournament tournament = Tournament.builder()
                                          .name(tournamentDTO.getName())
                                          .description(tournamentDTO.getDescription())
                                          .tournamentType(tournamentDTO.getTournamentType())
                                          .gameType(tournamentDTO.getGameType())
                                          .seedingMethod(tournamentDTO.getSeedingMethod())
                                          .organizerId(tournamentDTO.getOrganizerId())
                                          .startDate(tournamentDTO.getStartDate())
                                          .endDate(tournamentDTO.getEndDate())
                                          .status(Tournament.TournamentStatus.CREATED)
                                          .currentRound(0)
                                          .numberOfPlayers(tournamentDTO.getPlayerIds().size())
                                          .build();

        // Save tournament to get ID
        tournament = tournamentRepository.save(tournament);

        // Process players and seeding
        List<TournamentPlayer> tournamentPlayers = processPlayers(tournament, tournamentDTO);

        // Generate or load bracket
        List<TournamentMatch> matches;
        if (tournament.getTournamentType() == Tournament.TournamentType.SINGLE_ELIMINATION) {
            matches = generateSingleEliminationBracket(tournament, tournamentPlayers);
        }
        else {
            matches = loadDoubleEliminationBracket(tournament, tournamentPlayers);
        }

        // Set matches in tournament for return (transient field)
        tournament.setMatches(matches);

        // Set playerIds in tournament for return (transient field)
        tournament.setPlayerIds(tournamentDTO.getPlayerIds());

        tournamentRepository.save(tournament);

        log.info("Tournament created successfully: {}", tournament.getId());
        return tournament;
    }

    /**
     * Validate tournament request
     */
    private void validateTournamentRequest(TournamentRequestDTO tournamentDTO) {
        // Check that all playerIds exist
        for (UUID playerId : tournamentDTO.getPlayerIds()) {
            try {
                playerService.findPlayerById(playerId);
            }
            catch (Exception e) {
                throw new ResourceNotFoundException("Player not found with id: " + playerId);
            }
        }

        // Check for doubles tournament
        if (tournamentDTO.getGameType() == Tournament.GameType.DOUBLES) {
            // Ensure even number of players
            if (tournamentDTO.getPlayerIds().size() % 2 != 0) {
                throw new BadRequestException("Doubles tournaments require an even number of players");
            }

            // Validate team pairs
            if (tournamentDTO.getTeamPairs() == null || tournamentDTO.getTeamPairs().isEmpty()) {
                throw new BadRequestException("Team pairs must be specified for doubles tournaments");
            }

            Set<UUID> allPlayers = new HashSet<>(tournamentDTO.getPlayerIds());
            Set<UUID> pairedPlayers = getPairedPlayers(tournamentDTO, allPlayers);

            // Ensure all players are paired
            if (pairedPlayers.size() != allPlayers.size()) {
                throw new BadRequestException("All players must be assigned to teams");
            }
        }

        // Check for double elimination tournament size
        if (tournamentDTO.getTournamentType() == Tournament.TournamentType.DOUBLE_ELIMINATION) {
            int teamCount = tournamentDTO.getGameType() == Tournament.GameType.SINGLES
                    ? tournamentDTO.getPlayerIds().size()
                    : tournamentDTO.getTeamPairs().size();

            if (teamCount < 3 || teamCount > 16) {
                throw new BadRequestException("Double elimination tournaments support 3-16 teams");
            }
        }

        // Check for single elimination tournament size
        if (tournamentDTO.getTournamentType() == Tournament.TournamentType.SINGLE_ELIMINATION) {
            int teamCount = tournamentDTO.getGameType() == Tournament.GameType.SINGLES
                    ? tournamentDTO.getPlayerIds().size()
                    : tournamentDTO.getTeamPairs().size();

            if (teamCount < 2 || teamCount > 64) {
                throw new BadRequestException("Single elimination tournaments support 2-64 teams");
            }
        }
    }

    /**
     * Process players and apply seeding
     */
    private List<TournamentPlayer> processPlayers(Tournament tournament, TournamentRequestDTO tournamentDTO) {
        List<TournamentPlayer> tournamentPlayers = new ArrayList<>();
        List<UUID> playerIds = new ArrayList<>(tournamentDTO.getPlayerIds());

        // Apply seeding logic
        if (tournament.getSeedingMethod() == Tournament.SeedingMethod.RATING_BASED) {
            // Sort players by rating
            playerIds = sortPlayersByRating(playerIds, tournament.getGameType());
        }
        else if (tournament.getSeedingMethod() == Tournament.SeedingMethod.RANDOM) {
            // Shuffle for random seeding
            Collections.shuffle(playerIds);
        }

        // For singles: each player is their own entry
        if (tournament.getGameType() == Tournament.GameType.SINGLES) {
            for (int i = 0; i < playerIds.size(); i++) {
                TournamentPlayer tp = TournamentPlayer.builder()
                                                      .tournament(tournament)
                                                      .playerId(playerIds.get(i))
                                                      .seedPosition(i + 1)
                                                      .build();
                tournamentPlayers.add(playerRepository.save(tp));
            }
        }
        // For doubles: create entries with partners
        else {
            Map<UUID, UUID> partnerMap = new HashMap<>();
            for (TournamentRequestDTO.TeamPairDTO pair : tournamentDTO.getTeamPairs()) {
                partnerMap.put(pair.getPlayer1Id(), pair.getPlayer2Id());
                partnerMap.put(pair.getPlayer2Id(), pair.getPlayer1Id());
            }

            // Create tournament player entries with partners
            for (int i = 0; i < playerIds.size(); i++) {
                UUID playerId = playerIds.get(i);
                UUID partnerId = partnerMap.get(playerId);

                TournamentPlayer tp = TournamentPlayer.builder()
                                                      .tournament(tournament)
                                                      .playerId(playerId)
                                                      .partnerId(partnerId)
                                                      .seedPosition(i + 1)
                                                      .build();
                tournamentPlayers.add(playerRepository.save(tp));
            }
        }

        return tournamentPlayers;
    }

    /**
     * Generate a single elimination bracket for the tournament using a simple, robust algorithm:
     * 1. Create first-round matches based on next power of 2
     * 2. Assign players sequentially until we reach partial matches
     * 3. Build proper round progression
     */
    private List<TournamentMatch> generateSingleEliminationBracket(Tournament tournament, List<TournamentPlayer> players) {
        int teamCount = players.size();
        if (tournament.getGameType() == Tournament.GameType.DOUBLES) {
            teamCount = teamCount / 2;
        }

        log.info("Generating single elimination bracket for {} teams", teamCount);

        // Handle edge cases
        if (teamCount == 0) {
            return new ArrayList<>();
        }

        if (teamCount == 1) {
            // Just return a "champion" with no matches
            return new ArrayList<>();
        }

        // Find the next power of 2 greater than or equal to the number of teams
        int nextPowerOf2 = 1;
        while (nextPowerOf2 < teamCount) {
            nextPowerOf2 *= 2;
        }

        // Calculate number of rounds needed
        int rounds = (int) Math.ceil(Math.log(nextPowerOf2) / Math.log(2));

        // Calculate number of first-round matches (always half of nextPowerOf2)
        int firstRoundMatches = nextPowerOf2 / 2;

        // Calculate total matches (always teamCount - 1)
        int totalMatches = teamCount - 1;

        log.debug("Teams: {}, Next power of 2: {}, First round matches: {}, Total matches needed: {}",
                  teamCount, nextPowerOf2, firstRoundMatches, totalMatches);

        // Create all the matches we'll need
        List<TournamentMatch> matches = new ArrayList<>(totalMatches);

        // Create matches for each round
        for (int round = 1; round <= rounds; round++) {
            int matchesInRound = nextPowerOf2 / (1 << round);

            for (int matchNum = 1; matchNum <= matchesInRound; matchNum++) {
                // Updated match ID format: W{matchNum}R{round}
                TournamentMatch match = TournamentMatch.builder()
                                                       .id("W" + matchNum + "R" + round)
                                                       .bracketType(TournamentMatch.BracketType.WINNER)
                                                       .completed(false)
                                                       .tournament(tournament)
                                                       .round(round)
                                                       .build();

                matches.add(matchRepository.save(match));
            }
        }

        // Sort matches by round and match number
        matches.sort(Comparator.comparing(TournamentMatch::getRound)
                               .thenComparing(m -> {
                                   // Extract match number from "W{num}R{round}"
                                   String matchId = m.getId();
                                   int rIndex = matchId.indexOf('R');
                                   return Integer.parseInt(matchId.substring(1, rIndex));
                               }));

        // Connect matches - link each match to the next round
        connectMatchesForSingleElimination(matches);

        // Assign players to first-round matches
        assignPlayersToMatches(matches, players, teamCount, firstRoundMatches);

        // Auto-advance players with byes to next rounds
        advancePlayersThroughByeMatches(matches);

        return matches;
    }

    /**
     * Load a pre-defined double elimination bracket template
     */
    private List<TournamentMatch> loadDoubleEliminationBracket(Tournament tournament, List<TournamentPlayer> players) {
        int teamCount = players.size();
        if (tournament.getGameType() == Tournament.GameType.DOUBLES) {
            teamCount = teamCount / 2;
        }

        log.info("Loading double elimination bracket for {} teams", teamCount);

        // Load the template from resources
        String templatePath = "classpath:templates/double-elim-tournaments/" + teamCount + "player.json";

        try {
            Resource resource = resourceLoader.getResource(templatePath);
            InputStream inputStream = resource.getInputStream();

            Map<String, Object> templateData = objectMapper.readValue(inputStream,
                                                                      new TypeReference<>() {
                                                                      });

            List<Map<String, Object>> templateMatches = objectMapper.convertValue(
                    templateData.get("matches"),
                    new TypeReference<>() {
                    });

            // Calculate total rounds for this bracket
            int totalRounds = 1;
            if (teamCount > 1) {
                totalRounds = (int) Math.ceil(Math.log(teamCount) / Math.log(2)) + 1; // +1 for finals
            }

            // Create matches for this tournament based on the template
            List<TournamentMatch> matches = new ArrayList<>();
            Map<String, UUID> matchIdMap = new HashMap<>();

            // First pass: create all matches with unique IDs
            for (Map<String, Object> templateMatch : templateMatches) {
                String matchId = (String) templateMatch.get("id");
                String bracketTypeStr = (String) templateMatch.get("bracketType");
                TournamentMatch.BracketType bracketType =
                        TournamentMatch.BracketType.valueOf(bracketTypeStr);

                // Extract round from the match ID for bracket matches
                Integer round = getRound(matchId, totalRounds);

                TournamentMatch match = TournamentMatch.builder()
                                                       .id(matchId)
                                                       .bracketType(bracketType)
                                                       .completed(false)
                                                       .tournament(tournament)
                                                       .round(round)
                                                       .build();

                TournamentMatch savedMatch = matchRepository.save(match);
                matchIdMap.put(matchId, savedMatch.getMatchId());
                matches.add(savedMatch);
            }

            // Second pass: set next match references
            for (int i = 0; i < templateMatches.size(); i++) {
                Map<String, Object> templateMatch = templateMatches.get(i);
                TournamentMatch match = matches.get(i);

                // Set winner's next match
                String winnerNextMatchId = (String) templateMatch.get("winnerNextMatchId");
                if (winnerNextMatchId != null && !winnerNextMatchId.equals("null")) {
                    match.setWinnerNextMatchId(matchIdMap.get(winnerNextMatchId));
                }

                // Set loser's next match
                String loserNextMatchId = (String) templateMatch.get("loserNextMatchId");
                if (loserNextMatchId != null && !loserNextMatchId.equals("null")) {
                    match.setLoserNextMatchId(matchIdMap.get(loserNextMatchId));
                }

                matchRepository.save(match);
            }

            // Seed players into first round matches
            seedDoubleEliminationFirstRound(matches, players, teamCount);

            return matches;
        }
        catch (IOException e) {
            log.error("Error loading bracket template", e);
            throw new RuntimeException("Error loading bracket template", e);
        }
    }

    private static Set<UUID> getPairedPlayers(TournamentRequestDTO tournamentDTO, Set<UUID> allPlayers) {
        Set<UUID> pairedPlayers = new HashSet<>();

        for (TournamentRequestDTO.TeamPairDTO pair : tournamentDTO.getTeamPairs()) {
            if (pair.getPlayer1Id() == null || pair.getPlayer2Id() == null) {
                throw new BadRequestException("Both players must be specified in team pairs");
            }

            if (!allPlayers.contains(pair.getPlayer1Id()) || !allPlayers.contains(pair.getPlayer2Id())) {
                throw new BadRequestException("Team pair contains player not in the player list");
            }

            if (pairedPlayers.contains(pair.getPlayer1Id()) || pairedPlayers.contains(pair.getPlayer2Id())) {
                throw new BadRequestException("Player cannot be in multiple teams");
            }

            pairedPlayers.add(pair.getPlayer1Id());
            pairedPlayers.add(pair.getPlayer2Id());
        }
        return pairedPlayers;
    }

    /**
     * Sort players by their rating based on game type
     */
    private List<UUID> sortPlayersByRating(List<UUID> playerIds, Tournament.GameType gameType) {
        Map<UUID, Integer> playerRatings = new HashMap<>();

        // Get ratings for all players
        for (UUID playerId : playerIds) {
            Player player = playerService.findPlayerById(playerId);

            int rating;
            if (gameType == Tournament.GameType.SINGLES) {
                // Use singles ranked rating
                rating = player.getSinglesRankedRating();
            }
            else {
                // Use doubles ranked rating
                rating = player.getDoublesRankedRating();
            }

            playerRatings.put(playerId, rating);
        }

        // Sort players by rating (descending)
        return playerIds.stream()
                        .sorted((p1, p2) -> playerRatings.get(p2).compareTo(playerRatings.get(p1)))
                        .collect(Collectors.toList());
    }

    /**
     * Connect matches to establish proper tournament flow for single elimination
     */
    private void connectMatchesForSingleElimination(List<TournamentMatch> matches) {
        // Group matches by round
        Map<Integer, List<TournamentMatch>> matchesByRound = new HashMap<>();

        for (TournamentMatch match : matches) {
            int round = match.getRound();
            matchesByRound.computeIfAbsent(round, k -> new ArrayList<>()).add(match);
        }

        // For each round except the final, connect to next round
        int maxRound = matches.stream().mapToInt(TournamentMatch::getRound).max().orElse(0);

        for (int round = 1; round < maxRound; round++) {
            List<TournamentMatch> currentRoundMatches = matchesByRound.get(round);
            List<TournamentMatch> nextRoundMatches = matchesByRound.get(round + 1);

            if (currentRoundMatches == null || nextRoundMatches == null) continue;

            // Each pair of matches in current round feeds into one match in next round
            for (int i = 0; i < currentRoundMatches.size(); i++) {
                TournamentMatch currentMatch = currentRoundMatches.get(i);
                int nextMatchIndex = i / 2;

                if (nextMatchIndex < nextRoundMatches.size()) {
                    TournamentMatch nextMatch = nextRoundMatches.get(nextMatchIndex);
                    currentMatch.setWinnerNextMatchId(nextMatch.getMatchId());
                    matchRepository.save(currentMatch);
                }
            }
        }
    }

    /**
     * Assign players to first-round matches using the specified algorithm
     */
    private void assignPlayersToMatches(List<TournamentMatch> matches, List<TournamentPlayer> players,
                                        int teamCount, int firstRoundMatches) {
        // Get only the first-round matches
        List<TournamentMatch> firstRoundMatchList = matches.stream()
                                                           .filter(m -> m.getRound() == 1)
                                                           .toList();

        // Shuffle players if using random seeding
        List<TournamentPlayer> playerList = new ArrayList<>(players);
        if (playerList.size() > teamCount) {
            // For doubles, we need to handle differently
            playerList = playerList.subList(0, teamCount);
        }

        // For RANDOM seeding type, shuffle the players
        if (!playerList.isEmpty() && playerList.getFirst().getTournament().getSeedingMethod() == Tournament.SeedingMethod.RANDOM) {
            Collections.shuffle(playerList);
        }

        int playersAssigned = 0;
        int matchesProcessed = 0;

        int switchPoint = isPowerOfTwo(teamCount)
                ? teamCount
                : (firstRoundMatches * 2) - (teamCount - firstRoundMatches);

        log.debug("Switch point for single player assignment: {}", switchPoint);

        // Assign players to matches
        for (TournamentMatch match : firstRoundMatchList) {
            if (playersAssigned < switchPoint) {
                // Assign two players to this match
                playersAssigned = getPlayersAssigned(playerList, playersAssigned, match);
            }
            else {
                // Assign one player to this match (giving them a "bye")
                if (playersAssigned < playerList.size()) {
                    List<UUID> team1Ids = new ArrayList<>();
                    team1Ids.add(playerList.get(playersAssigned).getPlayerId());
                    match.setTeam1Ids(team1Ids);
                    playersAssigned++;
                }
            }

            matchRepository.save(match);
            matchesProcessed++;
        }

        log.debug("Assigned {} players to {} first-round matches", playersAssigned, matchesProcessed);
    }

    /**
     * Automatically advance players who have byes in the first round
     */
    private void advancePlayersThroughByeMatches(List<TournamentMatch> matches) {
        log.debug("Processing first round bye matches and advancing players");

        // Find all first round matches with only one player (team)
        List<TournamentMatch> byeMatches = matches.stream()
                                                  .filter(m -> m.getRound() == 1)  // Only first round matches
                                                  .filter(m -> m.getTeam1Ids() != null && !m.getTeam1Ids().isEmpty())  // Has team1
                                                  .filter(m -> m.getTeam2Ids() == null || m.getTeam2Ids().isEmpty())   // No team2
                                                  .filter(m -> !m.isCompleted())  // Not already completed
                                                  .collect(Collectors.toList());

        log.debug("Found {} first round bye matches to process", byeMatches.size());

        // Process each bye match
        for (TournamentMatch match : byeMatches) {
            // Mark match as completed
            match.setCompleted(true);

            // Set winner as team1
            match.setWinnerIds(match.getTeam1Ids());

            // Set loser as empty list to maintain consistency
            match.setLoserIds(new ArrayList<>());

            // Save the match
            matchRepository.save(match);

            // Propagate winner to next match
            propagateWinnerToNextMatch(match);

            log.debug("Processed first round bye match: {}, advanced player(s): {}", match.getId(), match.getTeam1Ids());
        }
    }

    /**
     * Extract round number from match ID
     */
    private static Integer getRound(String matchId, int totalRounds) {
        Integer round = null;

        // First try to extract round from new 'R' format
        if (matchId.contains("R")) {
            try {
                // Extract round part after "R"
                String roundPart = matchId.substring(matchId.indexOf("R") + 1);
                round = Integer.parseInt(roundPart);
                return round;
            }
            catch (NumberFormatException e) {
                // Fall back to legacy format if parsing fails
            }
        }

        if (matchId.startsWith("W") || matchId.startsWith("L")) {
            try {
                // Try to extract numeric part for W1, W2, etc.
                String numericPart = matchId.substring(1);
                round = Integer.parseInt(numericPart);
            }
            catch (NumberFormatException e) {
                // Ignore if not a simple numeric format
            }
        }
        else if (matchId.startsWith("F")) {
            // Finals are last rounds
            round = matchId.equals("F1") ? totalRounds + 1 : totalRounds + 2;
        }

        return round;
    }

    /**
     * Seed players into the first round of a double elimination tournament
     */
    private void seedDoubleEliminationFirstRound(List<TournamentMatch> matches, List<TournamentPlayer> players, int teamCount) {
        log.debug("Seeding double elimination tournament with {} players", teamCount);

        // Find all the first round winner bracket matches (W*R1)
        List<TournamentMatch> firstRoundMatches = matches.stream()
                                                         .filter(m -> m.getRound() == 1 && m.getBracketType() == TournamentMatch.BracketType.WINNER)
                                                         .sorted(Comparator.comparing(m -> getMatchNumber(m.getId())))
                                                         .toList();

        // Find second round winner bracket matches (W*R2) that might need direct seeding
        List<TournamentMatch> secondRoundMatches = matches.stream()
                                                          .filter(m -> m.getRound() == 2 && m.getBracketType() == TournamentMatch.BracketType.WINNER)
                                                          .sorted(Comparator.comparing(m -> getMatchNumber(m.getId())))
                                                          .toList();

        log.debug("Found {} R1 matches and {} R2 matches for seeding",
                  firstRoundMatches.size(), secondRoundMatches.size());

        if (firstRoundMatches.isEmpty() && secondRoundMatches.isEmpty()) {
            log.error("No appropriate matches found for tournament");
            throw new BadRequestException("Tournament bracket configuration error");
        }

        // Prepare player list
        List<TournamentPlayer> playerList = new ArrayList<>(players);
        if (playerList.size() > teamCount) {
            // For doubles, handle differently
            playerList = playerList.subList(0, teamCount);
        }

        // Apply seeding method
        if (!playerList.isEmpty() && playerList.getFirst().getTournament().getSeedingMethod() == Tournament.SeedingMethod.RANDOM) {
            Collections.shuffle(playerList);
        }

        int playersAssigned = 0;

        // Calculate switch point - when we should start assigning only one player per match
        // If there are exactly enough players to fill all first round matches with 2 players each, switchPoint = playerList.size()
        // Otherwise, calculate how many players should get byes

        log.debug("Switch point for single player assignment: {}", firstRoundMatches.size());

        // First assign players to first round matches
        for (TournamentMatch match : firstRoundMatches) {
            if (playersAssigned >= playerList.size()) {
                break; // No more players to assign
            }

            if (playersAssigned < firstRoundMatches.size() * 2) {
                // Assign two players to this match
                if (playersAssigned + 1 < playerList.size()) {
                    // Assign team 1
                    List<UUID> team1Ids = new ArrayList<>();
                    team1Ids.add(playerList.get(playersAssigned).getPlayerId());
                    match.setTeam1Ids(team1Ids);
                    playersAssigned++;

                    // Assign team 2
                    List<UUID> team2Ids = new ArrayList<>();
                    team2Ids.add(playerList.get(playersAssigned).getPlayerId());
                    match.setTeam2Ids(team2Ids);
                    playersAssigned++;
                }
                else {
                    // Only one player left, give them a bye
                    List<UUID> team1Ids = new ArrayList<>();
                    team1Ids.add(playerList.get(playersAssigned).getPlayerId());
                    match.setTeam1Ids(team1Ids);
                    playersAssigned++;
                }
            }
            else {
                // Assign one player to this match (giving them a "bye")
                List<UUID> team1Ids = new ArrayList<>();
                team1Ids.add(playerList.get(playersAssigned).getPlayerId());
                match.setTeam1Ids(team1Ids);
                playersAssigned++;
            }

            matchRepository.save(match);
            log.debug("Match {}: team1={}, team2={}", match.getId(), match.getTeam1Ids(), match.getTeam2Ids());
        }

        // If we have more players, assign them directly to R2 matches
        if (playersAssigned < playerList.size() && !secondRoundMatches.isEmpty()) {
            log.debug("Moving to R2 matches for remaining {} players", playerList.size() - playersAssigned);

            for (TournamentMatch match : secondRoundMatches) {
                if (playersAssigned >= playerList.size()) {
                    break; // No more players to assign
                }

                // For R2, only assign team1 to give a bye directly to R2
                List<UUID> team1Ids = new ArrayList<>();
                team1Ids.add(playerList.get(playersAssigned).getPlayerId());
                match.setTeam1Ids(team1Ids);
                playersAssigned++;

                matchRepository.save(match);
                log.debug("R2 Match {}: team1={}", match.getId(), match.getTeam1Ids());
            }
        }

        log.debug("Assigned {} players to matches", playersAssigned);
    }

    boolean isPowerOfTwo(int n) {
        // Handle edge case: 0 is not a power of 2
        if (n <= 0) return false;

        // A power of 2 has only one bit set to 1
        // So (n & (n-1)) should be 0 for powers of 2
        return (n & (n - 1)) == 0;
    }

    private int getPlayersAssigned(List<TournamentPlayer> playerList, int playersAssigned, TournamentMatch match) {
        if (playersAssigned < playerList.size()) {
            List<UUID> team1Ids = new ArrayList<>();
            team1Ids.add(playerList.get(playersAssigned).getPlayerId());
            match.setTeam1Ids(team1Ids);
            playersAssigned++;
        }

        if (playersAssigned < playerList.size()) {
            List<UUID> team2Ids = new ArrayList<>();
            team2Ids.add(playerList.get(playersAssigned).getPlayerId());
            match.setTeam2Ids(team2Ids);
            playersAssigned++;
        }
        return playersAssigned;
    }

    /**
     * Propagate winner to the next match
     */
    private void propagateWinnerToNextMatch(TournamentMatch match) {
        if (match.getWinnerNextMatchId() != null) {
            // Get next match
            TournamentMatch nextMatch = matchRepository.findById(match.getWinnerNextMatchId())
                                                       .orElse(null);

            if (nextMatch != null) {
                // Add winner to next match (team1 if empty, otherwise team2)
                if (nextMatch.getTeam1Ids() == null || nextMatch.getTeam1Ids().isEmpty()) {
                    nextMatch.setTeam1Ids(match.getWinnerIds());
                }
                else {
                    nextMatch.setTeam2Ids(match.getWinnerIds());
                }

                matchRepository.save(nextMatch);
            }
        }
    }

    /**
     * Helper method to extract match number from ID (e.g., '1' from 'W1R1')
     */
    private int getMatchNumber(String matchId) {
        if (matchId.contains("R")) {
            // New format with round: W1R1, W2R1, etc.
            int endIndex = matchId.indexOf("R");
            try {
                return Integer.parseInt(matchId.substring(1, endIndex));
            }
            catch (NumberFormatException e) {
                return -1; // Invalid format
            }
        }
        else {
            // Legacy format: W1, W2, etc.
            try {
                return Integer.parseInt(matchId.substring(1));
            }
            catch (NumberFormatException e) {
                return -1; // Invalid format
            }
        }
    }

    /**
     * Get a tournament by ID
     */
    public Tournament getTournament(UUID id) {
        log.debug("Getting tournament: {}", id);

        Tournament tournament = tournamentRepository.findById(id)
                                                    .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));

        // Load matches
        List<TournamentMatch> matches = matchRepository.findByTournament_Id(id);
        tournament.setMatches(matches);

        // Load player IDs
        List<TournamentPlayer> tournamentPlayers = playerRepository.findByTournament_Id(id);
        List<UUID> playerIds = tournamentPlayers.stream()
                                                .map(TournamentPlayer::getPlayerId)
                                                .collect(Collectors.toList());
        tournament.setPlayerIds(playerIds);

        return tournament;
    }

    /**
     * Get all tournaments
     */
    public List<Tournament> getAllTournaments() {
        log.debug("Getting all tournaments");
        return tournamentRepository.findAll();
    }

    /**
     * Update a tournament
     */
    public Tournament updateTournament(UUID id, Tournament tournament) {
        log.debug("Updating tournament: {}", id);

        if (!id.equals(tournament.getId())) {
            throw new BadRequestException("Tournament ID in path does not match body");
        }

        // Check if tournament exists
        tournamentRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));

        // Update and save
        return tournamentRepository.save(tournament);
    }

    /**
     * Delete a tournament
     */
    @Transactional
    public void deleteTournament(UUID id) {
        log.debug("Deleting tournament: {}", id);

        // Check if tournament exists
        Tournament tournament = tournamentRepository.findById(id)
                                                    .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));

        // Delete all tournament matches
        List<TournamentMatch> matches = matchRepository.findByTournament_Id(id);
        matchRepository.deleteAll(matches);

        // Delete all tournament players
        List<TournamentPlayer> players = playerRepository.findByTournament_Id(id);
        playerRepository.deleteAll(players);

        // Delete tournament
        tournamentRepository.delete(tournament);
    }

    /**
     * Get tournaments by organizer
     */
    public List<Tournament> getTournamentsByOrganizer(UUID organizerId) {
        log.debug("Getting tournaments for organizer: {}", organizerId);
        return tournamentRepository.findByOrganizerId(organizerId);
    }

    /**
     * Get tournaments by player
     */
    public List<Tournament> getTournamentsByPlayer(UUID playerId) {
        log.debug("Getting tournaments for player: {}", playerId);
        return tournamentRepository.findByPlayerId(playerId);
    }

    /**
     * Get tournaments by status
     */
    public List<Tournament> getTournamentsByStatus(Tournament.TournamentStatus status) {
        log.debug("Getting tournaments with status: {}", status);
        return tournamentRepository.findByStatus(status);
    }

    /**
     * Start a tournament
     */
    @Transactional
    public Tournament startTournament(UUID id) {
        log.debug("Starting tournament: {}", id);

        Tournament tournament = tournamentRepository.findById(id)
                                                    .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));

        if (tournament.getStatus() != Tournament.TournamentStatus.CREATED) {
            throw new BadRequestException("Tournament already started or completed");
        }

        tournament.setStatus(Tournament.TournamentStatus.IN_PROGRESS);
        tournament.setCurrentRound(1);

        return tournamentRepository.save(tournament);
    }

    /**
     * Complete a tournament
     */
    @Transactional
    public Tournament completeTournament(UUID id) {
        log.debug("Completing tournament: {}", id);

        Tournament tournament = tournamentRepository.findById(id)
                                                    .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));

        if (tournament.getStatus() != Tournament.TournamentStatus.IN_PROGRESS) {
            throw new BadRequestException("Tournament not in progress");
        }

        // Check if all matches are completed
        List<TournamentMatch> matches = matchRepository.findByTournament_Id(id);
        boolean allCompleted = matches.stream().allMatch(TournamentMatch::isCompleted);

        if (!allCompleted) {
            throw new BadRequestException("Cannot complete tournament - not all matches are finished");
        }

        tournament.setStatus(Tournament.TournamentStatus.COMPLETED);
        tournament.setEndDate(new Date());

        // Determine champion and runner-up
        determineChampionAndRunnerUp(tournament, matches);

        return tournamentRepository.save(tournament);
    }

    /**
     * Determine tournament champion and runner-up
     */
    private void determineChampionAndRunnerUp(Tournament tournament, List<TournamentMatch> matches) {
        // Find the final match (in single elimination) or championship match (in double elimination)
        Optional<TournamentMatch> finalMatch = matches.stream()
                                                      .filter(m -> m.getBracketType() == TournamentMatch.BracketType.CHAMPIONSHIP ||
                                                              (m.getBracketType() == TournamentMatch.BracketType.FINAL &&
                                                                      tournament.getTournamentType() == Tournament.TournamentType.SINGLE_ELIMINATION))
                                                      .findFirst();

        if (finalMatch.isPresent() && finalMatch.get().isCompleted()) {
            TournamentMatch match = finalMatch.get();

            // Champion is the winner of the final match
            if (match.getWinnerIds() != null && !match.getWinnerIds().isEmpty()) {
                // For singles, there's just one ID in the list
                if (tournament.getGameType() == Tournament.GameType.SINGLES) {
                    tournament.setChampionId(match.getWinnerIds().getFirst());
                }
            }

            // Runner-up is the loser of the final match
            if (match.getLoserIds() != null && !match.getLoserIds().isEmpty()) {
                // For singles, there's just one ID in the list
                if (tournament.getGameType() == Tournament.GameType.SINGLES) {
                    tournament.setRunnerUpId(match.getLoserIds().getFirst());
                }
            }
        }
    }

    /**
     * Update a match result
     */
    @Transactional
    public TournamentMatch updateMatchResult(UUID tournamentId, UUID matchId, TournamentMatch matchUpdate) {
        log.debug("Updating match result: {} in tournament: {}", matchId, tournamentId);

        // Check if match exists
        TournamentMatch match = matchRepository.findById(matchId)
                                               .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        // Check if match already completed
        if (match.isCompleted()) {
            throw new BadRequestException("Match already completed");
        }

        // Update match with result
        match.setWinnerIds(matchUpdate.getWinnerIds());
        match.setLoserIds(matchUpdate.getLoserIds());
        match.setCompleted(true);

        // Save match
        match = matchRepository.save(match);

        // Propagate winner to next match
        propagateWinnerToNextMatch(match);

        // For double elimination, also propagate loser to loser bracket
        if (match.getBracketType() == TournamentMatch.BracketType.WINNER &&
                match.getLoserNextMatchId() != null) {
            propagateLoserToNextMatch(match);
        }

        return match;
    }

    /**
     * Propagate loser to the next match in loser bracket
     */
    private void propagateLoserToNextMatch(TournamentMatch match) {
        if (match.getLoserNextMatchId() != null) {
            // Get next match
            TournamentMatch nextMatch = matchRepository.findById(match.getLoserNextMatchId())
                                                       .orElse(null);

            if (nextMatch != null) {
                // Add loser to next match (team1 if empty, otherwise team2)
                if (nextMatch.getTeam1Ids() == null || nextMatch.getTeam1Ids().isEmpty()) {
                    nextMatch.setTeam1Ids(match.getLoserIds());
                }
                else {
                    nextMatch.setTeam2Ids(match.getLoserIds());
                }

                matchRepository.save(nextMatch);
            }
        }
    }

    /**
     * Get matches for a tournament
     */
    public List<TournamentMatch> getTournamentMatches(UUID tournamentId) {
        log.debug("Getting matches for tournament: {}", tournamentId);

        // Check if tournament exists
        tournamentRepository.findById(tournamentId)
                            .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + tournamentId));

        return matchRepository.findByTournament_Id(tournamentId);
    }

    /**
     * Get matches by bracket type
     */
    public List<TournamentMatch> getTournamentMatchesByBracket(UUID tournamentId, TournamentMatch.BracketType bracketType) {
        log.debug("Getting {} bracket matches for tournament: {}", bracketType, tournamentId);

        // Check if tournament exists
        tournamentRepository.findById(tournamentId)
                            .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + tournamentId));

        return matchRepository.findByTournamentIdAndBracketType(tournamentId, String.valueOf(bracketType));
    }

    /**
     * Get players in a tournament
     */
    public List<TournamentPlayer> getTournamentPlayers(UUID tournamentId) {
        log.debug("Getting players for tournament: {}", tournamentId);

        // Check if tournament exists
        tournamentRepository.findById(tournamentId)
                            .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + tournamentId));

        return playerRepository.findByTournament_Id(tournamentId);
    }
}