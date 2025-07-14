import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { BaseHttpService } from './base-http.service';
import { AlertService } from './alert.services';
import { Player, Game, GameType } from '../_models/models';
import { catchError, map } from 'rxjs/operators';

export interface SlackConfig {
  enabled: boolean;
  botToken?: string;
  channelId?: string;
  webhookUrl?: string;
  autoPostMatches: boolean;
  autoPostAchievements: boolean;
  dailyDigest: boolean;
  weeklyDigest: boolean;
}

export interface SlackChallenge {
  id: string;
  challengerId: string;
  challengedId: string;
  challengerName: string;
  challengedName: string;
  gameType: GameType;
  message?: string;
  timestamp: Date;
  status: 'pending' | 'accepted' | 'declined' | 'expired';
  slackMessageId?: string;
}

export interface SlackMessage {
  text: string;
  attachments?: SlackAttachment[];
  blocks?: any[];
  channel?: string;
}

export interface SlackAttachment {
  color: string;
  title: string;
  title_link?: string;
  text: string;
  fields: SlackField[];
  footer?: string;
  footer_icon?: string;
  ts?: number;
}

export interface SlackField {
  title: string;
  value: string;
  short: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class SlackIntegrationService extends BaseHttpService {
  private configSubject = new BehaviorSubject<SlackConfig>({
    enabled: false,
    autoPostMatches: true,
    autoPostAchievements: true,
    dailyDigest: true,
    weeklyDigest: true
  });

  private challengesSubject = new BehaviorSubject<SlackChallenge[]>([]);
  
  public config$ = this.configSubject.asObservable();
  public challenges$ = this.challengesSubject.asObservable();

  // Demo data for testing
  private demoConfig: SlackConfig = {
    enabled: true,
    botToken: 'xoxb-demo-token',
    channelId: 'C1234567890',
    webhookUrl: 'https://hooks.slack.com/services/demo/webhook',
    autoPostMatches: true,
    autoPostAchievements: true,
    dailyDigest: true,
    weeklyDigest: true
  };

  private demoChallenges: SlackChallenge[] = [
    {
      id: '1',
      challengerId: '1',
      challengedId: '2',
      challengerName: 'Alex Chen',
      challengedName: 'Maria Rodriguez',
      gameType: GameType.SINGLES_RANKED,
      message: 'Ready for a ranked singles match?',
      timestamp: new Date(Date.now() - 30 * 60000),
      status: 'pending'
    },
    {
      id: '2',
      challengerId: '3',
      challengedId: '4',
      challengerName: 'John Smith',
      challengedName: 'Sarah Johnson',
      gameType: GameType.DOUBLES_NORMAL,
      message: 'Doubles match at 3pm?',
      timestamp: new Date(Date.now() - 60 * 60000),
      status: 'accepted'
    }
  ];

  constructor(http: HttpClient, alertService?: AlertService) {
    super(http, alertService);
    this.initializeDemoData();
  }

  private initializeDemoData(): void {
    this.configSubject.next(this.demoConfig);
    this.challengesSubject.next(this.demoChallenges);
  }

  // Configuration methods
  getConfig(): Observable<SlackConfig> {
    return this.config$;
  }

  updateConfig(config: SlackConfig): Observable<boolean> {
    this.configSubject.next(config);
    return of(true);
  }

  testConnection(): Observable<boolean> {
    const config = this.configSubject.value;
    if (!config.enabled || !config.webhookUrl) {
      return of(false);
    }

    const testMessage: SlackMessage = {
      text: '🏓 Ping Pong League connection test successful!',
      attachments: [{
        color: '#36a64f',
        title: 'Connection Test',
        text: 'Your Slack integration is working correctly.',
        fields: [
          { title: 'Status', value: 'Connected', short: true },
          { title: 'Time', value: new Date().toLocaleString(), short: true }
        ]
      }]
    };

    return this.sendSlackMessage(testMessage);
  }

  // Challenge methods
  sendChallenge(challengerId: string, challengedId: string, challengerName: string, challengedName: string, gameType: GameType, message?: string): Observable<boolean> {
    const challenge: SlackChallenge = {
      id: Date.now().toString(),
      challengerId,
      challengedId,
      challengerName,
      challengedName,
      gameType,
      message,
      timestamp: new Date(),
      status: 'pending'
    };

    const challenges = this.challengesSubject.value;
    this.challengesSubject.next([...challenges, challenge]);

    const slackMessage = this.buildChallengeMessage(challenge);
    return this.sendSlackMessage(slackMessage);
  }

  acceptChallenge(challengeId: string): Observable<boolean> {
    const challenges = this.challengesSubject.value;
    const challenge = challenges.find(c => c.id === challengeId);
    
    if (challenge) {
      challenge.status = 'accepted';
      this.challengesSubject.next([...challenges]);
      
      const acceptMessage: SlackMessage = {
        text: `✅ Challenge accepted!`,
        attachments: [{
          color: '#36a64f',
          title: '🏓 Match Confirmed',
          text: `${challenge.challengedName} accepted ${challenge.challengerName}'s challenge for ${this.getGameTypeDisplay(challenge.gameType)}!`,
          fields: [
            { title: 'Players', value: `${challenge.challengerName} vs ${challenge.challengedName}`, short: false },
            { title: 'Game Type', value: this.getGameTypeDisplay(challenge.gameType), short: true },
            { title: 'Status', value: 'Ready to play!', short: true }
          ],
          footer: 'Ping Pong League',
          ts: Math.floor(Date.now() / 1000)
        }]
      };

      return this.sendSlackMessage(acceptMessage);
    }

    return of(false);
  }

  declineChallenge(challengeId: string): Observable<boolean> {
    const challenges = this.challengesSubject.value;
    const challenge = challenges.find(c => c.id === challengeId);
    
    if (challenge) {
      challenge.status = 'declined';
      this.challengesSubject.next([...challenges]);
      
      const declineMessage: SlackMessage = {
        text: `❌ Challenge declined`,
        attachments: [{
          color: '#ff0000',
          title: '🏓 Challenge Declined',
          text: `${challenge.challengedName} declined ${challenge.challengerName}'s challenge.`,
          fields: [
            { title: 'Challenge', value: `${this.getGameTypeDisplay(challenge.gameType)}`, short: true },
            { title: 'Status', value: 'Declined', short: true }
          ],
          footer: 'Ping Pong League',
          ts: Math.floor(Date.now() / 1000)
        }]
      };

      return this.sendSlackMessage(declineMessage);
    }

    return of(false);
  }

  // Match result posting
  postMatchResult(game: Game, winner: Player, loser: Player): Observable<boolean> {
    const config = this.configSubject.value;
    if (!config.enabled || !config.autoPostMatches) {
      return of(false);
    }

    const gameTypeDisplay = this.getGameTypeDisplay(game.singlesGame ? 
      (game.ratedGame ? GameType.SINGLES_RANKED : GameType.SINGLES_NORMAL) :
      (game.ratedGame ? GameType.DOUBLES_RANKED : GameType.DOUBLES_NORMAL));

    const slackMessage: SlackMessage = {
      text: `🏆 Match Result`,
      attachments: [{
        color: '#2eb886',
        title: '🏓 Game Complete',
        text: `${winner.firstName} ${winner.lastName} defeated ${loser.firstName} ${loser.lastName}!`,
        fields: [
          { title: 'Winner', value: `${winner.firstName} ${winner.lastName}`, short: true },
          { title: 'Score', value: `${game.challengerTeamScore} - ${game.opponentTeamScore}`, short: true },
          { title: 'Game Type', value: gameTypeDisplay, short: true },
          { title: 'Date', value: new Date().toLocaleDateString(), short: true }
        ],
        footer: 'Ping Pong League',
        ts: Math.floor(Date.now() / 1000)
      }]
    };

    return this.sendSlackMessage(slackMessage);
  }

  // Stats and leaderboard posting
  postLeaderboard(topPlayers: Player[]): Observable<boolean> {
    const leaderboardText = topPlayers.slice(0, 5).map((player, index) => 
      `${index + 1}. ${player.firstName} ${player.lastName} (${player.singlesRankedRating})`
    ).join('\n');

    const slackMessage: SlackMessage = {
      text: `📊 Current Leaderboard`,
      attachments: [{
        color: '#f2c744',
        title: '🏆 Top 5 Players',
        text: leaderboardText,
        fields: [
          { title: 'Rankings', value: 'Singles Ranked ELO', short: true },
          { title: 'Updated', value: new Date().toLocaleDateString(), short: true }
        ],
        footer: 'Ping Pong League',
        ts: Math.floor(Date.now() / 1000)
      }]
    };

    return this.sendSlackMessage(slackMessage);
  }

  postPlayerStats(player: Player): Observable<boolean> {
    const winRate = Math.round((player.singlesRankedWins / (player.singlesRankedWins + player.singlesRankedLoses)) * 100);
    
    const slackMessage: SlackMessage = {
      text: `📈 Player Stats for ${player.firstName} ${player.lastName}`,
      attachments: [{
        color: '#36a64f',
        title: `🏓 ${player.firstName} ${player.lastName}'s Statistics`,
        text: `Statistics for ${player.firstName} ${player.lastName}`,
        fields: [
          { title: 'Singles Ranked Rating', value: player.singlesRankedRating.toString(), short: true },
          { title: 'Win Rate', value: `${winRate}%`, short: true },
          { title: 'Total Wins', value: player.singlesRankedWins.toString(), short: true },
          { title: 'Total Losses', value: player.singlesRankedLoses.toString(), short: true }
        ],
        footer: 'Ping Pong League',
        ts: Math.floor(Date.now() / 1000)
      }]
    };

    return this.sendSlackMessage(slackMessage);
  }

  // Achievement notifications
  postAchievement(player: Player, achievementName: string, achievementDescription: string): Observable<boolean> {
    const config = this.configSubject.value;
    if (!config.enabled || !config.autoPostAchievements) {
      return of(false);
    }

    const slackMessage: SlackMessage = {
      text: `🎉 New Achievement Unlocked!`,
      attachments: [{
        color: '#ff6b35',
        title: `🏆 ${achievementName}`,
        text: `${player.firstName} ${player.lastName} just earned a new achievement!`,
        fields: [
          { title: 'Player', value: `${player.firstName} ${player.lastName}`, short: true },
          { title: 'Achievement', value: achievementName, short: true },
          { title: 'Description', value: achievementDescription, short: false }
        ],
        footer: 'Ping Pong League',
        ts: Math.floor(Date.now() / 1000)
      }]
    };

    return this.sendSlackMessage(slackMessage);
  }

  // Daily/Weekly digests
  sendDailyDigest(stats: any): Observable<boolean> {
    const config = this.configSubject.value;
    if (!config.enabled || !config.dailyDigest) {
      return of(false);
    }

    const slackMessage: SlackMessage = {
      text: `📅 Daily Ping Pong Digest`,
      attachments: [{
        color: '#2eb886',
        title: `🏓 Daily Summary - ${new Date().toLocaleDateString()}`,
        text: `Daily ping pong activity summary`,
        fields: [
          { title: 'Matches Played', value: stats.matchesPlayed || '0', short: true },
          { title: 'Most Active Player', value: stats.mostActivePlayer || 'N/A', short: true },
          { title: 'Highest Rated Win', value: stats.highestRatedWin || 'N/A', short: false }
        ],
        footer: 'Ping Pong League Daily Digest',
        ts: Math.floor(Date.now() / 1000)
      }]
    };

    return this.sendSlackMessage(slackMessage);
  }

  // Private helper methods
  private sendSlackMessage(message: SlackMessage): Observable<boolean> {
    const config = this.configSubject.value;
    
    // For demo purposes, just simulate success
    console.log('Sending Slack message:', message);
    
    if (!config.enabled) {
      return of(false);
    }

    // In a real implementation, this would make an HTTP request to Slack
    return of(true).pipe(
      map(() => {
        if (this.alertService) {
          this.alertService.success('Message sent to Slack successfully!');
        }
        return true;
      }),
      catchError(error => {
        console.error('Failed to send Slack message:', error);
        if (this.alertService) {
          this.alertService.error('Failed to send message to Slack');
        }
        return of(false);
      })
    );
  }

  private buildChallengeMessage(challenge: SlackChallenge): SlackMessage {
    return {
      text: `⚔️ New Ping Pong Challenge!`,
      attachments: [{
        color: '#f2c744',
        title: '🏓 Challenge Request',
        text: `${challenge.challengerName} has challenged ${challenge.challengedName} to a ${this.getGameTypeDisplay(challenge.gameType)} match!`,
        fields: [
          { title: 'Challenger', value: challenge.challengerName, short: true },
          { title: 'Challenged', value: challenge.challengedName, short: true },
          { title: 'Game Type', value: this.getGameTypeDisplay(challenge.gameType), short: true },
          { title: 'Message', value: challenge.message || 'No message', short: false }
        ],
        footer: 'Ping Pong League',
        ts: Math.floor(challenge.timestamp.getTime() / 1000)
      }]
    };
  }

  private getGameTypeDisplay(gameType: GameType): string {
    switch (gameType) {
      case GameType.SINGLES_RANKED: return 'Singles Ranked';
      case GameType.DOUBLES_RANKED: return 'Doubles Ranked';
      case GameType.SINGLES_NORMAL: return 'Singles Normal';
      case GameType.DOUBLES_NORMAL: return 'Doubles Normal';
      default: return 'Unknown';
    }
  }

  // Bot command handlers (would be implemented on backend)
  handleSlashCommand(command: string, userId: string, parameters: string[]): string {
    switch (command) {
      case '/challenge':
        return 'Challenge request sent! Use the web app to complete the match.';
      case '/stats':
        return 'Check your stats at the ping pong league website.';
      case '/leaderboard':
        return 'Current leaderboard posted above!';
      default:
        return 'Unknown command. Available commands: /challenge, /stats, /leaderboard';
    }
  }
}