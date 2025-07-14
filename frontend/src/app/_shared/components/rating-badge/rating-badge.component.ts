import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';
import {PlayerUtilities} from '../../../player/player-utilities';

@Component({
  selector: 'app-rating-badge',
  template: `
    <div class="rating-badge" [ngClass]="ratingClass">
      {{ label || getRatingLabel() }}
    </div>
  `,
  styles: [`
    .rating-badge {
      padding: 4px 8px;
      border-radius: 16px;
      font-size: 12px;
      font-weight: 500;
      color: white;
      display: inline-block;
      text-align: center;
    }

    .rating-beginner {
      background-color: #78909c;
    }

    .rating-novice {
      background-color: #4caf50;
    }

    .rating-intermediate {
      background-color: #2196f3;
    }

    .rating-advanced {
      background-color: #9c27b0;
    }

    .rating-expert {
      background-color: #f9a825;
    }
  `],
  standalone: false
})
export class RatingBadgeComponent implements OnChanges {
  @Input() rating: number = 0;
  @Input() label: string = ''; // Optional custom label

  ratingClass: string = '';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['rating']) {
      this.ratingClass = PlayerUtilities.getRatingClass(this.rating);
    }
  }

  getRatingLabel(): string {
    return PlayerUtilities.getRatingLabel(this.rating);
  }
}
