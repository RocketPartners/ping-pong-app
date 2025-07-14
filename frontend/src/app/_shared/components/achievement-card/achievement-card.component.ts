import {Component, Input, OnInit} from '@angular/core';
import {AchievementCategory, AchievementDTO} from '../../../_models/achievement';
import {AchievementDetailService} from "../../../achievement/achievement-detail/achievement-detail.service";

@Component({
  selector: 'app-achievement-card',
  templateUrl: './achievement-card.component.html',
  styleUrls: ['./achievement-card.component.scss'],
  standalone: false
})
export class AchievementCardComponent implements OnInit {
  @Input() achievementData!: AchievementDTO;
  @Input() showProgress: boolean = true;
  @Input() showDetails: boolean = true;
  @Input() size: 'small' | 'medium' | 'large' = 'medium';

  // Whether the achievement is achieved
  isAchieved: boolean = false;

  // CSS classes
  categoryClass: string = '';
  sizeClass: string = '';

  constructor(private achievementDetailService: AchievementDetailService) {
  }

  ngOnInit(): void {
    this.isAchieved = this.achievementData?.playerProgress?.achieved || false;
    this.setCategoryClass();
    this.setSizeClass();
  }

  /**
   * Get the appropriate icon based on achievement data
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
   * Formats point value for display
   */
  formatPoints(): string {
    const points = this.achievementData?.achievement?.points || 0;
    return `${points} pts`;
  }

  /**
   * Open achievement details in side drawer
   */
  openDetails(event?: Event): void {
    // Prevent clicking on card actions from propagating to the card
    if (event) {
      event.stopPropagation();
    }

    this.achievementDetailService.open(this.achievementData);
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

  /**
   * Set the CSS class based on card size
   */
  private setSizeClass(): void {
    switch (this.size) {
      case 'small':
        this.sizeClass = 'achievement-card-small';
        break;
      case 'large':
        this.sizeClass = 'achievement-card-large';
        break;
      default:
        this.sizeClass = 'achievement-card-medium';
        break;
    }
  }
}
