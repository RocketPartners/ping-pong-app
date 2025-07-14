import {Component, OnDestroy, OnInit} from '@angular/core';
import {Subject} from 'rxjs';
import {OverlayRef} from '@angular/cdk/overlay';
import {AchievementCategory, AchievementDTO, AchievementType} from '../../_models/achievement';

@Component({
  selector: 'app-achievement-detail',
  templateUrl: './achievement-detail.component.html',
  styleUrls: ['./achievement-detail.component.scss'],
  standalone: false
})
export class AchievementDetailComponent implements OnInit, OnDestroy {
  // Achievement data
  achievementData!: AchievementDTO;

  // Reference to the overlay (for closing)
  overlayRef!: OverlayRef;

  // Enums for template
  AchievementCategory = AchievementCategory;
  AchievementType = AchievementType;

  // Achievement state
  isAchieved: boolean = false;
  categoryClass: string = '';

  // Cleanup subscription
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.isAchieved = this.achievementData?.playerProgress?.achieved || false;
    this.setCategoryClass();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Get appropriate icon name
   */
  getIconName(): string {
    if (!this.achievementData?.achievement?.icon) {
      // Default icons based on category if no specific icon is provided
      if (!this.isAchieved) {
        return 'lock';
      }

      switch (this.achievementData?.achievement?.category) {
        case AchievementCategory.EASY:
          return 'emoji_events';
        case AchievementCategory.MEDIUM:
          return 'star';
        case AchievementCategory.HARD:
          return 'workspace_premium';
        case AchievementCategory.LEGENDARY:
          return 'military_tech';
        default:
          return 'emoji_events';
      }
    }

    // Use custom icon if provided
    return this.achievementData.achievement.icon;
  }

  /**
   * Close the side drawer
   */
  close(): void {
    this.overlayRef.dispose();
  }

  /**
   * Format criteria object into human-readable text
   */
  formatCriteria(criteria: any): string {
    if (!criteria) return 'No criteria defined';

    try {
      // Handle if criteria is already a string
      if (typeof criteria === 'string') {
        return criteria;
      }

      const criteriaObj = typeof criteria === 'string' ? JSON.parse(criteria) : criteria;

      // If we have a raw threshold in the achievement data, use that
      if (this.achievementData?.achievement && 'threshold' in this.achievementData.achievement) {
        const threshold = (this.achievementData.achievement as any).threshold;

        // Try to guess the criteria type from the achievement name/description
        const name = this.achievementData.achievement.name.toLowerCase();
        const desc = this.achievementData.achievement.description.toLowerCase();

        if (name.includes('win') || desc.includes('win')) {
          return `Win ${threshold} ${threshold === 1 ? 'game' : 'games'}`;
        }

        if (name.includes('play') || desc.includes('play')) {
          return `Play ${threshold} ${threshold === 1 ? 'game' : 'games'}`;
        }

        if (name.includes('rating') || desc.includes('rating')) {
          return `Reach a rating of ${threshold}`;
        }

        if (name.includes('streak') || desc.includes('streak') ||
          name.includes('consecutive') || desc.includes('consecutive')) {
          return `Win ${threshold} games in a row`;
        }

        // Generic fallback
        return `Complete ${threshold} ${threshold === 1 ? 'time' : 'times'}`;
      }

      // Standard criteria object handling
      switch (criteriaObj.type) {
        case 'WIN_COUNT':
          return `Win ${criteriaObj.threshold} ${criteriaObj.gameType || 'games'}`;

        case 'GAME_COUNT':
          return `Play ${criteriaObj.threshold} ${criteriaObj.gameType || 'games'}`;

        case 'WIN_STREAK':
          return `Win ${criteriaObj.threshold} games in a row`;

        case 'RATING_THRESHOLD':
          return `Reach a rating of ${criteriaObj.threshold} in ${criteriaObj.gameType || 'any game type'}`;

        case 'WIN_RATE':
          return `Maintain a ${criteriaObj.threshold}% win rate over at least ${criteriaObj.secondaryValue || 10} games`;

        default:
          // If we can't determine the type, just show the threshold
          if (criteriaObj.threshold) {
            return `Complete ${criteriaObj.threshold} ${criteriaObj.threshold === 1 ? 'time' : 'times'}`;
          }

          // Instead of returning raw JSON, provide a more readable format
          try {
            const parts = [];
            for (const key in criteriaObj) {
              if (criteriaObj.hasOwnProperty(key)) {
                parts.push(`${key}: ${criteriaObj[key]}`);
              }
            }
            return parts.join(', ');
          } catch {
            return 'Custom achievement criteria';
          }
      }
    } catch (e) {
      return 'Achievement criteria information unavailable';
    }
  }

  /**
   * Set the CSS class based on achievement category
   */
  private setCategoryClass(): void {
    if (!this.achievementData?.achievement?.category) return;

    switch (this.achievementData.achievement.category) {
      case AchievementCategory.EASY:
        this.categoryClass = 'achievement-easy';
        break;
      case AchievementCategory.MEDIUM:
        this.categoryClass = 'achievement-medium';
        break;
      case AchievementCategory.HARD:
        this.categoryClass = 'achievement-hard';
        break;
      case AchievementCategory.LEGENDARY:
        this.categoryClass = 'achievement-legendary';
        break;
      default:
        this.categoryClass = 'achievement-easy';
        break;
    }
  }
}
