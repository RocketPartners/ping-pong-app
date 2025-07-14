import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {PlayerUtilities} from '../../../player/player-utilities';

@Component({
  selector: 'app-win-rate-bar',
  template: `
    <div class="win-rate-container">
      <div class="win-rate-header" *ngIf="showLabels">
        <span class="win-rate-label">{{ label }}</span>
        <span class="win-rate-value" [ngClass]="winRateClass">{{ winRate }}%</span>
      </div>
      <div class="win-rate-bar">
        <div class="win-rate-progress"
             [style.width.%]="winRate"
             [ngClass]="winRateClass">
        </div>
      </div>
    </div>
  `,
  styles: [`
    .win-rate-container {
      width: 100%;
    }

    .win-rate-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 4px;
    }

    .win-rate-label {
      font-size: 12px;
      color: #666;
    }

    .win-rate-value {
      font-size: 12px;
      font-weight: bold;
    }

    .win-rate-bar {
      height: 6px;
      background-color: #e0e0e0;
      border-radius: 3px;
      overflow: hidden;
    }

    .win-rate-progress {
      height: 100%;
      border-radius: 3px;
      transition: width 0.3s ease;
    }

    .win-rate-struggling {
      color: #f44336;
    }

    .win-rate-struggling.win-rate-progress {
      background-color: #f44336;
    }

    .win-rate-poor {
      color: #ff9800;
    }

    .win-rate-poor.win-rate-progress {
      background-color: #ff9800;
    }

    .win-rate-average {
      color: #ffc107;
    }

    .win-rate-average.win-rate-progress {
      background-color: #ffc107;
    }

    .win-rate-good {
      color: #4caf50;
    }

    .win-rate-good.win-rate-progress {
      background-color: #4caf50;
    }

    .win-rate-excellent {
      color: #2196f3;
    }

    .win-rate-excellent.win-rate-progress {
      background-color: #2196f3;
    }
  `],
  standalone: false
})
export class WinRateBarComponent implements OnChanges {
  @Input() winRate: number = 0;
  @Input() label: string = 'Win Rate';
  @Input() showLabels: boolean = true;

  winRateClass: string = '';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['winRate']) {
      this.winRateClass = PlayerUtilities.getWinRateClass(this.winRate);
    }
  }
}
