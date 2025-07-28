# Simple frontend without nginx
FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./
RUN npm ci --legacy-peer-deps

# Copy source and build
COPY . .
RUN npm run build --configuration=production

# Install Angular CLI globally to serve the app
RUN npm install -g @angular/cli

# Serve the built application directly with Angular CLI
EXPOSE 4200
CMD ["ng", "serve", "--host", "0.0.0.0", "--port", "4200", "--configuration", "production"]