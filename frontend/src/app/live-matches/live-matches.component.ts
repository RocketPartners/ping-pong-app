import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { LiveMatchService, LiveMatch, SpectatorComment, MatchEvent } from '../_services/live-match.service';
import { GameType } from '../_models/models';
import { SharedComponentsModule } from '../_shared/shared-components.module';

@Component({
  selector: 'app-live-matches',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatBadgeModule,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    FormsModule,
    RouterLink,
    SharedComponentsModule
  ],
  templateUrl: './live-matches.component.html',
  styleUrls: ['./live-matches.component.scss']
})
export class LiveMatchesComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  liveMatches: LiveMatch[] = [];
  selectedMatch: LiveMatch | null = null;
  comments: SpectatorComment[] = [];
  matchEvents: MatchEvent[] = [];
  newComment = '';
  currentUser = 'Spectator' + Math.floor(Math.random() * 1000);
  
  // Tab selection
  selectedTabIndex = 0;

  constructor(private liveMatchService: LiveMatchService) {}

  ngOnInit(): void {
    this.loadLiveMatches();
    this.loadComments();
    this.loadMatchEvents();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadLiveMatches(): void {
    this.liveMatchService.getLiveMatches()
      .pipe(takeUntil(this.destroy$))
      .subscribe(matches => {
        this.liveMatches = matches;
        if (!this.selectedMatch && matches.length > 0) {
          this.selectMatch(matches[0]);
        }
      });
  }

  private loadComments(): void {
    this.liveMatchService.comments$
      .pipe(takeUntil(this.destroy$))
      .subscribe(comments => {
        this.comments = comments;
      });
  }

  private loadMatchEvents(): void {
    this.liveMatchService.matchEvents$
      .pipe(takeUntil(this.destroy$))
      .subscribe(events => {
        this.matchEvents = events;
      });
  }

  selectMatch(match: LiveMatch): void {
    if (this.selectedMatch) {
      this.liveMatchService.leaveMatchAsSpectator(this.selectedMatch.id);
    }
    this.selectedMatch = match;
    this.liveMatchService.joinMatchAsSpectator(match.id);
  }

  getGameTypeDisplay(gameType: GameType): string {
    switch (gameType) {
      case GameType.SINGLES_RANKED: return 'Singles Ranked';
      case GameType.DOUBLES_RANKED: return 'Doubles Ranked';
      case GameType.SINGLES_NORMAL: return 'Singles Normal';
      case GameType.DOUBLES_NORMAL: return 'Doubles Normal';
      default: return 'Unknown';
    }
  }

  getMatchDuration(startTime: Date): string {
    const now = new Date();
    const diff = now.getTime() - startTime.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(minutes / 60);
    
    if (hours > 0) {
      return `${hours}h ${minutes % 60}m`;
    }
    return `${minutes}m`;
  }

  getTeamNames(match: LiveMatch): { team1: string; team2: string } {
    if (match.gameType === GameType.SINGLES_RANKED || match.gameType === GameType.SINGLES_NORMAL) {
      return {
        team1: `${match.players[0].firstName} ${match.players[0].lastName}`,
        team2: `${match.players[1].firstName} ${match.players[1].lastName}`
      };
    } else {
      return {
        team1: `${match.players[0].firstName} & ${match.players[1].firstName}`,
        team2: `${match.players[2].firstName} & ${match.players[3].firstName}`
      };
    }
  }

  getTeamRating(match: LiveMatch, team: 'team1' | 'team2'): number {
    const isDoubles = match.gameType === GameType.DOUBLES_RANKED || match.gameType === GameType.DOUBLES_NORMAL;
    const isRanked = match.gameType === GameType.SINGLES_RANKED || match.gameType === GameType.DOUBLES_RANKED;
    
    if (team === 'team1') {
      if (isDoubles) {
        const rating1 = isRanked ? match.players[0].doublesRankedRating : match.players[0].doublesNormalRating;
        const rating2 = isRanked ? match.players[1].doublesRankedRating : match.players[1].doublesNormalRating;
        return Math.round((rating1 + rating2) / 2);
      } else {
        return isRanked ? match.players[0].singlesRankedRating : match.players[0].singlesNormalRating;
      }
    } else {
      if (isDoubles) {
        const rating1 = isRanked ? match.players[2].doublesRankedRating : match.players[2].doublesNormalRating;
        const rating2 = isRanked ? match.players[3].doublesRankedRating : match.players[3].doublesNormalRating;
        return Math.round((rating1 + rating2) / 2);
      } else {
        return isRanked ? match.players[1].singlesRankedRating : match.players[1].singlesNormalRating;
      }
    }
  }

  sendComment(): void {
    if (this.newComment.trim() && this.selectedMatch) {
      this.liveMatchService.addComment(this.selectedMatch.id, this.newComment.trim(), this.currentUser);
      this.newComment = '';
    }
  }

  addReaction(commentId: string, emoji: string): void {
    this.liveMatchService.addReaction(commentId, emoji);
  }

  getReactionCount(comment: SpectatorComment, emoji: string): number {
    const reaction = comment.reactions?.find(r => r.emoji === emoji);
    return reaction ? reaction.count : 0;
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

  getEventIcon(eventType: string): string {
    switch (eventType) {
      case 'score-update': return 'sports_tennis';
      case 'game-start': return 'play_arrow';
      case 'game-end': return 'stop';
      case 'match-end': return 'flag';
      case 'spectator-join': return 'person_add';
      case 'spectator-leave': return 'person_remove';
      case 'comment': return 'chat';
      default: return 'event';
    }
  }

  getEventMessage(event: MatchEvent): string {
    switch (event.type) {
      case 'score-update':
        return `Score update: ${event.data.score.team1} - ${event.data.score.team2}`;
      case 'spectator-join':
        return `Spectator joined (${event.data.spectatorCount} watching)`;
      case 'spectator-leave':
        return `Spectator left (${event.data.spectatorCount} watching)`;
      case 'comment':
        return `${event.data.username}: ${event.data.message}`;
      default:
        return 'Match event';
    }
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'waiting': return 'orange';
      case 'in-progress': return 'green';
      case 'completed': return 'blue';
      default: return 'gray';
    }
  }
}