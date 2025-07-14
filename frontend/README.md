# PingPong Stats & Ranking System

<div align="center">
  <img src="https://img.shields.io/badge/Angular-19.2-DD0031?style=for-the-badge&logo=angular&logoColor=white" alt="Angular 19.2" />
  <img src="https://img.shields.io/badge/Material-19.2-3f51b5?style=for-the-badge&logo=material-design&logoColor=white" alt="Angular Material 19.2" />
  <img src="https://img.shields.io/badge/Chart.js-4.4-FF6384?style=for-the-badge&logo=chart.js&logoColor=white" alt="Chart.js 4.4" />
  <img src="https://img.shields.io/badge/TypeScript-5.8-3178C6?style=for-the-badge&logo=typescript&logoColor=white" alt="TypeScript 5.8" />
</div>

<br />

A comprehensive platform for table tennis enthusiasts to track matches, analyze performance metrics, and compete in a dynamic Elo-based ranking system. The application features a responsive, material design interface with advanced data visualization, match tracking, and player progression analytics.

## 🌟 Key Features

- **Player Profiles & Authentication**: Secure account system with personalized player profiles
- **Match Management**: Create and record singles/doubles matches with detailed scoring
- **Advanced ELO Rating System**: Sophisticated algorithm adjusts ratings based on match outcomes and opponent skill
- **Performance Analytics**: Rich data visualization with win-rate tracking, game type distribution, and playing style analysis
- **Comprehensive Statistics**: Track progress over time with detailed player stats and historical data
- **Leaderboards**: Filter and sort players by game type, rating, and win rate
- **Player Style Profiling**: Identify and develop your unique playing style with skill radar visualization
- **Customizable UI Themes**: Toggle between light and dark mode with customized primary color themes
- **Skill Development Insights**: Get personalized recommendations to improve your game

## 🚀 Getting Started

### Prerequisites

- Node.js 18.x or higher
- npm 9.x or higher

### Installation

```bash
# Clone the repository
git clone https://github.com/yourusername/pingpong-website.git

# Navigate to project directory
cd pingpong-website

# Install dependencies
npm install

# Start the development server
npm start
```

Visit `http://localhost:4200/` to access the application.

## 💻 Technology Stack

### Frontend

- **Framework**: Angular 19
- **UI Components**: Angular Material
- **Data Visualization**: Chart.js with ng2-charts
- **Grid System**: AG Grid Angular
- **State Management**: RxJS Observables
- **Styling**: SCSS with custom theming system

### Architecture Highlights

- **Component-Based Design**: Modular, reusable components
- **Comprehensive Type Safety**: TypeScript interfaces throughout
- **Centralized Styling System**: Shared variables, mixins, and utilities
- **Responsive Design**: Mobile-first approach with flexible layouts

## 📱 Application Structure

```
src/
├── app/                  # Application code
│   ├── _config/          # App configuration
│   ├── _helpers/         # Guards and interceptors
│   ├── _models/          # TypeScript interfaces
│   ├── _services/        # API communication and business logic
│   ├── _shared/          # Shared components and modules
│   ├── achievement/      # Achievement system
│   ├── game/             # Game/match components
│   ├── player/           # Player profile components
│   ├── tournament/       # Tournament system
│   └── ...               # Other feature modules
├── assets/               # Static assets
└── styles/               # Global styling system
    ├── abstracts/        # Variables, functions, mixins
    ├── base/             # Base styles, reset, typography
    ├── components/       # Component-specific styles
    ├── layout/           # Layout containers
    └── themes/           # Light/dark themes
```

## 🎨 Styling System

The application uses a comprehensive SCSS architecture with:

- **Tokens**: Central repository for colors, spacing, typography
- **Mixins**: Reusable style patterns
- **Themes**: Light/dark mode with customizable primary colors
- **Component Styles**: Consistent patterns across UI elements

### Theme System

```scss
// Example of theme usage
.element {
  color: var(--text-color);
  background: var(--surface-color);
  
  .dark-theme & {
    // Dark theme overrides handled automatically
  }
}
```

## 📊 Data Visualization

The application includes various visual representations of player data:

- **Rating History Charts**: Track ELO progression over time
- **Win Rate Distribution**: Compare performance across game types
- **Style Radar**: Visual representation of playing strengths
- **Rank Position Tracking**: Monitor leaderboard movement

## 🧩 Core Components

### Shared Component Examples

```html
<!-- Loading Spinner -->
<app-loading-spinner 
  [diameter]="40" 
  [message]="'Loading player data...'"
></app-loading-spinner>

<!-- Player Card -->
<app-player-card 
  [player]="player"
  [compact]="true"
></app-player-card>

<!-- Rating Badge -->
<app-rating-badge 
  [rating]="player.singlesRankedRating"
></app-rating-badge>
```

## 🔄 HTTP Communication

Services extend `BaseHttpService` for consistent API communication:

```typescript
export class PlayerService extends BaseHttpService {
  private endpoint = '/api/players';

  getPlayerById(id: string): Observable<Player | null> {
    return this.get<Player>(`${this.endpoint}/${id}`, undefined, null);
  }

  updatePlayer(player: PlayerUpdateDto): Observable<Player> {
    return this.put<Player>(`${this.endpoint}/${player.id}`, player);
  }
}
```

## 🛣️ Roadmap

- **Tournament Management**: Create and manage tournaments with brackets
- **Social Features**: Friend system, player messaging, match invitations
- **Advanced Statistics**: More detailed performance analytics
- **Offline Support**: Play and record matches without internet connection
- **Progressive Web App**: Installation on mobile devices
- **Internationalization**: Multi-language support

## 🔧 Development Guidelines

### Creating New Components

1. Utilize shared components whenever possible
2. Follow Angular best practices and style guide
3. Implement proper typing with interfaces
4. Use the styling system for consistent UI
5. Write unit tests for complex logic

### Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📜 Documentation

API documentation is available in-code with JSDoc comments. Components and services include purpose, usage, and parameter descriptions.

## 📝 License

Distributed under the MIT License. See `LICENSE` for more information.

---

<div align="center">
  Developed with ❤️ by the PingPong Stats Team
</div>
