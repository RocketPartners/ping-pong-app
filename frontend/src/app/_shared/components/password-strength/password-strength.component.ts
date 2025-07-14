import {Component, Input, OnChanges, SimpleChanges} from '@angular/core';

@Component({
  selector: 'app-password-strength',
  template: `
    <div class="password-strength-container" *ngIf="password">
      <div class="password-strength-label">Password Strength: {{ getStrengthLabel() }}</div>
      <div class="password-strength-meter">
        <div class="password-strength-meter-fill"
             [style.width.%]="strength"
             [ngClass]="strengthClass">
        </div>
      </div>
      <div class="password-feedback">{{ feedback }}</div>
    </div>
  `,
  styles: [`
    .password-strength-container {
      margin: 8px 0 16px;
    }

    .password-strength-label {
      font-size: 14px;
      margin-bottom: 4px;
      color: #666;
    }

    .password-strength-meter {
      height: 8px;
      background-color: #e0e0e0;
      border-radius: 4px;
      overflow: hidden;
      margin-bottom: 8px;
    }

    .password-strength-meter-fill {
      height: 100%;
      transition: width 0.3s ease-in-out;
    }

    .password-strength-meter-fill.weak {
      background-color: #f44336;
    }

    .password-strength-meter-fill.fair {
      background-color: #ff9800;
    }

    .password-strength-meter-fill.good {
      background-color: #2196f3;
    }

    .password-strength-meter-fill.strong {
      background-color: #4caf50;
    }

    .password-feedback {
      font-size: 12px;
      color: #666;
    }
  `],
  standalone: false
})
export class PasswordStrengthComponent implements OnChanges {
  @Input() password: string = '';

  strength: number = 0;
  strengthClass: string = '';
  feedback: string = '';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['password']) {
      this.calculateStrength();
    }
  }

  getStrengthLabel(): string {
    if (this.strength <= 25) return 'Weak';
    if (this.strength <= 50) return 'Fair';
    if (this.strength <= 75) return 'Good';
    return 'Strong';
  }

  private calculateStrength(): void {
    if (!this.password) {
      this.strength = 0;
      this.feedback = '';
      this.strengthClass = 'weak';
      return;
    }

    let strength = 0;

    // Length check
    if (this.password.length >= 8) {
      strength += 25;
    }

    // Character type checks
    if (/[A-Z]/.test(this.password)) {
      strength += 25;
    }

    if (/[0-9]/.test(this.password)) {
      strength += 25;
    }

    if (/[$@$!%*?&#^()_+\-=\[\]{};':"\\|,.<>\/?]/.test(this.password)) {
      strength += 25;
    }

    // Set class based on strength
    if (strength <= 25) {
      this.strengthClass = 'weak';
      this.feedback = 'Weak: Try a longer password with special characters';
    } else if (strength <= 50) {
      this.strengthClass = 'fair';
      this.feedback = 'Fair: Adding numbers or special characters would help';
    } else if (strength <= 75) {
      this.strengthClass = 'good';
      this.feedback = 'Good: Your password is reasonably strong';
    } else {
      this.strengthClass = 'strong';
      this.feedback = 'Strong: Your password meets all security criteria';
    }

    this.strength = strength;
  }
}
