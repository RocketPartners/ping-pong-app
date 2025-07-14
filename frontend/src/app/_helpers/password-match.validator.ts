import {AbstractControl, ValidationErrors, ValidatorFn} from '@angular/forms';

/**
 * Custom validator for password matching
 * Use with form group to validate that password and confirmPassword fields match
 */
export const passwordMatchValidator: ValidatorFn = (
  control: AbstractControl
): ValidationErrors | null => {
  const password = control.get('password');
  const confirmPassword = control.get('confirmPassword');

  // Return null if controls haven't initialized or if no values
  if (!password || !confirmPassword || !password.value || !confirmPassword.value) {
    return null;
  }

  // Return null if another validator has already found an error
  if (confirmPassword.errors && !confirmPassword.errors['passwordMismatch']) {
    return null;
  }

  // Check if passwords match
  const match = password.value === confirmPassword.value;

  // Set error on confirmPassword if they don't match
  if (!match) {
    confirmPassword.setErrors({passwordMismatch: true});
    return {passwordMismatch: true};
  } else {
    // Clear the passwordMismatch error but keep other errors if any
    if (confirmPassword.errors) {
      const errors = {...confirmPassword.errors};
      delete errors['passwordMismatch'];
      confirmPassword.setErrors(Object.keys(errors).length ? errors : null);
    }
    return null;
  }
};
