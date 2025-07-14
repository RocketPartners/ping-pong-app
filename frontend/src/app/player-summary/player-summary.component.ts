import {Component, Input} from '@angular/core';
import {Player} from '../_models/models';
import {PlayerUtilities} from '../player/player-utilities';
import {Router} from '@angular/router';

@Component({
  selector: 'app-player-summary',
  templateUrl: './player-summary.component.html',
  styleUrls: ['./player-summary.component.scss'],
  standalone: false
})
export class PlayerSummaryComponent {
  @Input() player: Player | undefined;
  @Input() compact: boolean = false;
  @Input() showActions: boolean = true;
  @Input() clickable: boolean = true;

  constructor(private router: Router) {
  }

  // Helper method for safely checking if player styles exist
  hasPlayerStyles(): boolean {
    return !!this.player &&
      Array.isArray(this.player.playerStyles) &&
      this.player.playerStyles.length > 0;
  }

  // Helper method to safely get player styles
  getPlayerStyles(): string[] {
    return this.player?.playerStyles || [];
  }

  // Player utilities
  getPlayerInitials(): string {
    return PlayerUtilities.getPlayerInitials(this.player);
  }

  getRatingClass(rating: number): string {
    return PlayerUtilities.getRatingClass(rating);
  }

// Action methods
  navigateToProfile(): void {
    if (this.clickable && this.player && this.player.username) {
      this.router.navigate(['/player', this.player.username]);
    }
  }

  // Stats calculations
  getSinglesWinRate(): number {
    if (!this.player) return 0;

    const total = (this.player.singlesRankedWins || 0) + (this.player.singlesRankedLoses || 0);
    return total > 0 ? Math.round((this.player.singlesRankedWins || 0) / total * 100) : 0;
  }

  getDoublesWinRate(): number {
    if (!this.player) return 0;

    const total = (this.player.doublesRankedWins || 0) + (this.player.doublesRankedLoses || 0);
    return total > 0 ? Math.round((this.player.doublesRankedWins || 0) / total * 100) : 0;
  }

  getTotalGames(): number {
    return PlayerUtilities.getTotalMatches(this.player);
  }
}
