import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, Subscription } from 'rxjs';
import { BaseHttpService } from './base-http.service';
import { AlertService } from './alert.services';
import { LoggingService } from './logging.service';
import { environment } from '../../environments/environment';
import { 
  EasterEgg, 
  EasterEggStats, 
  EggHunterLeaderboardDto, 
  RecentEggFindDto, 
  EggClaimResult 
} from '../_models/easter-egg';
import { EasterEggEvent } from '../_models/easter-egg-event';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

@Injectable({
  providedIn: 'root'
})
export class EasterEggService extends BaseHttpService implements OnDestroy {
  private apiUrl = '/api/easter-eggs';
  
  private currentEggSubject = new BehaviorSubject<EasterEgg | null>(null);
  public currentEgg$ = this.currentEggSubject.asObservable();
  
  private myStatsSubject = new BehaviorSubject<EasterEggStats | null>(null);
  public myStats$ = this.myStatsSubject.asObservable();
  
  // WebSocket client and subscriptions
  private stompClient: Client | null = null;
  private wsSubscriptions: Subscription[] = [];

  constructor(
    http: HttpClient,
    alertService: AlertService,
    private logger: LoggingService
  ) {
    super(http, alertService);
    this.initializeWebSocket();
  }

  ngOnDestroy(): void {
    this.disconnectWebSocket();
  }

  /**
   * Initialize WebSocket connection for real-time updates
   */
  private initializeWebSocket(): void {
    try {
      // Don't initialize WebSocket in testing environments
      if (typeof window === 'undefined') {
        this.logger.debug('Skipping WebSocket in non-browser environment');
        return;
      }

      const wsUrl = `${environment.apiUrl}/easter-eggs-ws`;
      this.logger.info('Initializing WebSocket connection', { url: wsUrl });
      
      // Create SockJS connection with explicit configuration
      const socket = new SockJS(wsUrl, null, {
        transports: ['websocket', 'xhr-streaming', 'xhr-polling']
      });

      this.stompClient = new Client({
        webSocketFactory: () => socket as any,
        debug: (str) => this.logger.debug('STOMP Debug', { message: str }),
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        
        onConnect: (frame) => {
          this.logger.info('WebSocket Connected Successfully', { frame });
          this.subscribeToEasterEggEvents();
        },
        
        onDisconnect: (frame) => {
          this.logger.warn('WebSocket Disconnected', { frame });
        },
        
        onStompError: (frame) => {
          this.logger.error('STOMP Protocol Error', new Error('STOMP Error'), { headers: frame.headers, body: frame.body });
        },
        
        onWebSocketError: (event) => {
          this.logger.error('WebSocket Connection Error', new Error('WebSocket Error'), { event });
        },
        
        onWebSocketClose: (event) => {
          this.logger.info('WebSocket Closed', { event });
        }
      });

      this.logger.debug('Activating STOMP client');
      this.stompClient.activate();
      
    } catch (error) {
      this.logger.error('Fatal error initializing WebSocket', error as Error);
    }
  }

  /**
   * Subscribe to easter egg events topic
   */
  private subscribeToEasterEggEvents(): void {
    if (!this.stompClient?.connected) {
      this.logger.warn('Cannot subscribe - STOMP client not connected');
      return;
    }

    try {
      // Subscribe to the easter egg events topic
      const subscription = this.stompClient.subscribe('/topic/easter-eggs', (message) => {
        try {
          const event: EasterEggEvent = JSON.parse(message.body);
          this.logger.info('Received real-time easter egg event', { eventType: event.eventType, eggId: event.eggId });
          this.handleEasterEggEvent(event);
        } catch (parseError) {
          this.logger.error('Error parsing WebSocket message', parseError as Error, { rawMessage: message.body });
        }
      });

      this.logger.info('Successfully subscribed to easter egg events topic');
      
      // Store subscription for cleanup
      this.wsSubscriptions.push(subscription as any);
      
    } catch (error) {
      this.logger.error('Failed to subscribe to easter egg events', error as Error);
    }
  }

  /**
   * Handle real-time easter egg events
   */
  private handleEasterEggEvent(event: EasterEggEvent): void {
    this.logger.debug('Processing easter egg event', { eventType: event.eventType, eggId: event.eggId });
    
    switch (event.eventType) {
      case 'EGG_SPAWNED':
        // Convert event to EasterEgg object
        const newEgg: EasterEgg = {
          id: event.eggId,
          pageLocation: event.pageLocation!,
          cssSelector: event.cssSelector!,
          coordinates: event.coordinates!,
          type: event.eggType!,
          pointValue: event.pointValue!,
          isActive: true,
          spawnedAt: new Date(event.timestamp)
        };
        this.currentEggSubject.next(newEgg);
        this.logger.info('New easter egg spawned via WebSocket', { eggId: newEgg.id, type: newEgg.type, pageLocation: newEgg.pageLocation });
        break;

      case 'EGG_CLAIMED':
        // Remove the current egg since it was claimed
        const currentEgg = this.currentEggSubject.value;
        if (currentEgg && currentEgg.id === event.eggId) {
          this.currentEggSubject.next(null);
          this.logger.info('Easter egg claimed', { eggId: event.eggId, claimedBy: event.claimedByPlayer });
          
          // Show notification if claimed by someone else
          if (event.claimedByPlayer) {
            this.alertService?.success(`🥚 ${event.claimedByPlayer} found an easter egg!`);
          }
        }
        break;

      case 'EGG_EXPIRED':
        // Remove the current egg since it expired
        const expiredEgg = this.currentEggSubject.value;
        if (expiredEgg && expiredEgg.id === event.eggId) {
          this.currentEggSubject.next(null);
          this.logger.debug('Easter egg expired', { eggId: event.eggId });
        }
        break;
    }
  }

  /**
   * Disconnect WebSocket connection
   */
  private disconnectWebSocket(): void {
    if (this.stompClient) {
      try {
        this.stompClient.deactivate();
        this.logger.info('WebSocket disconnected successfully');
      } catch (error) {
        this.logger.error('Error disconnecting WebSocket', error as Error);
      }
    }
    
    // Clean up subscriptions
    this.wsSubscriptions.forEach(sub => sub.unsubscribe());
    this.wsSubscriptions = [];
  }

  /**
   * Get the currently active easter egg
   */
  getCurrentEgg(): Observable<EasterEgg | null> {
    return this.get<EasterEgg>(`${this.apiUrl}/current`);
  }

  /**
   * Refresh current egg and update subject
   */
  refreshCurrentEgg(): void {
    this.logger.debug('Fetching current easter egg from API');
    this.getCurrentEgg().subscribe({
      next: (egg) => {
        this.logger.debug('Successfully fetched current easter egg', { eggId: egg?.id, type: egg?.type });
        this.currentEggSubject.next(egg);
      },
      error: (error) => {
        this.logger.error('Error refreshing current egg', error);
        this.currentEggSubject.next(null);
      }
    });
  }

  /**
   * Attempt to claim an easter egg
   */
  claimEgg(eggId: string): Observable<EggClaimResult> {
    return this.post<EggClaimResult>(`${this.apiUrl}/${eggId}/claim`, {}) as Observable<EggClaimResult>;
  }

  /**
   * Get current player's easter egg statistics
   */
  getMyStats(): Observable<EasterEggStats> {
    return this.get<EasterEggStats>(`${this.apiUrl}/my-stats`) as Observable<EasterEggStats>;
  }

  /**
   * Refresh my stats and update subject
   */
  refreshMyStats(): void {
    this.getMyStats().subscribe({
      next: (stats) => this.myStatsSubject.next(stats),
      error: (error) => {
        this.logger.error('Error refreshing easter egg stats', error);
        this.myStatsSubject.next(null);
      }
    });
  }

  /**
   * Get the secret leaderboard (only accessible to players who found eggs)
   */
  getSecretLeaderboard(limit: number = 10): Observable<EggHunterLeaderboardDto[]> {
    return this.get<EggHunterLeaderboardDto[]>(`${this.apiUrl}/secret-leaderboard?limit=${limit}`) as Observable<EggHunterLeaderboardDto[]>;
  }

  /**
   * Get recent egg finds for activity display
   */
  getRecentFinds(limit: number = 10): Observable<RecentEggFindDto[]> {
    return this.get<RecentEggFindDto[]>(`${this.apiUrl}/recent-finds?limit=${limit}`) as Observable<RecentEggFindDto[]>;
  }

  /**
   * Check if easter egg system is healthy
   */
  healthCheck(): Observable<string> {
    return this.get<string>(`${this.apiUrl}/health`) as Observable<string>;
  }

  /**
   * Handle successful egg claim - refresh data and show celebration
   */
  handleSuccessfulClaim(result: EggClaimResult): void {
    // Refresh current egg and stats
    this.refreshCurrentEgg();
    this.refreshMyStats();
    
    // Show success message with points earned
    const message = `🥚 Found ${result.eggType} egg! +${result.pointsEarned} points (Total: ${result.newTotal})`;
    this.alertService?.success(message);
    
    // Show achievement notifications if any
    if (result.achievementsUnlocked && result.achievementsUnlocked.length > 0) {
      const achievementMessage = `🏆 Achievement${result.achievementsUnlocked.length > 1 ? 's' : ''} unlocked: ${result.achievementsUnlocked.join(', ')}`;
      setTimeout(() => this.alertService?.success(achievementMessage), 1000);
    }
  }

  /**
   * Initialize the service - load current egg and stats
   */
  initialize(): void {
    this.refreshCurrentEgg();
    this.refreshMyStats();
  }

  /**
   * Get current egg value from subject
   */
  get currentEggValue(): EasterEgg | null {
    return this.currentEggSubject.value;
  }

  /**
   * Get current stats value from subject
   */
  get myStatsValue(): EasterEggStats | null {
    return this.myStatsSubject.value;
  }
}