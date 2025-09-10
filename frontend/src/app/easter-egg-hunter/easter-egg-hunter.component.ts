import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { EasterEggStatsComponent } from '../_shared/components/easter-egg-stats/easter-egg-stats.component';

@Component({
  selector: 'app-easter-egg-hunter',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    EasterEggStatsComponent
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>
          <mat-icon>egg_alt</mat-icon>
          Easter Egg Hunter
        </h1>
        <p class="page-subtitle">Track your egg hunting progress and compete on the secret leaderboard!</p>
      </div>

      <!-- Main Stats Card - Full Width -->
      <div class="stats-section">
        <app-easter-egg-stats></app-easter-egg-stats>
      </div>

      <div class="content-grid">
        <!-- How to Hunt Section -->
        <mat-card class="info-card">
          <mat-card-header>
            <mat-card-title>
              <mat-icon>help_outline</mat-icon>
              How to Hunt Easter Eggs
            </mat-card-title>
          </mat-card-header>
          
          <mat-card-content>
            <div class="hunt-instructions">
              <div class="instruction-item">
                <mat-icon class="instruction-icon">visibility</mat-icon>
                <div class="instruction-text">
                  <strong>Keep Your Eyes Peeled</strong>
                  <p>Easter eggs randomly appear throughout the site as clickable icons.</p>
                </div>
              </div>

              <div class="instruction-item">
                <mat-icon class="instruction-icon">mouse</mat-icon>
                <div class="instruction-text">
                  <strong>Click to Claim</strong>
                  <p>Click on any egg you find to claim points and add it to your collection.</p>
                </div>
              </div>

              <div class="instruction-item">
                <mat-icon class="instruction-icon">speed</mat-icon>
                <div class="instruction-text">
                  <strong>Act Fast</strong>
                  <p>Eggs are shared across all users - once someone claims it, it's gone!</p>
                </div>
              </div>

              <div class="instruction-item">
                <mat-icon class="instruction-icon">stars</mat-icon>
                <div class="instruction-text">
                  <strong>Egg Rarity & Points</strong>
                  <p>Common (5) • Uncommon (10) • Rare (25) • Epic (50) • Legendary (100) • Mythical (250)</p>
                </div>
              </div>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Current Hunt Status -->
        <mat-card class="status-card">
          <mat-card-header>
            <mat-card-title>
              <mat-icon>search</mat-icon>
              Hunt Status & Tips
            </mat-card-title>
          </mat-card-header>
          
          <mat-card-content>
            <div class="hunt-status">
              <div class="status-indicator">
                <div class="status-dot active"></div>
                <span>Hunting System Active</span>
              </div>
              
              <div class="hunt-tips">
                <h4>💡 Pro Hunter Tips</h4>
                <ul>
                  <li>Eggs spawn automatically every few minutes</li>
                  <li>Check different pages - eggs appear site-wide</li>
                  <li>Rare eggs are more likely on certain pages</li>
                  <li>Look for subtle animations and glowing effects</li>
                  <li>First egg found unlocks the secret leaderboard!</li>
                </ul>
              </div>

              <div class="current-hunt-info">
                <mat-icon>schedule</mat-icon>
                <span>New eggs spawn regularly - keep exploring!</span>
              </div>
            </div>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .page-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 20px;
    }

    .page-header {
      text-align: center;
      margin-bottom: 30px;
    }

    .page-header h1 {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      color: #333;
      margin-bottom: 10px;
    }

    .page-header h1 mat-icon {
      font-size: 36px;
      width: 36px;
      height: 36px;
      color: #ff9800;
    }

    .page-subtitle {
      color: #666;
      font-size: 1.1em;
      margin: 0;
    }

    .stats-section {
      margin-bottom: 30px;
    }

    .content-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 24px;
      align-items: start;
    }

    @media (max-width: 968px) {
      .content-grid {
        grid-template-columns: 1fr;
        gap: 20px;
      }
    }

    .info-card,
    .status-card {
      height: fit-content;
    }

    .hunt-instructions {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .instruction-item {
      display: flex;
      align-items: flex-start;
      gap: 15px;
    }

    .instruction-icon {
      color: #1976d2;
      margin-top: 2px;
    }

    .instruction-text strong {
      color: #333;
      display: block;
      margin-bottom: 5px;
    }

    .instruction-text p {
      margin: 0;
      color: #666;
      font-size: 0.9em;
      line-height: 1.4;
    }

    .hunt-status {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .status-indicator {
      display: flex;
      align-items: center;
      gap: 10px;
      font-weight: 500;
    }

    .status-dot {
      width: 12px;
      height: 12px;
      border-radius: 50%;
    }

    .status-dot.active {
      background-color: #4caf50;
      animation: pulse 2s ease-in-out infinite;
    }

    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.5; }
    }

    .hunt-tips {
      background: #f5f5f5;
      padding: 16px;
      border-radius: 8px;
      margin: 0;
    }

    .hunt-tips h4 {
      margin: 0 0 12px 0;
      color: #333;
      font-size: 1em;
      font-weight: 600;
    }

    .hunt-tips ul {
      margin: 0;
      padding-left: 20px;
      color: #666;
      line-height: 1.6;
    }

    .hunt-tips li {
      margin-bottom: 8px;
    }

    .hunt-tips li:last-child {
      margin-bottom: 0;
    }

    .current-hunt-info {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #666;
      font-style: italic;
    }

    .current-hunt-info mat-icon {
      color: #ff9800;
    }
  `]
})
export class EasterEggHunterComponent {
}