import {Injectable} from '@angular/core';
import {CanActivate, Router, UrlTree} from '@angular/router';
import {KioskAuthService} from '../_services/kiosk-auth.service';

@Injectable({providedIn: 'root'})
export class KioskReadyGuard implements CanActivate {
  constructor(private kioskAuth: KioskAuthService, private router: Router) {}

  canActivate(): boolean | UrlTree {
    if (!this.kioskAuth.hasStoredToken()) {
      return this.router.parseUrl('/kiosk/pair');
    }
    if (this.kioskAuth.secondsUntilExpiry() <= 0) {
      this.kioskAuth.unpair();
      return this.router.parseUrl('/kiosk/pair');
    }
    this.kioskAuth.activateStoredToken();
    return true;
  }
}
