import {Component, Input, OnInit} from '@angular/core';
import {AchievementCategory, AchievementDTO, CelebrationLevel} from '../../../_models/achievement';
import {AchievementDetailService} from "../../../achievement/achievement-detail/achievement-detail.service";
import {NotificationService} from '../../../_services/notification.service';

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
  @Input() showDependencies: boolean = true;
  @Input() size: 'small' | 'medium' | 'large' = 'medium';

  // Whether the achievement is achieved
  isAchieved: boolean = false;

  // CSS classes
  categoryClass: string = '';
  sizeClass: string = '';
  
  // Dependencies
  hasBlockingDependencies: boolean = false;
  dependencyCount: number = 0;
  
  // Celebration level
  celebrationLevel: CelebrationLevel = CelebrationLevel.NORMAL;

  constructor(
    private achievementDetailService: AchievementDetailService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.isAchieved = this.achievementData?.playerProgress?.achieved || false;
    this.setCategoryClass();
    this.setSizeClass();
    this.checkDependencies();
    this.setCelebrationLevel();
  }

  /**
   * Check if this achievement was recently unlocked (from notifications)
   */
  isRecentlyUnlocked(): boolean {
    return this.achievementData?.recentlyUnlocked || false;
  }

  /**
   * Get CSS classes for the achievement card including recent notification styling
   */
  getCardClasses(): string {
    let classes = `achievement-card ${this.categoryClass} ${this.sizeClass}`;
    
    if (this.isRecentlyUnlocked()) {
      classes += ' recently-unlocked';
    }
    
    if (!this.isAchieved) {
      classes += ' not-achieved';
    }
    
    return classes;
  }

  /**
   * Check if this is an easter egg achievement
   */
  isEasterEggAchievement(): boolean {
    if (!this.achievementData?.achievement?.criteria) {
      return false;
    }
    
    const criteriaType = this.achievementData.achievement.criteria.type || '';
    return criteriaType.toString().startsWith('EASTER_EGG_');
  }

  /**
   * Get the appropriate icon based on achievement data
   */
  getIconName(): string {
    // For easter egg achievements that aren't unlocked, show mystery icon
    if (this.isEasterEggAchievement() && !this.isAchieved) {
      return 'help_outline';
    }

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
   * Get the achievement name (hidden for unearned easter eggs)
   */
  getAchievementName(): string {
    if (this.isEasterEggAchievement() && !this.isAchieved) {
      return '??? Hidden Achievement';
    }
    return this.achievementData.achievement.name;
  }

  /**
   * Get the achievement description (hidden for unearned easter eggs)
   */
  getAchievementDescription(): string {
    if (this.isEasterEggAchievement() && !this.isAchieved) {
      return 'Find easter eggs to unlock this mysterious achievement...';
    }
    return this.achievementData.achievement.description;
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

  /**
   * Check if this achievement has blocking dependencies
   */
  private checkDependencies(): void {
    if (this.achievementData?.playerDependencyInfo) {
      this.dependencyCount = this.achievementData.playerDependencyInfo.totalDependencies || 0;
      this.hasBlockingDependencies = this.achievementData.playerDependencyInfo.hasBlockingDependencies || false;
    }
  }

  /**
   * Set celebration level based on achievement properties
   */
  private setCelebrationLevel(): void {
    if (this.achievementData?.achievement) {
      // Determine celebration level based on category and achievement properties
      if (this.achievementData.achievement.category === AchievementCategory.LEGENDARY) {
        this.celebrationLevel = CelebrationLevel.EPIC;
      } else if (this.achievementData.achievement.category === AchievementCategory.HARD) {
        this.celebrationLevel = CelebrationLevel.SPECIAL;
      } else {
        this.celebrationLevel = CelebrationLevel.NORMAL;
      }
    }
  }

  /**
   * Get dependency status text
   */
  getDependencyStatusText(): string {
    if (!this.showDependencies || this.dependencyCount === 0) {
      return '';
    }
    
    if (this.hasBlockingDependencies) {
      return `${this.dependencyCount} dependency required`;
    }
    
    return `Part of ${this.dependencyCount} achievement chain`;
  }

  /**
   * Get contextual information about how the achievement was earned
   */
  getContextualInfo(): string {
    if (!this.isAchieved || !this.achievementData?.playerProgress) {
      return '';
    }

    const progress = this.achievementData.playerProgress;
    let context = '';

    if (progress.opponentName) {
      context += `vs ${progress.opponentName}`;
    }

    if (progress.gameDatePlayed) {
      const gameDate = new Date(progress.gameDatePlayed);
      context += context ? ` on ${gameDate.toLocaleDateString()}` : `on ${gameDate.toLocaleDateString()}`;
    }

    return context;
  }

  /**
   * Trigger achievement celebration
   */
  triggerCelebration(): void {
    if (this.isAchieved) {
      this.notificationService.showAchievementCelebration(
        this.achievementData.achievement,
        this.celebrationLevel,
        this.getContextualInfo()
      );
    }
  }

  /**
   * Get celebration level display text
   */
  getCelebrationLevelText(): string {
    switch (this.celebrationLevel) {
      case CelebrationLevel.EPIC:
        return 'Epic Achievement!';
      case CelebrationLevel.SPECIAL:
        return 'Special Achievement!';
      default:
        return 'Achievement Unlocked!';
    }
  }
}
