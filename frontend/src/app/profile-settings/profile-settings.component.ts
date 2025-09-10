// In profile-settings.component.ts
import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {AccountService} from '../_services/account.service';
import {AlertService} from '../_services/alert.services';
import {ThemeService} from '../_services/theme.service';
import {Player} from '../_models/models';
import {first} from 'rxjs/operators';
import {Router} from '@angular/router';


@Component({
  selector: 'app-profile-settings',
  templateUrl: './profile-settings.component.html',
  styleUrls: ['./profile-settings.component.scss'],
  standalone: false
})
export class ProfileSettingsComponent implements OnInit {
  form!: FormGroup;
  loading = false;
  submitted = false;
  player: Player | undefined;
  useColorAsTheme = true; // Default to enabling personalized theme

  // Icon selection properties
  showIconPicker = false;
  selectedIcon: string | null = null;
  selectedColor: string | null = null;

  // Predefined icons and colors
  profileIcons = [
    'person', 'face', 'sports_tennis', 'sports', 'fitness_center',
    'emoji_events', 'stars', 'bolt', 'local_fire_department',
    'favorite', 'mood', 'sentiment_very_satisfied', 'psychology',
    'whatshot', 'brightness_7', 'flash_on', 'auto_awesome',
    'military_tech', 'emoji_objects', 'wb_sunny'
  ];

  colorOptions = [
    '#1976d2', '#e91e63', '#9c27b0', '#673ab7',
    '#3f51b5', '#2196f3', '#03a9f4', '#00bcd4',
    '#009688', '#4caf50', '#8bc34a', '#cddc39',
    '#ffeb3b', '#ffc107', '#ff9800', '#ff5722'
  ];

  constructor(
    private formBuilder: FormBuilder,
    private accountService: AccountService,
    private alertService: AlertService,
    private themeService: ThemeService,
    private router: Router
  ) {
    this.player = this.accountService.playerValue?.player;

    // Load the theme preference
    const savedPreference = localStorage.getItem('useProfileColorAsTheme');
    if (savedPreference !== null) {
      this.useColorAsTheme = savedPreference === 'true';
    }
  }

  // convenience getter for easy access to form fields
  get f() {
    return this.form.controls;
  }

  ngOnInit() {
    this.initializeForm();
    this.parseExistingProfileImage();
  }

  initializeForm() {
    if (!this.player) {
      return;
    }

    this.form = this.formBuilder.group({
      firstName: [this.player.firstName, [Validators.required, Validators.minLength(2)]],
      lastName: [this.player.lastName, [Validators.required, Validators.minLength(2)]],
      username: [this.player.username, [Validators.required, Validators.minLength(4), Validators.pattern('^[a-zA-Z0-9._-]*$')]],
      email: [this.player.email, [Validators.required, Validators.email]],
      birthday: [this.player.birthday ? new Date(this.player.birthday) : null, Validators.required],
      useProfileColorAsTheme: [this.useColorAsTheme],
      easterEggHuntingEnabled: [this.player.easterEggHuntingEnabled ?? true]
    });
  }

  // Parse existing profile image to extract icon and color
  parseExistingProfileImage() {
    if (this.player?.profileImage) {
      try {
        // Assuming profileImage is stored as "icon:color" format
        const parts = this.player.profileImage.split(':');
        if (parts.length === 2) {
          this.selectedIcon = parts[0];
          this.selectedColor = parts[1];

          // Apply the custom color if theme personalization is enabled
          if (this.useColorAsTheme && this.selectedColor) {
            this.themeService.setCustomPrimaryColor(this.selectedColor);
          }
        }
      } catch (e) {
        console.error('Error parsing profile image', e);
      }
    }
  }

  toggleIconPicker() {
    this.showIconPicker = !this.showIconPicker;
  }

  selectIcon(icon: string) {
    this.selectedIcon = icon;
    // If no color is selected yet, select the first one
    if (!this.selectedColor) {
      this.selectedColor = this.colorOptions[0];
    }
  }

  selectColor(color: string) {
    this.selectedColor = color;

    // Apply color immediately as a preview if theme personalization is enabled
    if (this.form.get('useProfileColorAsTheme')?.value) {
      this.themeService.setCustomPrimaryColor(color);
    }
  }

  clearProfileSelection() {
    this.selectedIcon = null;
    this.selectedColor = null;

    // Reset to default theme color
    this.themeService.resetCustomColor();
  }

  toggleUseColorAsTheme() {
    const newValue = !this.useColorAsTheme;
    this.useColorAsTheme = newValue;
    this.form.get('useProfileColorAsTheme')?.setValue(newValue);

    // Apply or remove custom color based on the toggle
    if (newValue && this.selectedColor) {
      this.themeService.setCustomPrimaryColor(this.selectedColor);
    } else {
      this.themeService.resetCustomColor();
    }

    // Save preference
    localStorage.setItem('useProfileColorAsTheme', newValue.toString());
  }

  onSubmit() {
    this.submitted = true;
    this.alertService.clear();

    // stop here if form is invalid
    if (this.form.invalid) {
      return;
    }

    this.loading = true;

    // Save the theme personalization preference
    this.useColorAsTheme = this.form.get('useProfileColorAsTheme')?.value || false;
    localStorage.setItem('useProfileColorAsTheme', this.useColorAsTheme.toString());

    // Create the profile image string (icon:color format)
    const profileImage = this.selectedIcon && this.selectedColor
      ? `${this.selectedIcon}:${this.selectedColor}`
      : null;

    // Create updated player object (excluding the theme preference field)
    const {useProfileColorAsTheme, ...formValues} = this.form.value;
    const updatedPlayer = {
      ...this.player,
      ...formValues,
      profileImage
    };

    // Apply or remove custom color based on preference
    if (this.useColorAsTheme && this.selectedColor) {
      this.themeService.setCustomPrimaryColor(this.selectedColor);
    } else {
      this.themeService.resetCustomColor();
    }

    // Update the profile
    this.updateProfile(updatedPlayer);
  }

  private updateProfile(updatedPlayer: Player) {
    this.accountService.updateProfile(updatedPlayer)
      .pipe(first())
      .subscribe({
        next: () => {
          this.alertService.success('Profile updated successfully', true);
          this.loading = false;
          this.router.navigate(['/home']);
        },
        error: (error) => {
          this.alertService.error('Failed to update profile. ' + error);
          this.loading = false;
        }
      });
  }
}
