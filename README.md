# Ping Pong App

A full-stack ping pong tournament and player management system with Elo ratings, achievements, and live match tracking.

## Architecture

- **Frontend**: Angular 18+ with Material Design
- **Backend**: Java Spring Boot with PostgreSQL
- **Infrastructure**: AWS (ECS, ECR, S3, CloudFront)
- **Deployment**: Docker containers with Terraform

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

- 🏓 Player registration and profiles
- 🏆 Tournament management (single/double elimination)
- 📊 Elo rating system with customizable curves
- 🎯 Achievement system
- 📱 Responsive design
- 🔐 JWT authentication
- 📧 Email notifications
- 📈 Statistics and analytics
- 🎨 Dark/light theme support

## Deployment

The application is designed to run on AWS with:
- ECS Fargate for container orchestration
- CloudFront CDN for frontend delivery
- RDS PostgreSQL for data storage
- ECR for container registry

See `/infra` directory for complete infrastructure setup.

## Development

Each component has its own README with detailed setup instructions:
- [Frontend Documentation](./frontend/README.md)
- [Backend Documentation](./backend/README.md)
- [Infrastructure Documentation](./infra/README.md)