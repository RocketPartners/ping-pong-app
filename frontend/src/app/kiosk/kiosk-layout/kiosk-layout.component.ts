import {Component, HostBinding, OnDestroy, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {AppConfig} from '../../_config/app.config';

const HEARTBEAT_INTERVAL_MS = 60_000;

@Component({
  selector: 'app-kiosk-layout',
  templateUrl: './kiosk-layout.component.html',
  styleUrls: ['./kiosk-layout.component.scss'],
  standalone: false
})
export class KioskLayoutComponent implements OnInit, OnDestroy {
  @HostBinding('class.kiosk-mode') readonly kioskModeClass = true;
  private heartbeatTimer: number | null = null;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    document.body.classList.add('kiosk-mode');
    // Periodic heartbeat keeps this kiosk's lastSeenAt fresh so admins see it
    // as online. The jwt.interceptor attaches the kiosk's token automatically.
    this.heartbeat();
    this.heartbeatTimer = window.setInterval(() => this.heartbeat(), HEARTBEAT_INTERVAL_MS);
  }

  ngOnDestroy(): void {
    document.body.classList.remove('kiosk-mode');
    if (this.heartbeatTimer !== null) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  private heartbeat(): void {
    this.http.post(`${AppConfig.apiUrl}/api/auth/kiosk-devices/heartbeat`, {}).subscribe({
      error: () => {/* tolerated — error.interceptor handles 401 */}
    });
  }
}
