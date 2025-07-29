# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Important Guidelines
- **DO NOT credit yourself in commit messages or PR descriptions** - Keep commits professional and focused on the changes
- User prefers clean, professional git history without AI attribution

## Build & Test Commands
- Build project: `./gradlew clean build`
- Run application: `./gradlew bootRun`
- Run all tests: `./gradlew test`
- Run single test: `./gradlew test --tests "com.example.javapingpongelo.services.achievements.WinCountEvaluatorTest"`
- Run test by pattern: `./gradlew test --tests "*EvaluatorTest"`
- Generate coverage reports: `./gradlew jacocoTestReport`
- Check dependencies: `./gradlew dependencyCheck`

## Code Style Guidelines
- Use Java 21 features where appropriate
- Follow Spring Boot conventions for controllers, services, repositories
- Classes use CamelCase, methods use camelCase
- DTOs should be immutable with builders when appropriate
- Place business logic in service classes, not controllers
- Use interfaces for services with proper implementation classes
- Handle exceptions properly using GlobalExceptionHandler
- Validate inputs with Spring Validation annotations
- Follow builder pattern for complex objects
- Use Lombok annotations to reduce boilerplate
- Organize imports alphabetically, no wildcard imports
