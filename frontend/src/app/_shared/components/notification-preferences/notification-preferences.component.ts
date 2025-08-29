import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { NotificationService, NotificationPreferences } from '../../../_services/notification.service';
import { CelebrationLevel } from '../../../_models/achievement';

@Component({
  selector: 'app-notification-preferences',
  templateUrl: './notification-preferences.component.html',
  styleUrls: ['./notification-preferences.component.scss'],
  standalone: false
})
export class NotificationPreferencesComponent implements OnInit {
  preferencesForm: FormGroup;
  currentPreferences: NotificationPreferences;
  
  // Duration options in milliseconds
  durationOptions = [
    { value: 2000, label: '2 seconds' },
    { value: 3000, label: '3 seconds' },
    { value: 5000, label: '5 seconds' },
    { value: 8000, label: '8 seconds' },
    { value: 10000, label: '10 seconds' }
  ];

  constructor(
    private fb: FormBuilder,
    private notificationService: NotificationService
  ) {
    this.currentPreferences = this.notificationService.getPreferences();
    
    this.preferencesForm = this.fb.group({
      enableInApp: [this.currentPreferences.enableInApp],
      enableSound: [this.currentPreferences.enableSound],
      enableDesktop: [this.currentPreferences.enableDesktop],
      celebrationDuration: [this.currentPreferences.celebrationDuration],
      showContextualStories: [this.currentPreferences.showContextualStories]
    });
  }

  ngOnInit(): void {
    // Subscribe to form changes and auto-save
    this.preferencesForm.valueChanges.subscribe(values => {
      this.savePreferences(values);
    });
  }

  /**
   * Save preferences
   */
  private savePreferences(preferences: Partial<NotificationPreferences>): void {
    this.notificationService.updatePreferences(preferences);
    this.currentPreferences = this.notificationService.getPreferences();
  }

  /**
   * Test normal notification
   */
  testNormal(): void {
    this.notificationService.testCelebration(CelebrationLevel.NORMAL);
  }

  /**
   * Test special notification
   */
  testSpecial(): void {
    this.notificationService.testCelebration(CelebrationLevel.SPECIAL);
  }

  /**
   * Test epic notification
   */
  testEpic(): void {
    this.notificationService.testCelebration(CelebrationLevel.EPIC);
  }

  /**
   * Reset to defaults
   */
  resetToDefaults(): void {
    const defaults: NotificationPreferences = {
      enableInApp: true,
      enableSound: true,
      enableDesktop: true,
      celebrationDuration: 5000,
      showContextualStories: true
    };
    
    this.preferencesForm.patchValue(defaults);
    this.notificationService.updatePreferences(defaults);
  }

  /**
   * Check if desktop notifications are supported
   */
  get isDesktopNotificationSupported(): boolean {
    return 'Notification' in window;
  }

  /**
   * Get desktop notification permission status
   */
  get desktopNotificationPermission(): string {
    if (!this.isDesktopNotificationSupported) {
      return 'not-supported';
    }
    return Notification.permission;
  }

  /**
   * Request desktop notification permission
   */
  requestDesktopPermission(): void {
    if (this.isDesktopNotificationSupported && Notification.permission === 'default') {
      Notification.requestPermission().then(permission => {
        if (permission === 'granted') {
          // Update form if permission was granted
          this.preferencesForm.patchValue({ enableDesktop: true });
        }
      });
    }
  }

  /**
   * Get permission status text
   */
  getPermissionStatusText(): string {
    switch (this.desktopNotificationPermission) {
      case 'granted':
        return 'Granted';
      case 'denied':
        return 'Denied';
      case 'default':
        return 'Not requested';
      case 'not-supported':
        return 'Not supported';
      default:
        return 'Unknown';
    }
  }

  /**
   * Get permission status color
   */
  getPermissionStatusColor(): string {
    switch (this.desktopNotificationPermission) {
      case 'granted':
        return 'primary';
      case 'denied':
        return 'warn';
      case 'default':
        return 'accent';
      default:
        return '';
    }
  }
}