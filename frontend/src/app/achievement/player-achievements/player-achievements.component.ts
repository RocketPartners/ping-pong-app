import {Component, Input, OnInit} from '@angular/core';
import {Observable, of} from 'rxjs';
import {AchievementService} from '../../_services/achievement.service';
import {AchievementCategory, AchievementDTO} from '../../_models/achievement';

@Component({
  selector: 'app-player-achievements',
  templateUrl: './player-achievements.component.html',
  styleUrls: ['./player-achievements.component.scss'],
  standalone: false
})
export class PlayerAchievementsComponent implements OnInit {
  // Player ID to fetch achievements for
  @Input() playerId!: string;

  // Display options
  @Input() showCount: boolean = true;
  @Input() maxDisplay: number = 8;
  @Input() showCategory: AchievementCategory | null = null;

  // Achievements data
  earnedAchievements$: Observable<AchievementDTO[]> = of([]);
  recentAchievements$: Observable<AchievementDTO[]> = of([]);

  // Loading state
  loading: boolean = true;

  constructor(private achievementService: AchievementService) {
  }

  ngOnInit(): void {
    if (this.playerId) {
      this.loadAchievements();
    }
  }

  /**
   * Load player achievements
   */
  loadAchievements(): void {
    this.loading = true;

    // Get all player achievements
    this.earnedAchievements$ = this.achievementService.getPlayerEarnedAchievements(this.playerId);

    // Recent achievements are the same, but will be sorted by date
    this.recentAchievements$ = this.achievementService.getPlayerEarnedAchievements(this.playerId);

    this.loading = false;
  }

  /**
   * Filter achievements by category if needed
   */
  filterAchievements(achievements: AchievementDTO[]): AchievementDTO[] {
    if (!achievements) return [];
    if (!this.showCategory) return achievements;

    return achievements.filter(a => a.achievement?.category === this.showCategory);
  }

  /**
   * Sort achievements by date (most recent first)
   */
  sortByDate(achievements: AchievementDTO[]): AchievementDTO[] {
    if (!achievements) return [];

    return [...achievements].sort((a, b) => {
      if (!a.playerProgress?.dateEarned) return 1;
      if (!b.playerProgress?.dateEarned) return -1;

      return new Date(b.playerProgress.dateEarned).getTime() -
        new Date(a.playerProgress.dateEarned).getTime();
    });
  }

  /**
   * Sort achievements by category
   */
  sortByCategory(achievements: AchievementDTO[]): AchievementDTO[] {
    if (!achievements) return [];

    return [...achievements].sort((a, b) => {
      // First by category weight
      const catCompare = this.getCategoryWeight(a.achievement?.category) -
        this.getCategoryWeight(b.achievement?.category);

      // Then by name if same category
      return catCompare !== 0 ? catCompare : (a.achievement?.name || '').localeCompare(b.achievement?.name || '');
    });
  }

  /**
   * Get total points for displayed achievements
   */
  getTotalPoints(achievements: AchievementDTO[]): number {
    if (!achievements) return 0;

    return achievements.reduce((sum, a) => sum + (a.achievement?.points || 0), 0);
  }

  /**
   * Limit the number of achievements to display
   */
  limitAchievements(achievements: AchievementDTO[]): AchievementDTO[] {
    if (!achievements) return [];

    return achievements.slice(0, this.maxDisplay);
  }

  /**
   * Get numeric weight for categories (for sorting)
   */
  private getCategoryWeight(category?: AchievementCategory): number {
    if (!category) return 5;

    switch (category) {
      case AchievementCategory.LEGENDARY:
        return 1; // Show legendary first
      case AchievementCategory.HARD:
        return 2;
      case AchievementCategory.MEDIUM:
        return 3;
      case AchievementCategory.EASY:
        return 4;
      default:
        return 5;
    }
  }
}
