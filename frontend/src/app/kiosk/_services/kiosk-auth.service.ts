import {Injectable} from '@angular/core';
import {AuthenticatedPlayer, Player} from '../../_models/models';
import {AppConfig} from '../../_config/app.config';
import {AccountService} from '../../_services/account.service';

const KIOSK_STORAGE_KEY = 'kioskAuthenticatedPlayer';
const SESSION_PLAYER_KEY = 'player';

@Injectable({providedIn: 'root'})
export class KioskAuthService {
  constructor(private account: AccountService) {}

  hasStoredToken(): boolean {
    return !!localStorage.getItem(KIOSK_STORAGE_KEY);
  }

  /**
   * Copy the kiosk-paired JWT from localStorage into sessionStorage AND push it
   * into the AccountService BehaviorSubject so jwt.interceptor attaches the
   * Authorization header on subsequent HttpClient calls.
   */
  activateStoredToken(): void {
    const raw = localStorage.getItem(KIOSK_STORAGE_KEY);
    if (!raw) return;
    try {
      const parsed = JSON.parse(raw) as AuthenticatedPlayer;
      this.account.setAuthenticatedPlayer(parsed);
    } catch {
      // fall back to just copying the raw value so the next load retries
      sessionStorage.setItem(SESSION_PLAYER_KEY, raw);
    }
  }

  /**
   * Opens a pairing request the kiosk will poll against. An admin approves it
   * from the main app's admin UI; polling sees the mint and we activate it.
   */
  async createPairRequest(deviceName?: string): Promise<{id: string; expiresAt: string}> {
    const baseUrl = AppConfig.apiUrl;
    const res = await fetch(`${baseUrl}/api/auth/kiosk-pair-requests`, {
      method: 'POST',
      headers: {'Content-Type': 'application/json', Accept: 'application/json'},
      body: JSON.stringify({deviceName: deviceName || ''})
    });
    if (!res.ok) throw new Error(`Could not start pairing: ${res.status}`);
    return (await res.json()) as {id: string; expiresAt: string};
  }

  /**
   * Polls a pairing request. Returns null for still-pending; throws on
   * denied/expired; returns the finalized Player when approved + stored.
   */
  async pollPairRequest(id: string): Promise<Player | null> {
    const baseUrl = AppConfig.apiUrl;
    const res = await fetch(`${baseUrl}/api/auth/kiosk-pair-requests/${id}`, {
      headers: {Accept: 'application/json'}
    });
    if (res.status === 404) throw new Error('Pairing request no longer exists.');
    if (!res.ok) throw new Error(`Poll failed: ${res.status}`);

    const payload = (await res.json()) as {status: string; token?: string};
    if (payload.status === 'APPROVED' && payload.token) {
      return this.finalizePair(payload.token);
    }
    if (payload.status === 'DENIED') throw new Error('Pairing denied by admin.');
    if (payload.status === 'EXPIRED') throw new Error('Pairing request expired.');
    return null;
  }

  /**
   * Redeems a 6-digit pairing code for a long-lived kiosk JWT and stores it.
   * Uses native fetch so the app-wide JwtInterceptor can't overwrite our
   * Authorization header with a stale admin token from sessionStorage.
   */
  async pairWithCode(code: string): Promise<Player> {
    const baseUrl = AppConfig.apiUrl;
    const res = await fetch(`${baseUrl}/api/auth/kiosk-pairing-codes/redeem`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json'
      },
      body: JSON.stringify({code})
    });

    if (res.status === 400) {
      throw new Error('Pairing code is invalid or expired.');
    }
    if (!res.ok) {
      throw new Error(`Unexpected server error: ${res.status}`);
    }

    const payload = (await res.json()) as {token: string; username: string};
    if (!payload?.token) {
      throw new Error('Server did not return a kiosk token.');
    }

    return this.finalizePair(payload.token);
  }

  /**
   * Fallback: paste a long-lived JWT directly. Kept for scripted/headless pairing
   * and emergencies. Validates by fetching the kiosk-system player.
   */
  async pairWithToken(token: string): Promise<Player> {
    const baseUrl = AppConfig.apiUrl;
    const res = await fetch(`${baseUrl}/api/players/username/kiosk-system`, {
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: 'application/json'
      }
    });

    if (res.status === 401) {
      throw new Error('Token rejected by server — it may be expired or malformed.');
    }
    if (res.status === 404) {
      throw new Error(
        'Kiosk service account not found. Run POST /api/auth/kiosk-token on the backend first.'
      );
    }
    if (!res.ok) {
      throw new Error(`Unexpected server error: ${res.status}`);
    }

    return this.finalizePair(token);
  }

  private async finalizePair(token: string): Promise<Player> {
    const baseUrl = AppConfig.apiUrl;
    const res = await fetch(`${baseUrl}/api/players/username/kiosk-system`, {
      headers: {Authorization: `Bearer ${token}`, Accept: 'application/json'}
    });
    if (!res.ok) {
      throw new Error(`Could not load kiosk player: ${res.status}`);
    }
    const player = (await res.json()) as Player;
    const authenticated: AuthenticatedPlayer = {player, token};
    localStorage.setItem(KIOSK_STORAGE_KEY, JSON.stringify(authenticated));
    this.account.setAuthenticatedPlayer(authenticated);
    return player;
  }

  unpair(): void {
    localStorage.removeItem(KIOSK_STORAGE_KEY);
    this.account.setAuthenticatedPlayer(null);
  }

  getPairedPlayer(): AuthenticatedPlayer | null {
    const raw = localStorage.getItem(KIOSK_STORAGE_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthenticatedPlayer;
    } catch {
      return null;
    }
  }

  getExpiryDate(): Date | null {
    const secs = this.secondsUntilExpiry();
    if (!Number.isFinite(secs)) return null;
    return new Date(Date.now() + secs * 1000);
  }

  /**
   * Returns the seconds until the stored kiosk JWT expires. Negative if expired,
   * Infinity if no token or exp claim is missing/unparseable.
   */
  secondsUntilExpiry(): number {
    const raw = localStorage.getItem(KIOSK_STORAGE_KEY);
    if (!raw) return -Infinity;
    try {
      const {token} = JSON.parse(raw) as AuthenticatedPlayer;
      if (!token) return -Infinity;
      const payload = JSON.parse(atob(token.split('.')[1]));
      if (typeof payload?.exp !== 'number') return Infinity;
      return payload.exp - Math.floor(Date.now() / 1000);
    } catch {
      return -Infinity;
    }
  }
}
