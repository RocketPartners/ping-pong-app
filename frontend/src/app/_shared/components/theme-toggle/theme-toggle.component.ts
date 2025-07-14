import {Component, OnDestroy, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {MatTooltipModule} from '@angular/material/tooltip';
import {ThemeMode, ThemeService} from '../../../_services/theme.service';
import {Subscription} from 'rxjs';

@Component({
  selector: 'app-theme-toggle',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, MatTooltipModule],
  template: `
    <button
      mat-icon-button
      [matTooltip]="currentTheme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'"
      aria-label="Toggle theme"
      (click)="toggleTheme()">
      <mat-icon>{{ currentTheme === 'dark' ? 'light_mode' : 'dark_mode' }}</mat-icon>
    </button>
  `,
  styles: [`
    button {
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      position: relative;
      overflow: hidden;
    }

    button:hover {
      transform: rotate(30deg);
      background-color: rgba(var(--primary-color-rgb), 0.1);
    }

    button:focus {
      outline: none;
      box-shadow: 0 0 0 2px rgba(var(--primary-color-rgb), 0.3);
    }

    button:active {
      transform: rotate(180deg);
    }

    mat-icon {
      transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }

    button:active mat-icon {
      transform: scale(0.9);
    }
  `]
})
export class ThemeToggleComponent implements OnInit, OnDestroy {
  currentTheme: ThemeMode = 'light';
  private subscription!: Subscription;

  constructor(private themeService: ThemeService) {
  }

  ngOnInit(): void {
    this.subscription = this.themeService.currentTheme$.subscribe(theme => {
      this.currentTheme = theme;
    });
  }
  
  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }
}
