import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';

// Material Form Controls
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatRadioModule} from '@angular/material/radio';
import {MatSliderModule} from '@angular/material/slider';
import {MatDatepickerModule} from '@angular/material/datepicker';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatAutocompleteModule} from '@angular/material/autocomplete';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';

// Custom Form Components
import {FormFieldComponent} from './components/form-field/form-field.component';
import {PasswordStrengthComponent} from './components/password-strength/password-strength.component';
import {FormErrorComponent} from './components/form-error/form-error.component';

/**
 * Form Controls Module
 *
 * Centralizes form controls, validators, and related components
 * to ensure consistent form handling across the application.
 */
@NgModule({
  declarations: [
    FormFieldComponent,
    PasswordStrengthComponent,
    FormErrorComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatRadioModule,
    MatSliderModule,
    MatDatepickerModule,
    MatButtonModule,
    MatIconModule,
    MatAutocompleteModule,
    MatSlideToggleModule
  ],
  exports: [
    // Re-export Angular modules
    ReactiveFormsModule,
    FormsModule,

    // Re-export Material modules
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatRadioModule,
    MatSliderModule,
    MatDatepickerModule,
    MatButtonModule,
    MatIconModule,
    MatAutocompleteModule,
    MatSlideToggleModule,

    // Export custom components
    FormFieldComponent,
    PasswordStrengthComponent,
    FormErrorComponent
  ]
})
export class FormControlsModule {
}
