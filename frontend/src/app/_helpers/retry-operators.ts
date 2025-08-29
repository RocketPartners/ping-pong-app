import { Observable, timer, throwError } from 'rxjs';
import { retryWhen, tap, mergeMap } from 'rxjs/operators';

export interface RetryConfig {
  maxRetries: number;
  delayMs: number;
  backoffMultiplier?: number;
  retryCondition?: (error: any) => boolean;
}

/**
 * Custom retry operator with exponential backoff
 */
export function retryWithBackoff<T>(config: RetryConfig) {
  const { maxRetries, delayMs, backoffMultiplier = 2, retryCondition } = config;
  
  return (source: Observable<T>) => 
    source.pipe(
      retryWhen(errors =>
        errors.pipe(
          mergeMap((error, index) => {
            const retryCount = index + 1;
            
            // Check if we should retry
            const shouldRetry = retryCondition ? retryCondition(error) : isRetryableError(error);
            
            if (retryCount <= maxRetries && shouldRetry) {
              const delay = delayMs * Math.pow(backoffMultiplier, index);
              
              console.warn(`Request failed, retrying in ${delay}ms (attempt ${retryCount}/${maxRetries}):`, error);
              
              return timer(delay);
            } else {
              console.error(`Request failed after ${index} retries:`, error);
              return throwError(() => error);
            }
          })
        )
      )
    );
}

/**
 * Default retry condition for HTTP errors
 */
function isRetryableError(error: any): boolean {
  // Retry on network errors, server errors, but not client errors
  if (!error?.status) {
    return true; // Network errors
  }
  
  return error.status >= 500 || error.status === 408 || error.status === 429;
}

/**
 * Retry configuration presets
 */
export const RetryPresets = {
  /**
   * Quick retry for fast operations
   */
  QUICK: {
    maxRetries: 2,
    delayMs: 500,
    backoffMultiplier: 1.5
  } as RetryConfig,
  
  /**
   * Standard retry for most operations
   */
  STANDARD: {
    maxRetries: 3,
    delayMs: 1000,
    backoffMultiplier: 2
  } as RetryConfig,
  
  /**
   * Persistent retry for critical operations
   */
  PERSISTENT: {
    maxRetries: 5,
    delayMs: 2000,
    backoffMultiplier: 2
  } as RetryConfig,
  
  /**
   * Analytics-specific retry (more tolerant of failures)
   */
  ANALYTICS: {
    maxRetries: 2,
    delayMs: 1500,
    backoffMultiplier: 2,
    retryCondition: (error: any) => {
      // Only retry on network errors or server errors, not validation errors
      return !error?.status || error.status >= 500;
    }
  } as RetryConfig
};