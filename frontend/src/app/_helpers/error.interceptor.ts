import {Injectable} from '@angular/core';
import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {Router} from '@angular/router';
import {Observable, throwError} from 'rxjs';
import {catchError} from 'rxjs/operators';
import {AccountService} from "../_services/account.service";


@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(private accountService: AccountService, private router: Router) {
  }

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(catchError(err => {
      if ([401, 403].includes(err.status) && this.accountService.playerValue) {
        // Kiosk has its own auth model — redirect to pair screen rather than the
        // regular /login flow the app's logout() would force.
        if (this.router.url.startsWith('/kiosk')) {
          localStorage.removeItem('kioskAuthenticatedPlayer');
          sessionStorage.removeItem('player');
          this.router.navigate(['/kiosk/pair']);
        } else {
          // Auto logout if 401 or 403 response returned from api
          this.accountService.logout();
        }
      }

      // TODO Enhance error message handling
      let errorMessage = 'An unknown error occurred';

      if (err.error?.message) {
        errorMessage = err.error.message;
      } else if (err.error instanceof ErrorEvent) {
        // Client-side error
        errorMessage = `Error: ${err.error.message}`;
      } else if (err.status) {
        // Server-side error
        errorMessage = `Error Code: ${err.status}\nMessage: ${err.message}`;
      }

      console.error(err);
      return throwError(() => errorMessage);
    }))
  }
}
