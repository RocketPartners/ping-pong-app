import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

export enum LogLevel {
  DEBUG = 0,
  INFO = 1,
  WARN = 2,
  ERROR = 3,
  OFF = 4
}

@Injectable({
  providedIn: 'root'
})
export class LoggingService {
  private readonly logLevel: LogLevel = environment.production ? LogLevel.WARN : LogLevel.DEBUG;

  debug(message: string, ...args: any[]): void {
    this.log(LogLevel.DEBUG, 'DEBUG', message, args);
  }

  info(message: string, ...args: any[]): void {
    this.log(LogLevel.INFO, 'INFO', message, args);
  }

  warn(message: string, ...args: any[]): void {
    this.log(LogLevel.WARN, 'WARN', message, args);
  }

  error(message: string, error?: Error, ...args: any[]): void {
    this.log(LogLevel.ERROR, 'ERROR', message, error ? [error, ...args] : args);
    
    // In production, you might want to send errors to a logging service
    if (environment.production && error) {
      this.sendErrorToService(message, error);
    }
  }

  private log(level: LogLevel, levelName: string, message: string, args: any[]): void {
    if (level < this.logLevel) {
      return;
    }

    const timestamp = new Date().toISOString();
    const prefix = `[${timestamp}] [${levelName}]`;

    switch (level) {
      case LogLevel.DEBUG:
        console.debug(prefix, message, ...args);
        break;
      case LogLevel.INFO:
        console.info(prefix, message, ...args);
        break;
      case LogLevel.WARN:
        console.warn(prefix, message, ...args);
        break;
      case LogLevel.ERROR:
        console.error(prefix, message, ...args);
        break;
    }
  }

  private sendErrorToService(message: string, error: Error): void {
    // TODO: Implement actual error reporting service integration
    // This could send to Sentry, LogRocket, or custom error tracking
    try {
      // Example: Send to analytics or error tracking service
      // errorTrackingService.captureException(error, { extra: { message } });
    } catch (e) {
      // Fallback to console if error service fails
      console.error('Failed to send error to logging service:', e);
    }
  }

  /**
   * Set log level programmatically (useful for debugging)
   */
  setLogLevel(level: LogLevel): void {
    (this as any).logLevel = level;
  }
}