# Website Architecture Guide
## Based on Ping-Pong Tournament Platform

This comprehensive guide documents the architectural patterns, best practices, and methodologies used in a production Angular application. Use this as a blueprint for building modern, scalable web applications.

---

## Table of Contents

1. [Project Structure & Organization](#project-structure--organization)
2. [Angular Architecture Patterns](#angular-architecture-patterns)
3. [Service Layer Architecture](#service-layer-architecture)
4. [SCSS/CSS Architecture](#scsscss-architecture)
5. [Development Workflow](#development-workflow)
6. [Configuration Management](#configuration-management)
7. [Security Patterns](#security-patterns)
8. [Performance Optimization](#performance-optimization)
9. [Implementation Checklist](#implementation-checklist)

---

## Project Structure & Organization

### Folder Organization Pattern

```
src/app/
├── _config/          # Application configuration (settings, feature flags)
├── _helpers/         # Utilities, guards, interceptors, validators
├── _models/          # TypeScript interfaces, enums, type definitions
├── _services/        # Core business logic services
├── _shared/          # Reusable components and modules
├── feature-modules/  # Feature-based modules (lazy-loadable)
└── page-components/  # Top-level page components
```

### Key Principles

1. **Underscore Prefixing**: Infrastructure folders use `_` prefix to distinguish from feature modules
2. **Feature-Based Organization**: Each major feature gets its own module with internal structure
3. **Clear Separation of Concerns**: Models, services, components, and utilities are clearly separated
4. **Shared Resource Strategy**: Multiple levels of sharing (components, dashboard, forms)

### Module Architecture

```typescript
// Feature module structure
FeatureModule/
├── feature.module.ts           # Module definition
├── feature-routing.module.ts   # Feature-specific routes
├── components/                 # Feature components
├── services/                   # Feature services
└── models/                     # Feature-specific models
```

---

## Angular Architecture Patterns

### Component Strategy

**Mixed Component Approach:**
- **Legacy Components**: Traditional NgModule-based for complex features
- **Standalone Components**: New, reusable components for better tree-shaking
- **Shared Component Library**: Atomic, reusable components in `_shared/`

```typescript
// Standalone component example
@Component({
  selector: 'app-theme-toggle',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, MatTooltipModule]
})
export class ThemeToggleComponent {
  @Input() size: 'small' | 'medium' | 'large' = 'medium';
  @Output() themeChanged = new EventEmitter<string>();
}
```

### Routing Architecture

```typescript
// Centralized routing with guards
const routes: Routes = [
  // Feature grouping
  {path: 'tournaments', component: TournamentListComponent, canActivate: [AuthGuard]},
  {path: 'tournaments/create', component: TournamentCreateComponent, canActivate: [AuthGuard]},
  {path: 'tournaments/:id', component: TournamentDetailComponent, canActivate: [AuthGuard]},
  
  // Parameterized routes
  {path: 'player/:username', component: PlayerProfileComponent, canActivate: [AuthGuard]},
  
  // Default redirects
  {path: '', redirectTo: '/dashboard', pathMatch: 'full'},
  {path: '**', component: PageNotFoundComponent}
];
```

### State Management Pattern

**Service-Based State with RxJS:**
```typescript
@Injectable({ providedIn: 'root' })
export class DataService {
  private dataSubject = new BehaviorSubject<DataType>(initialState);
  public data$ = this.dataSubject.asObservable();
  
  updateData(newData: DataType): void {
    this.dataSubject.next(newData);
  }
}
```

---

## Service Layer Architecture

### BaseHttpService Pattern

**Foundation Service for All HTTP Operations:**
```typescript
@Injectable({ providedIn: 'root' })
export abstract class BaseHttpService {
  protected cache = new Map<string, {data: any, timestamp: number}>();
  private readonly CACHE_LIFETIME = 60000; // 1 minute
  
  constructor(
    protected http: HttpClient,
    protected alertService?: AlertService
  ) {}
  
  protected getCached<T>(endpoint: string, params?: HttpParams, defaultValue?: T): Observable<T | null> {
    // Caching logic with timestamp validation
  }
  
  protected handleError<T>(operation: string, defaultValue?: T): (error: any) => Observable<T> {
    // Standardized error handling
  }
}
```

### Service Inheritance Pattern

```typescript
@Injectable({ providedIn: 'root' })
export class PlayerService extends BaseHttpService {
  constructor(http: HttpClient, alertService: AlertService) {
    super(http, alertService);
  }
  
  getPlayer(username: string): Observable<Player | null> {
    return this.getCached<Player>(`/api/players/${username}`, undefined, null);
  }
}
```

### Error Handling Strategy

**Three-Layer Error Handling:**

1. **Global Interceptor**: Handles authentication errors (401/403)
2. **Service Layer**: Processes HTTP errors, provides user-friendly messages
3. **Component Layer**: Handles business logic errors

```typescript
// Error interceptor
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 || error.status === 403) {
        accountService.logout();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
```

### Authentication Pattern

```typescript
@Injectable({ providedIn: 'root' })
export class AccountService extends BaseHttpService {
  private authenticatedPlayerSubject = new BehaviorSubject<AuthenticatedPlayer | null>(null);
  public authenticatedPlayer$ = this.authenticatedPlayerSubject.asObservable();
  
  login(credentials: LoginRequest): Observable<AuthenticatedPlayer> {
    return this.post<AuthenticatedPlayer>('/api/auth/login', credentials)
      .pipe(tap(user => this.setCurrentUser(user)));
  }
  
  private setCurrentUser(user: AuthenticatedPlayer): void {
    sessionStorage.setItem('currentUser', JSON.stringify(user));
    this.authenticatedPlayerSubject.next(user);
  }
}
```

---

## SCSS/CSS Architecture

### 7-1 Architecture Pattern

```
src/styles/
├── abstracts/     # Variables, mixins, functions
├── base/          # Reset, global styles
├── components/    # Component-specific styles  
├── layout/        # Layout components
├── themes/        # Light/dark theme implementations
├── utilities/     # Utility classes
└── main.scss      # Main import file
```

### Design Token System

```scss
// tokens.scss - Design system foundation
:root {
  // Color system
  --primary-color: #1976d2;
  --primary-color-rgb: 25, 118, 210;
  --secondary-color: #424242;
  
  // Spacing system (4px base)
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
  
  // Typography
  --font-size-sm: 0.875rem;
  --font-size-md: 1rem;
  --font-size-lg: 1.125rem;
  
  // Border radius
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
  
  // Shadows
  --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.12);
  --shadow-md: 0 4px 6px rgba(0, 0, 0, 0.12);
  --shadow-lg: 0 10px 15px rgba(0, 0, 0, 0.12);
}
```

### Theme System

```scss
// Dark theme implementation
.dark-theme {
  --background-color: #121212;
  --surface-color: #1e1e1e;
  --text-primary: rgba(255, 255, 255, 0.87);
  --text-secondary: rgba(255, 255, 255, 0.60);
  
  // Elevated surface colors (Material Design)
  --surface-1: #1f1f1f;
  --surface-2: #232323;
  --surface-3: #252525;
}

// Component dark mode support
:host-context(.dark-theme) {
  .component-container {
    background-color: var(--surface-color);
    color: var(--text-primary);
  }
}
```

### Mixin Library

```scss
// mixins.scss - Reusable style patterns
@mixin card-container {
  background: var(--surface-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  padding: var(--spacing-lg);
}

@mixin flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

@mixin responsive-breakpoint($breakpoint) {
  @if $breakpoint == mobile {
    @media (max-width: 767px) { @content; }
  }
  @if $breakpoint == tablet {
    @media (min-width: 768px) and (max-width: 1023px) { @content; }
  }
  @if $breakpoint == desktop {
    @media (min-width: 1024px) { @content; }
  }
}
```

---

## Development Workflow

### Build Configuration

```json
// package.json scripts
{
  "scripts": {
    "start": "ng serve",
    "build": "ng build",
    "build:prod": "ng build --configuration production",
    "test": "ng test",
    "test:single": "ng test --include=src/app/path/to/spec.ts",
    "lint": "ng lint",
    "e2e": "ng e2e"
  }
}
```

### Angular.json Configuration

```json
{
  "build": {
    "options": {
      "styles": [
        "@angular/material/prebuilt-themes/indigo-pink.css",
        "src/styles/main.scss",
        "src/styles.scss"
      ],
      "budgets": [
        {
          "type": "initial",
          "maximumWarning": "500kb",
          "maximumError": "1mb"
        },
        {
          "type": "anyComponentStyle", 
          "maximumWarning": "2kb",
          "maximumError": "4kb"
        }
      ]
    }
  }
}
```

### TypeScript Configuration

```json
// tsconfig.json - Strict mode enabled
{
  "compilerOptions": {
    "strict": true,
    "noImplicitReturns": true,
    "noFallthroughCasesInSwitch": true,
    "strictPropertyInitialization": true
  }
}
```

---

## Configuration Management

### Centralized Configuration

```typescript
// app.config.ts
export const AppConfig = {
  production: false,
  apiUrl: 'http://localhost:3000/api',
  
  features: {
    enableSlackIntegration: true,
    enableDarkMode: true,
    enableTournaments: true
  },
  
  ui: {
    pageSize: 10,
    cacheTimeout: 60000,
    animationDuration: 300
  },
  
  auth: {
    tokenKey: 'authToken',
    sessionTimeout: 3600000 // 1 hour
  }
};
```

### Environment-Specific Settings

```typescript
// environments/environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:3000',
  features: {
    enableAnalytics: false,
    enableLogging: true
  }
};
```

---

## Security Patterns

### JWT Authentication

```typescript
// jwt.interceptor.ts
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const currentUser = sessionStorage.getItem('currentUser');
  const isApiUrl = req.url.startsWith('/api');
  const isPasswordReset = req.url.includes('/password-reset');
  
  if (currentUser && isApiUrl && !isPasswordReset) {
    const token = JSON.parse(currentUser).token;
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }
  
  return next(req);
};
```

### Route Guards

```typescript
// auth.guard.ts
export const AuthGuard: CanActivateFn = (route, state) => {
  const accountService = inject(AccountService);
  const router = inject(Router);
  
  if (accountService.isAuthenticated()) {
    return true;
  }
  
  router.navigate(['/login'], { 
    queryParams: { returnUrl: state.url } 
  });
  return false;
};
```

### Input Validation

```typescript
// form validators
export const customValidators = {
  passwordMatch: (password: string, confirmPassword: string): ValidatorFn => {
    return (control: AbstractControl): ValidationErrors | null => {
      // Password matching logic
    };
  },
  
  passwordStrength: (): ValidatorFn => {
    return (control: AbstractControl): ValidationErrors | null => {
      // Password strength validation
    };
  }
};
```

---

## Performance Optimization

### Lazy Loading Strategy

```typescript
// app-routing.module.ts
const routes: Routes = [
  {
    path: 'tournaments',
    loadChildren: () => import('./tournament/tournament.module').then(m => m.TournamentModule)
  },
  {
    path: 'achievements', 
    loadChildren: () => import('./achievement/achievement.module').then(m => m.AchievementModule)
  }
];
```

### Caching Strategy

```typescript
// Service-level caching
export class DataService extends BaseHttpService {
  getCachedData<T>(endpoint: string, cacheTime = 60000): Observable<T> {
    const cacheKey = this.generateCacheKey(endpoint);
    const cached = this.cache.get(cacheKey);
    
    if (cached && Date.now() - cached.timestamp < cacheTime) {
      return of(cached.data);
    }
    
    return this.http.get<T>(endpoint).pipe(
      tap(data => this.cache.set(cacheKey, { data, timestamp: Date.now() }))
    );
  }
}
```

### OnPush Change Detection

```typescript
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `...`
})
export class OptimizedComponent {
  constructor(private cdr: ChangeDetectorRef) {}
  
  updateData(): void {
    // Update logic
    this.cdr.markForCheck();
  }
}
```

---

## Implementation Checklist

### Project Setup
- [ ] Initialize Angular project with strict mode
- [ ] Set up 7-1 SCSS architecture
- [ ] Configure build budgets and optimization
- [ ] Implement design token system
- [ ] Set up theme switching infrastructure

### Architecture Implementation
- [ ] Create BaseHttpService with caching
- [ ] Implement error handling interceptor
- [ ] Set up authentication service and guards
- [ ] Create shared component library
- [ ] Organize feature modules with routing

### Development Standards
- [ ] Configure TypeScript strict mode
- [ ] Set up linting and formatting rules
- [ ] Implement consistent naming conventions
- [ ] Create reusable mixins and utilities
- [ ] Set up development and production builds

### Security & Performance
- [ ] Implement JWT authentication flow
- [ ] Add input validation and sanitization
- [ ] Configure lazy loading for features
- [ ] Implement service-level caching
- [ ] Add error boundaries and fallbacks

### Testing & Quality
- [ ] Set up unit testing framework
- [ ] Implement component testing patterns
- [ ] Add end-to-end testing
- [ ] Configure code coverage thresholds
- [ ] Set up continuous integration

---

## Dependencies & Versions

### Core Dependencies
```json
{
  "@angular/core": "^19.2.6",
  "@angular/material": "^19.2.3",
  "@angular/cdk": "^19.2.3",
  "rxjs": "~7.8.0",
  "ag-grid-angular": "^31.2.1",
  "chart.js": "^4.4.8",
  "ng2-charts": "^8.0.0"
}
```

### Development Tools
```json
{
  "@angular/cli": "^19.2.7",
  "typescript": "^5.8.2",
  "jasmine-core": "~5.1.0",
  "karma": "~6.4.0"
}
```

---

This architecture guide provides a solid foundation for building modern, scalable Angular applications. Adapt the patterns and practices to fit your specific project requirements while maintaining consistency and best practices throughout your development process.