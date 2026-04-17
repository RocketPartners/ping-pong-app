import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable, timer} from 'rxjs';
import {switchMap, shareReplay, catchError} from 'rxjs/operators';
import {of} from 'rxjs';
import {BaseHttpService} from './base-http.service';
import {HttpClient} from '@angular/common/http';
import {AlertService} from './alert.services';
import {AccountService} from './account.service';

export interface PendingKioskPairing {
  id: string;
  deviceName: string;
  userAgent?: string;
  createdAt: string;
  expiresAt: string;
}

export interface KioskDevice {
  id: string;
  deviceName: string;
  approvedBy: string;
  userAgent?: string;
  approvedAt: string;
  lastSeenAt: string;
  revoked: boolean;
}

@Injectable({providedIn: 'root'})
export class KioskAdminService extends BaseHttpService {
  private endpoint = '/api/auth/kiosk-pair-requests';

  private pendingSubject = new BehaviorSubject<PendingKioskPairing[]>([]);
  pending$: Observable<PendingKioskPairing[]> = this.pendingSubject.asObservable();

  constructor(
    http: HttpClient,
    alertService: AlertService,
    private account: AccountService
  ) {
    super(http, alertService);
    // Poll pending kiosk pairings every 5s while an admin is logged in.
    timer(0, 5000).pipe(
      switchMap(() => {
        const player = this.account.playerValue;
        if (!player || player.player?.role !== 'ADMIN') return of([]);
        return this.get<PendingKioskPairing[]>(this.endpoint, undefined, []).pipe(
          catchError(() => of([]))
        );
      })
    ).subscribe(list => this.pendingSubject.next(list || []));
  }

  refresh(): Observable<PendingKioskPairing[] | null> {
    return this.get<PendingKioskPairing[]>(this.endpoint, undefined, []);
  }

  approve(id: string): Observable<any> {
    return this.post(`${this.endpoint}/${id}/approve`, {});
  }

  deny(id: string): Observable<any> {
    return this.post(`${this.endpoint}/${id}/deny`, {});
  }

  listDevices(): Observable<KioskDevice[] | null> {
    return this.get<KioskDevice[]>('/api/auth/kiosk-devices', undefined, []);
  }

  revokeDevice(id: string): Observable<any> {
    return this.delete(`/api/auth/kiosk-devices/${id}`);
  }
}
