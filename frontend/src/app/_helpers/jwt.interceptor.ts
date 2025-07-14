import {Injectable} from '@angular/core';
import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {Observable} from 'rxjs';
import {AccountService} from "../_services/account.service";
import {AppConfig} from "../_config/app.config";

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  private baseUrl = AppConfig.apiUrl;

  constructor(private accountService: AccountService) {
  }

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Add auth header with jwt if user is logged in and request is to the api url
    const player = this.accountService.playerValue;
    const isLoggedIn = player?.token;
    const isApiUrl = request.url.startsWith(this.baseUrl);
    // Exclude password reset endpoints from authentication
    const isPasswordResetUrl = request.url.includes('/api/password-reset/');

    if (isLoggedIn && isApiUrl && !isPasswordResetUrl) {
      request = request.clone({
        setHeaders: {Authorization: `Bearer ${player.token}`}
      });
    }

    return next.handle(request);
  }
}
