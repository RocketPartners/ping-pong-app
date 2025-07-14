import {Component, OnDestroy, OnInit} from '@angular/core';
import {Subject} from 'rxjs';
import {OverlayRef} from '@angular/cdk/overlay';
import {Game, Player} from '../../_models/models';
import {Router} from '@angular/router';

@Component({
  selector: 'app-game-detail',
  templateUrl: './game-sidebar.component.html',
  styleUrls: ['./game-sidebar.component.scss'],
  standalone: false
})
export class GameSidebarComponent implements OnInit, OnDestroy {
  // Game data
  game!: Game;
  playerData: Record<string, Player> = {};

  // Reference to the overlay (for closing)
  overlayRef!: OverlayRef;

  // Cleanup subscription
  private destroy$ = new Subject<void>();

  constructor(private router: Router) {
  }

  ngOnInit(): void {
    // Initialization if needed
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Close the side drawer
   */
  close(): void {
    this.overlayRef.dispose();
  }

  /**
   * Get player name by ID
   */
  getPlayerName(playerId: string): string {
    const player = this.playerData[playerId];
    return player ? `${player.firstName} ${player.lastName}` : 'Unknown Player';
  }

  /**
   * Get player initials for avatar
   */
  getPlayerInitials(playerId: string): string {
    const player = this.playerData[playerId];
    if (!player) return '??';
    return `${player.firstName.charAt(0)}${player.lastName.charAt(0)}`;
  }

  /**
   * Get team names as a comma-separated string
   */
  getTeamNames(playerIds: string[]): string {
    return playerIds.map(id => this.getPlayerName(id)).join(', ');
  }

  /**
   * Get game type as a string
   */
  getGameType(): string {
    const gameType = this.game.singlesGame ? 'Singles' : 'Doubles';
    const gameMode = this.game.ratedGame ? 'Ranked' : 'Normal';
    return `${gameType} ${gameMode}`;
  }

  /**
   * Format date
   */
  formatDate(date: Date | undefined): string {
    if (!date) return 'Unknown Date';
    return new Date(date).toLocaleString();
  }

  /**
   * Get winner information in verbose format
   */
  getWinnerInfo(): string {
    if (this.game.singlesGame) {
      if (this.game.challengerWin) {
        return `Winner: ${this.getPlayerName(this.game.challengerId)}`;
      } else {
        return `Winner: ${this.getPlayerName(this.game.opponentId)}`;
      }
    } else {
      if (this.game.challengerTeamWin) {
        return `Winner: Challenger Team (${this.getTeamNames(this.game.challengerTeam)})`;
      } else {
        return `Winner: Opponent Team (${this.getTeamNames(this.game.opponentTeam)})`;
      }
    }
  }

  /**
   * Get winner information in condensed format
   */
  getWinnerFormatted(): string {
    if (this.game.singlesGame) {
      return this.game.challengerWin ?
        this.getPlayerName(this.game.challengerId) :
        this.getPlayerName(this.game.opponentId);
    } else {
      return this.game.challengerTeamWin ? 'Challenger Team' : 'Opponent Team';
    }
  }

  /**
   * Calculate score difference between teams
   */
  getScoreDifference(): string {
    const diff = Math.abs(this.game.challengerTeamScore - this.game.opponentTeamScore);
    const winner = this.game.challengerTeamWin ? 'Challenger' : 'Opponent';
    return `${diff} points (${winner})`;
  }

  /**
   * Check if player ratings are available
   */
  hasPlayerRatings(): boolean {
    // Check if we have any player data to display stats
    return Object.keys(this.playerData).length > 0;
  }

  /**
   * Get player rating based on game type
   */
  getPlayerRating(playerId: string, isSingles: boolean = true): number {
    const player = this.playerData[playerId];
    if (!player) return 0;

    if (isSingles) {
      return this.game.ratedGame ? player.singlesRankedRating : player.singlesNormalRating;
    } else {
      return this.game.ratedGame ? player.doublesRankedRating : player.doublesNormalRating;
    }
  }

  /**
   * Get player wins based on game type
   */
  getPlayerWins(playerId: string, isSingles: boolean = true): number {
    const player = this.playerData[playerId];
    if (!player) return 0;

    if (isSingles) {
      return this.game.ratedGame ? player.singlesRankedWins : player.singlesNormalWins;
    } else {
      return this.game.ratedGame ? player.doublesRankedWins : player.doublesNormalWins;
    }
  }

  /**
   * Get player losses based on game type
   */
  getPlayerLosses(playerId: string, isSingles: boolean = true): number {
    const player = this.playerData[playerId];
    if (!player) return 0;

    if (isSingles) {
      return this.game.ratedGame ? player.singlesRankedLoses : player.singlesNormalLoses;
    } else {
      return this.game.ratedGame ? player.doublesRankedLoses : player.doublesNormalLoses;
    }
  }

  /**
   * Navigate to player profile
   */
  navigateToPlayer(playerId: string): void {
    this.close();
    this.router.navigate(['/players', playerId]);
  }

  /**
   * View full match details
   */
  viewMatchDetails(): void {
    this.close();
    this.router.navigate(['/games', this.game.gameId]);
  }
  
  /**
   * Track by function for ngFor performance optimization
   */
  trackByPlayerId(index: number, playerId: string): string {
    return playerId;
  }
}
