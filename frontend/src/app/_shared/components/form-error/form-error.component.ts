import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-form-error',
  template: `
    <div *ngIf="showError" class="form-error-container">
      <mat-icon *ngIf="showIcon">{{ icon }}</mat-icon>
      <span>{{ message }}</span>
    </div>
  `,
  styles: [`
    .form-error-container {
      background-color: #ffebee;
      color: #d32f2f;
      padding: 12px;
      border-radius: 4px;
      margin-bottom: 16px;
      display: flex;
      align-items: center;
    }

    mat-icon {
      margin-right: 8px;
      flex-shrink: 0;
    }

    span {
      flex: 1;
    }
  `],
  standalone: false
})
export class FormErrorComponent {
  @Input() message: string = '';
  @Input() showError: boolean = true;
  @Input() showIcon: boolean = true;
  @Input() icon: string = 'error_outline';
}
