import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-error-message',
  template: `
    <div class="error-message mat-elevation-z2" *ngIf="message">
      <mat-icon>{{ icon }}</mat-icon>
      <span>{{ message }}</span>
    </div>
  `,
  styles: [`
    .error-message {
      display: flex;
      align-items: center;
      background-color: #ffebee;
      color: #d32f2f;
      padding: 16px;
      border-radius: 4px;
      margin-bottom: 24px;
    }

    .error-message mat-icon {
      margin-right: 8px;
      flex-shrink: 0;
    }

    .error-message span {
      flex: 1;
    }
  `],
  standalone: false
})
export class ErrorMessageComponent {
  @Input() message: string = '';
  @Input() icon: string = 'error';
}
