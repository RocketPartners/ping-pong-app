import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { Subscription } from 'rxjs';
import { EasterEggService } from '../../../_services/easter-egg.service';
import { EasterEggStats, EggHunterLeaderboardDto } from '../../../_models/easter-egg';
import { AlertService } from '../../../_services/alert.services';

@Component({
  selector: 'app-easter-egg-stats',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatChipsModule,
    MatDividerModule
  ],
  templateUrl: './easter-egg-stats.component.html',
  styleUrls: ['./easter-egg-stats.component.scss']
})
export class EasterEggStatsComponent implements OnInit, OnDestroy {
  myStats: EasterEggStats | null = null;
  leaderboard: EggHunterLeaderboardDto[] = [];
  showLeaderboard: boolean = false;
  loadingLeaderboard: boolean = false;

  private subscriptions: Subscription = new Subscription();

  constructor(
    private easterEggService: EasterEggService,
    private alertService: AlertService
  ) {}

  ngOnInit(): void {
    // Subscribe to stats updates
    this.subscriptions.add(
      this.easterEggService.myStats$.subscribe(stats => {
        this.myStats = stats;
      })
    );

    // Initialize stats if not already loaded
    if (!this.easterEggService.myStatsValue) {
      this.easterEggService.refreshMyStats();
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  get canAccessSecretLeaderboard(): boolean {
    return this.myStats !== null && this.myStats.totalEggsFound > 0;
  }

  refreshStats(): void {
    this.easterEggService.refreshMyStats();
  }

  toggleLeaderboard(): void {
    this.showLeaderboard = !this.showLeaderboard;
    
    if (this.showLeaderboard && this.leaderboard.length === 0) {
      this.loadLeaderboard();
    }
  }

  private loadLeaderboard(): void {
    if (!this.canAccessSecretLeaderboard) return;

    this.loadingLeaderboard = true;
    
    this.easterEggService.getSecretLeaderboard(20).subscribe({
      next: (leaderboard) => {
        this.leaderboard = leaderboard;
        this.loadingLeaderboard = false;
      },
      error: (error) => {
        console.error('Error loading secret leaderboard:', error);
        this.loadingLeaderboard = false;
        this.alertService.error('Failed to load secret leaderboard');
      }
    });
  }
}