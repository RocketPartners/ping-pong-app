import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable, of} from 'rxjs';
import {map, catchError} from 'rxjs/operators';
import {retryWithBackoff, RetryPresets} from '../_helpers/retry-operators';
import {BaseHttpService} from './base-http.service';
import {HttpClient} from '@angular/common/http';
import {
  Achievement,
  AchievementAnalytics,
  AchievementCategory,
  AchievementConfiguration,  
  AchievementDependency,
  AchievementDTO,
  AchievementNotification,
  AchievementType,
  AnalyticsSummary,
  CelebrationLevel,
  PlayerDependencyTree
} from '../_models/achievement';
import {AlertService} from './alert.services';

@Injectable({
  providedIn: 'root'
})
export class AchievementService extends BaseHttpService {
  private endpoint = '/api/achievements';
  private playerEndpoint = '/api/players';
  private adminEndpoint = '/api/admin/achievements';

  // Latest earned achievement notifications
  private achievementNotifications = new BehaviorSubject<AchievementNotification[]>([]);
  public achievementNotifications$ = this.achievementNotifications.asObservable();

  // Track if there are unseen notifications
  private hasUnseenNotifications = new BehaviorSubject<boolean>(false);
  public hasUnseenNotifications$ = this.hasUnseenNotifications.asObservable();

  constructor(
    http: HttpClient,
    alertService: AlertService  // Keep this as required, not optional
  ) {
    super(http, alertService);  // Pass to parent
    this.loadNotificationsFromStorage();
  }

  /**
   * Get all visible achievements
   */
  getAllAchievements(): Observable<Achievement[]> {
    return this.get<Achievement[]>(this.endpoint, undefined, []).pipe(
      map(result => result || [])
    );
  }

  /**
   * Get all achievements (admin only)
   */
  getAllAchievementsAdmin(): Observable<Achievement[]> {
    return this.get<Achievement[]>(`${this.endpoint}/all`, undefined, []).pipe(
      map(result => result || [])
    );
  }

  /**
   * Get a specific achievement by ID
   */
  getAchievement(id: string): Observable<Achievement | null> {
    return this.get<Achievement>(`${this.endpoint}/${id}`, undefined, null);
  }

  /**
   * Get all achievements for a player (now includes recentlyUnlocked flag and auto-acknowledges notifications)
   */
  getPlayerAchievements(playerId: string): Observable<AchievementDTO[]> {
    return this.get<any[] | null>(`${this.playerEndpoint}/${playerId}/achievements`, undefined, []).pipe(
      retryWithBackoff(RetryPresets.STANDARD),
      map(result => this.transformAchievements(result || [])),
      catchError(error => {
        console.error(`Failed to load achievements for player ${playerId} after retries:`, error);
        throw error;
      })
    );
  }

  /**
   * Get recent achievement notifications for a player
   */
  getRecentAchievementNotifications(playerId: string): Observable<any[]> {
    return this.get<any[] | null>(`${this.playerEndpoint}/${playerId}/recent-achievements`, undefined, []).pipe(
      map(result => result || []),
      catchError(error => {
        console.error(`Failed to load recent achievement notifications for player ${playerId}:`, error);
        return of([]);
      })
    );
  }

  /**
   * Acknowledge recent achievement notifications for a player
   */
  acknowledgeRecentAchievements(playerId: string): Observable<any> {
    return this.post<any>(`${this.playerEndpoint}/${playerId}/recent-achievements/acknowledge`, {}).pipe(
      catchError(error => {
        console.error(`Failed to acknowledge recent achievement notifications for player ${playerId}:`, error);
        return of(null);
      })
    );
  }

  /**
   * Get only earned achievements for a player
   */
  getPlayerEarnedAchievements(playerId: string): Observable<AchievementDTO[]> {
    return this.get<any[] | null>(`${this.endpoint}/player/${playerId}/achieved`, undefined, []).pipe(
      map(result => this.transformAchievements(result || []))
    );
  }

  /**
   * Recalculate player achievements (admin only)
   */
  recalculatePlayerAchievements(playerId: string): Observable<any> {
    return this.post<any>(`${this.playerEndpoint}/${playerId}/achievements/recalculate`, {});
  }

  /**
   * Handle achievement notifications
   * This will be called after specific actions that might trigger achievements
   * like completing a game, match, or tournament
   */
  checkForNewAchievements(playerId: string): void {
    // Get current earned achievements count
    this.getPlayerAchievements(playerId).pipe(
      map(achievements => achievements.filter(a => a.playerProgress && a.playerProgress.achieved))
    ).subscribe({
      next: (achievements) => {
        // Get the last check timestamp from storage
        const lastCheck = localStorage.getItem('achievementLastCheck');
        const lastCheckDate = lastCheck ? new Date(lastCheck) : new Date(0);

        // Filter achievements earned since last check
        const newAchievements = achievements.filter(a => {
          if (!a.playerProgress?.dateEarned) return false;
          const earnedDate = new Date(a.playerProgress.dateEarned);
          return earnedDate > lastCheckDate;
        });

        // Update last check time
        localStorage.setItem('achievementLastCheck', new Date().toISOString());

        // If new achievements, create notifications
        if (newAchievements.length > 0) {
          const notifications: AchievementNotification[] = newAchievements.map(achievement => ({
            achievement: achievement.achievement,
            timestamp: new Date(),
            seen: false
          }));

          // Show alerts for new achievements
          notifications.forEach(notification => {
            if (this.alertService) {
              this.alertService.success(`Achievement Unlocked: ${notification.achievement.name}`, true);
            }
          });

          // Add to notifications list
          this.addNotifications(notifications);
        }
      }
    });
  }

  /**
   * Mark all notifications as seen
   */
  markNotificationsAsSeen(): void {
    const current = this.achievementNotifications.value;
    const updated = current.map(n => ({...n, seen: true}));

    localStorage.setItem('achievementNotifications', JSON.stringify(updated));
    this.achievementNotifications.next(updated);
    this.hasUnseenNotifications.next(false);
  }

  /**
   * Clear all notifications
   */
  clearNotifications(): void {
    localStorage.removeItem('achievementNotifications');
    this.achievementNotifications.next([]);
    this.hasUnseenNotifications.next(false);
  }

  // ===================================================================
  // ADMIN CONFIGURATION METHODS
  // ===================================================================

  /**
   * Load achievement configurations from YAML file
   */
  loadAchievementConfig(filename: string): Observable<any> {
    return this.post<any>(`${this.adminEndpoint}/config/load?filename=${encodeURIComponent(filename)}`, {});
  }

  /**
   * Apply loaded configurations to database
   */
  applyAchievementConfig(): Observable<any> {
    return this.post<any>(`${this.adminEndpoint}/config/apply`, {});
  }

  /**
   * Load and apply configurations in one step
   */
  loadAndApplyConfig(filename: string = 'achievements-config.yaml'): Observable<any> {
    return this.post<any>(`${this.adminEndpoint}/config/load-and-apply?filename=${encodeURIComponent(filename)}`, {});
  }

  /**
   * Validate loaded configurations
   */
  validateConfigurations(): Observable<any> {
    return this.get<any>(`${this.adminEndpoint}/config/validate`);
  }

  /**
   * Export current achievements to YAML
   */
  exportConfigurations(): Observable<string> {
    return this.get<string>(`${this.adminEndpoint}/config/export`, undefined, '').pipe(
      map(result => result || '')
    );
  }

  /**
   * Get loaded configurations
   */
  getLoadedConfigurations(): Observable<{ [key: string]: AchievementConfiguration }> {
    return this.get<{ [key: string]: AchievementConfiguration }>(`${this.adminEndpoint}/config/loaded`, undefined, {}).pipe(
      map(result => result || {})
    );
  }

  // ===================================================================
  // ANALYTICS METHODS
  // ===================================================================

  /**
   * Get analytics summary for all achievements
   */
  getAnalyticsSummary(): Observable<AnalyticsSummary | null> {
    return this.get<AnalyticsSummary>(`${this.adminEndpoint}/analytics/summary`).pipe(
      retryWithBackoff(RetryPresets.ANALYTICS),
      catchError(error => {
        console.error('Failed to load analytics summary after retries:', error);
        throw error;
      })
    );
  }

  /**
   * Get detailed analytics for specific achievement
   */
  getAchievementAnalytics(achievementId: string): Observable<any> {
    return this.get<any>(`${this.adminEndpoint}/analytics/${achievementId}`);
  }

  /**
   * Get achievements that need attention (too easy/hard, declining, etc.)
   */
  getAchievementsNeedingAttention(): Observable<AchievementAnalytics[]> {
    return this.get<AchievementAnalytics[]>(`${this.adminEndpoint}/analytics/attention`, undefined, []).pipe(
      map(result => result || [])
    );
  }

  /**
   * Manually trigger analytics recalculation for all achievements
   */
  recalculateAnalytics(): Observable<any> {
    return this.post<any>(`${this.adminEndpoint}/analytics/recalculate`, {});
  }

  /**
   * Force recalculation of stale analytics
   */
  recalculateStaleAnalytics(): Observable<any> {
    return this.post<any>(`${this.adminEndpoint}/analytics/recalculate-stale`, {});
  }

  // ===================================================================
  // DEPENDENCY METHODS
  // ===================================================================

  /**
   * Get achievement dependencies for a specific achievement
   */
  getAchievementDependencies(achievementId: string): Observable<AchievementDependency[]> {
    return this.get<AchievementDependency[]>(`${this.endpoint}/${achievementId}/dependencies`, undefined, []).pipe(
      map(result => result || [])
    );
  }

  /**
   * Get full dependency tree for a player (for visualization)
   */
  getPlayerDependencyTree(playerId: string): Observable<PlayerDependencyTree | null> {
    return this.get<PlayerDependencyTree>(`${this.playerEndpoint}/${playerId}/dependency-tree`);
  }

  // ===================================================================
  // ENHANCED PLAYER METHODS
  // ===================================================================

  /**
   * Evaluate all achievements for a specific player (admin/testing)
   */
  evaluateAllAchievementsForPlayer(playerId: string): Observable<any> {
    return this.post<any>(`${this.adminEndpoint}/test-evaluation/${playerId}`, {});
  }

  /**
   * Reset all player progress (DANGEROUS - requires confirmation)
   */
  resetAllPlayerProgress(confirmCode: string): Observable<any> {
    return this.post<any>(`${this.adminEndpoint}/reset-progress?confirmReset=${encodeURIComponent(confirmCode)}`, {});
  }

  /**
   * Get all achievements with admin details
   */
  getAdminAchievementsList(): Observable<Achievement[]> {
    return this.get<Achievement[]>(`${this.adminEndpoint}/list`, undefined, []).pipe(
      map(result => result || [])
    );
  }

  // ===================================================================
  // ENHANCED NOTIFICATION METHODS
  // ===================================================================

  /**
   * Enhanced achievement notification with celebration levels
   * @deprecated Use NotificationService.showAchievementCelebration instead
   */
  showAchievementCelebration(
    achievement: Achievement,
    celebrationLevel: CelebrationLevel,
    contextualMessage?: string
  ): void {
    // This method is deprecated - the NotificationService should be used directly
    console.warn('AchievementService.showAchievementCelebration is deprecated. Use NotificationService.showAchievementCelebration instead.');
    
    const notification: AchievementNotification = {
      achievement,
      timestamp: new Date(),
      seen: false,
      celebrationLevel,
      contextualMessage
    };

    // Add to notifications
    this.addNotifications([notification]);

    // Show appropriate celebration based on level
    this.displayCelebration(notification);
  }

  /**
   * Display celebration based on level
   */
  private displayCelebration(notification: AchievementNotification): void {
    const { achievement, celebrationLevel, contextualMessage } = notification;
    
    let message = `Achievement Unlocked: ${achievement.name}`;
    if (contextualMessage) {
      message += ` - ${contextualMessage}`;
    }

    switch (celebrationLevel) {
      case CelebrationLevel.EPIC:
        // Epic celebrations could trigger a modal or full-screen animation
        if (this.alertService) {
          this.alertService.success(message + ' 🎉🏆✨', true);
        }
        // TODO: Trigger full-screen celebration component
        break;

      case CelebrationLevel.SPECIAL:
        // Special celebrations with longer duration and styling
        if (this.alertService) {
          this.alertService.success(message + ' 🎊⭐', true);
        }
        // TODO: Trigger enhanced notification component
        break;

      case CelebrationLevel.NORMAL:
      default:
        // Standard notification
        if (this.alertService) {
          this.alertService.success(message + ' 🏅', true);
        }
        break;
    }
  }

  /**
   * Transform flat API response to the expected nested structure
   */
  private transformAchievements(achievements: any[]): AchievementDTO[] {
    return achievements.map(item => {
      // Calculate percent complete based on progress and threshold
      const threshold = item.threshold || 1;
      const progress = item.progress || 0;
      const percentComplete = Math.min(Math.round((progress / threshold) * 100), 100);

      return {
        achievement: {
          id: item.id || '',
          name: item.name || 'Unknown Achievement',
          description: item.description || '',
          category: item.category || AchievementCategory.EASY,
          type: item.type || AchievementType.ONE_TIME,
          criteria: {
            type: item.criteriaType || 'UNKNOWN',
            threshold: item.threshold || 1
          },
          icon: item.icon || 'emoji_events',
          points: item.points || 0,
          isVisible: true
        },
        playerProgress: {
          id: item.progressId || `progress-${item.id}` || '',
          playerId: item.playerId || '',
          achievementId: item.id || '',
          achieved: item.achieved || false,
          progress: item.progress || 0,
          dateEarned: item.dateEarned,
          opponentName: item.opponentName,
          gameDatePlayed: item.gameDatePlayed,
          dependenciesMet: item.dependenciesMet !== undefined ? item.dependenciesMet : true,
          availableToEarn: item.availableToEarn !== undefined ? item.availableToEarn : true
        },
        percentComplete: percentComplete,
        recentlyUnlocked: item.recentlyUnlocked || false
      };
    });
  }

  /**
   * Add notifications to the list
   */
  private addNotifications(notifications: AchievementNotification[]): void {
    const current = this.achievementNotifications.value;
    const updated = [...notifications, ...current];

    // Keep only the latest 10 notifications
    const trimmed = updated.slice(0, 10);

    // Store in local storage
    localStorage.setItem('achievementNotifications', JSON.stringify(trimmed));

    // Update subjects
    this.achievementNotifications.next(trimmed);
    this.hasUnseenNotifications.next(trimmed.some(n => !n.seen));
  }

  /**
   * Load notifications from storage on service init
   */
  private loadNotificationsFromStorage(): void {
    const stored = localStorage.getItem('achievementNotifications');
    if (stored) {
      const notifications = JSON.parse(stored) as AchievementNotification[];
      this.achievementNotifications.next(notifications);
      this.hasUnseenNotifications.next(notifications.some(n => !n.seen));
    }
  }
}
