import {Component, OnDestroy, OnInit} from '@angular/core';
import {Subscription, interval} from 'rxjs';
import {KioskAdminService, PendingKioskPairing, KioskDevice} from '../../_services/kiosk-admin.service';
import {AlertService} from '../../_services/alert.services';

@Component({
  selector: 'app-kiosk-pairings',
  templateUrl: './kiosk-pairings.component.html',
  styleUrls: ['./kiosk-pairings.component.scss'],
  standalone: false
})
export class KioskPairingsComponent implements OnInit, OnDestroy {
  pending: PendingKioskPairing[] = [];
  devices: KioskDevice[] = [];
  busyId: string | null = null;

  private subs: Subscription[] = [];

  constructor(private kioskAdmin: KioskAdminService, private alert: AlertService) {}

  ngOnInit(): void {
    this.subs.push(this.kioskAdmin.pending$.subscribe(list => this.pending = list));
    this.refreshDevices();
    // Refresh paired list every 5s so last-seen stays fresh.
    this.subs.push(interval(5000).subscribe(() => this.refreshDevices()));
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  refreshDevices(): void {
    this.kioskAdmin.listDevices().subscribe(list => {
      this.devices = (list || []).filter(d => !d.revoked);
    });
  }

  approve(req: PendingKioskPairing): void {
    this.busyId = req.id;
    this.kioskAdmin.approve(req.id).subscribe({
      next: () => {
        this.alert.success(`Approved ${req.deviceName}.`);
        this.busyId = null;
        this.kioskAdmin.refresh().subscribe();
        this.refreshDevices();
      },
      error: err => {
        this.alert.error(err?.error?.message || 'Could not approve.');
        this.busyId = null;
      }
    });
  }

  deny(req: PendingKioskPairing): void {
    this.busyId = req.id;
    this.kioskAdmin.deny(req.id).subscribe({
      next: () => {
        this.alert.success(`Denied ${req.deviceName}.`);
        this.busyId = null;
        this.kioskAdmin.refresh().subscribe();
      },
      error: err => {
        this.alert.error(err?.error?.message || 'Could not deny.');
        this.busyId = null;
      }
    });
  }

  revoke(device: KioskDevice): void {
    if (!confirm(`Revoke kiosk "${device.deviceName}"? It will be kicked back to the pairing screen on its next request.`)) return;
    this.busyId = device.id;
    this.kioskAdmin.revokeDevice(device.id).subscribe({
      next: () => {
        this.alert.success(`Revoked ${device.deviceName}.`);
        this.busyId = null;
        this.refreshDevices();
      },
      error: err => {
        this.alert.error(err?.error?.message || 'Could not revoke.');
        this.busyId = null;
      }
    });
  }

  isOnline(device: KioskDevice): boolean {
    if (!device.lastSeenAt) return false;
    const last = new Date(device.lastSeenAt).getTime();
    return Date.now() - last < 120_000;
  }

  sinceLastSeen(device: KioskDevice): string {
    if (!device.lastSeenAt) return 'never';
    const diffMs = Date.now() - new Date(device.lastSeenAt).getTime();
    const s = Math.floor(diffMs / 1000);
    if (s < 60) return `${s}s ago`;
    const m = Math.floor(s / 60);
    if (m < 60) return `${m}m ago`;
    const h = Math.floor(m / 60);
    if (h < 24) return `${h}h ago`;
    const d = Math.floor(h / 24);
    return `${d}d ago`;
  }

  parseUA(ua?: string): string {
    if (!ua) return 'Unknown';
    if (ua.includes('iPad')) return 'iPad';
    if (ua.includes('iPhone')) return 'iPhone';
    if (ua.includes('Android')) return 'Android';
    if (ua.includes('Mac OS')) return 'macOS';
    if (ua.includes('Windows')) return 'Windows';
    if (ua.includes('Linux')) return 'Linux';
    return 'Browser';
  }
}
