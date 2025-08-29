import { Injectable, ErrorHandler, Injector } from '@angular/core';
import { AlertService } from './alert.services';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  
  constructor(private injector: Injector) {}

  handleError(error: Error | any): void {
    // Get AlertService using injector to avoid circular dependency
    const alertService = this.injector.get(AlertService);
    
    console.error('Global error caught:', error);
    
    // Determine error type and provide appropriate user feedback
    if (error?.status) {
      // HTTP errors
      switch (error.status) {
        case 0:
          alertService.error('Network connection lost. Please check your internet connection.');
          break;
        case 401:
          alertService.error('Authentication expired. Please log in again.');
          break;
        case 403:
          alertService.error('You do not have permission to perform this action.');
          break;
        case 404:
          alertService.error('The requested resource was not found.');
          break;
        case 500:
          alertService.error('A server error occurred. Please try again later.');
          break;
        default:
          alertService.error('An unexpected error occurred. Please try again.');
      }
    } else if (error?.message?.includes('ChunkLoadError')) {
      // Code splitting chunk load errors
      alertService.error(
        'Failed to load application resources. Please refresh the page.',
        'Chunk Load Error'
      );
      // Optionally auto-refresh after a delay
      setTimeout(() => {
        window.location.reload();
      }, 3000);
    } else if (error?.name === 'TypeError' && error?.message?.includes('Cannot read properties')) {
      // Common null/undefined access errors
      alertService.error('A data loading error occurred. Please refresh the page.');
    } else {
      // Generic errors
      alertService.error('An unexpected error occurred. Please try again.');
    }
    
    // In development, re-throw the error for debugging
    if (!this.isProduction()) {
      throw error;
    }
  }

  private isProduction(): boolean {
    // This would typically check environment configuration
    return false; // Always false for debugging during development
  }
}