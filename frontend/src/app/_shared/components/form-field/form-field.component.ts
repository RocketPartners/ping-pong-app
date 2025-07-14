import {Component, ContentChild, Input} from '@angular/core';
import {FormControl, NgControl} from '@angular/forms';

@Component({
  selector: 'app-form-field',
  template: `
    <mat-form-field appearance="outline" [class.full-width]="fullWidth">
      <mat-label>{{ label }}</mat-label>

      <ng-content></ng-content>

      <mat-icon *ngIf="icon" matPrefix>{{ icon }}</mat-icon>

      <mat-hint *ngIf="hint">{{ hint }}</mat-hint>

      <mat-error *ngIf="control && control.invalid && control.touched">
        <ng-container *ngFor="let error of getErrors()">
          {{ error }}
        </ng-container>
      </mat-error>
    </mat-form-field>
  `,
  styles: [`
    :host {
      display: block;
      margin-bottom: 16px;
    }

    .full-width {
      width: 100%;
    }

    mat-icon {
      margin-right: 8px;
    }
  `],
  standalone: false
})
export class FormFieldComponent {
  @Input() label: string = '';
  @Input() icon: string = '';
  @Input() hint: string = '';
  @Input() fullWidth: boolean = true;

  // Access the ngControl of the input inside this component
  @ContentChild(NgControl) control: NgControl | null = null;

  /**
   * Map of error messages for common validations
   */
  private errorMessages: Record<string, string> = {
    required: 'This field is required',
    email: 'Please enter a valid email address',
    minlength: 'Input is too short',
    maxlength: 'Input is too long',
    pattern: 'Input format is invalid',
    min: 'Value is too small',
    max: 'Value is too large',
    passwordMismatch: 'Passwords do not match'
  };

  /**
   * Get all active errors for the control
   */
  getErrors(): string[] {
    if (!this.control || !this.control.errors) {
      return [];
    }

    const errors: string[] = [];
    const formControl = this.control.control as FormControl;

    // Handle minlength and maxlength with dynamic values
    if (formControl.hasError('minlength')) {
      const minLength = formControl.getError('minlength').requiredLength;
      errors.push(`Minimum length is ${minLength} characters`);
      return errors;
    }

    if (formControl.hasError('maxlength')) {
      const maxLength = formControl.getError('maxlength').requiredLength;
      errors.push(`Maximum length is ${maxLength} characters`);
      return errors;
    }

    // Handle other errors
    Object.keys(this.control.errors).forEach(key => {
      if (this.errorMessages[key]) {
        errors.push(this.errorMessages[key]);
      }
    });

    return errors;
  }
}
