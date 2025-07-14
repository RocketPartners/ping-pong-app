package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.utils.EloCurveConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service for Elo rating calculations.
 * This service handles all rating calculations for both singles and doubles matches.
 * Includes support for rating curves to boost lower-rated players and protect floor ratings.
 */
@Service
@Slf4j
public class EloService {
    // Constants for Elo calculations
    public final double K_FACTOR = 50;

    public final int SINGLES_EXPONENT_DENOMINATOR = 400;

    public final int DOUBLES_EXPONENT_DENOMINATOR = 500;

    public final int EXPONENT_BASE = 10;


    @Autowired
    EloCurveConfig curveConfig;

    /**
     * Calculates the expected probability of player A winning against player B in doubles.
     *
     * @param playerARating The Elo rating of player A
     * @param playerBRating The Elo rating of player B
     * @return The probability (0-1) that player A will win
     */
    public double doublesPlayerProbability(double playerARating, double playerBRating) {
        double exponent = (playerBRating - playerARating) / DOUBLES_EXPONENT_DENOMINATOR;
        return 1 / (1 + Math.pow(EXPONENT_BASE, exponent));
    }

    /**
     * Calculate the new Elo rating for a player who won a singles ranked game.
     *
     * @param winningPlayer      The player who won
     * @param losingPlayer       The player who lost
     * @param winningPlayerScore The winner's score
     * @param losingPlayerScore  The loser's score
     * @return The new Elo rating for the winning player
     */
    public double newSinglesEloRankedRatingForWinner(Player winningPlayer, Player losingPlayer,
                                                     int winningPlayerScore, int losingPlayerScore) {
        int gamesPlayed = winningPlayer.getSinglesRankedWins() + winningPlayer.getSinglesRankedLoses();
        double kFactor = calculateRelativeKFactor(gamesPlayed);
        double pointFactor = calculatePointFactor(winningPlayerScore, losingPlayerScore);
        double expectedProbability = expectedSinglesPlayerProbability(
                winningPlayer.getSinglesRankedRating(), losingPlayer.getSinglesRankedRating());

        double ratingChange = kFactor * pointFactor * (1 - expectedProbability);

        // Apply the curve to the rating change
        ratingChange = applyRatingCurve(winningPlayer.getSinglesRankedRating(), ratingChange);

        double newRating = winningPlayer.getSinglesRankedRating() + ratingChange;

        log.debug("Singles ranked win: Player {} new rating {}, curved change {}",
                  winningPlayer.getUsername(), newRating, ratingChange);

        return newRating;
    }

    /**
     * Calculate a relative K-factor based on player experience.
     * <p>
     * This formula is designed to have a greater impact on the rating for new players
     * while providing stability and less rating fluctuation for experienced players.
     * Specifically, after playing 300 games, a player's rating becomes more representative
     * of their skill level.
     *
     * @param gamesPlayed The number of games the player has played
     * @return The adjusted K-factor
     */
    public double calculateRelativeKFactor(int gamesPlayed) {
        return (K_FACTOR / (1 + ((double) gamesPlayed / 300)));
    }

    /**
     * Calculate a point factor based on the score difference.
     * <p>
     * To consider the points scored by each team, this factor multiplies the K parameter
     * of each player and is based on the absolute difference in points between the two teams.
     * The impact of a match must be greater when a team wins by a large margin, i.e.,
     * an overwhelming victory.
     *
     * @param challengerScore The challenger's score
     * @param opponentScore   The opponent's score
     * @return The point factor multiplier
     */
    public double calculatePointFactor(int challengerScore, int opponentScore) {
        return (2 + Math.pow(Math.log10(Math.abs(challengerScore - opponentScore) + 1), 3));
    }

    /**
     * Calculates the expected probability of player A winning against player B in singles.
     *
     * @param playerARating The Elo rating of player A
     * @param playerBRating The Elo rating of player B
     * @return The probability (0-1) that player A will win
     */
    public double expectedSinglesPlayerProbability(double playerARating, double playerBRating) {
        double exponent = (playerBRating - playerARating) / SINGLES_EXPONENT_DENOMINATOR;
        return 1 / (1 + Math.pow(EXPONENT_BASE, exponent));
    }

    /**
     * Applies rating curve to adjust Elo changes based on player's current rating
     *
     * @param currentRating The player's current rating
     * @param ratingChange  The calculated rating change (positive for gain, negative for loss)
     * @return The adjusted rating change
     */
    public double applyRatingCurve(int currentRating, double ratingChange) {
        if (!curveConfig.isEnabled()) {
            return ratingChange;
        }

        // For rating gains (positive change)
        if (ratingChange > 0 && currentRating < curveConfig.getUpperThreshold()) {
            // Calculate a smoothly decreasing boost as rating approaches threshold
            double boostFactor = curveConfig.getGainBoostFactor() *
                    (1.0 - Math.pow((double) currentRating / curveConfig.getUpperThreshold(), 2));

            return ratingChange * (1.0 + boostFactor);
        }
        // For rating losses (negative change)
        else if (ratingChange < 0 && currentRating < curveConfig.getLowerThreshold()) {
            // Calculate increasing protection as rating drops
            double protectionFactor = (1.0 - curveConfig.getLossReductionFactor()) *
                    (1.0 - (double) currentRating / curveConfig.getLowerThreshold());

            // Make the negative change less negative
            return ratingChange * (1.0 - protectionFactor);
        }

        return ratingChange;
    }

    /**
     * Calculate the new Elo rating for a player who lost a singles ranked game.
     *
     * @param losingPlayer       The player who lost
     * @param winningPlayer      The player who won
     * @param losingPlayerScore  The loser's score
     * @param winningPlayerScore The winner's score
     * @return The new Elo rating for the losing player
     */
    public double newSinglesRankedEloRatingForLoser(Player losingPlayer, Player winningPlayer,
                                                    int losingPlayerScore, int winningPlayerScore) {
        int gamesPlayed = losingPlayer.getSinglesRankedWins() + losingPlayer.getSinglesRankedLoses();
        double kFactor = calculateRelativeKFactor(gamesPlayed);
        double pointFactor = calculatePointFactor(losingPlayerScore, winningPlayerScore);
        double expectedProbability = expectedSinglesPlayerProbability(
                losingPlayer.getSinglesRankedRating(), winningPlayer.getSinglesRankedRating());

        double ratingChange = kFactor * pointFactor * (0 - expectedProbability);

        // Apply the curve to the rating change
        ratingChange = applyRatingCurve(losingPlayer.getSinglesRankedRating(), ratingChange);

        double newRating = losingPlayer.getSinglesRankedRating() + ratingChange;

        log.debug("Singles ranked loss: Player {} new rating {}, curved change {}",
                  losingPlayer.getUsername(), newRating, ratingChange);

        return newRating;
    }

    /**
     * Calculate the new Elo rating for a player who won a singles normal game.
     *
     * @param winningPlayer      The player who won
     * @param losingPlayer       The player who lost
     * @param winningPlayerScore The winner's score
     * @param losingPlayerScore  The loser's score
     * @return The new Elo rating for the winning player
     */
    public double newSinglesEloNormalRatingForWinner(Player winningPlayer, Player losingPlayer,
                                                     int winningPlayerScore, int losingPlayerScore) {
        int gamesPlayed = winningPlayer.getSinglesNormalWins() + winningPlayer.getSinglesNormalLoses();
        double kFactor = calculateRelativeKFactor(gamesPlayed);
        double pointFactor = calculatePointFactor(winningPlayerScore, losingPlayerScore);
        double expectedProbability = expectedSinglesPlayerProbability(
                winningPlayer.getSinglesNormalRating(), losingPlayer.getSinglesNormalRating());

        double ratingChange = kFactor * pointFactor * (1 - expectedProbability);

        // Apply the curve to the rating change
        ratingChange = applyRatingCurve(winningPlayer.getSinglesNormalRating(), ratingChange);

        double newRating = winningPlayer.getSinglesNormalRating() + ratingChange;

        log.debug("Singles normal win: Player {} new rating {}, curved change {}",
                  winningPlayer.getUsername(), newRating, ratingChange);

        return newRating;
    }

    /**
     * Calculate the new Elo rating for a player who lost a singles normal game.
     *
     * @param losingPlayer       The player who lost
     * @param winningPlayer      The player who won
     * @param losingPlayerScore  The loser's score
     * @param winningPlayerScore The winner's score
     * @return The new Elo rating for the losing player
     */
    public double newSinglesNormalEloRatingForLoser(Player losingPlayer, Player winningPlayer,
                                                    int losingPlayerScore, int winningPlayerScore) {
        int gamesPlayed = losingPlayer.getSinglesNormalWins() + losingPlayer.getSinglesNormalLoses();
        double kFactor = calculateRelativeKFactor(gamesPlayed);
        double pointFactor = calculatePointFactor(losingPlayerScore, winningPlayerScore);
        double expectedProbability = expectedSinglesPlayerProbability(
                losingPlayer.getSinglesNormalRating(), winningPlayer.getSinglesNormalRating());

        double ratingChange = kFactor * pointFactor * (0 - expectedProbability);

        // Apply the curve to the rating change
        ratingChange = applyRatingCurve(losingPlayer.getSinglesNormalRating(), ratingChange);

        double newRating = losingPlayer.getSinglesNormalRating() + ratingChange;

        log.debug("Singles normal loss: Player {} new rating {}, curved change {}",
                  losingPlayer.getUsername(), newRating, ratingChange);

        return newRating;
    }

    /**
     * Calculate the new Elo ratings for all players in a doubles ranked game.
     *
     * @param game    The game data
     * @param playerA First winning player
     * @param playerB Second winning player
     * @param playerC First losing player
     * @param playerD Second losing player
     * @return List of new ratings for all players in order [playerA, playerB, playerC, playerD]
     */
    public List<Double> newDoublesRankedEloRatingForEachPlayer(Game game,
                                                               Player playerA, Player playerB,
                                                               Player playerC, Player playerD) {
        // Calculate K-factors based on player experience
        double kFactorA = calculateRelativeKFactor(playerA.getDoublesRankedWins() + playerA.getDoublesRankedLoses());
        double kFactorB = calculateRelativeKFactor(playerB.getDoublesRankedWins() + playerB.getDoublesRankedLoses());
        double kFactorC = calculateRelativeKFactor(playerC.getDoublesRankedWins() + playerC.getDoublesRankedLoses());
        double kFactorD = calculateRelativeKFactor(playerD.getDoublesRankedWins() + playerD.getDoublesRankedLoses());

        // Point factor based on score difference
        double pointFactor = calculatePointFactor(game.getChallengerTeamScore(), game.getOpponentTeamScore());

        // Calculate team probabilities
        double winningTeamProbability = expectedDoublesTeamProbability(
                playerA.getDoublesRankedRating(), playerB.getDoublesRankedRating(),
                playerC.getDoublesRankedRating(), playerD.getDoublesRankedRating()
        );

        double losingTeamProbability = expectedDoublesTeamProbability(
                playerC.getDoublesRankedRating(), playerD.getDoublesRankedRating(),
                playerA.getDoublesRankedRating(), playerB.getDoublesRankedRating()
        );

        // Calculate individual player probabilities
        double playerAProbability = expectedDoublesPlayerProbability(
                playerA.getDoublesRankedRating(), playerC.getDoublesRankedRating(), playerD.getDoublesRankedRating()
        );

        double playerBProbability = expectedDoublesPlayerProbability(
                playerB.getDoublesRankedRating(), playerC.getDoublesRankedRating(), playerD.getDoublesRankedRating()
        );

        double playerCProbability = expectedDoublesPlayerProbability(
                playerC.getDoublesRankedRating(), playerA.getDoublesRankedRating(), playerB.getDoublesRankedRating()
        );

        double playerDProbability = expectedDoublesPlayerProbability(
                playerD.getDoublesRankedRating(), playerA.getDoublesRankedRating(), playerB.getDoublesRankedRating()
        );

        // Calculate rating changes
        double ratingChangeA = kFactorA * pointFactor * (1 - (winningTeamProbability - playerAProbability));
        double ratingChangeB = kFactorB * pointFactor * (1 - (winningTeamProbability - playerBProbability));
        double ratingChangeC = kFactorC * pointFactor * (0 - (losingTeamProbability - playerCProbability));
        double ratingChangeD = kFactorD * pointFactor * (0 - (losingTeamProbability - playerDProbability));

        // Apply curves to all rating changes
        ratingChangeA = applyRatingCurve(playerA.getDoublesRankedRating(), ratingChangeA);
        ratingChangeB = applyRatingCurve(playerB.getDoublesRankedRating(), ratingChangeB);
        ratingChangeC = applyRatingCurve(playerC.getDoublesRankedRating(), ratingChangeC);
        ratingChangeD = applyRatingCurve(playerD.getDoublesRankedRating(), ratingChangeD);

        // Calculate new ratings
        double newRatingA = playerA.getDoublesRankedRating() + ratingChangeA;
        double newRatingB = playerB.getDoublesRankedRating() + ratingChangeB;
        double newRatingC = playerC.getDoublesRankedRating() + ratingChangeC;
        double newRatingD = playerD.getDoublesRankedRating() + ratingChangeD;

        log.debug("Doubles ranked: New ratings A:{}, B:{}, C:{}, D:{} with curves applied",
                  newRatingA, newRatingB, newRatingC, newRatingD);

        return new ArrayList<>(Arrays.asList(newRatingA, newRatingB, newRatingC, newRatingD));
    }

    /**
     * Calculate the new Elo ratings for all players in a doubles normal game.
     *
     * @param game    The game data
     * @param playerA First winning player
     * @param playerB Second winning player
     * @param playerC First losing player
     * @param playerD Second losing player
     * @return List of new ratings for all players in order [playerA, playerB, playerC, playerD]
     */
    public List<Double> newDoublesNormalEloRatingForEachPlayer(Game game,
                                                               Player playerA, Player playerB,
                                                               Player playerC, Player playerD) {
        // Calculate K-factors based on player experience
        double kFactorA = calculateRelativeKFactor(playerA.getDoublesNormalWins() + playerA.getDoublesNormalLoses());
        double kFactorB = calculateRelativeKFactor(playerB.getDoublesNormalWins() + playerB.getDoublesNormalLoses());
        double kFactorC = calculateRelativeKFactor(playerC.getDoublesNormalWins() + playerC.getDoublesNormalLoses());
        double kFactorD = calculateRelativeKFactor(playerD.getDoublesNormalWins() + playerD.getDoublesNormalLoses());

        // Point factor based on score difference
        double pointFactor = calculatePointFactor(game.getChallengerTeamScore(), game.getOpponentTeamScore());

        // Calculate team probabilities
        double winningTeamProbability = expectedDoublesTeamProbability(
                playerA.getDoublesNormalRating(), playerB.getDoublesNormalRating(),
                playerC.getDoublesNormalRating(), playerD.getDoublesNormalRating()
        );

        double losingTeamProbability = expectedDoublesTeamProbability(
                playerC.getDoublesNormalRating(), playerD.getDoublesNormalRating(),
                playerA.getDoublesNormalRating(), playerB.getDoublesNormalRating()
        );

        // Calculate individual player probabilities
        double playerAProbability = expectedDoublesPlayerProbability(
                playerA.getDoublesNormalRating(), playerC.getDoublesNormalRating(), playerD.getDoublesNormalRating()
        );

        double playerBProbability = expectedDoublesPlayerProbability(
                playerB.getDoublesNormalRating(), playerC.getDoublesNormalRating(), playerD.getDoublesNormalRating()
        );

        double playerCProbability = expectedDoublesPlayerProbability(
                playerC.getDoublesNormalRating(), playerA.getDoublesNormalRating(), playerB.getDoublesNormalRating()
        );

        double playerDProbability = expectedDoublesPlayerProbability(
                playerD.getDoublesNormalRating(), playerA.getDoublesNormalRating(), playerB.getDoublesNormalRating()
        );

        // Calculate rating changes
        double ratingChangeA = kFactorA * pointFactor * (1 - (winningTeamProbability - playerAProbability));
        double ratingChangeB = kFactorB * pointFactor * (1 - (winningTeamProbability - playerBProbability));
        double ratingChangeC = kFactorC * pointFactor * (0 - (losingTeamProbability - playerCProbability));
        double ratingChangeD = kFactorD * pointFactor * (0 - (losingTeamProbability - playerDProbability));

        // Apply curves to all rating changes
        ratingChangeA = applyRatingCurve(playerA.getDoublesNormalRating(), ratingChangeA);
        ratingChangeB = applyRatingCurve(playerB.getDoublesNormalRating(), ratingChangeB);
        ratingChangeC = applyRatingCurve(playerC.getDoublesNormalRating(), ratingChangeC);
        ratingChangeD = applyRatingCurve(playerD.getDoublesNormalRating(), ratingChangeD);

        // Calculate new ratings
        double newRatingA = playerA.getDoublesNormalRating() + ratingChangeA;
        double newRatingB = playerB.getDoublesNormalRating() + ratingChangeB;
        double newRatingC = playerC.getDoublesNormalRating() + ratingChangeC;
        double newRatingD = playerD.getDoublesNormalRating() + ratingChangeD;

        log.debug("Doubles normal: New ratings A:{}, B:{}, C:{}, D:{} with curves applied",
                  newRatingA, newRatingB, newRatingC, newRatingD);

        return new ArrayList<>(Arrays.asList(newRatingA, newRatingB, newRatingC, newRatingD));
    }

    /**
     * Calculate the expected probability of a player winning against two opponents in doubles.
     */
    public double expectedDoublesPlayerProbability(double playerARating, double playerBRating, double playerCRating) {
        return ((doublesPlayerProbability(playerARating, playerBRating) +
                doublesPlayerProbability(playerARating, playerCRating)) / 2);
    }

    /**
     * Calculate the expected probability of a doubles team winning.
     */
    public double expectedDoublesTeamProbability(double playerARating, double playerBRating,
                                                 double playerCRating, double playerDRating) {
        return ((expectedDoublesPlayerProbability(playerARating, playerCRating, playerDRating) +
                expectedDoublesPlayerProbability(playerBRating, playerCRating, playerDRating)) / 2);
    }
}