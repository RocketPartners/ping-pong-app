// src/app/_services/tournament-engine.service.ts
// Simple tournament display service that works with our backend

import { Injectable } from '@angular/core';
import { 
  TournamentType, 
  BracketData, 
  Match, 
  MatchStatus, 
  Participant,
  TournamentParticipant,
  UpdateMatchRequest 
} from '../_models/tournament-new';

@Injectable({
  providedIn: 'root'
})
export class TournamentEngineService {

  constructor() {}

  /**
   * Creates tournament bracket data from backend tournament information
   * This converts our backend tournament data into display format
   */
  async createTournamentDisplay(
    tournamentId: string,
    participants: TournamentParticipant[],
    type: TournamentType
  ): Promise<BracketData> {
    // This method will be replaced with backend API calls
    // For now, return empty bracket structure
    return {
      stage: [],
      match: [],
      match_game: [],
      participant: participants.map(p => ({
        id: p.id,
        tournament_id: parseInt(tournamentId, 10) || 1,
        name: p.name
      }))
    };
  }

  /**
   * Simple placeholder methods - these will be replaced with backend API calls
   */
  
  async getBracketData(tournamentId: string): Promise<BracketData> {
    // This will be replaced with backend API call
    return {
      stage: [],
      match: [],
      match_game: [],
      participant: []
    };
  }

  async getReadyMatches(tournamentId: string): Promise<Match[]> {
    // This will be replaced with backend API call
    return [];
  }

  async getTournamentWinner(tournamentId: string): Promise<Participant | null> {
    // This will be replaced with backend API call
    return null;
  }

  async isTournamentCompleted(tournamentId: string): Promise<boolean> {
    // This will be replaced with backend API call
    return false;
  }

  async getTournamentProgress(tournamentId: string): Promise<{
    totalMatches: number;
    completedMatches: number;
    pendingMatches: number;
    readyMatches: number;
  }> {
    // This will be replaced with backend API call
    return {
      totalMatches: 0,
      completedMatches: 0,
      pendingMatches: 0,
      readyMatches: 0
    };
  }
}