import {getStyleLevelLabel, PLAYER_STYLE_NAMES, StyleRating,} from '../_models/player-style';
import {Player} from "../_models/models";

/**
 * Utility functions for working with Player objects
 * Helps maintain type safety and reduces code duplication across components
 */
export class PlayerUtilities {
  /**
   * Maps player style enum values to readable names
   */
  static readonly PLAYER_STYLE_NAMES = PLAYER_STYLE_NAMES;

  /**
   * Rating thresholds for different skill levels
   */
  static readonly RATING = {
    BEGINNER: 0,
    NOVICE: 1000,
    INTERMEDIATE: 1500,
    ADVANCED: 1800,
    EXPERT: 2000
  };

  /**
   * Win rate thresholds for performance levels
   */
  static readonly WIN_RATE = {
    STRUGGLING: 0,
    POOR: 30,
    AVERAGE: 45,
    GOOD: 55,
    EXCELLENT: 70
  };

  /**
   * Gets player initials in a safe way, handling undefined values
   */
  static getPlayerInitials(player?: Player): string {
    if (!player) return '??';

    const firstInitial = player.firstName ? player.firstName.charAt(0) : '?';
    const lastInitial = player.lastName ? player.lastName.charAt(0) : '?';

    return `${firstInitial}${lastInitial}`;
  }

  /**
   * Gets full player name safely
   */
  static getPlayerFullName(player?: Player): string {
    if (!player) return '';
    return `${player.firstName || ''} ${player.lastName || ''}`.trim();
  }

  /**
   * Calculates total matches played by the player
   */
  static getTotalMatches(player?: Player): number {
    if (!player) return 0;

    return (player.singlesRankedWins || 0) +
      (player.singlesRankedLoses || 0) +
      (player.doublesRankedWins || 0) +
      (player.doublesRankedLoses || 0) +
      (player.singlesNormalWins || 0) +
      (player.singlesNormalLoses || 0) +
      (player.doublesNormalWins || 0) +
      (player.doublesNormalLoses || 0);
  }

  /**
   * Calculates total wins across all game types
   */
  static getTotalWins(player?: Player): number {
    if (!player) return 0;

    return (player.singlesRankedWins || 0) +
      (player.doublesRankedWins || 0) +
      (player.singlesNormalWins || 0) +
      (player.doublesNormalWins || 0);
  }

  /**
   * Calculates total losses across all game types
   */
  static getTotalLosses(player?: Player): number {
    if (!player) return 0;

    return (player.singlesRankedLoses || 0) +
      (player.doublesRankedLoses || 0) +
      (player.singlesNormalLoses || 0) +
      (player.doublesNormalLoses || 0);
  }

  /**
   * Calculates overall win rate across all game types
   */
  static getOverallWinRate(player?: Player): number {
    if (!player) return 0;

    const totalWins = this.getTotalWins(player);
    const totalMatches = this.getTotalMatches(player);

    return totalMatches > 0
      ? Math.round((totalWins / totalMatches) * 100)
      : 0;
  }

  /**
   * Gets a CSS class based on player rating
   */
  static getRatingClass(rating: number): string {
    if (rating >= this.RATING.EXPERT) return 'rating-expert';
    if (rating >= this.RATING.ADVANCED) return 'rating-advanced';
    if (rating >= this.RATING.INTERMEDIATE) return 'rating-intermediate';
    if (rating >= this.RATING.NOVICE) return 'rating-novice';
    return 'rating-beginner';
  }

  /**
   * Gets a human-readable rating label based on player rating
   */
  static getRatingLabel(rating: number): string {
    if (rating >= this.RATING.EXPERT) return 'Expert';
    if (rating >= this.RATING.ADVANCED) return 'Advanced';
    if (rating >= this.RATING.INTERMEDIATE) return 'Intermediate';
    if (rating >= this.RATING.NOVICE) return 'Novice';
    return 'Beginner';
  }

  /**
   * Gets a CSS class based on win rate
   */
  static getWinRateClass(winRate: number): string {
    if (winRate >= this.WIN_RATE.EXCELLENT) return 'win-rate-excellent';
    if (winRate >= this.WIN_RATE.GOOD) return 'win-rate-good';
    if (winRate >= this.WIN_RATE.AVERAGE) return 'win-rate-average';
    if (winRate >= this.WIN_RATE.POOR) return 'win-rate-poor';
    return 'win-rate-struggling';
  }


  /**
   * Gets a style level label based on rating value
   * Delegates to the central function in player-style.ts
   */
  static getStyleLevelLabel(rating: number): string {
    return getStyleLevelLabel(rating);
  }

  /**
   * Gets the top N style ratings for a player
   */
  static getTopStyles(limit: number, player?: Player,): StyleRating[] {
    if (!player || !player.styleRatings) return [];

    return [...player.styleRatings]
      .sort((a, b) => b.rating - a.rating)
      .slice(0, limit);
  }

  /**
   * Gets a rating for a specific style
   */
  static getStyleRating(player: Player, styleType: string): number {
    if (!player || !player.styleRatings) return 0;

    const styleRating = player.styleRatings.find(s => s.styleType === styleType);
    return styleRating ? styleRating.rating : 0;
  }

  static getProfileIconName(profileImage: string | undefined): string {
    if (profileImage) {
      const parts = profileImage.split(':');
      return parts[0];
    }
    return 'person';
  }

  static getProfileIconColor(profileImage: string | undefined): string {
    if (profileImage) {
      const parts = profileImage.split(':');
      return parts[1];
    }
    return '#757575';
  }

  public static getLowestStyles(count: number, player?: Player | null): StyleRating[] {
    if (!player?.styleRatings?.length) return [];

    return [...player.styleRatings]
      .sort((a, b) => a.rating - b.rating)
      .slice(0, count);
  }
}
