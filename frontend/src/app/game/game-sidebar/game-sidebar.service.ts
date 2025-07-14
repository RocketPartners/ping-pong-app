import {Injectable} from '@angular/core';
import {Overlay, OverlayConfig, OverlayRef} from '@angular/cdk/overlay';
import {ComponentPortal} from '@angular/cdk/portal';
import {Game} from '../../_models/models';
import {GameSidebarComponent} from './game-sidebar.component';
import {GameService} from '../../_services/game.service';
import {PlayerService} from '../../_services/player.service';
import {forkJoin, Observable, of} from 'rxjs';
import {catchError, map} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class GameSidebarService {
  private overlayRef: OverlayRef | null = null;

  constructor(
    private overlay: Overlay,
    private gameService: GameService,
    private playerService: PlayerService
  ) {
  }

  open(gameId: string): void {
    // Close any existing drawer
    this.close();

    // Fetch game details
    this.gameService.getGameDetails(gameId).pipe(
      catchError(error => {
        console.error('Error fetching game details:', error);
        return of(null);
      })
    ).subscribe(game => {
      if (game) {
        // Create overlay config with center positioning
        const config: OverlayConfig = {
          hasBackdrop: true,
          backdropClass: 'game-drawer-backdrop',
          panelClass: 'game-drawer-panel',
          positionStrategy: this.overlay
            .position()
            .global()
            .centerHorizontally()
            .centerVertically(),
          scrollStrategy: this.overlay.scrollStrategies.block(),
          disposeOnNavigation: true // Auto close on navigation
        };

        // Create the overlay
        this.overlayRef = this.overlay.create(config);

        // Create the component portal
        const componentPortal = new ComponentPortal(GameSidebarComponent);

        // Attach the portal to the overlay
        const componentRef = this.overlayRef.attach(componentPortal);

        // Pass data to the component
        componentRef.instance.game = game;
        componentRef.instance.overlayRef = this.overlayRef;

        // Calculate game properties
        game.opponentTeamWin = !game.challengerTeamWin;

        // If it's a singles game, make sure challenger/opponent win flags are set correctly
        if (game.singlesGame) {
          game.challengerWin = game.challengerTeamWin;
          game.opponentWin = game.opponentTeamWin;
        }

        // Fetch player information
        this.fetchPlayerData(game).pipe(
          catchError(error => {
            console.error('Error fetching player data:', error);
            return of({});
          })
        ).subscribe(playerData => {
          componentRef.instance.playerData = playerData;
        });

        // Handle backdrop clicks
        this.overlayRef.backdropClick().subscribe(() => this.close());
      }
    });
  }

  close(): void {
    if (this.overlayRef) {
      this.overlayRef.dispose();
      this.overlayRef = null;
    }
  }

  private fetchPlayerData(game: Game): Observable<any> {
    // Collect unique player IDs
    const playerIds = new Set<string>();

    // Add challenger and opponent IDs
    if (game.challengerId) playerIds.add(game.challengerId);
    if (game.opponentId) playerIds.add(game.opponentId);

    // Add team members if it's a doubles game
    if (game.doublesGame) {
      game.challengerTeam.forEach(id => playerIds.add(id));
      game.opponentTeam.forEach(id => playerIds.add(id));
    }

    // Convert Set to Array
    const uniquePlayerIds = Array.from(playerIds);

    // Create an Observable to fetch each player
    const playerRequests: Observable<any>[] = uniquePlayerIds.map(id =>
      this.playerService.getPlayerById(id).pipe(
        map(player => ({id, player})),
        catchError(error => {
          console.error(`Error fetching player with ID ${id}:`, error);
          return of({id, player: null});
        })
      )
    );

    // If there are no players to fetch, return an empty object
    if (playerRequests.length === 0) {
      return of({});
    }

    // Fetch all players in parallel and combine results into a map
    return forkJoin(playerRequests).pipe(
      map(results => {
        const playerMap: Record<string, any> = {};
        results.forEach(({id, player}) => {
          if (player) {
            // Add convenience properties
            if (!player.fullName) {
              player.fullName = `${player.firstName} ${player.lastName}`;
            }

            // Calculate win rates if not already present
            if (player.singlesRankedWins !== undefined && player.singlesRankedLoses !== undefined) {
              const totalGames = player.singlesRankedWins + player.singlesRankedLoses;
              player.singlesRankedWinRate = totalGames > 0 ?
                (player.singlesRankedWins / totalGames) * 100 : 0;
            }

            if (player.doublesRankedWins !== undefined && player.doublesRankedLoses !== undefined) {
              const totalGames = player.doublesRankedWins + player.doublesRankedLoses;
              player.doublesRankedWinRate = totalGames > 0 ?
                (player.doublesRankedWins / totalGames) * 100 : 0;
            }

            if (player.singlesNormalWins !== undefined && player.singlesNormalLoses !== undefined) {
              const totalGames = player.singlesNormalWins + player.singlesNormalLoses;
              player.singlesNormalWinRate = totalGames > 0 ?
                (player.singlesNormalWins / totalGames) * 100 : 0;
            }

            if (player.doublesNormalWins !== undefined && player.doublesNormalLoses !== undefined) {
              const totalGames = player.doublesNormalWins + player.doublesNormalLoses;
              player.doublesNormalWinRate = totalGames > 0 ?
                (player.doublesNormalWins / totalGames) * 100 : 0;
            }

            playerMap[id] = player;
          }
        });
        return playerMap;
      })
    );
  }
}
