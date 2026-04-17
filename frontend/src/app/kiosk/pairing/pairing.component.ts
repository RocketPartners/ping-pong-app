import {Component, OnDestroy} from '@angular/core';
import {Router} from '@angular/router';
import {KioskAuthService} from '../_services/kiosk-auth.service';

const CODE_LENGTH = 6;
const POLL_INTERVAL_MS = 2000;

type Mode = 'request' | 'waiting' | 'code' | 'token';

@Component({
  selector: 'app-kiosk-pairing',
  templateUrl: './pairing.component.html',
  styleUrls: ['./pairing.component.scss'],
  standalone: false
})
export class PairingComponent implements OnDestroy {
  mode: Mode = 'request';
  busy = false;
  error = '';

  requestId: string | null = null;
  deviceName = '';

  code = '';
  token = '';
  readonly digitSlots = Array.from({length: CODE_LENGTH}, (_, i) => i);

  private pollTimer: number | null = null;

  constructor(private kioskAuth: KioskAuthService, private router: Router) {}

  ngOnDestroy(): void {
    this.stopPolling();
  }

  async requestPairing(): Promise<void> {
    this.busy = true;
    this.error = '';
    try {
      const fallbackName = this.deviceName.trim() ||
        (navigator.platform + ' — ' + new Date().toLocaleDateString());
      const req = await this.kioskAuth.createPairRequest(fallbackName);
      this.requestId = req.id;
      this.mode = 'waiting';
      this.startPolling();
    } catch (e: any) {
      this.error = e?.message || 'Could not request pairing.';
    } finally {
      this.busy = false;
    }
  }

  cancelPairing(): void {
    this.stopPolling();
    this.requestId = null;
    this.mode = 'request';
  }

  private startPolling(): void {
    this.stopPolling();
    const tick = async () => {
      if (!this.requestId) return;
      try {
        const player = await this.kioskAuth.pollPairRequest(this.requestId);
        if (player) {
          this.stopPolling();
          this.router.navigate(['/kiosk']);
          return;
        }
      } catch (e: any) {
        this.stopPolling();
        this.error = e?.message || 'Pairing failed.';
        this.requestId = null;
        this.mode = 'request';
        return;
      }
      this.pollTimer = window.setTimeout(tick, POLL_INTERVAL_MS);
    };
    this.pollTimer = window.setTimeout(tick, POLL_INTERVAL_MS);
  }

  private stopPolling(): void {
    if (this.pollTimer !== null) {
      clearTimeout(this.pollTimer);
      this.pollTimer = null;
    }
  }

  // --- Fallback flows -------------------------------------------------------

  switchToCodeMode(): void {
    this.stopPolling();
    this.mode = 'code';
    this.code = '';
    this.error = '';
  }

  switchToTokenMode(): void {
    this.stopPolling();
    this.mode = 'token';
    this.token = '';
    this.error = '';
  }

  switchToRequestMode(): void {
    this.stopPolling();
    this.mode = 'request';
    this.code = '';
    this.token = '';
    this.error = '';
  }

  tapDigit(digit: number): void {
    if (this.busy || this.mode !== 'code') return;
    this.error = '';
    if (this.code.length >= CODE_LENGTH) return;
    this.code += String(digit);
    if (this.code.length === CODE_LENGTH) {
      void this.submitCode();
    }
  }

  backspace(): void {
    if (this.busy || this.mode !== 'code') return;
    this.code = this.code.slice(0, -1);
    this.error = '';
  }

  clearCode(): void {
    this.code = '';
    this.error = '';
  }

  async submitCode(): Promise<void> {
    if (this.code.length !== CODE_LENGTH) {
      this.error = `Enter all ${CODE_LENGTH} digits.`;
      return;
    }
    this.busy = true;
    this.error = '';
    try {
      await this.kioskAuth.pairWithCode(this.code);
      this.router.navigate(['/kiosk']);
    } catch (e: any) {
      this.error = e?.message || 'Pairing failed.';
      this.code = '';
    } finally {
      this.busy = false;
    }
  }

  async submitToken(): Promise<void> {
    const trimmed = this.token.trim();
    if (!trimmed) {
      this.error = 'Paste a token first.';
      return;
    }
    this.busy = true;
    this.error = '';
    try {
      await this.kioskAuth.pairWithToken(trimmed);
      this.router.navigate(['/kiosk']);
    } catch (e: any) {
      this.error = e?.message || 'Pairing failed.';
    } finally {
      this.busy = false;
    }
  }
}
