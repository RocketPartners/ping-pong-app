import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { trigger, state, style, transition, animate } from '@angular/animations';
import { NotificationService } from '../../../_services/notification.service';
import { AchievementNotification, CelebrationLevel } from '../../../_models/achievement';

@Component({
  selector: 'app-notification-panel',
  templateUrl: './notification-panel.component.html',
  styleUrls: ['./notification-panel.component.scss'],
  standalone: false,
  animations: [
    trigger('slideInOut', [
      state('in', style({
        transform: 'translateX(0)',
        opacity: 1
      })),
      state('out', style({
        transform: 'translateX(100%)',
        opacity: 0
      })),
      transition('out => in', animate('300ms ease-out')),
      transition('in => out', animate('200ms ease-in'))
    ]),
    trigger('notificationItem', [
      transition(':enter', [
        style({ transform: 'translateX(100%)', opacity: 0 }),
        animate('250ms ease-out', style({ transform: 'translateX(0)', opacity: 1 }))
      ]),
      transition(':leave', [
        animate('200ms ease-in', style({ transform: 'translateX(100%)', opacity: 0 }))
      ])
    ])
  ]
})
export class NotificationPanelComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  isOpen = false;
  notifications: AchievementNotification[] = [];
  unreadCount = 0;
  
  constructor(private notificationService: NotificationService) {}

  ngOnInit(): void {
    // Subscribe to notifications
    this.notificationService.getNotifications()
      .pipe(takeUntil(this.destroy$))
      .subscribe(notifications => {
        this.notifications = notifications;
      });
    
    // Subscribe to unread count
    this.notificationService.getUnreadCount()
      .pipe(takeUntil(this.destroy$))
      .subscribe(count => {
        this.unreadCount = count;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Toggle notification panel
   */
  togglePanel(): void {
    this.isOpen = !this.isOpen;
    
    if (this.isOpen) {
      // Mark all as seen when panel is opened
      setTimeout(() => {
        this.notificationService.markAllAsSeen();
      }, 1000);
    }
  }

  /**
   * Close notification panel
   */
  closePanel(): void {
    this.isOpen = false;
  }

  /**
   * Get notification icon based on celebration level
   */
  getNotificationIcon(notification: AchievementNotification): string {
    switch (notification.celebrationLevel) {
      case CelebrationLevel.EPIC:
        return 'auto_awesome';
      case CelebrationLevel.SPECIAL:
        return 'star';
      default:
        return 'emoji_events';
    }
  }

  /**
   * Get notification CSS class based on celebration level
   */
  getNotificationClass(notification: AchievementNotification): string {
    const baseClass = 'notification-item';
    const levelClass = `level-${notification.celebrationLevel?.toLowerCase() || 'normal'}`;
    const seenClass = notification.seen ? 'seen' : 'unseen';
    
    return `${baseClass} ${levelClass} ${seenClass}`;
  }

  /**
   * Get achievement category color
   */
  getCategoryColor(notification: AchievementNotification): string {
    switch (notification.achievement.category) {
      case 'EASY':
        return '#4caf50';
      case 'MEDIUM':
        return '#2196f3';
      case 'HARD':
        return '#9c27b0';
      case 'LEGENDARY':
        return '#ffc107';
      default:
        return '#2196f3';
    }
  }

  /**
   * Format timestamp for display
   */
  formatTimestamp(timestamp: Date): string {
    const now = new Date();
    const diff = now.getTime() - new Date(timestamp).getTime();
    
    const minutes = Math.floor(diff / (1000 * 60));
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    
    if (minutes < 1) {
      return 'Just now';
    } else if (minutes < 60) {
      return `${minutes}m ago`;
    } else if (hours < 24) {
      return `${hours}h ago`;
    } else if (days < 7) {
      return `${days}d ago`;
    } else {
      return new Date(timestamp).toLocaleDateString();
    }
  }

  /**
   * Get celebration prefix text
   */
  getCelebrationPrefix(notification: AchievementNotification): string {
    switch (notification.celebrationLevel) {
      case CelebrationLevel.EPIC:
        return 'EPIC ACHIEVEMENT!';
      case CelebrationLevel.SPECIAL:
        return 'SPECIAL ACHIEVEMENT!';
      default:
        return 'Achievement Unlocked!';
    }
  }

  /**
   * Mark specific notification as seen
   */
  markAsSeen(notification: AchievementNotification): void {
    this.notificationService.markAsSeen(notification.achievement.id);
  }

  /**
   * Clear all notifications
   */
  clearAll(): void {
    this.notificationService.clearAllNotifications();
  }

  /**
   * Replay celebration for a notification
   */
  replayCelebration(notification: AchievementNotification): void {
    this.notificationService.showAchievementCelebration(
      notification.achievement,
      notification.celebrationLevel || CelebrationLevel.NORMAL,
      notification.contextualMessage || ''
    );
  }

  /**
   * Handle click outside panel
   */
  onBackdropClick(): void {
    this.closePanel();
  }

  /**
   * Handle keyboard events
   */
  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.closePanel();
    }
  }

  /**
   * Track by function for notifications list
   */
  trackByNotification(index: number, notification: AchievementNotification): string {
    return `${notification.achievement.id}-${notification.timestamp.getTime()}`;
  }

  /**
   * Test different celebration levels
   */
  testNormalCelebration(): void {
    this.notificationService.testCelebration(CelebrationLevel.NORMAL);
  }

  testSpecialCelebration(): void {
    this.notificationService.testCelebration(CelebrationLevel.SPECIAL);
  }

  testEpicCelebration(): void {
    this.notificationService.testCelebration(CelebrationLevel.EPIC);
  }
}