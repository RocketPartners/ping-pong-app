import {Component, Input} from '@angular/core';
import {PlayerUtilities} from '../../../player/player-utilities';
import {Player} from '../../../_models/models';

@Component({
  selector: 'app-profile-avatar',
  templateUrl: './profile-avatar.component.html',
  styleUrls: ['./profile-avatar.component.scss'],
  standalone: false
})
export class ProfileAvatarComponent {
  @Input() player: Player | undefined;
  @Input() size: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | 'xxl' = 'md';
  @Input() clickable: boolean = false;

  // Expose PlayerUtilities methods to template
  readonly playerUtils = PlayerUtilities;

  /**
   * Get initials from player name
   */
  getInitials(): string {
    return PlayerUtilities.getPlayerInitials(this.player);
  }

  /**
   * Check if player has a profile image
   */
  hasProfileImage(): boolean {
    return !!this.player?.profileImage;
  }

  /**
   * Get profile icon name
   */
  getProfileIconName(): string {
    if (!this.player?.profileImage) return '';
    return PlayerUtilities.getProfileIconName(this.player.profileImage);
  }

  /**
   * Get profile icon color
   */
  getProfileIconColor(): string {
    if (!this.player?.profileImage) return '';
    return PlayerUtilities.getProfileIconColor(this.player.profileImage);
  }

  /**
   * Get background color based on player rating
   * Used as fallback when no profile image is set
   */
  getRatingBackgroundColor(): string {
    if (!this.player) return '#757575'; // Default gray
    return this.getBackgroundColorByRating(this.player.singlesRankedRating);
  }

  /**
   * Map rating to a color
   */
  private getBackgroundColorByRating(rating: number): string {
    if (rating >= 2000) return '#2196f3'; // Expert - Blue
    if (rating >= 1800) return '#4caf50'; // Advanced - Green
    if (rating >= 1500) return '#ffeb3b'; // Intermediate - Yellow
    if (rating >= 1000) return '#ff9800'; // Novice - Orange
    return '#f44336'; // Beginner - Red
  }
}
