import { Component, OnInit, OnDestroy, HostBinding } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { trigger, state, style, transition, animate, keyframes } from '@angular/animations';
import { NotificationService } from '../../../_services/notification.service';
import { Achievement, CelebrationLevel } from '../../../_models/achievement';

interface CelebrationData {
  achievement: Achievement;
  level: CelebrationLevel;
  contextualMessage: string;
  isActive: boolean;
}

@Component({
  selector: 'app-celebration-overlay',
  templateUrl: './celebration-overlay.component.html',
  styleUrls: ['./celebration-overlay.component.scss'],
  standalone: false,
  animations: [
    trigger('overlayAnimation', [
      state('hidden', style({ 
        opacity: 0, 
        transform: 'scale(0.8)' 
      })),
      state('visible', style({ 
        opacity: 1, 
        transform: 'scale(1)' 
      })),
      transition('hidden => visible', [
        animate('0.5s ease-out')
      ]),
      transition('visible => hidden', [
        animate('0.3s ease-in')
      ])
    ]),
    trigger('achievementAnimation', [
      state('normal', style({ transform: 'scale(1) rotate(0deg)' })),
      transition('* => normal', [
        animate('1s ease-out', keyframes([
          style({ transform: 'scale(0) rotate(-180deg)', offset: 0 }),
          style({ transform: 'scale(1.2) rotate(0deg)', offset: 0.7 }),
          style({ transform: 'scale(1) rotate(0deg)', offset: 1 })
        ]))
      ])
    ]),
    trigger('textAnimation', [
      state('visible', style({ opacity: 1, transform: 'translateY(0)' })),
      transition('* => visible', [
        style({ opacity: 0, transform: 'translateY(30px)' }),
        animate('0.8s 0.3s ease-out')
      ])
    ]),
    trigger('particleAnimation', [
      transition('* => *', [
        animate('2s ease-out', keyframes([
          style({ transform: 'translateY(0) scale(1)', opacity: 1, offset: 0 }),
          style({ transform: 'translateY(-100px) scale(1.5)', opacity: 0.8, offset: 0.5 }),
          style({ transform: 'translateY(-200px) scale(0)', opacity: 0, offset: 1 })
        ]))
      ])
    ])
  ]
})
export class CelebrationOverlayComponent implements OnInit, OnDestroy {
  @HostBinding('class.celebration-active') isActive = false;
  
  private destroy$ = new Subject<void>();
  
  currentCelebration: CelebrationData | null = null;
  overlayState = 'hidden';
  achievementState = 'normal';
  textState = 'visible';
  
  particles: Array<{id: number, x: number, y: number, delay: number}> = [];
  
  constructor(private notificationService: NotificationService) {}

  ngOnInit(): void {
    this.notificationService.celebration$
      .pipe(takeUntil(this.destroy$))
      .subscribe(celebrationData => {
        if (celebrationData) {
          this.showCelebration(
            celebrationData.achievement,
            celebrationData.config.level,
            celebrationData.contextualMessage || ''
          );
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Show celebration overlay
   */
  private showCelebration(achievement: Achievement, level: CelebrationLevel, contextualMessage: string): void {
    this.currentCelebration = {
      achievement,
      level,
      contextualMessage,
      isActive: true
    };
    
    this.isActive = true;
    this.overlayState = 'visible';
    
    // Reset animation states
    this.achievementState = 'normal';
    this.textState = 'visible';
    
    // Generate particles for special/epic celebrations
    if (level === CelebrationLevel.SPECIAL || level === CelebrationLevel.EPIC) {
      this.generateParticles(level === CelebrationLevel.EPIC ? 20 : 10);
    }
    
    // Auto-hide after duration
    const duration = this.getCelebrationDuration(level);
    setTimeout(() => {
      this.hideCelebration();
    }, duration);
  }

  /**
   * Hide celebration overlay
   */
  hideCelebration(): void {
    this.overlayState = 'hidden';
    
    setTimeout(() => {
      this.isActive = false;
      this.currentCelebration = null;
      this.particles = [];
    }, 300);
  }

  /**
   * Get celebration duration based on level
   */
  private getCelebrationDuration(level: CelebrationLevel): number {
    switch (level) {
      case CelebrationLevel.EPIC:
        return 8000;
      case CelebrationLevel.SPECIAL:
        return 5000;
      default:
        return 3000;
    }
  }

  /**
   * Generate particles for animation
   */
  private generateParticles(count: number): void {
    this.particles = [];
    for (let i = 0; i < count; i++) {
      this.particles.push({
        id: i,
        x: Math.random() * 100, // Percentage position
        y: Math.random() * 100,
        delay: Math.random() * 2000 // Random delay up to 2 seconds
      });
    }
  }

  /**
   * Get celebration icon based on level
   */
  getCelebrationIcon(): string {
    if (!this.currentCelebration) return 'emoji_events';
    
    switch (this.currentCelebration.level) {
      case CelebrationLevel.EPIC:
        return 'auto_awesome';
      case CelebrationLevel.SPECIAL:
        return 'star';
      default:
        return 'emoji_events';
    }
  }

  /**
   * Get celebration title based on level
   */
  getCelebrationTitle(): string {
    if (!this.currentCelebration) return '';
    
    switch (this.currentCelebration.level) {
      case CelebrationLevel.EPIC:
        return 'EPIC ACHIEVEMENT!';
      case CelebrationLevel.SPECIAL:
        return 'SPECIAL ACHIEVEMENT!';
      default:
        return 'ACHIEVEMENT UNLOCKED!';
    }
  }

  /**
   * Get celebration CSS class based on level
   */
  getCelebrationClass(): string {
    if (!this.currentCelebration) return '';
    
    return `celebration-${this.currentCelebration.level.toLowerCase()}`;
  }

  /**
   * Get achievement category color
   */
  getCategoryColor(): string {
    if (!this.currentCelebration) return '#2196f3';
    
    switch (this.currentCelebration.achievement.category) {
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
   * Format points display
   */
  formatPoints(): string {
    if (!this.currentCelebration) return '';
    
    const points = this.currentCelebration.achievement.points;
    return `+${points} points`;
  }

  /**
   * Close celebration manually
   */
  closeCelebration(): void {
    this.hideCelebration();
  }

  /**
   * Handle keyboard events
   */
  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && this.isActive) {
      this.closeCelebration();
    }
  }
}