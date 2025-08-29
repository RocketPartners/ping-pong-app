import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { Subject, takeUntil, finalize } from 'rxjs';
import { AchievementService } from '../../../_services/achievement.service';
import { AlertService } from '../../../_services/alert.services';
import { PerformanceMonitorService } from '../../../_services/performance-monitor.service';
import { 
  AnalyticsSummary, 
  AchievementAnalytics, 
  CalculatedDifficulty 
} from '../../../_models/achievement';

@Component({
  selector: 'app-analytics-dashboard',
  templateUrl: './analytics-dashboard.component.html',
  styleUrls: ['./analytics-dashboard.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AnalyticsDashboardComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  // Loading states
  loading = false;
  refreshingAnalytics = false;
  
  // Data
  analyticsSummary: AnalyticsSummary | null = null;
  achievementsNeedingAttention: AchievementAnalytics[] = [];
  
  // Chart data
  completionRateDistribution: { label: string, value: number }[] = [];
  difficultyDistribution: { label: string, value: number }[] = [];
  recentTrends: { label: string, value: number }[] = [];

  private componentStartTime = performance.now();

  constructor(
    private achievementService: AchievementService,
    private alertService: AlertService,
    private cdr: ChangeDetectorRef,
    private performanceMonitor: PerformanceMonitorService
  ) { }

  ngOnInit(): void {
    this.performanceMonitor.recordComponentLoad('AnalyticsDashboardComponent', this.componentStartTime);
    this.loadAnalyticsSummary();
    this.loadAchievementsNeedingAttention();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load analytics summary data
   */
  loadAnalyticsSummary(): void {
    this.loading = true;
    const startTime = performance.now();
    
    this.achievementService.getAnalyticsSummary()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
          this.performanceMonitor.recordMetric('analytics_summary_load', performance.now() - startTime);
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (summary) => {
          if (summary) {
            this.analyticsSummary = summary;
            this.processChartData(summary);
          }
          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('Error loading analytics summary:', error);
          this.alertService.error('Failed to load analytics summary');
          this.cdr.markForCheck();
        }
      });
  }

  /**
   * Load achievements that need attention
   */
  loadAchievementsNeedingAttention(): void {
    this.achievementService.getAchievementsNeedingAttention()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (achievements) => {
          this.achievementsNeedingAttention = achievements;
          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('Error loading achievements needing attention:', error);
          this.alertService.error('Failed to load achievements needing attention');
          this.cdr.markForCheck();
        }
      });
  }

  /**
   * Process chart data from analytics summary
   */
  private processChartData(summary: AnalyticsSummary): void {
    // Completion rate distribution
    if (summary.completionRateDistribution) {
      this.completionRateDistribution = Object.entries(summary.completionRateDistribution)
        .map(([label, value]) => ({ label, value }));
    }

    // Difficulty distribution
    if (summary.difficultyDistribution) {
      this.difficultyDistribution = Object.entries(summary.difficultyDistribution)
        .map(([label, value]) => ({ label: this.formatDifficultyLabel(label), value }));
    }

    // Recent trends
    if (summary.recentTrends) {
      this.recentTrends = Object.entries(summary.recentTrends)
        .map(([label, value]) => ({ label, value }));
    }
  }


  /**
   * Manually trigger analytics recalculation
   */
  recalculateAnalytics(): void {
    this.refreshingAnalytics = true;
    this.cdr.markForCheck();
    
    this.achievementService.recalculateAnalytics()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.refreshingAnalytics = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          this.alertService.success('Analytics recalculation started. This may take a few minutes.', true);
          // Reload data after a short delay
          setTimeout(() => {
            this.loadAnalyticsSummary();
            this.loadAchievementsNeedingAttention();
          }, 2000);
          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('Error recalculating analytics:', error);
          this.alertService.error('Failed to start analytics recalculation');
          this.cdr.markForCheck();
        }
      });
  }

  /**
   * Recalculate stale analytics only
   */
  recalculateStaleAnalytics(): void {
    this.achievementService.recalculateStaleAnalytics()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.alertService.success('Stale analytics recalculation completed');
          this.loadAnalyticsSummary();
          this.loadAchievementsNeedingAttention();
          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('Error recalculating stale analytics:', error);
          this.alertService.error('Failed to recalculate stale analytics');
          this.cdr.markForCheck();
        }
      });
  }

  /**
   * Get difficulty badge color
   */
  getDifficultyBadgeColor(difficulty: CalculatedDifficulty): string {
    switch (difficulty) {
      case CalculatedDifficulty.VERY_EASY: return 'accent';
      case CalculatedDifficulty.EASY: return 'primary';
      case CalculatedDifficulty.MODERATE: return 'warn';
      case CalculatedDifficulty.HARD: return 'warn';
      case CalculatedDifficulty.VERY_HARD: return 'warn';
      default: return 'primary';
    }
  }

  /**
   * Get completion trend icon
   */
  getTrendIcon(trend: string): string {
    switch (trend.toUpperCase()) {
      case 'INCREASING': return 'trending_up';
      case 'DECREASING': return 'trending_down';
      case 'STABLE': 
      default: return 'trending_flat';
    }
  }

  /**
   * Get trend color class
   */
  getTrendColorClass(trend: string): string {
    switch (trend.toUpperCase()) {
      case 'INCREASING': return 'trend-positive';
      case 'DECREASING': return 'trend-negative';
      case 'STABLE':
      default: return 'trend-neutral';
    }
  }

  /**
   * Format percentage for display
   */
  formatPercentage(value: number): string {
    return `${value.toFixed(1)}%`;
  }

  /**
   * Format large numbers for display
   */
  formatNumber(value: number): string {
    if (value >= 1000) {
      return `${(value / 1000).toFixed(1)}k`;
    }
    return value.toString();
  }

  /**
   * Track by function for achievements list
   */
  trackByAchievementId(index: number, achievement: AchievementAnalytics): string {
    return achievement.achievementId;
  }

  /**
   * Format difficulty labels for display (public version)
   */
  formatDifficultyLabel(difficulty: string): string {
    return difficulty.replace(/_/g, ' ').toLowerCase()
      .replace(/\b\w/g, l => l.toUpperCase());
  }
}