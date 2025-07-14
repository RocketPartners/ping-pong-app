import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-loading-spinner',
  template: `
    <div class="loading-container" 
         [class.overlay]="overlay" 
         [style.min-height.px]="minHeight">
      <div class="spinner-content" role="status" aria-live="polite">
        <mat-spinner [diameter]="diameter" [strokeWidth]="strokeWidth" [color]="color" aria-label="Loading"></mat-spinner>
        <div *ngIf="message" class="spinner-message" aria-live="polite">{{ message }}</div>
      </div>
    </div>
  `,
  styles: [`
    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 100px;
      padding: 20px;
      width: 100%;
    }

    .loading-container.overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-color: rgba(var(--surface-color-rgb, 255, 255, 255), 0.8);
      z-index: 100;
      backdrop-filter: blur(2px);
    }

    .spinner-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      animation: fadeIn 0.3s ease-in-out;
    }
    
    .spinner-message {
      margin-top: 12px;
      color: var(--text-color-secondary, rgba(0, 0, 0, 0.6));
      font-size: 14px;
      text-align: center;
      max-width: 250px;
    }
    
    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }
  `],
  standalone: false
})
export class LoadingSpinnerComponent {
  @Input() diameter: number = 40;
  @Input() strokeWidth: number = 4;
  @Input() color: 'primary' | 'accent' | 'warn' = 'primary';
  @Input() message: string = '';
  @Input() overlay: boolean = false;
  @Input() minHeight: number = 100;
}
