import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { trigger, state, style, transition, animate } from '@angular/animations';
import { AchievementCategory, CelebrationLevel } from '../../../_models/achievement';

@Component({
  selector: 'app-achievement-progress',
  templateUrl: './achievement-progress.component.html',
  styleUrls: ['./achievement-progress.component.scss'],
  standalone: false,
  animations: [
    trigger('progressAnimation', [
      state('initial', style({ width: '0%' })),
      state('progress', style({ width: '{{ progressWidth }}%' }), { params: { progressWidth: 0 } }),
      transition('initial => progress', animate('1.5s ease-out'))
    ]),
    trigger('completionPulse', [
      state('incomplete', style({ transform: 'scale(1)', opacity: 1 })),
      state('complete', style({ transform: 'scale(1.05)', opacity: 1 })),
      transition('incomplete => complete', [
        animate('0.3s ease-out', style({ transform: 'scale(1.1)', opacity: 0.8 })),
        animate('0.3s ease-in', style({ transform: 'scale(1.05)', opacity: 1 }))
      ])
    ])
  ]
})
export class AchievementProgressComponent implements OnInit, OnChanges {
  @Input() progress: number = 0;
  @Input() category: AchievementCategory = AchievementCategory.EASY;
  @Input() celebrationLevel: CelebrationLevel = CelebrationLevel.NORMAL;
  @Input() isAchieved: boolean = false;
  @Input() animated: boolean = true;
  @Input() showText: boolean = true;
  @Input() size: 'small' | 'medium' | 'large' = 'medium';

  animationState = 'initial';
  completionState = 'incomplete';
  
  // Progress bar styling
  progressBarClass = '';
  progressBarHeight = 6;
  
  ngOnInit(): void {
    this.setProgressBarStyling();
    this.initializeAnimation();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['progress'] || changes['isAchieved']) {
      this.updateAnimationStates();
    }
    if (changes['category'] || changes['size']) {
      this.setProgressBarStyling();
    }
  }

  private initializeAnimation(): void {
    if (this.animated) {
      // Delay animation start slightly for better visual effect
      setTimeout(() => {
        this.animationState = 'progress';
      }, 100);
    } else {
      this.animationState = 'progress';
    }
  }

  private updateAnimationStates(): void {
    if (this.isAchieved && this.completionState === 'incomplete') {
      this.completionState = 'complete';
    } else if (!this.isAchieved && this.completionState === 'complete') {
      this.completionState = 'incomplete';
    }
  }

  private setProgressBarStyling(): void {
    // Set category-based styling
    switch (this.category) {
      case AchievementCategory.EASY:
        this.progressBarClass = 'progress-easy';
        break;
      case AchievementCategory.MEDIUM:
        this.progressBarClass = 'progress-medium';
        break;
      case AchievementCategory.HARD:
        this.progressBarClass = 'progress-hard';
        break;
      case AchievementCategory.LEGENDARY:
        this.progressBarClass = 'progress-legendary';
        break;
    }

    // Set size-based styling
    switch (this.size) {
      case 'small':
        this.progressBarHeight = 4;
        break;
      case 'large':
        this.progressBarHeight = 10;
        break;
      default:
        this.progressBarHeight = 6;
        break;
    }

    // Add celebration styling
    if (this.isAchieved && this.celebrationLevel === CelebrationLevel.EPIC) {
      this.progressBarClass += ' progress-epic';
    } else if (this.isAchieved && this.celebrationLevel === CelebrationLevel.SPECIAL) {
      this.progressBarClass += ' progress-special';
    }
  }

  /**
   * Get progress text based on achievement status
   */
  getProgressText(): string {
    if (this.isAchieved) {
      switch (this.celebrationLevel) {
        case CelebrationLevel.EPIC:
          return 'Epic Achievement!';
        case CelebrationLevel.SPECIAL:
          return 'Special Achievement!';
        default:
          return 'Complete!';
      }
    }
    
    return `${Math.round(this.progress)}% Complete`;
  }

  /**
   * Get progress text CSS class
   */
  getProgressTextClass(): string {
    let baseClass = 'progress-text';
    
    if (this.isAchieved) {
      switch (this.celebrationLevel) {
        case CelebrationLevel.EPIC:
          return baseClass + ' text-epic';
        case CelebrationLevel.SPECIAL:
          return baseClass + ' text-special';
        default:
          return baseClass + ' text-complete';
      }
    }
    
    return baseClass;
  }
}