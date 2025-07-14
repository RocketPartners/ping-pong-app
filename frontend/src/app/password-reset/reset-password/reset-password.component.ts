import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {PasswordResetService} from '../../_services/password-reset.service';
import {AlertService} from '../../_services/alert.services';
import {passwordMatchValidator} from "../../_helpers/password-match.validator";

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss'],
  standalone: false
})
export class ResetPasswordComponent implements OnInit {
  form!: FormGroup;
  token: string = '';
  loading = false;
  submitted = false;
  tokenValidating = true; // Start with validating state
  invalidToken = false;
  resetComplete = false;
  errorMessage = '';
  hidePassword = true;
  hideConfirmPassword = true;

  constructor(
    private formBuilder: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private passwordResetService: PasswordResetService,
    private alertService: AlertService
  ) {
  }

  // Convenience getter for easy access to form fields
  get f() {
    return this.form.controls;
  }

  // Password match status for UI indicator
  get passwordsMatch(): boolean {
    const password = this.form?.get('password')?.value;
    const confirmPassword = this.form?.get('confirmPassword')?.value;
    return password && confirmPassword && password === confirmPassword;
  }

  ngOnInit(): void {
    // Create form first in case token is invalid
    this.form = this.formBuilder.group({
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern('(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[$@$!%*?&#])[A-Za-z\\d$@$!%*?&#].{8,}')
      ]],
      confirmPassword: ['', Validators.required]
    }, {
      validators: passwordMatchValidator
    });

    // Password match validation on input changes, not just submit
    this.form.get('confirmPassword')?.valueChanges.subscribe(() => {
      if (this.form.get('password')?.value) {
        this.form.updateValueAndValidity();
      }
    });

    this.form.get('password')?.valueChanges.subscribe(() => {
      if (this.form.get('confirmPassword')?.value) {
        this.form.updateValueAndValidity();
      }
    });

    // Get token from route parameters
    this.route.queryParams.subscribe(params => {
      this.token = params['token'] || '';

      if (!this.token) {
        this.tokenValidating = false;
        this.invalidToken = true;
        return;
      }

      // Validate token with backend
      this.validateToken();
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
    this.passwordResetService.resetPassword(
      this.token,
      this.f.password.value,
      this.f.confirmPassword.value
    ).subscribe({
      next: (response) => {
        this.loading = false;
        this.resetComplete = true;
        this.alertService.success('Your password has been reset successfully. You can now log in with your new password.');
      },
      error: (error) => {
        this.loading = false;
        // More specific error handling based on backend responses
        if (error.includes('expired') || error.includes('invalid')) {
          this.errorMessage = 'This password reset link has expired or is invalid. Please request a new one.';
        } else if (error.includes('match')) {
          this.errorMessage = 'Passwords do not match. Please try again.';
        } else {
          this.errorMessage = 'An error occurred during password reset. Please try again.';
        }
        console.error('Password reset error:', error);
      }
    });
  }

  validateToken(): void {
    this.tokenValidating = true;
    this.invalidToken = false;

    // Add debug information
    console.log('Validating token:', this.token);

    // Try direct validation first
    this.passwordResetService.validateTokenDirect(this.token)
      .subscribe({
        next: (response) => {
          console.log('Direct token validation response:', response);
          this.checkValidationResponse(response);
        },
        error: (error) => {
          console.error('Direct token validation error:', error);

          // Fallback to the original validation method
          this.passwordResetService.validateToken(this.token)
            .subscribe({
              next: (response) => {
                console.log('Standard token validation response:', response);
                this.checkValidationResponse(response);
              },
              error: (error) => {
                console.error('Standard token validation error:', error);
                this.tokenValidating = false;
                this.invalidToken = true;
              }
            });
        }
      });
  }

  private checkValidationResponse(response: any): void {
    this.tokenValidating = false;
    if (!response || !response.success) {
      this.invalidToken = true;
    }
  }
}
