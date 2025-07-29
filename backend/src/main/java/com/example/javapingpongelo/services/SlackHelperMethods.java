package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.repositories.GameRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class SlackHelperMethods {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private SlackService slackService;

    @Autowired
    private IPlayerService playerService;

    public void checkAndNotifyWinStreaks(Game game) {
        try {
            if (game.isSinglesGame()) {
                Player winner = game.isChallengerWin() ? 
                    playerService.findPlayerById(game.getChallengerId()) : 
                    playerService.findPlayerById(game.getOpponentId());
                
                if (winner != null) {
                    int streak = calculateWinStreak(winner, game.isRatedGame());
                    if (streak >= 3) {
                        slackService.postWinStreak(winner, streak);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error checking win streaks for game: {}", game.getGameId(), e);
        }
    }
    
    private int calculateWinStreak(Player player, boolean isRanked) {
        try {
            List<Game> recentGames = gameRepository.findByPlayerId(player.getPlayerId()).stream()
                .filter(g -> g.isRatedGame() == isRanked && g.isSinglesGame())
                .sorted((g1, g2) -> g2.getDatePlayed().compareTo(g1.getDatePlayed()))
                .limit(20)
                .collect(java.util.stream.Collectors.toList());
            
            int streak = 0;
            for (Game game : recentGames) {
                boolean playerWon = (game.getChallengerId().equals(player.getPlayerId()) && game.isChallengerWin()) ||
                                  (game.getOpponentId().equals(player.getPlayerId()) && game.isOpponentWin());
                
                if (playerWon) {
                    streak++;
                } else {
                    break;
                }
            }
            
            return streak;
        } catch (Exception e) {
            log.error("Error calculating win streak for player: {}", player.getPlayerId(), e);
            return 0;
        }
    }
}