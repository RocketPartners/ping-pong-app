import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {BaseHttpService} from './base-http.service';
import {HttpClient, HttpParams} from '@angular/common/http';
import {AppConfig} from '../_config/app.config';
import {AlertService} from "./alert.services";

@Injectable({
  providedIn: 'root'
})
export class PasswordResetService extends BaseHttpService {
  private endpoint = '/api/password-reset';
  private apiUrl = AppConfig.apiUrl;

  constructor(
    http: HttpClient,
    alertService: AlertService
  ) {
    super(http, alertService);
  }

  /**
   * Request a password reset email
   * @param email User's email address
   * @returns Observable of response
   */
  requestPasswordReset(email: string): Observable<any> {
    return this.post(`${this.endpoint}/request`, {email});
  }

  /**
   * Validate a password reset token
   * @param token The reset token to validate
   * @returns Observable of validation result
   */
  validateToken(token: string): Observable<any> {
    // Create HttpParams instance and add the token parameter
    const params = new HttpParams().set('token', token);
    console.log('Validating token with URL:', `${this.endpoint}/validate`, 'Params:', params.toString());
    return this.get(`${this.endpoint}/validate`, params);
  }

  /**
   * Direct validation using HttpClient (for debugging)
   */
  validateTokenDirect(token: string): Observable<any> {
    const url = `${this.apiUrl}${this.endpoint}/validate?token=${token}`;
    console.log('Direct validation URL:', url);
    return this.http.get(url);
  }

  /**
   * Reset a password with a valid token
   * @param token Valid reset token
   * @param password New password
   * @param confirmPassword Confirm new password
   * @returns Observable of reset result
   */
  resetPassword(token: string, password: string, confirmPassword: string): Observable<any> {
    return this.post(`${this.endpoint}/reset`, {
      token,
      password,
      confirmPassword
    });
  }
}
