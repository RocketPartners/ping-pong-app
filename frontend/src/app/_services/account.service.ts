import {Injectable} from '@angular/core';
import {Router} from '@angular/router';
import {BehaviorSubject, Observable} from 'rxjs';
import {map} from 'rxjs/operators';

// Import central model interfaces
import {AuthenticatedPlayer, LoginRequest, Player, RegistrationRequest} from '../_models/models';
import {BaseHttpService} from './base-http.service';
import {HttpClient} from "@angular/common/http";
import {AlertService} from "./alert.services";

@Injectable({providedIn: 'root'})
export class AccountService extends BaseHttpService {
  public authenticatedPlayer: Observable<AuthenticatedPlayer | null>;
  // API endpoints
  private authUrl = `${this.baseUrl}/api/auth`;
  private playersUrl = `${this.baseUrl}/api/players`;
  // Authentication state
  private authenticatedPlayerSubject: BehaviorSubject<AuthenticatedPlayer | null>;

  constructor(
    private router: Router,
    http: HttpClient,
    alertService: AlertService
  ) {
    super(http, alertService);
    this.authenticatedPlayerSubject = new BehaviorSubject<AuthenticatedPlayer | null>(
      JSON.parse(sessionStorage.getItem('player')!)
    );
    this.authenticatedPlayer = this.authenticatedPlayerSubject.asObservable();
  }

  /**
   * Get the current authenticated player value
   */
  public get playerValue(): AuthenticatedPlayer | null {
    return this.authenticatedPlayerSubject.value;
  }

  /**
   * Log in with username and password
   * @param username User's username
   * @param password User's password
   * @returns Observable of AuthenticatedPlayer
   */
  login(username: string, password: string): Observable<AuthenticatedPlayer> {
    const loginRequest: LoginRequest = {username, password};

    return this.post<AuthenticatedPlayer>(`/api/auth/login`, loginRequest)
      .pipe(map(authenticatedPlayer => {
        if (authenticatedPlayer) {
          // Store player details and jwt token in session storage
          sessionStorage.setItem('player', JSON.stringify(authenticatedPlayer));
          this.authenticatedPlayerSubject.next(authenticatedPlayer);
        }
        return authenticatedPlayer as AuthenticatedPlayer;
      }));
  }

  /**
   * Alternative login method using the players/authenticate endpoint
   * @param username User's username
   * @param password User's password
   * @returns Observable of AuthenticatedPlayer
   */
  alternativeLogin(username: string, password: string): Observable<AuthenticatedPlayer> {
    const loginRequest: LoginRequest = {username, password};

    return this.post<AuthenticatedPlayer>(`/api/players/authenticate`, loginRequest)
      .pipe(map(authenticatedPlayer => {
        if (authenticatedPlayer) {
          sessionStorage.setItem('player', JSON.stringify(authenticatedPlayer));
          this.authenticatedPlayerSubject.next(authenticatedPlayer);
        }
        return authenticatedPlayer as AuthenticatedPlayer;
      }));
  }

  /**
   * Log out the current user
   */
  logout(): void {
    // Call the backend logout endpoint if it requires server-side cleanup
    this.post(`/api/auth/logout`, {}).subscribe({
      next: () => this.completeLogout(),
      error: () => this.completeLogout() // Still logout locally even if server fails
    });
  }

  /**
   * Register a new player
   * @param player Player registration data
   * @returns Observable of registration result
   */
  register(player: RegistrationRequest): Observable<any> {
    return this.post(`/api/auth/register`, player);
  }

  /**
   * Update player profile
   * @param player Updated player data
   * @returns Observable of update result
   */
  updateProfile(player: Player): Observable<any> {
    return this.put<any>(`/api/players/${player.playerId}`, player)
      .pipe(map(response => {
        // If successful update, update the stored player data
        if (response) {
          // Get current authenticated player
          const currentPlayer = this.authenticatedPlayerSubject.value;
          if (currentPlayer) {
            // Update the player object with new data
            const updatedAuthPlayer = {
              ...currentPlayer,
              player: {
                ...currentPlayer.player,
                ...player
              }
            };

            // Store updated player in session storage
            sessionStorage.setItem('player', JSON.stringify(updatedAuthPlayer));

            // Update the subject
            this.authenticatedPlayerSubject.next(updatedAuthPlayer);
          }
        }
        return response;
      }));
  }

  /**
   * Complete logout process by clearing storage and redirecting
   */
  private completeLogout(): void {
    // Remove player from session storage and reset the subject
    sessionStorage.removeItem('player');
    this.authenticatedPlayerSubject.next(null);
    this.router.navigate(['/login']);
  }
}
