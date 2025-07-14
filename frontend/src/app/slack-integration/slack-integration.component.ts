import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipsModule } from '@angular/material/chips';
import { MatListModule } from '@angular/material/list';
import { MatBadgeModule } from '@angular/material/badge';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { SlackIntegrationService, SlackConfig, SlackChallenge } from '../_services/slack-integration.service';
import { PlayerService } from '../_services/player.service';
import { Player } from '../_models/models';

@Component({
  selector: 'app-slack-integration',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatTabsModule,
    MatChipsModule,
    MatListModule,
    MatBadgeModule
  ],
  templateUrl: './slack-integration.component.html',
  styleUrls: ['./slack-integration.component.scss']
})
export class SlackIntegrationComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  configForm: FormGroup;
  config: SlackConfig = {
    enabled: false,
    autoPostMatches: true,
    autoPostAchievements: true,
    dailyDigest: true,
    weeklyDigest: true
  };
  
  challenges: SlackChallenge[] = [];
  players: Player[] = [];
  isTestingConnection = false;
  connectionTestResult: 'success' | 'error' | null = null;

  // Sample bot commands for display
  botCommands = [
    {
      command: '/challenge @user [singles|doubles] [ranked|normal]',
      description: 'Challenge another player to a match',
      example: '/challenge @john singles ranked'
    },
    {
      command: '/stats [@user]',
      description: 'View your stats or another player\'s stats',
      example: '/stats @maria'
    },
    {
      command: '/leaderboard [singles|doubles]',
      description: 'Show current leaderboard',
      example: '/leaderboard singles'
    },
    {
      command: '/recent [@user]',
      description: 'Show recent match history',
      example: '/recent @alex'
    }
  ];

  constructor(
    private fb: FormBuilder,
    private slackService: SlackIntegrationService,
    private playerService: PlayerService
  ) {
    this.createForm();
  }

  ngOnInit(): void {
    this.loadConfig();
    this.loadChallenges();
    this.loadPlayers();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private createForm(): void {
    this.configForm = this.fb.group({
      enabled: [false],
      botToken: ['', Validators.pattern(/^xoxb-.*/)],
      channelId: ['', Validators.pattern(/^C[A-Z0-9]{10}$/)],
      webhookUrl: ['', [Validators.pattern(/^https:\/\/hooks\.slack\.com\/services\/.*$/)]],
      autoPostMatches: [true],
      autoPostAchievements: [true],
      dailyDigest: [true],
      weeklyDigest: [true]
    });
  }

  private loadConfig(): void {
    this.slackService.getConfig()
      .pipe(takeUntil(this.destroy$))
      .subscribe(config => {
        this.config = config;
        this.configForm.patchValue(config);
      });
  }

  private loadChallenges(): void {
    this.slackService.challenges$
      .pipe(takeUntil(this.destroy$))
      .subscribe(challenges => {
        this.challenges = challenges;
      });
  }

  private loadPlayers(): void {
    this.playerService.getPlayers()
      .pipe(takeUntil(this.destroy$))
      .subscribe((players: Player[] | null) => {
        if (players) {
          this.players = players;
        }
      });
  }

  onSaveConfig(): void {
    if (this.configForm.valid) {
      const newConfig: SlackConfig = {
        ...this.config,
        ...this.configForm.value
      };
      
      this.slackService.updateConfig(newConfig).subscribe(success => {
        if (success) {
          this.config = newConfig;
        }
      });
    }
  }

  onTestConnection(): void {
    this.isTestingConnection = true;
    this.connectionTestResult = null;
    
    this.slackService.testConnection().subscribe(
      success => {
        this.isTestingConnection = false;
        this.connectionTestResult = success ? 'success' : 'error';
        
        // Clear result after 5 seconds
        setTimeout(() => {
          this.connectionTestResult = null;
        }, 5000);
      },
      error => {
        this.isTestingConnection = false;
        this.connectionTestResult = 'error';
        
        setTimeout(() => {
          this.connectionTestResult = null;
        }, 5000);
      }
    );
  }

  onAcceptChallenge(challengeId: string): void {
    this.slackService.acceptChallenge(challengeId).subscribe();
  }

  onDeclineChallenge(challengeId: string): void {
    this.slackService.declineChallenge(challengeId).subscribe();
  }

  onPostLeaderboard(): void {
    const topPlayers = this.players
      .sort((a, b) => b.singlesRankedRating - a.singlesRankedRating)
      .slice(0, 10);
    
    this.slackService.postLeaderboard(topPlayers).subscribe();
  }

  onSendDailyDigest(): void {
    const stats = {
      matchesPlayed: '12',
      mostActivePlayer: 'Alex Chen',
      highestRatedWin: 'Maria Rodriguez (1850 ELO)'
    };
    
    this.slackService.sendDailyDigest(stats).subscribe();
  }

  getPlayerByName(name: string): Player | undefined {
    return this.players.find(p => `${p.firstName} ${p.lastName}` === name);
  }

  getChallengeStatusColor(status: string): string {
    switch (status) {
      case 'pending': return 'orange';
      case 'accepted': return 'green';
      case 'declined': return 'red';
      case 'expired': return 'grey';
      default: return 'grey';
    }
  }

  getChallengeStatusIcon(status: string): string {
    switch (status) {
      case 'pending': return 'schedule';
      case 'accepted': return 'check_circle';
      case 'declined': return 'cancel';
      case 'expired': return 'access_time';
      default: return 'help';
    }
  }

  getGameTypeDisplay(gameType: string): string {
    switch (gameType) {
      case 'SINGLES_RANKED': return 'Singles Ranked';
      case 'DOUBLES_RANKED': return 'Doubles Ranked';
      case 'SINGLES_NORMAL': return 'Singles Normal';
      case 'DOUBLES_NORMAL': return 'Doubles Normal';
      default: return gameType;
    }
  }

  getTimeAgo(timestamp: Date): string {
    const now = new Date();
    const diff = now.getTime() - timestamp.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    
    if (days > 0) return `${days}d ago`;
    if (hours > 0) return `${hours}h ago`;
    if (minutes > 0) return `${minutes}m ago`;
    return 'Just now';
  }

  onGenerateSlackApp(): void {
    // This would open instructions for creating a Slack app
    window.open('https://api.slack.com/apps?new_app=1', '_blank');
  }
}