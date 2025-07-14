import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {PasswordResetService} from '../../_services/password-reset.service';
import {AlertService} from '../../_services/alert.services';

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss'],
  standalone: false
})
export class ForgotPasswordComponent implements OnInit {
  form!: FormGroup;
  loading = false;
  submitted = false;
  emailSent = false;
  errorMessage = '';

  constructor(
    private formBuilder: FormBuilder,
    private passwordResetService: PasswordResetService,
    private alertService: AlertService
  ) {
  }

  // Convenience getter for easy access to form fields
  get f() {
    return this.form.controls;
  }

  ngOnInit(): void {
    this.form = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.alertService.clear();

    // Stop here if form is invalid
    if (this.form.invalid) {
      return;
    }

    this.loading = true;
    this.passwordResetService.requestPasswordReset(this.f.email.value)
      .subscribe({
        next: (response) => {
          this.loading = false;
          this.emailSent = true;
          // Reset form but keep the email displayed
          this.submitted = false;
          // Note: We don't reset the form to keep the email visible to the user
        },
        error: (error) => {
          this.loading = false;
          // Don't expose specific errors for security reasons
          this.errorMessage = 'An error occurred. Please try again later.';
          // For debugging purposes only
          console.error('Password reset request error:', error);
        }
      });
  }
}
