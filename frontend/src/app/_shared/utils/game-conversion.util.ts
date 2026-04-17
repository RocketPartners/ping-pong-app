import {Game, Player} from '../../_models/models';

export interface MatchGameScore {
  team1Score: number;
  team2Score: number;
}

export interface GameConversionInput {
  matchType: 'singles' | 'doubles';
  isRanked: boolean;
  team1: Player[];
  team2: Player[];
  games: MatchGameScore[];
}

export function buildGameDtos(input: GameConversionInput): Game[] {
  const {matchType, isRanked, team1, team2, games} = input;

  return games.map(game => {
    const team1Wins = game.team1Score > game.team2Score;

    if (matchType === 'singles') {
      return {
        challengerId: team1[0]?.playerId || '',
        challengerTeam: [],
        challengerTeamScore: game.team1Score,
        challengerTeamWin: false,
        challengerWin: team1Wins,
        doublesGame: false,
        normalGame: !isRanked,
        ratedGame: isRanked,
        singlesGame: true,
        opponentId: team2[0]?.playerId || '',
        opponentTeam: [],
        opponentTeamScore: game.team2Score,
        opponentTeamWin: false,
        opponentWin: !team1Wins
      } as Game;
    }

    return {
      challengerId: '',
      challengerTeam: team1.map(p => p.playerId).filter(Boolean),
      challengerTeamScore: game.team1Score,
      challengerTeamWin: team1Wins,
      challengerWin: false,
      doublesGame: true,
      normalGame: !isRanked,
      ratedGame: isRanked,
      singlesGame: false,
      opponentId: '',
      opponentTeam: team2.map(p => p.playerId).filter(Boolean),
      opponentTeamScore: game.team2Score,
      opponentTeamWin: !team1Wins,
      opponentWin: false
    } as Game;
  });
}
