import { environment } from '../../environments/environment';

/**
 * Application Configuration
 * 
 * Central place for app-wide configuration settings
 */
export const AppConfig = {
  // API Configuration
  apiUrl: environment.apiUrl,
  
  // App Information
  version: '1.1.0', // Incrementing for our improvements
  appName: 'Ping Pong Elo Rating System',
  
  // UI Configuration
  defaultPageSize: 10,
  dateFormat: 'mediumDate',
  longDateFormat: 'MMMM d, y',
  
  // Game Settings
  defaultBestOf: 3,
  maxScore: 21, // Maximum allowed score per game
  winBy2Threshold: 11, // Score needed to trigger win-by-2 rule
  
  // Animation Settings
  defaultAnimationDuration: 300, // ms
  
  // Feature Flags
  features: {
    enableThemeSwitching: true,
    enableNotifications: true,
    enableAchievements: true,
    enableTournaments: true,
    enableInfiniteGames: true
  }
};
