import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subject, interval } from 'rxjs';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { BaseHttpService } from './base-http.service';
import { AlertService } from './alert.services';
import { Game, Player, GameType } from '../_models/models';
import { takeUntil, retry, catchError } from 'rxjs/operators';

export interface LiveMatch {
  id: string;
  players: Player[];
  currentScore: { team1: number; team2: number };
  gameInProgress: boolean;
  gameType: GameType;
  spectatorCount: number;
  startTime: Date;
  currentGame?: number;
  bestOf: number;
  status: 'waiting' | 'in-progress' | 'completed';
}

export interface MatchEvent {
  type: 'score-update' | 'game-start' | 'game-end' | 'match-end' | 'spectator-join' | 'spectator-leave' | 'comment';
  matchId: string;
  data: any;
  timestamp: Date;
}

export interface SpectatorComment {
  id: string;
  username: string;
  message: string;
  timestamp: Date;
  reactions?: { emoji: string; count: number }[];
}

export interface MatchPrediction {
  matchId: string;
  predictedWinner: 'team1' | 'team2';
  confidence: number;
  userId: string;
}

@Injectable({
  providedIn: 'root'
})
export class LiveMatchService extends BaseHttpService {
  private wsSubject: WebSocketSubject<any> | null = null;
  private destroy$ = new Subject<void>();

  // Observable streams
  private liveMatchesSubject = new BehaviorSubject<LiveMatch[]>([]);
  private matchEventsSubject = new BehaviorSubject<MatchEvent[]>([]);
  private commentsSubject = new BehaviorSubject<SpectatorComment[]>([]);
  private predictionsSubject = new BehaviorSubject<MatchPrediction[]>([]);

  public liveMatches$ = this.liveMatchesSubject.asObservable();
  public matchEvents$ = this.matchEventsSubject.asObservable();
  public comments$ = this.commentsSubject.asObservable();
  public predictions$ = this.predictionsSubject.asObservable();

  // Demo data for impressive display
  private demoMatches: LiveMatch[] = [
    {
      id: 'live-1',
      players: [
        { id: 1, firstName: 'Alex', lastName: 'Chen', singlesRankedRating: 1850 } as Player,
        { id: 2, firstName: 'Maria', lastName: 'Rodriguez', singlesRankedRating: 1820 } as Player
      ],
      currentScore: { team1: 2, team2: 1 },
      gameInProgress: true,
      gameType: GameType.SINGLES_RANKED,
      spectatorCount: 23,
      startTime: new Date(Date.now() - 45 * 60000), // 45 minutes ago
      currentGame: 4,
      bestOf: 5,
      status: 'in-progress'
    },
    {
      id: 'live-2',
      players: [
        { id: 3, firstName: 'John', lastName: 'Smith', doublesRankedRating: 1950 } as Player,
        { id: 4, firstName: 'Sarah', lastName: 'Johnson', doublesRankedRating: 1920 } as Player,
        { id: 5, firstName: 'Mike', lastName: 'Davis', doublesRankedRating: 1940 } as Player,
        { id: 6, firstName: 'Emma', lastName: 'Wilson', doublesRankedRating: 1910 } as Player
      ],
      currentScore: { team1: 1, team2: 0 },
      gameInProgress: true,
      gameType: GameType.DOUBLES_RANKED,
      spectatorCount: 31,
      startTime: new Date(Date.now() - 20 * 60000), // 20 minutes ago
      currentGame: 2,
      bestOf: 3,
      status: 'in-progress'
    }
  ];

  private demoComments: SpectatorComment[] = [
    {
      id: '1',
      username: 'PingPongFan92',
      message: 'What a rally! 🔥',
      timestamp: new Date(Date.now() - 30000),
      reactions: [{ emoji: '🔥', count: 5 }, { emoji: '👏', count: 3 }]
    },
    {
      id: '2',
      username: 'TableTennisExpert',
      message: 'Alex\'s backhand is unstoppable today',
      timestamp: new Date(Date.now() - 60000),
      reactions: [{ emoji: '💯', count: 2 }]
    },
    {
      id: '3',
      username: 'SpinMaster',
      message: 'Prediction: Maria takes this set',
      timestamp: new Date(Date.now() - 90000),
      reactions: [{ emoji: '🎯', count: 4 }]
    }
  ];

  constructor(http: HttpClient, alertService?: AlertService) {
    super(http, alertService);
    this.initializeDemoData();
    this.simulateLiveUpdates();
  }

  private initializeDemoData(): void {
    this.liveMatchesSubject.next(this.demoMatches);
    this.commentsSubject.next(this.demoComments);
  }

  private simulateLiveUpdates(): void {
    // Simulate live score updates every 30-60 seconds
    interval(45000).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.simulateScoreUpdate();
    });

    // Simulate new comments every 20-40 seconds
    interval(30000).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.simulateNewComment();
    });

    // Simulate spectator count changes
    interval(15000).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.simulateSpectatorChange();
    });
  }

  private simulateScoreUpdate(): void {
    const matches = this.liveMatchesSubject.value;
    if (matches.length === 0) return;

    const randomMatch = matches[Math.floor(Math.random() * matches.length)];
    if (Math.random() > 0.7) { // 30% chance of score update
      const scoreChange = Math.random() > 0.5 ? 'team1' : 'team2';
      if (scoreChange === 'team1') {
        randomMatch.currentScore.team1++;
      } else {
        randomMatch.currentScore.team2++;
      }

      const event: MatchEvent = {
        type: 'score-update',
        matchId: randomMatch.id,
        data: { score: randomMatch.currentScore, scoringTeam: scoreChange },
        timestamp: new Date()
      };

      this.addMatchEvent(event);
      this.liveMatchesSubject.next([...matches]);
    }
  }

  private simulateNewComment(): void {
    const comments = [
      'Incredible defense!',
      'What a shot! 🏓',
      'This match is getting intense',
      'Amazing rally!',
      'Great tactics from both players',
      'The crowd is going wild! 🎉',
      'Unbelievable comeback',
      'Perfect spin on that serve'
    ];

    const newComment: SpectatorComment = {
      id: Date.now().toString(),
      username: `Spectator${Math.floor(Math.random() * 1000)}`,
      message: comments[Math.floor(Math.random() * comments.length)],
      timestamp: new Date(),
      reactions: []
    };

    const currentComments = this.commentsSubject.value;
    this.commentsSubject.next([newComment, ...currentComments.slice(0, 19)]); // Keep last 20 comments
  }

  private simulateSpectatorChange(): void {
    const matches = this.liveMatchesSubject.value;
    matches.forEach(match => {
      const change = Math.floor(Math.random() * 6) - 2; // -2 to +4 change
      match.spectatorCount = Math.max(0, match.spectatorCount + change);
    });
    this.liveMatchesSubject.next([...matches]);
  }

  private addMatchEvent(event: MatchEvent): void {
    const events = this.matchEventsSubject.value;
    this.matchEventsSubject.next([event, ...events.slice(0, 49)]); // Keep last 50 events
  }

  // Public API methods
  getLiveMatches(): Observable<LiveMatch[]> {
    return this.liveMatches$;
  }

  getMatchById(matchId: string): Observable<LiveMatch | undefined> {
    return new Observable(observer => {
      this.liveMatches$.subscribe(matches => {
        observer.next(matches.find(m => m.id === matchId));
      });
    });
  }

  joinMatchAsSpectator(matchId: string): Observable<boolean> {
    const matches = this.liveMatchesSubject.value;
    const match = matches.find(m => m.id === matchId);
    if (match) {
      match.spectatorCount++;
      this.liveMatchesSubject.next([...matches]);
      
      const event: MatchEvent = {
        type: 'spectator-join',
        matchId,
        data: { spectatorCount: match.spectatorCount },
        timestamp: new Date()
      };
      this.addMatchEvent(event);
    }
    return new Observable(observer => observer.next(true));
  }

  leaveMatchAsSpectator(matchId: string): Observable<boolean> {
    const matches = this.liveMatchesSubject.value;
    const match = matches.find(m => m.id === matchId);
    if (match && match.spectatorCount > 0) {
      match.spectatorCount--;
      this.liveMatchesSubject.next([...matches]);
      
      const event: MatchEvent = {
        type: 'spectator-leave',
        matchId,
        data: { spectatorCount: match.spectatorCount },
        timestamp: new Date()
      };
      this.addMatchEvent(event);
    }
    return new Observable(observer => observer.next(true));
  }

  addComment(matchId: string, message: string, username: string): Observable<boolean> {
    const newComment: SpectatorComment = {
      id: Date.now().toString(),
      username,
      message,
      timestamp: new Date(),
      reactions: []
    };

    const comments = this.commentsSubject.value;
    this.commentsSubject.next([newComment, ...comments]);

    const event: MatchEvent = {
      type: 'comment',
      matchId,
      data: newComment,
      timestamp: new Date()
    };
    this.addMatchEvent(event);

    return new Observable(observer => observer.next(true));
  }

  addReaction(commentId: string, emoji: string): Observable<boolean> {
    const comments = this.commentsSubject.value;
    const comment = comments.find(c => c.id === commentId);
    if (comment) {
      if (!comment.reactions) comment.reactions = [];
      const existingReaction = comment.reactions.find(r => r.emoji === emoji);
      if (existingReaction) {
        existingReaction.count++;
      } else {
        comment.reactions.push({ emoji, count: 1 });
      }
      this.commentsSubject.next([...comments]);
    }
    return new Observable(observer => observer.next(true));
  }

  submitPrediction(prediction: MatchPrediction): Observable<boolean> {
    const predictions = this.predictionsSubject.value;
    this.predictionsSubject.next([...predictions, prediction]);
    return new Observable(observer => observer.next(true));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.wsSubject) {
      this.wsSubject.complete();
    }
  }
}