import { Injectable } from '@angular/core';
import { Subject, Observable, BehaviorSubject } from 'rxjs';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';
import { Achievement, CelebrationLevel, AchievementNotification, NotificationChannels, AchievementCategory, AchievementType } from '../_models/achievement';

export interface NotificationPreferences {
  enableInApp: boolean;
  enableSound: boolean;
  enableDesktop: boolean;
  celebrationDuration: number; // milliseconds
  showContextualStories: boolean;
}

export interface CelebrationConfig {
  level: CelebrationLevel;
  duration: number;
  sound?: string;
  animation?: string;
  particles?: boolean;
  fullScreen?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notificationsSubject = new BehaviorSubject<AchievementNotification[]>([]);
  public notifications$ = this.notificationsSubject.asObservable();
  
  private celebrationSubject = new Subject<{achievement: Achievement, config: CelebrationConfig, contextualMessage?: string}>();
  public celebration$ = this.celebrationSubject.asObservable();
  
  private unreadCountSubject = new BehaviorSubject<number>(0);
  public unreadCount$ = this.unreadCountSubject.asObservable();

  private defaultPreferences: NotificationPreferences = {
    enableInApp: true,
    enableSound: true,
    enableDesktop: true,
    celebrationDuration: 5000,
    showContextualStories: true
  };

  private celebrationConfigs: Map<CelebrationLevel, CelebrationConfig> = new Map([
    [CelebrationLevel.NORMAL, {
      level: CelebrationLevel.NORMAL,
      duration: 3000,
      sound: 'achievement-normal.mp3',
      animation: 'bounce',
      particles: false,
      fullScreen: false
    }],
    [CelebrationLevel.SPECIAL, {
      level: CelebrationLevel.SPECIAL,
      duration: 5000,
      sound: 'achievement-special.mp3',
      animation: 'pulse',
      particles: true,
      fullScreen: false
    }],
    [CelebrationLevel.EPIC, {
      level: CelebrationLevel.EPIC,
      duration: 8000,
      sound: 'achievement-epic.mp3',
      animation: 'explosion',
      particles: true,
      fullScreen: true
    }]
  ]);

  private preferences: NotificationPreferences = { ...this.defaultPreferences };
  private notificationQueue: AchievementNotification[] = [];

  constructor(private snackBar: MatSnackBar) {
    this.loadPreferences();
    this.requestNotificationPermission();
  }

  /**
   * Show achievement celebration with appropriate level
   */
  showAchievementCelebration(
    achievement: Achievement, 
    celebrationLevel: CelebrationLevel = CelebrationLevel.NORMAL,
    contextualMessage: string = ''
  ): void {
    const config = this.celebrationConfigs.get(celebrationLevel) || this.celebrationConfigs.get(CelebrationLevel.NORMAL)!;
    
    // Create notification object
    const notification: AchievementNotification = {
      achievement,
      timestamp: new Date(),
      seen: false,
      celebrationLevel,
      contextualMessage,
      channels: {
        inApp: this.preferences.enableInApp,
        slackChannel: false, // Handled by backend
        slackDm: false, // Handled by backend
        email: false // Handled by backend
      }
    };

    // Add to notifications list
    this.addNotification(notification);

    // Trigger celebration animation
    if (this.preferences.enableInApp) {
      this.celebrationSubject.next({
        achievement,
        config,
        contextualMessage: this.generateContextualStory(achievement, contextualMessage)
      });
    }

    // Play sound
    if (this.preferences.enableSound && config.sound) {
      this.playNotificationSound(config.sound);
    }

    // Show desktop notification
    if (this.preferences.enableDesktop) {
      this.showDesktopNotification(achievement, celebrationLevel, contextualMessage);
    }

    // Show snackbar notification
    this.showSnackbarNotification(achievement, celebrationLevel, contextualMessage);
  }

  /**
   * Add notification to the list
   */
  private addNotification(notification: AchievementNotification): void {
    const currentNotifications = this.notificationsSubject.value;
    const updatedNotifications = [notification, ...currentNotifications];
    
    // Keep only the last 50 notifications
    if (updatedNotifications.length > 50) {
      updatedNotifications.splice(50);
    }
    
    this.notificationsSubject.next(updatedNotifications);
    this.updateUnreadCount();
  }

  /**
   * Mark notification as seen
   */
  markAsSeen(notificationId: string): void {
    const notifications = this.notificationsSubject.value.map(n => 
      n.achievement.id === notificationId ? { ...n, seen: true } : n
    );
    this.notificationsSubject.next(notifications);
    this.updateUnreadCount();
  }

  /**
   * Mark all notifications as seen
   */
  markAllAsSeen(): void {
    const notifications = this.notificationsSubject.value.map(n => ({ ...n, seen: true }));
    this.notificationsSubject.next(notifications);
    this.updateUnreadCount();
  }

  /**
   * Clear all notifications
   */
  clearAllNotifications(): void {
    this.notificationsSubject.next([]);
    this.updateUnreadCount();
  }

  /**
   * Get notifications
   */
  getNotifications(): Observable<AchievementNotification[]> {
    return this.notifications$;
  }

  /**
   * Get unread count
   */
  getUnreadCount(): Observable<number> {
    return this.unreadCount$;
  }

  /**
   * Update unread count
   */
  private updateUnreadCount(): void {
    const unreadCount = this.notificationsSubject.value.filter(n => !n.seen).length;
    this.unreadCountSubject.next(unreadCount);
  }

  /**
   * Show snackbar notification
   */
  private showSnackbarNotification(
    achievement: Achievement, 
    level: CelebrationLevel, 
    contextualMessage: string
  ): void {
    const config: MatSnackBarConfig = {
      duration: this.celebrationConfigs.get(level)?.duration || 3000,
      verticalPosition: 'top',
      horizontalPosition: 'right',
      panelClass: [`snackbar-${level.toLowerCase()}`, 'achievement-snackbar']
    };

    const message = this.formatSnackbarMessage(achievement, level, contextualMessage);
    this.snackBar.open(message, 'VIEW', config);
  }

  /**
   * Format snackbar message
   */
  private formatSnackbarMessage(
    achievement: Achievement, 
    level: CelebrationLevel, 
    contextualMessage: string
  ): string {
    const prefix = this.getCelebrationPrefix(level);
    const baseMessage = `${prefix} ${achievement.name}!`;
    
    if (contextualMessage && this.preferences.showContextualStories) {
      return `${baseMessage} ${contextualMessage}`;
    }
    
    return baseMessage;
  }

  /**
   * Get celebration prefix based on level
   */
  private getCelebrationPrefix(level: CelebrationLevel): string {
    switch (level) {
      case CelebrationLevel.EPIC:
        return '🎆 EPIC ACHIEVEMENT';
      case CelebrationLevel.SPECIAL:
        return '⭐ SPECIAL ACHIEVEMENT';
      default:
        return '🏆 Achievement Unlocked';
    }
  }

  /**
   * Show desktop notification
   */
  private showDesktopNotification(
    achievement: Achievement, 
    level: CelebrationLevel, 
    contextualMessage: string
  ): void {
    if (!('Notification' in window) || Notification.permission !== 'granted') {
      return;
    }

    const title = this.getCelebrationPrefix(level);
    const body = contextualMessage 
      ? `${achievement.name} - ${contextualMessage}`
      : achievement.name;

    const notification = new Notification(title, {
      body,
      icon: '/assets/icons/achievement-icon.png',
      badge: '/assets/icons/achievement-badge.png',
      tag: `achievement-${achievement.id}`,
      requireInteraction: level === CelebrationLevel.EPIC
    });

    // Auto close after duration
    const duration = this.celebrationConfigs.get(level)?.duration || 3000;
    setTimeout(() => notification.close(), duration);
  }

  /**
   * Play notification sound
   */
  private playNotificationSound(soundFile: string): void {
    try {
      const audio = new Audio(`/assets/sounds/${soundFile}`);
      audio.volume = 0.5;
      audio.play().catch(error => {
        console.warn('Could not play notification sound:', error);
      });
    } catch (error) {
      console.warn('Audio not supported:', error);
    }
  }

  /**
   * Generate contextual story for achievement
   */
  private generateContextualStory(achievement: Achievement, baseContext: string): string {
    if (!this.preferences.showContextualStories) {
      return baseContext;
    }

    // If we already have a contextual message, enhance it
    if (baseContext) {
      return this.enhanceContextualMessage(achievement, baseContext);
    }

    // Generate a contextual story based on achievement properties
    return this.generateAchievementStory(achievement);
  }

  /**
   * Enhance existing contextual message
   */
  private enhanceContextualMessage(achievement: Achievement, context: string): string {
    const stories = [
      `What an incredible moment! ${context}`,
      `Outstanding performance! ${context}`,
      `Absolutely fantastic! ${context}`,
      `Remarkable achievement! ${context}`,
      `Simply amazing! ${context}`
    ];
    
    return stories[Math.floor(Math.random() * stories.length)];
  }

  /**
   * Generate achievement story based on category
   */
  private generateAchievementStory(achievement: Achievement): string {
    const stories = {
      EASY: [
        "Great start on your ping pong journey!",
        "Every champion starts with the basics!",
        "Building momentum, one achievement at a time!",
        "Nice work getting started!"
      ],
      MEDIUM: [
        "Your skills are really developing!",
        "Impressive progress on the table!",
        "You're finding your rhythm!",
        "Solid performance - keep it up!"
      ],
      HARD: [
        "Now this is serious ping pong mastery!",
        "Your dedication is paying off!",
        "Elite-level achievement unlocked!",
        "Outstanding skill demonstration!"
      ],
      LEGENDARY: [
        "LEGENDARY status achieved! You are among the ping pong elite!",
        "This is the stuff of legends! Incredible achievement!",
        "You've reached the pinnacle of ping pong excellence!",
        "History will remember this moment!"
      ]
    };

    const categoryStories = stories[achievement.category] || stories.EASY;
    return categoryStories[Math.floor(Math.random() * categoryStories.length)];
  }

  /**
   * Request notification permission
   */
  private requestNotificationPermission(): void {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission().then(permission => {
        if (permission === 'granted') {
          console.log('Notification permission granted');
        }
      });
    }
  }

  /**
   * Load user preferences
   */
  private loadPreferences(): void {
    try {
      const stored = localStorage.getItem('achievement-notification-preferences');
      if (stored) {
        this.preferences = { ...this.defaultPreferences, ...JSON.parse(stored) };
      }
    } catch (error) {
      console.warn('Could not load notification preferences:', error);
    }
  }

  /**
   * Save user preferences
   */
  updatePreferences(preferences: Partial<NotificationPreferences>): void {
    this.preferences = { ...this.preferences, ...preferences };
    try {
      localStorage.setItem('achievement-notification-preferences', JSON.stringify(this.preferences));
    } catch (error) {
      console.warn('Could not save notification preferences:', error);
    }
  }

  /**
   * Get current preferences
   */
  getPreferences(): NotificationPreferences {
    return { ...this.preferences };
  }

  /**
   * Test celebration with different levels
   */
  testCelebration(level: CelebrationLevel): void {
    const testAchievement: Achievement = {
      id: 'test-' + Date.now(),
      name: `Test ${level} Achievement`,
      description: `This is a test ${level.toLowerCase()} achievement`,
      category: level === CelebrationLevel.EPIC ? AchievementCategory.LEGENDARY : AchievementCategory.MEDIUM,
      type: AchievementType.ONE_TIME,
      criteria: {},
      icon: 'emoji_events',
      points: level === CelebrationLevel.EPIC ? 1000 : 100,
      isVisible: true
    };

    const contextualMessage = `You've successfully tested the ${level.toLowerCase()} celebration system!`;
    this.showAchievementCelebration(testAchievement, level, contextualMessage);
  }
}