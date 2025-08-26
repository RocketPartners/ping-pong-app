import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable} from 'rxjs';
import {map} from 'rxjs/operators';
import {BaseHttpService} from './base-http.service';
import {HttpClient} from '@angular/common/http';
import {
  Achievement,
  AchievementCategory,
  AchievementDTO,
  AchievementNotification,
  AchievementType
} from '../_models/achievement';
import {AlertService} from './alert.services';

@Injectable({
  providedIn: 'root'
})
export class AchievementService extends BaseHttpService {
  private endpoint = '/api/achievements';
  private playerEndpoint = '/api/players';

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
   * Get all achievements for a player
   */
  getPlayerAchievements(playerId: string): Observable<AchievementDTO[]> {
    return this.get<any[] | null>(`${this.playerEndpoint}/${playerId}/achievements`, undefined, []).pipe(
      map(result => this.transformAchievements(result || []))
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
          gameDatePlayed: item.gameDatePlayed
        },
        percentComplete: percentComplete
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
