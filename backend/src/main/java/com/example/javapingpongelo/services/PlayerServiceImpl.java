package com.example.javapingpongelo.services;

import com.example.javapingpongelo.configuration.DomainRestrictionConfig;
import com.example.javapingpongelo.events.AchievementPlayerRegistrationListener;
import com.example.javapingpongelo.models.*;
import com.example.javapingpongelo.models.dto.PlayerStyleAverageDTO;
import com.example.javapingpongelo.models.dto.PlayerStyleTopDTO;
import com.example.javapingpongelo.models.exceptions.BadRequestException;
import com.example.javapingpongelo.models.exceptions.ResourceNotFoundException;
import com.example.javapingpongelo.repositories.PlayerRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Example;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Transactional
@Slf4j
public class PlayerServiceImpl implements IPlayerService {

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AchievementPlayerRegistrationListener achievementListener;

    @Autowired
    private PlayerEloHistoryService eloHistoryService;
    
    @Autowired
    private DomainRestrictionConfig domainRestrictionConfig;
    
    @Autowired
    private InvitationService invitationService;
    
    @Autowired
    private EmailVerificationService emailVerificationService;

    @Override
    public Player registerNewUserAccount(Player player) throws BadRequestException {
        if (emailExists(player.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        if (usernameExists(player.getUsername())) {
            throw new BadRequestException("Username already exists");
        }
        
        // Check domain restriction
        if (domainRestrictionConfig.isDomainRestrictionEnabled()) {
            String email = player.getEmail();
            String invitationCode = player.getInvitationCode();
            
            if (!invitationService.isEmailAllowedToRegister(email, invitationCode)) {
                throw new BadRequestException("Registration for this email domain is restricted. Please use a valid invitation code.");
            }
            
            // If the invitation code is valid, mark it as used
            if (invitationCode != null && !invitationCode.isEmpty()) {
                invitationService.markInvitationCodeAsUsed(invitationCode);
            }
        }

        // Hash password before saving
        player.setPassword(passwordEncoder.encode(player.getPassword()));

        // Set default fields
        player.setSinglesRankedRating(1000);
        player.setDoublesRankedRating(1000);
        player.setSinglesNormalRating(1000);
        player.setDoublesNormalRating(1000);
        
        // Set email verification status to false initially
        player.setEmailVerified(false);

        // Initialize style ratings with default values (50 for each style)
        player.initializeStyleRatings();

        // Save the player
        Player savedPlayer = playerRepository.save(player);
        
        // Send verification email if required
        if (domainRestrictionConfig.isEmailVerificationRequired()) {
            emailVerificationService.createVerificationTokenAndSendEmail(savedPlayer);
        } else {
            // Auto-verify if email verification is not required
            savedPlayer.setEmailVerified(true);
            playerRepository.save(savedPlayer);
        }

        // Initialize achievements - get the listener by name to avoid circular dependency
        try {
            achievementListener.onPlayerRegistration(savedPlayer);
        }
        catch (Exception e) {
            log.warn("Error initializing achievements for new player: {}", player.getUsername(), e);
            // Don't fail registration if achievement initialization fails
        }

        return savedPlayer;
    }

    @Override
    public Player findPlayerByName(String firstName, String lastName) {
        Player probe = new Player();
        probe.setFirstName(firstName);
        probe.setLastName(lastName);
        return playerRepository.findOne(Example.of(probe)).orElseThrow();
    }

    @Override
    public List<Player> findAllPlayers() {
        return playerRepository.findAll();
    }

    @Override
    public Player createPlayer(Player player) {
        playerRepository.save(player);
        return player;
    }

    @Override
    public Player findPlayerByUsername(String username) {
        Player probe = new Player();
        probe.setUsername(username);
        return playerRepository.findByUsername(username);
    }

    @Override
    public Player findPlayerByEmail(String email) {
        return playerRepository.findByEmail(email);
    }

    @Override
    public Player findPlayerById(UUID id) {
        return playerRepository.findById(id).orElseThrow();
    }

    @Override
    public void updatePlayerSinglesRankedElo(Player player, double eloUpdate, Game game) {
        // Capture the previous ELO before updating
        int previousElo = player.getSinglesRankedRating();
        int newElo = (int) Math.round(eloUpdate);

        // Update win/loss record
        if (previousElo < newElo) {
            player.setSinglesRankedWins(player.getSinglesRankedWins() + 1);
        }
        else {
            player.setSinglesRankedLoses(player.getSinglesRankedLoses() + 1);
        }

        // Update the player's ELO rating
        player.setSinglesRankedRating(newElo);
        playerRepository.save(player);

        // Record the ELO change in history
        // Note: In a real implementation, you would pass the actual gameId
        eloHistoryService.recordEloChange(
                player.getPlayerId(),
                game.getGameId(), // gameId - would come from the actual game object
                GameType.SINGLES_RANKED,
                previousElo,
                newElo
        );
    }

    @Override
    public void updatePlayerDoublesRankedElo(Player player, Double eloUpdate, boolean isWin, Game game) {
        // Capture the previous ELO before updating
        int previousElo = player.getDoublesRankedRating();
        int newElo = (int) Math.round(eloUpdate);

        // Update win/loss record
        if (isWin) {
            player.setDoublesRankedWins(player.getDoublesRankedWins() + 1);
        }
        else {
            player.setDoublesRankedLoses(player.getDoublesRankedLoses() + 1);
        }

        // Update the player's ELO rating
        player.setDoublesRankedRating(newElo);
        playerRepository.save(player);

        // Record the ELO change in history
        eloHistoryService.recordEloChange(
                player.getPlayerId(),
                game.getGameId(),
                GameType.DOUBLES_RANKED,
                previousElo,
                newElo
        );
    }

    @Override
    public void updatePlayerDoublesNormalElo(Player player, Double eloUpdate, boolean isWin, Game game) {
        // Capture the previous ELO before updating
        int previousElo = player.getDoublesNormalRating();
        int newElo = (int) Math.round(eloUpdate);

        // Update win/loss record
        if (isWin) {
            player.setDoublesNormalWins(player.getDoublesNormalWins() + 1);
        }
        else {
            player.setDoublesNormalLoses(player.getDoublesNormalLoses() + 1);
        }

        // Update the player's ELO rating
        player.setDoublesNormalRating(newElo);
        playerRepository.save(player);

        // Record the ELO change in history
        eloHistoryService.recordEloChange(
                player.getPlayerId(),
                game.getGameId(),
                GameType.DOUBLES_NORMAL,
                previousElo,
                newElo
        );
    }

    @Override
    public void updatePlayerSinglesNormalElo(Player player, double eloUpdate, Game game) {
        // Capture the previous ELO before updating
        int previousElo = player.getSinglesNormalRating();
        int newElo = (int) Math.round(eloUpdate);

        // Update win/loss record
        if (previousElo < newElo) {
            player.setSinglesNormalWins(player.getSinglesNormalWins() + 1);
        }
        else {
            player.setSinglesNormalLoses(player.getSinglesNormalLoses() + 1);
        }

        // Update the player's ELO rating
        player.setSinglesNormalRating(newElo);
        playerRepository.save(player);

        // Record the ELO change in history
        eloHistoryService.recordEloChange(
                player.getPlayerId(),
                game.getGameId(),
                GameType.SINGLES_NORMAL,
                previousElo,
                newElo
        );
    }

    @Override
    public void updatePassword(Player player, String newPassword) {
        player.setPassword(passwordEncoder.encode(newPassword));
        playerRepository.save(player);
    }

    @Override
    public List<String> findAllPlayerUsernames() {
        return playerRepository.findAllUsernames();
    }

    @Override
    public void savePlayer(Player player) {
        playerRepository.save(player);
    }

    @Override
    public void deletePlayer(UUID id) {
        log.info("Deleting player with ID: {}", id);

        if (id == null) {
            log.warn("Attempted to delete player with null or empty ID");
            throw new IllegalArgumentException("Player ID cannot be null or empty");
        }

        try {
            // Make sure the player exists
            Player player = playerRepository.findById(id).orElse(null);
            if (player == null) {
                log.warn("Attempted to delete non-existent player: {}", id);
                throw new ResourceNotFoundException("Player not found with id: " + id);
            }

            // Before deleting, you might want to handle references to this player
            // For example:
            // 1. Delete or anonymize games/matches associated with this player
            // 2. Or throw an exception if this player has games/matches

            // For this implementation, we'll assume the deletion is allowed
            playerRepository.deleteById(id);
            log.info("Successfully deleted player with ID: {}", id);
        }
        catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", id, e);
            throw new BadRequestException("Invalid player ID format");
        }
        catch (ResourceNotFoundException e) {
            // Re-throw this exception so it can be handled correctly
            throw e;
        }
        catch (Exception e) {
            log.error("Error deleting player with ID: {}", id, e);
            throw new RuntimeException("Failed to delete player", e);
        }
    }

    @Override
    public Player authenticatePlayer(String username, String password) {
        log.debug("Authenticating player: {}", username);

        if (username == null || username.trim().isEmpty()) {
            log.warn("Attempted to authenticate with null or empty username");
            throw new BadRequestException("Username cannot be null or empty");
        }

        if (password == null || password.trim().isEmpty()) {
            log.warn("Attempted to authenticate with null or empty password");
            throw new BadRequestException("Password cannot be null or empty");
        }

        try {
            // Find player by username
            Player player = findPlayerByUsername(username);
            if (player == null) {
                log.warn("Authentication failed: player not found with username: {}", username);
                return null;
            }
            
            // Check if email verification is required and user is not verified
            if (domainRestrictionConfig.isEmailVerificationRequired() && !player.isEmailVerified()) {
                log.warn("Authentication failed: email not verified for user: {}", username);
                throw new BadRequestException("Email not verified. Please check your email for verification instructions.");
            }

            // Verify password
            if (passwordEncoder.matches(password, player.getPassword())) {
                log.info("Authentication successful for player: {}", username);
                return player;
            }
            else {
                log.warn("Authentication failed: incorrect password for username: {}", username);
                return null;
            }
        }
        catch (BadRequestException e) {
            // Rethrow this exception to preserve the message
            throw e;
        }
        catch (Exception e) {
            log.error("Error during authentication for username: {}", username, e);
            throw new RuntimeException("Authentication failed", e);
        }
    }

    @Override
    @Transactional
    public Player updatePlayer(Player player) {
        log.debug("Updating player: {}", player.getUsername());

        if (player.getPlayerId() == null) {
            log.warn("Attempted to update player without ID");
            throw new IllegalArgumentException("Player ID cannot be null");
        }

        try {
            // Check if player exists
            Player existingPlayer = playerRepository.findById(player.getPlayerId())
                                                    .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + player.getPlayerId()));

            // Check if username is being changed and if the new username is already taken
            if (!existingPlayer.getUsername().equals(player.getUsername())
                    && playerRepository.findByUsername(player.getUsername()) != null) {
                throw new BadRequestException("Username already taken: " + player.getUsername());
            }

            // Check if email is being changed and if the new email is already taken
            if (!existingPlayer.getEmail().equals(player.getEmail())
                    && playerRepository.findByEmail(player.getEmail()) != null) {
                throw new BadRequestException("Email already taken: " + player.getEmail());
            }

            // Update fields
            existingPlayer.setFirstName(player.getFirstName());
            existingPlayer.setLastName(player.getLastName());
            existingPlayer.setEmail(player.getEmail());
            existingPlayer.setUsername(player.getUsername());
            existingPlayer.setBirthday(player.getBirthday());
            existingPlayer.setProfileImage(player.getProfileImage());

            // Only update password if provided
            if (player.getPassword() != null && !player.getPassword().isEmpty()) {
                // Check if matching password is provided and matches
                if (player.getMatchingPassword() == null ||
                        !player.getPassword().equals(player.getMatchingPassword())) {
                    throw new BadRequestException("Passwords do not match");
                }

                // Encode and set the new password
                existingPlayer.setPassword(passwordEncoder.encode(player.getPassword()));
            }

            // Update style ratings if provided
            if (player.getStyleRatings() != null && !player.getStyleRatings().isEmpty()) {
                for (PlayerStyleRating rating : player.getStyleRatings()) {
                    existingPlayer.setStyleRating(rating.getStyleType(), rating.getRating());
                }
            }

            // Don't allow direct updates to ratings and statistics
            // Those should be updated through game/match processing

            // Save the updated player
            Player updatedPlayer = playerRepository.save(existingPlayer);
            log.info("Successfully updated player: {}", updatedPlayer.getUsername());
            return updatedPlayer;
        }
        catch (ResourceNotFoundException | BadRequestException e) {
            // Re-throw these exceptions so they can be handled correctly
            throw e;
        }
        catch (Exception e) {
            log.error("Error updating player: {}", player.getUsername(), e);
            throw new RuntimeException("Failed to update player", e);
        }
    }

    @Override
    @Transactional
    public Player updatePlayerStyleRatings(UUID playerId, Map<PlayerStyle, Integer> styleRatings) {
        log.debug("Updating style ratings for player: {}", playerId);

        Player player = playerRepository.findById(playerId)
                                        .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + playerId));

        for (Map.Entry<PlayerStyle, Integer> entry : styleRatings.entrySet()) {
            player.setStyleRating(entry.getKey(), entry.getValue());
        }

        return playerRepository.save(player);
    }

    @Override
    public Map<PlayerStyle, Integer> getPlayerStyleRatings(UUID playerId) {
        log.debug("Getting style ratings for player: {}", playerId);

        Player player = playerRepository.findById(playerId)
                                        .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + playerId));

        Map<PlayerStyle, Integer> ratings = new HashMap<>();
        for (PlayerStyle style : PlayerStyle.values()) {
            ratings.put(style, player.getStyleRating(style));
        }

        return ratings;
    }

    @Override
    public List<PlayerStyleAverageDTO> getAverageStyleRatings() {
        log.debug("Calculating average style ratings among all players");

        List<Player> allPlayers = playerRepository.findAll();
        Map<PlayerStyle, List<Integer>> allRatings = new HashMap<>();

        // Initialize lists for each style
        for (PlayerStyle style : PlayerStyle.values()) {
            allRatings.put(style, new ArrayList<>());
        }

        // Collect all ratings
        for (Player player : allPlayers) {
            for (PlayerStyle style : PlayerStyle.values()) {
                allRatings.get(style).add(player.getStyleRating(style));
            }
        }

        // Calculate averages
        List<PlayerStyleAverageDTO> result = new ArrayList<>();
        for (PlayerStyle style : PlayerStyle.values()) {
            List<Integer> ratings = allRatings.get(style);
            double average = ratings.isEmpty() ? 0.0 :
                    ratings.stream().mapToInt(Integer::intValue).average().orElse(0.0);

            result.add(PlayerStyleAverageDTO.builder()
                                            .style(style)
                                            .averageRating(Math.round(average * 100.0) / 100.0) // Round to 2 decimal places
                                            .build());
        }

        return result;
    }

    @Override
    public List<PlayerStyleTopDTO> getHighestStyleRatings() {
        log.debug("Finding highest style ratings among all players");

        List<Player> allPlayers = playerRepository.findAll();
        Map<PlayerStyle, PlayerStyleTopDTO> highest = new HashMap<>();

        // Initialize with default values
        for (PlayerStyle style : PlayerStyle.values()) {
            highest.put(style, PlayerStyleTopDTO.builder()
                                                .style(style)
                                                .rating(0)
                                                .build());
        }

        // Find highest ratings
        for (Player player : allPlayers) {
            for (PlayerStyle style : PlayerStyle.values()) {
                int rating = player.getStyleRating(style);
                PlayerStyleTopDTO current = highest.get(style);

                if (rating > current.getRating()) {
                    highest.put(style, PlayerStyleTopDTO.builder()
                                                        .style(style)
                                                        .rating(rating)
                                                        .playerId(player.getPlayerId())
                                                        .playerUsername(player.getUsername())
                                                        .playerFullName(player.getFullName())
                                                        .build());
                }
            }
        }

        return new ArrayList<>(highest.values());
    }

    private boolean emailExists(String email) {
        return playerRepository.findByEmail(email) != null;
    }

    private boolean usernameExists(String username) {
        return playerRepository.findByUsername(username) != null;
    }

    @Override
    public void promotePlayerToAdmin(String email) {
        Player player = playerRepository.findByEmail(email);
        if (player == null) {
            throw new ResourceNotFoundException("Player not found with email: " + email);
        }
        
        log.info("Promoting player to admin: {} {} ({})", player.getFirstName(), player.getLastName(), player.getEmail());
        player.setRole("ADMIN");
        playerRepository.save(player);
    }

    @Override
    public void demotePlayerFromAdmin(String email) {
        Player player = playerRepository.findByEmail(email);
        if (player == null) {
            throw new ResourceNotFoundException("Player not found with email: " + email);
        }
        
        log.info("Demoting player from admin: {} {} ({})", player.getFirstName(), player.getLastName(), player.getEmail());
        player.setRole("USER");
        playerRepository.save(player);
    }
}