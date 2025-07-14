import {Injectable, Optional} from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpHeaders, HttpParams} from '@angular/common/http';
import {Observable, of, tap, throwError} from 'rxjs';
import {catchError} from 'rxjs/operators';
import {AppConfig} from '../_config/app.config';
import {AlertService} from './alert.services';

/**
 * Base HTTP service that standardizes error handling and common HTTP operations
 */
@Injectable({
  providedIn: 'root'
})
export class BaseHttpService {
  protected baseUrl = AppConfig.apiUrl;
  // Cache storage and configuration
  private cache = new Map<string, { data: any, timestamp: number }>();
  private cacheLifetime = 60000; // 1 minute cache lifetime in milliseconds

  constructor(
    protected http: HttpClient,
    @Optional() protected alertService?: AlertService
  ) {
  }

  /**
   * Manually clear the entire cache or a specific endpoint
   * @param endpoint Optional specific endpoint to clear
   */
  public clearCache(endpoint?: string): void {
    if (endpoint) {
      // Clear all cache entries that start with this endpoint
      [...this.cache.keys()]
        .filter(key => key.startsWith(`${endpoint}:`))
        .forEach(key => this.cache.delete(key));
    } else {
      // Clear entire cache
      this.cache.clear();
    }
  }

  /**
   * Standardized GET request with error handling
   * @param endpoint API endpoint to call
   * @param params Optional query parameters
   * @param defaultValue Optional default value to return on error
   */
  protected get<T>(endpoint: string, params?: HttpParams, defaultValue?: T | null): Observable<T | null> {
    return this.http.get<T>(`${this.baseUrl}${endpoint}`, {params})
      .pipe(
        catchError(error => this.handleError<T>(error, defaultValue))
      );
  }

  /**
   * Standardized POST request with error handling
   * @param endpoint API endpoint to call
   * @param body Request body
   * @param defaultValue Optional default value to return on error
   */
  protected post<T>(endpoint: string, body: any, defaultValue?: T | null): Observable<T | null> {
    const headers = new HttpHeaders({'Content-Type': 'application/json'});

    return this.http.post<T>(`${this.baseUrl}${endpoint}`, body, {headers})
      .pipe(
        catchError(error => this.handleError<T>(error, defaultValue))
      );
  }

  /**
   * Standardized PUT request with error handling
   * @param endpoint API endpoint to call
   * @param body Request body
   * @param defaultValue Optional default value to return on error
   */
  protected put<T>(endpoint: string, body: any, defaultValue?: T | null): Observable<T | null> {
    const headers = new HttpHeaders({'Content-Type': 'application/json'});

    return this.http.put<T>(`${this.baseUrl}${endpoint}`, body, {headers})
      .pipe(
        catchError(error => this.handleError<T>(error, defaultValue))
      );
  }

  /**
   * Standardized DELETE request with error handling
   * @param endpoint API endpoint to call
   * @param defaultValue Optional default value to return on error
   */
  protected delete<T>(endpoint: string, defaultValue?: T | null): Observable<T | null> {
    return this.http.delete<T>(`${this.baseUrl}${endpoint}`)
      .pipe(
        catchError(error => this.handleError<T>(error, defaultValue))
      );
  }

  /**
   * Standardized PATCH request with error handling
   * @param endpoint API endpoint to call
   * @param body Request body
   * @param defaultValue Optional default value to return on error
   */
  protected patch<T>(endpoint: string, body: any, defaultValue?: T | null): Observable<T | null> {
    const headers = new HttpHeaders({'Content-Type': 'application/json'});

    return this.http.patch<T>(`${this.baseUrl}${endpoint}`, body, {headers})
      .pipe(
        catchError(error => this.handleError<T>(error, defaultValue))
      );
  }

  /**
   * Standardized error handler with optional default return value
   * @param error HTTP error
   * @param defaultValue Optional default value to return instead of throwing error
   */
  protected handleError<T>(error: HttpErrorResponse, defaultValue?: T | null): Observable<T | null> {
    let errorMessage = 'An unknown error occurred';

    if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = `Error: ${error.error.message}`;
    } else if (error.status) {
      // Server-side error
      errorMessage = `Error Code: ${error.status}\nMessage: ${error.message}`;
    }

    console.error(errorMessage);

    // Display error using AlertService if available
    if (this.alertService) {
      // For 401 errors, show a user-friendly message
      if (error.status === 401) {
        this.alertService.error('Your session has expired. Please log in again.');
      }
      // For 403 errors
      else if (error.status === 403) {
        this.alertService.error('You don\'t have permission to perform this action.');
      }
      // For other errors, use the error message we extracted
      else {
        this.alertService.error(errorMessage);
      }
    }

    // If a default value is provided, return it
    if (defaultValue !== undefined) {
      return of(defaultValue);
    }

    return throwError(() => new Error(errorMessage));
  }

  /**
   * GET request with caching support
   * @param endpoint API endpoint to call
   * @param params Optional query parameters
   * @param defaultValue Optional default value to return on error
   * @param bypassCache Set to true to force a fresh request
   */
  protected getCached<T>(
    endpoint: string,
    params?: HttpParams,
    defaultValue?: T | null,
    bypassCache: boolean = false
  ): Observable<T | null> {
    const cacheKey = `${endpoint}:${params?.toString() || ''}`;

    // Check cache if not bypassing
    if (!bypassCache) {
      const cached = this.cache.get(cacheKey);
      if (cached && (Date.now() - cached.timestamp < this.cacheLifetime)) {
        console.log(`Cache hit for ${cacheKey}`);
        return of(cached.data);
      }
    }

    // If not in cache or bypassing cache, make the request
    return this.get<T>(endpoint, params, defaultValue).pipe(
      tap(data => {
        // Only cache successful responses with data
        if (data !== null && data !== undefined) {
          this.cache.set(cacheKey, {
            data,
            timestamp: Date.now()
          });
        }
      })
    );
  }
}
