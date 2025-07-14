# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

- `npm start` - Start development server
- `npm run build` - Build for production
- `npm run watch` - Development build with watch mode
- `npm test` - Run unit tests
- `npm test -- --include=src/app/path/to/spec.ts` - Run specific test file

## Code Style Guidelines

- **TypeScript**: Use strict mode, proper typing for all properties and methods
- **Imports**: Group by category (Angular core, then components, then services)
- **Naming**: PascalCase for classes/interfaces, camelCase for methods/properties
- **Components**: Use standalone components with proper input/output declarations
- **Services**: Extend BaseHttpService for API calls with standardized error handling
- **Error Handling**: Use the AlertService for user-facing error messages
- **CSS/SCSS**: Follow 7-1 architecture pattern with global theme variables

## Project Structure

- Feature-based modules with lazy loading
- Shared components in _shared directory
- Models and interfaces in _models directory
- Services in _services directory
- Helpers and utilities in _helpers directory
