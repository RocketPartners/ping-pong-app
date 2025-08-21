# Tournament Frontend Architecture Plan

## 🎯 **Architecture Overview**

### **Hybrid Approach: Frontend Logic + Backend Validation**
- **Frontend**: `brackets-manager` handles tournament logic and `brackets-viewer` handles visualization
- **Backend**: Simplified to data persistence + validation + match result processing
- **Sync Strategy**: Frontend optimistically updates, backend validates and persists

## 📊 **Data Flow Architecture**

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Frontend      │    │     Backend      │    │    Database     │
│                 │    │                  │    │                 │
│ brackets-manager│◄──►│ Tournament API   │◄──►│ Tournament Data │
│ brackets-viewer │    │ Match Results    │    │ Match Results   │
│ Angular Service │    │ Player Data      │    │ Player Data     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## 🏗️ **Implementation Phases**

### **Phase 1: Frontend Tournament Engine**
1. **Install Dependencies**
   ```bash
   npm install brackets-manager brackets-viewer
   ```

2. **Create Angular Services**
   - `TournamentEngineService` - Wraps brackets-manager
   - `TournamentApiService` - Handles backend sync
   - `TournamentStateService` - Manages local state

3. **Create Components**
   - `TournamentBracketComponent` - Main bracket display
   - `MatchDetailComponent` - Match details and score input
   - `TournamentManagementComponent` - Admin controls

### **Phase 2: Backend API Simplification**
1. **Redesign Models** for data persistence only
2. **Simplify API Endpoints**
3. **Add validation endpoints**
4. **Remove complex tournament logic**

### **Phase 3: Integration & Polish**
1. **Real-time sync** between frontend and backend
2. **Conflict resolution** for concurrent updates
3. **Mobile responsiveness**
4. **Error handling and recovery**

## 🗃️ **New Backend API Design**

### **Simplified Models**
```java
// Minimal tournament data storage
@Entity
public class Tournament {
    private Long id;
    private String name;
    private TournamentType type;
    private TournamentStatus status;
    private String bracketData; // JSON blob from brackets-manager
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Entity
public class TournamentParticipant {
    private Long id;
    private Long tournamentId;
    private Long playerId;
    private Integer seed;
    private TournamentParticipantStatus status;
}

@Entity
public class TournamentMatchResult {
    private Long id;
    private Long tournamentId;
    private String matchId; // brackets-manager match ID
    private Long winnerId;
    private Long loserId;
    private Integer winnerScore;
    private Integer loserScore;
    private LocalDateTime completedAt;
}
```

### **Simplified API Endpoints**
```
GET    /api/tournaments                    # List tournaments
POST   /api/tournaments                    # Create tournament
GET    /api/tournaments/{id}               # Get tournament + bracket data
PUT    /api/tournaments/{id}/bracket       # Update bracket state
POST   /api/tournaments/{id}/matches       # Submit match result
PUT    /api/tournaments/{id}/status        # Update tournament status
```

## 🔧 **Frontend Service Architecture**

### **TournamentEngineService**
```typescript
@Injectable()
export class TournamentEngineService {
  private manager = new BracketsManager();
  
  // Initialize tournament from backend data
  initializeTournament(tournamentData: any): Promise<void>
  
  // Get current bracket state for visualization
  getBracketData(): BracketData
  
  // Advance player in bracket (optimistic)
  advancePlayer(matchId: number, winnerId: number): Promise<void>
  
  // Reset match (admin function)
  resetMatch(matchId: number): Promise<void>
  
  // Get match details
  getMatch(matchId: number): Match
  
  // Get tournament status
  getTournamentStatus(): TournamentStatus
}
```

### **TournamentApiService**
```typescript
@Injectable()
export class TournamentApiService extends BaseHttpService {
  
  // Sync bracket state with backend
  syncBracketState(tournamentId: number, bracketData: any): Promise<void>
  
  // Submit match result to backend
  submitMatchResult(matchResult: MatchResult): Promise<void>
  
  // Get tournament data from backend
  getTournament(tournamentId: number): Promise<Tournament>
  
  // Validate bracket state with backend
  validateBracketState(bracketData: any): Promise<ValidationResult>
}
```

## 🎨 **Component Architecture**

### **TournamentBracketComponent**
```typescript
@Component({
  selector: 'app-tournament-bracket',
  template: `
    <div class="tournament-container">
      <div #bracketContainer class="bracket-container"></div>
      <app-match-detail 
        *ngIf="selectedMatch" 
        [match]="selectedMatch"
        (matchUpdated)="onMatchUpdated($event)">
      </app-match-detail>
    </div>
  `
})
export class TournamentBracketComponent {
  @ViewChild('bracketContainer') bracketContainer!: ElementRef;
  
  private viewer?: BracketsViewer;
  selectedMatch?: Match;
  
  async ngOnInit() {
    await this.initializeBracket();
    this.setupEventListeners();
  }
  
  private initializeBracket(): Promise<void>
  private setupEventListeners(): void
  private onMatchClick(match: Match): void
  private onMatchUpdated(result: MatchResult): Promise<void>
}
```

## 🔄 **Sync Strategy**

### **Optimistic Updates**
1. User action triggers frontend update
2. UI updates immediately for responsiveness
3. Backend sync happens asynchronously
4. On conflict, backend wins and frontend updates

### **Conflict Resolution**
```typescript
async submitMatchResult(result: MatchResult): Promise<void> {
  try {
    // Optimistic update
    this.engineService.advancePlayer(result.matchId, result.winnerId);
    
    // Sync with backend
    await this.apiService.submitMatchResult(result);
    
    // Confirm sync successful
    await this.syncBracketState();
  } catch (error) {
    // Rollback optimistic update
    await this.refreshFromBackend();
    throw error;
  }
}
```

## 🎯 **Benefits of This Approach**

### **Frontend Benefits**
- ✅ **Rich UX** - Instant feedback, smooth interactions
- ✅ **Proven Logic** - brackets-manager handles complex tournament rules
- ✅ **Beautiful UI** - brackets-viewer provides professional visualization
- ✅ **Offline Capable** - Can work without constant backend connection

### **Backend Benefits**
- ✅ **Simplified Code** - Remove complex tournament advancement logic
- ✅ **Better Performance** - Fewer API calls, simpler operations
- ✅ **Easier Testing** - Just CRUD operations and validation
- ✅ **Scalability** - Less server processing per tournament action

### **Overall Benefits**
- ✅ **Separation of Concerns** - UI logic in frontend, data persistence in backend
- ✅ **Maintainability** - Use proven libraries instead of custom logic
- ✅ **User Experience** - Fast, responsive tournament interactions
- ✅ **Developer Experience** - Work with well-documented tournament library

## 🚨 **Migration Strategy**

### **Phase 1: Parallel Development**
- Keep existing backend tournament logic
- Build new frontend with brackets-manager
- Test side-by-side

### **Phase 2: Feature Flag**
- Add feature flag to switch between old/new system
- Gradually migrate tournaments to new system
- Monitor for issues

### **Phase 3: Full Migration**
- Remove old tournament logic from backend
- Simplify backend models and API
- Full production deployment

## 📝 **Next Steps**

1. **Install npm packages** and create basic Angular integration
2. **Prototype tournament bracket display** with sample data
3. **Design new backend API** for simplified data operations
4. **Create migration plan** from current backend to new architecture
5. **Implement real-time sync** between frontend and backend

## 🎨 **Styling Integration**

The tournament brackets will integrate with your existing theme system:
- Use your current dark/light theme variables
- Match your existing component styling patterns
- Integrate with Angular Material where appropriate (modals, buttons, etc.)
- Custom CSS for bracket-specific styling to match your design preferences

---

**This architecture gives us the best of both worlds: rich frontend tournament experience with solid backend data integrity.**