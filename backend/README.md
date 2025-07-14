# 🏓 Ping Pong Elo Rating System

A modern, feature-rich Spring Boot application for tracking and analyzing ping pong matches using an advanced Elo rating system.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

## 📋 Overview

This application transforms ping pong from casual office fun into a competitive sport with detailed statistics, rankings, and achievements. It features a sophisticated Elo algorithm, player style analytics, tournaments management, and progressive achievements.

## ✨ Key Features

### Player Management
- **Comprehensive Player Profiles**
  - Personal details and customizable profile pictures
  - Performance statistics across different game types
  - Skill history tracking with visual ELO progression charts
  - Individual player style ratings

### Game & Match System
- **Multi-Format Matches**
  - Singles and doubles formats
  - Ranked and casual (normal) play categories
  - Detailed match history with statistics
  - Game confirmation and rejection workflow

### Advanced Rating System
- **Sophisticated Elo Algorithm**
  - Dynamic K-factors based on player experience
  - Point differential weighting (bigger wins = bigger rating changes)
  - Rating curve with adjustable thresholds (currently 800-1600)
  - Separate rating tracks for different game formats

### Player Style Analytics
- **Style Rating System**
  - 10 distinct playing styles (Power, Spin, Creative, Aggressive, etc.)
  - Player-to-player style reviews
  - Community average style ratings
  - Top player recognition for each style

### Achievement System
- **Progressive Challenges**
  - Multi-tier achievements (Easy, Medium, Hard, Legendary)
  - Performance-based evaluators (Win streaks, Unique opponents, Rating thresholds)
  - One-time and progressive achievement types
  - Achievement points and leaderboard

### Tournament Management
- **Complete Tournament Support**
  - Multiple tournament formats including double elimination
  - Automatic bracket generation
  - Match scheduling and progression tracking
  - Tournament-specific statistics

### Security & Communication
- **Modern Security**
  - JWT-based authentication
  - Role-based access control
  - Secure password management with reset functionality
  - Environment variable configuration for sensitive data
  
- **Email Notifications**
  - Game confirmation requests
  - Game rejection notifications
  - Password reset links
  - Customizable email templates

## 🚀 Getting Started

### Prerequisites

- Java 21 or higher (21 is recommended)
- Gradle 7.0+ or use the included Gradle wrapper
- PostgreSQL (for production) or H2 (for development)

### Quick Start

1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/java-ping-pong-elo.git
   cd java-ping-pong-elo
   ```

2. **Build the application:**
   ```bash
   ./gradlew clean build
   ```

3. **Run the application:**
   ```bash
   ./gradlew bootRun
   ```

4. **Access the application:**
   The server will start on `http://localhost:8080`

### Database Setup

For development, the application uses H2 database by default with files stored in the `./data` directory.

For production, configure your PostgreSQL database in `application.properties` or through environment variables.

## 🛠️ Configuration

### Environment Variables

For secure deployment, configure these environment variables:

```
# Security
JWT_SECRET=your_secure_jwt_secret
JWT_EXPIRATION=86400000

# Email
MAIL_HOST=your_smtp_server
MAIL_PORT=587
MAIL_USERNAME=your_email_username
MAIL_PASSWORD=your_email_password

# CORS
ALLOWED_ORIGINS=https://yourfrontend.com,https://other-allowed-origin.com

# Database (Production)
SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/pingpongdb
SPRING_DATASOURCE_USERNAME=db_username
SPRING_DATASOURCE_PASSWORD=db_password
```

### Elo Curve Configuration

The application uses a custom Elo curve that can be configured in `application.properties`:

```properties
elo.curve.enabled=true
elo.curve.upper-threshold=1600  # Ratings above this receive reduced gains
elo.curve.lower-threshold=800   # Ratings below this receive boosted gains
elo.curve.gain-boost-factor=1.2 # Boost factor for lower-rated players
elo.curve.loss-reduction-factor=0.8 # Reduction factor for lower-rated players
```

## 🧮 How the Elo Rating Works

The application uses a modified Elo rating system specifically tuned for ping pong:

```
New Rating = Old Rating + K × Point Factor × (Actual Result - Expected Result)
```

Where:
- **K-factor** decreases as players gain experience
- **Point Factor** increases with larger score differentials
- **Actual Result** is 1 for a win, 0 for a loss
- **Expected Result** is calculated based on rating differences

The Elo curve further adjusts rating changes to help newer players catch up while stabilizing ratings for experienced players.

## 🔄 API Reference

### Authentication
- `POST /api/auth/register` - Register a new player
- `POST /api/auth/login` - Login and receive JWT token
- `POST /api/auth/logout` - Logout current session

### Players
- `GET /api/players` - Get all players
- `GET /api/players/{id}` - Get player by ID
- `GET /api/players/username/{username}` - Get player by username
- `PUT /api/players/{id}` - Update player details
- `GET /api/players/{id}/style-ratings` - Get player style ratings
- `PUT /api/players/{id}/style-ratings` - Update player style ratings

### Games
- `POST /api/games` - Save new games
- `GET /api/games` - Get all games
- `GET /api/games/{id}` - Get game by ID
- `GET /api/games/player/{playerId}` - Get games by player ID
- `DELETE /api/games/{id}` - Delete a game
- `PATCH /api/games/reset` - Reset all player ratings

### Matches
- `POST /api/matches` - Create a new match
- `GET /api/matches` - Get all matches
- `GET /api/matches/{id}` - Get match by ID
- `GET /api/matches/player/{playerId}` - Get matches by player ID
- `PUT /api/matches/{id}` - Update a match
- `POST /api/matches/{id}/conclude` - Conclude a match (calculate ratings)
- `DELETE /api/matches/{id}` - Delete a match

### Tournaments
- `POST /api/tournaments` - Create a new tournament
- `GET /api/tournaments` - Get all tournaments
- `GET /api/tournaments/{id}` - Get tournament by ID
- `PUT /api/tournaments/{id}` - Update tournament
- `POST /api/tournaments/{id}/start` - Start a tournament

### Game Confirmations
- `POST /api/confirmations` - Create a game confirmation request
- `PUT /api/confirmations/{id}/approve` - Approve a confirmation request
- `PUT /api/confirmations/{id}/reject` - Reject a confirmation request

### Achievements
- `GET /api/achievements` - Get all achievements
- `GET /api/achievements/{id}` - Get achievement by ID
- `GET /api/achievements/player/{playerId}` - Get player achievements
- `POST /api/achievements/evaluate/{playerId}` - Evaluate achievements for player

## 🧪 Testing

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.example.javapingpongelo.services.achievements.WinCountEvaluatorTest"

# Generate test coverage report
./gradlew jacocoTestReport
```

### Email Template Testing

The application includes tools for testing email templates:

#### Browser Preview
```
http://localhost:8080/test/email-preview/confirmation
http://localhost:8080/test/email-preview/rejection
```

#### Send Test Emails
```
http://localhost:8080/api/test/email/confirmation?email=your@email.com
http://localhost:8080/api/test/email/rejection?email=your@email.com
```

## 🛡️ Security

### Password Policy
- Passwords are stored using BCrypt encryption
- Password reset tokens expire after 30 minutes
- Authentication uses JWT with a configurable expiration time

### API Security
- JWT authentication for all protected endpoints
- CORS protection with configurable allowed origins
- Role-based access control for administrative functions

## 🚦 Development Workflow

1. Create a feature branch from `main`
2. Implement and test your changes locally
3. Ensure all tests pass with `./gradlew test`
4. Submit a pull request for review
5. After approval, merge to `main`

## 📚 Technologies

- **Core**: Java 21, Spring Boot 3.4.3
- **Security**: Spring Security, JJWT
- **Database**: Spring Data JPA, PostgreSQL (prod), H2 (dev)
- **Build**: Gradle 7+
- **Testing**: JUnit 5, Mockito
- **Other**: Lombok, Thymeleaf (email templates)

## 📝 License

[MIT License](LICENSE)

## 👥 Contributing

Contributions are welcome! Please feel free to submit a pull request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a pull request

## 🙏 Acknowledgements

- The Elo rating system, developed by Arpad Elo
- Spring Boot framework and community
- All contributors who have helped shape this project