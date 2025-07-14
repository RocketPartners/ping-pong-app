import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators} from '@angular/forms';
import {first} from 'rxjs/operators';

import {AccountService} from "../_services/account.service";
import {AlertService} from "../_services/alert.services";

@Component({
  templateUrl: 'register.component.html',
  styleUrls: ['register.component.scss'],
  standalone: false
})
export class RegisterComponent implements OnInit {
  form!: FormGroup;
  loading = false;
  submitted = false;
  hidePassword = true;
  hideRepeatPassword = true;

  // Password strength variables
  passwordStrength = 0;
  passwordFeedback = '';

  constructor(
    private formBuilder: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private accountService: AccountService,
    private alertService: AlertService
  ) {
    // redirect to home if already logged in
    if (this.accountService.playerValue) {
      this.router.navigate(['/']);
    }
  }

  // convenience getter for easy access to form fields
  get f() {
    return this.form.controls;
  }

  ngOnInit() {
    this.form = this.formBuilder.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      username: ['', [Validators.required, Validators.minLength(4), Validators.pattern('^[a-zA-Z0-9._-]*$')]],
      birthday: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [
        Validators.required,
        Validators.minLength(6),
        Validators.pattern('(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[$@$!%*?&])[A-Za-z\\d$@$!%*?&].{6,}')
      ]],
      repeatPassword: ['', Validators.required]
    }, {
      validators: this.passwordsMatch
    });

    // Monitor password changes for strength calculation
    this.form.get('password')?.valueChanges.subscribe(
      (password: string) => {
        this.checkPasswordStrength(password);
      }
    );
  }

  // Password match custom validator
  passwordsMatch(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password');
    const repeatPassword = control.get('repeatPassword');

    // Return null if controls haven't initialized yet or if there's no value
    if (!password || !repeatPassword) {
      return null;
    }

    // Return null if another validator has already found an error on the matchingControl
    if (repeatPassword.errors && !repeatPassword.errors.passwordMismatch) {
      return null;
    }

    // Return error if passwords don't match
    if (password.value !== repeatPassword.value) {
      repeatPassword.setErrors({passwordMismatch: true});
      return {passwordMismatch: true};
    } else {
      // Clear the error if the passwords match
      if (repeatPassword.errors) {
        // Keep any other existing errors
        const errors = {...repeatPassword.errors};
        delete errors.passwordMismatch;

        // Set remaining errors or null if no errors remain
        repeatPassword.setErrors(Object.keys(errors).length ? errors : null);
      }
      return null;
    }
  }

  // Calculate password strength
  checkPasswordStrength(password: string): void {
    if (!password) {
      this.passwordStrength = 0;
      this.passwordFeedback = '';
      return;
    }

    let strength = 0;
    let feedback = '';

    // Length check
    if (password.length >= 8) {
      strength += 25;
    }

    // Character type checks
    if (/[A-Z]/.test(password)) {
      strength += 25;
    }
    if (/[0-9]/.test(password)) {
      strength += 25;
    }
    if (/[$@$!%*?&#^()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) {
      strength += 25;
    }

    // Provide feedback based on strength
    if (strength <= 25) {
      feedback = 'Weak: Try a longer password with special characters';
    } else if (strength <= 50) {
      feedback = 'Fair: Adding numbers or special characters would help';
    } else if (strength <= 75) {
      feedback = 'Good: Your password is reasonably strong';
    } else {
      feedback = 'Strong: Your password meets all security criteria';
    }

    this.passwordStrength = strength;
    this.passwordFeedback = feedback;
  }

  onSubmit() {
    this.submitted = true;
    // reset alert on submit
    this.alertService.clear();

    // stop here if form is invalid
    if (this.form.invalid) {
      return;
    }

    this.loading = true;
    // Make sure the form value matches what the backend expects
    const formData = {
      ...this.form.value,
      matchingPassword: this.form.value.repeatPassword // rename to match backend expectations
    };

    this.accountService.register(formData)
      .pipe(first())
      .subscribe({
        next: () => {
          this.alertService.success('Registration successful', true);
          this.router.navigate(['/login'], {queryParams: {registered: true}});
        },
        error: error => {
          this.alertService.error(error);
          this.loading = false;
        }
      });
  }
}
