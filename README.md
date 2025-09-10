# Ping Pong Elo Rocket 🏓🚀

A comprehensive ping pong tournament and player management system with advanced Elo ratings, dynamic achievements, Easter egg hunting, and real-time features.

## Architecture

- **Frontend**: Angular 19+ with Material Design & Custom Components
- **Backend**: Java 21 Spring Boot with WebSocket support
- **Database**: PostgreSQL with JPA/Hibernate and optimistic locking
- **Real-time**: STOMP WebSocket messaging for live updates
- **Infrastructure**: AWS (ECS Fargate, ECR, S3, CloudFront, RDS)
- **Deployment**: Multi-stage Docker builds with GitHub Actions CI/CD

## Project Structure

```
ping-pong-app/
├── frontend/          # Angular frontend application
├── backend/           # Java Spring Boot backend API
├── infra/             # Terraform infrastructure as code
└── docs/              # Documentation
```

## Quick Start

### Prerequisites
- Node.js 18+
- Java 21+
- Docker
- AWS CLI
- Terraform

### Development

1. **Backend**:
   ```bash
   cd backend
   ./gradlew bootRun
   ```

2. **Frontend**:
   ```bash
   cd frontend
   npm install
   npm start
   ```

3. **Infrastructure**:
   ```bash
   cd infra
   terraform init
   terraform plan
   terraform apply
   ```

## Features

### Core Gameplay
- 🏓 **Player Management**: Registration, profiles, and comprehensive player statistics
- 🏆 **Tournament Engine**: Advanced single/double elimination with bracket visualization
- 📊 **Dynamic Elo System**: Customizable rating curves with gain/loss adjustments
- 🎮 **Match Builder**: Intuitive interface for recording and managing matches
- 📈 **Live Statistics**: Real-time player rankings and performance analytics

### Achievement System
- 🎯 **Smart Achievement Engine**: YAML-configurable achievement system with 25+ achievements
- 🏅 **Dynamic Unlocking**: Context-aware achievements that unlock based on player actions
- 🔔 **Real-time Notifications**: Instant achievement notifications with visual celebrations
- 📊 **Achievement Analytics**: Track completion rates and player progression
- 🎨 **Beautiful Achievement Cards**: Animated cards with rarity-based styling

### Easter Egg Hunting
- 🥚 **Interactive Easter Eggs**: Hidden clickable eggs throughout the application
- 🎲 **Random Spawning**: Configurable spawn rates with 6 rarity tiers (Common to Mythical)
- 🎨 **Multiple Variants**: 3 unique visual designs per rarity tier (18 total designs)
- 🏆 **Hunter Leaderboard**: Secret leaderboard accessible only to egg finders
- ⚡ **Real-time Updates**: WebSocket-powered live egg spawning and claiming
- 📊 **Hunting Statistics**: Track finds, streaks, and performance metrics

### User Experience
- 📱 **Responsive Design**: Mobile-first approach with Material Design components
- 🔐 **Secure Authentication**: JWT-based auth with email verification
- 📧 **Email System**: Automated notifications for tournaments and achievements
- 🎨 **Modern UI/UX**: Clean, intuitive interface with smooth animations
- ⚡ **Real-time Updates**: Live match updates and notifications via WebSocket

### Developer Features
- 🔧 **Configuration Management**: Externalized configuration for all major features
- 📝 **Comprehensive Logging**: Structured logging with configurable levels
- 🏥 **Health Monitoring**: Built-in health checks and metrics
- 🔒 **Security Best Practices**: Rate limiting, input validation, and secure defaults

## Configuration

### Achievement System
Achievements are configured via YAML in `backend/src/main/resources/achievements-config.yaml`:
```yaml
achievements:
  - id: "first_win"
    name: "First Victory"
    description: "Win your first match"
    points: 50
    rarity: "COMMON"
    triggerType: "MATCH_RESULT"
    # ... additional configuration
```

### Easter Egg System
Easter egg spawn rates and behavior configured in `application.properties`:
```properties
easter-egg.spawn.common-rate=45.0
easter-egg.spawn.mythical-rate=0.5
easter-egg.max-active-eggs=3
easter-egg.cleanup.inactive-eggs-minutes=5
```

## Deployment

### AWS Infrastructure
- **ECS Fargate**: Container orchestration with auto-scaling
- **CloudFront CDN**: Global frontend delivery with edge caching
- **RDS PostgreSQL**: Multi-AZ database with automated backups
- **ECR**: Private container registry for Docker images
- **Application Load Balancer**: WebSocket and HTTP traffic routing

### Automated CI/CD
- **GitHub Actions**: Automated build, test, and deployment pipeline
- **Multi-stage Docker**: Optimized builds with security scanning
- **Infrastructure as Code**: Complete AWS setup via Terraform
- **Blue/Green Deployments**: Zero-downtime deployments with rollback capability

See `/infra` directory for complete infrastructure setup and deployment configuration.

## Development

Each component has its own README with detailed setup instructions:
- [Frontend Documentation](./frontend/README.md)
- [Backend Documentation](./backend/README.md)
- [Infrastructure Documentation](./infra/README.md)