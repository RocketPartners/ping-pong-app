import {Component, Input, OnInit} from '@angular/core';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {PlayerUtilities} from '../player/player-utilities';
import {Player} from '../_models/models';
import {PlayerStyle} from "../_models/player-style";

@Component({
  selector: 'app-player-card',
  templateUrl: './player-card.component.html',
  styleUrls: ['./player-card.component.scss'],
  animations: [
    trigger('flipState', [
      state('default', style({
        transform: 'rotateY(0)'
      })),
      state('flipped', style({
        transform: 'rotateY(180deg)'
      })),
      transition('default => flipped', [
        animate('0.5s')
      ]),
      transition('flipped => default', [
        animate('0.5s')
      ])
    ]),
    trigger('fadeIn', [
      transition(':enter', [
        style({opacity: 0}),
        animate('300ms ease-in', style({opacity: 1}))
      ])
    ])
  ],
  standalone: false
})
export class PlayerCardComponent implements OnInit {
  @Input() player: Player | undefined;
  @Input() showFlip: boolean = true;
  @Input() compact: boolean = false;
  @Input() showHeader: boolean = true;
  @Input() highlight: boolean = false;
  flip: string = 'default';
  // Player stats calculated from input
  singlesRating: number = 0;
  doublesRating: number = 0;
  totalMatches: number = 0;
  overallWinRate: number = 0;
  recentActivity: boolean = false;
  // Use player utilities for consistent behavior
  playerStyleNames = PlayerUtilities.PLAYER_STYLE_NAMES;
  protected readonly Math = Math;

  ngOnInit(): void {
    if (this.player) {
      this.singlesRating = this.player.singlesRankedRating;
      this.doublesRating = this.player.doublesRankedRating;

      // Calculate total matches and win rate
      this.totalMatches = PlayerUtilities.getTotalMatches(this.player);
      this.overallWinRate = PlayerUtilities.getOverallWinRate(this.player);

      // Simulate recent activity based on matches (in a real app, you'd check game dates)
      this.recentActivity = this.totalMatches > 5;
    }
  }

  /**
   * Toggles the card flip animation
   */
  toggleFlip(): void {
    if (!this.showFlip) return;
    this.flip = this.flip === 'default' ? 'flipped' : 'default';
  }

  /**
   * Gets the player's initials to display in the avatar
   */
  getPlayerInitials(): string {
    return PlayerUtilities.getPlayerInitials(this.player);
  }

  /**
   * Gets the player's full name
   */
  getPlayerFullName(): string {
    return PlayerUtilities.getPlayerFullName(this.player);
  }

  /**
   * Gets a list of player styles safely
   */
  getPlayerStyles(): PlayerStyle[] {
    return this.player?.playerStyles || [];
  }

  /**
   * Gets CSS class for rating display
   */
  getRatingClass(rating: number): string {
    return PlayerUtilities.getRatingClass(rating);
  }

  /**
   * Gets human-readable label for rating level
   */
  getRatingLabel(rating: number): string {
    return PlayerUtilities.getRatingLabel(rating);
  }

  /**
   * Gets CSS class for win rate display
   */
  getWinRateClass(winRate: number): string {
    return PlayerUtilities.getWinRateClass(winRate);
  }


  /**
   * Calculates total wins safely
   */
  getTotalWins(): number {
    return PlayerUtilities.getTotalWins(this.player);
  }

  /**
   * Calculates total losses safely
   */
  getTotalLosses(): number {
    return PlayerUtilities.getTotalLosses(this.player);
  }
}
