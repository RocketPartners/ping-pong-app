import {Component, Input, OnInit} from '@angular/core';
import {Observable, of, combineLatest} from 'rxjs';
import {map, catchError} from 'rxjs/operators';
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
      
      // Acknowledge recent achievement notifications after a short delay
      // This simulates the user having viewed the achievements page
      setTimeout(() => {
        this.acknowledgeRecentAchievements();
      }, 2000);
    }
  }

  /**
   * Load player achievements
   */
  loadAchievements(): void {
    this.loading = true;

    // Get all player achievements AND recent notifications
    const allAchievements$ = this.achievementService.getPlayerAchievements(this.playerId).pipe(
      map(achievements => achievements.filter(a => a.playerProgress?.achieved)),
      catchError(error => {
        console.error('Error loading player achievements:', error);
        return of([]);
      })
    );

    const recentNotifications$ = this.achievementService.getRecentAchievementNotifications(this.playerId).pipe(
      catchError(error => {
        console.error('Error loading recent achievement notifications:', error);
        return of([]);
      })
    );

    // Combine both sources to mark recently unlocked achievements
    this.earnedAchievements$ = combineLatest([allAchievements$, recentNotifications$]).pipe(
      map(([achievements, recentNotifications]) => {
        console.log('Debug - All achievements:', achievements.length);
        console.log('Debug - Recent notifications:', recentNotifications);
        
        // Create a set of recent achievement IDs for fast lookup
        const recentIds = new Set(recentNotifications.map(r => {
          const id = r.achievement?.id || r.id;
          console.log('Debug - Recent notification ID:', id, 'from object:', r);
          return id;
        }));
        
        console.log('Debug - Recent IDs set:', Array.from(recentIds));
        
        // Mark achievements as recently unlocked if they appear in notifications
        const result = achievements.map(achievement => {
          const achievementId = achievement.achievement?.id;
          const isRecent = recentIds.has(achievementId);
          console.log(`Debug - Achievement ${achievement.achievement?.name} (ID: ${achievementId}) - isRecent: ${isRecent}`);
          
          return {
            ...achievement,
            recentlyUnlocked: isRecent
          };
        });
        
        // TEMPORARY: If no recent achievements are found, mark the first 2 achievements as recent for testing
        if (result.filter(a => a.recentlyUnlocked).length === 0 && result.length > 0) {
          console.log('Debug - No recent achievements found, marking first 2 as recent for testing');
          result[0].recentlyUnlocked = true;
          if (result.length > 1) {
            result[1].recentlyUnlocked = true;
          }
        }
        
        console.log('Debug - Final result with recent flags:', result.filter(a => a.recentlyUnlocked));
        return result;
      })
    );

    // Recent achievements are the same, but will be sorted by date
    this.recentAchievements$ = this.earnedAchievements$;

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
   * Check if an achievement is recently unlocked
   */
  isRecentlyUnlocked(achievement: AchievementDTO): boolean {
    return achievement.recentlyUnlocked || false;
  }

  /**
   * Acknowledge recent achievement notifications
   * This should be called when the user visits the achievements page
   */
   acknowledgeRecentAchievements(): void {
    // Temporary solution: Since the backend acknowledge endpoint doesn't exist yet,
    // we'll just broadcast the event to clear the UI notifications
    console.log('Acknowledging recent achievement notifications for player:', this.playerId);
    
    // Broadcast an event that can be picked up by the notifications component
    // This will ensure the UI updates immediately
    window.dispatchEvent(new CustomEvent('achievement-notifications-acknowledged', {
      detail: { playerId: this.playerId }
    }));
    
    // TODO: Once backend endpoint is created, uncomment this:
    // this.achievementService.acknowledgeRecentAchievements(this.playerId).subscribe({
    //   next: () => {
    //     console.log('Successfully acknowledged recent achievement notifications for player:', this.playerId);
    //   },
    //   error: (error) => {
    //     console.error('Error acknowledging recent achievement notifications:', error);
    //   }
    // });
  }

  /**
   * Get CSS class for achievement highlighting
   */
  getAchievementClass(achievement: AchievementDTO): string {
    const baseClass = 'achievement-item';
    const recentClass = this.isRecentlyUnlocked(achievement) ? 'recently-unlocked' : '';
    const categoryClass = `category-${achievement.achievement?.category?.toLowerCase() || 'easy'}`;
    
    return `${baseClass} ${recentClass} ${categoryClass}`.trim();
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
