import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject, takeUntil, finalize } from 'rxjs';
import { AchievementService } from '../../../_services/achievement.service';
import { AlertService } from '../../../_services/alert.services';
import { Achievement } from '../../../_models/achievement';

@Component({
  selector: 'app-performance-monitor',
  templateUrl: './performance-monitor.component.html',
  styleUrls: ['./performance-monitor.component.scss'],
  standalone: false
})
export class PerformanceMonitorComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  loading = false;
  evaluating = false;
  resetting = false;
  
  achievements: Achievement[] = [];
  selectedPlayerId = '';

  constructor(
    private achievementService: AchievementService,
    private alertService: AlertService
  ) { }

  ngOnInit(): void {
    this.loadAchievements();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load all achievements for display
   */
  loadAchievements(): void {
    this.loading = true;
    
    this.achievementService.getAdminAchievementsList()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loading = false)
      )
      .subscribe({
        next: (achievements) => {
          this.achievements = achievements;
        },
        error: (error) => {
          console.error('Error loading achievements:', error);
          this.alertService.error('Failed to load achievements');
        }
      });
  }

  /**
   * Test achievement evaluation for a player
   */
  testPlayerEvaluation(): void {
    if (!this.selectedPlayerId.trim()) {
      this.alertService.error('Please enter a valid player ID');
      return;
    }

    this.evaluating = true;
    
    this.achievementService.evaluateAllAchievementsForPlayer(this.selectedPlayerId)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.evaluating = false)
      )
      .subscribe({
        next: (response) => {
          this.alertService.success(`Achievement evaluation completed for player: ${this.selectedPlayerId}`);
        },
        error: (error) => {
          console.error('Error evaluating achievements:', error);
          this.alertService.error('Failed to evaluate achievements for player');
        }
      });
  }

  /**
   * Reset all player progress (DANGEROUS)
   */
  resetAllProgress(): void {
    const confirmationCode = 'CONFIRM_RESET_ALL';
    const userConfirmation = prompt(
      `⚠️ DANGER: This will permanently delete ALL player achievement progress!\n\n` +
      `Type "${confirmationCode}" to confirm:`
    );

    if (userConfirmation !== confirmationCode) {
      this.alertService.error('Reset cancelled - confirmation code did not match');
      return;
    }

    this.resetting = true;
    
    this.achievementService.resetAllPlayerProgress(confirmationCode)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.resetting = false)
      )
      .subscribe({
        next: (response) => {
          this.alertService.success('All player achievement progress has been reset');
        },
        error: (error) => {
          console.error('Error resetting progress:', error);
          this.alertService.error('Failed to reset player progress');
        }
      });
  }

  /**
   * Get achievement category color
   */
  getCategoryColor(category: string): string {
    switch (category?.toUpperCase()) {
      case 'EASY': return 'primary';
      case 'MEDIUM': return 'accent';
      case 'HARD': return 'warn';
      case 'LEGENDARY': return 'warn';
      default: return 'primary';
    }
  }

  /**
   * Get count of visible achievements
   */
  getVisibleAchievements(): number {
    return this.achievements.filter(a => a.isVisible).length;
  }

  /**
   * Get count of achievements by category
   */
  getAchievementsByCategory(category: string, returnArray = false): any {
    const filtered = this.achievements.filter(a => a.category.toString() === category.toUpperCase());
    return returnArray ? filtered : filtered.length;
  }

  /**
   * Get unique categories
   */
  getCategories(): string[] {
    const categories = [...new Set(this.achievements.map(a => a.category.toString()))];
    return categories.sort();
  }
}