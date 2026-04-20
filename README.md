# Notification Dispatcher

Portfolio-ready polyglot microservices project focused on reliability, idempotency, async delivery, and real-time notifications.

## Highlights

- Java 21 + Spring Boot API for request validation, idempotency, rate limiting, and audit persistence.
- Node.js + Socket.IO service for real-time push delivery and browser subscriptions.
- .NET 8 worker for email and SMS template processing on separate channels.
- PostgreSQL for audit history and Redis for rate limiting, idempotency, and pub/sub dispatching.

## Architecture

```text
Client -> Java API (8080) -> PostgreSQL
                     |
                     +-> Redis pub/sub -> Node.js push service (3000) -> Browser
                     |
                     +-> Redis pub/sub -> .NET worker -> delivery log channel
```

## Tech Stack

| Service | Stack | Responsibility |
| --- | --- | --- |
| API | Java 21, Spring Boot 3.3, JPA, Redis | Validation, idempotency, rate limiting, audit trail |
| Push | Node.js, Express, Socket.IO, ioredis | WebSocket subscriptions and push fan-out |
| Worker | .NET 8 Worker Service, StackExchange.Redis | Email/SMS template rendering and delivery logs |
| Infra | PostgreSQL 16, Redis 7, Docker Compose | Persistence, messaging, local orchestration |

## Repository Layout

```text
notification-service-java/
notification-service-nodejs/
notification-worker-dotnet/
scripts/
docker-compose.yml
init-db.sql
```

## Quick Start

### Prerequisites

- Docker Desktop or Docker Engine with Compose support
- Node.js 20+ for local Node tests
- .NET 8 SDK for local worker build
- Java 21 if you want to run the API outside Docker

### Environment

Copy `.env.example` to `.env` and adjust values if needed.

Key defaults:

- `POSTGRES_HOST=localhost`
- `POSTGRES_PORT=5432`
- `REDIS_HOST=localhost`
- `REDIS_PORT=6379`
- `JAVA_SERVICE_PORT=8080`
- `NODE_SERVICE_PORT=3000`

### Run the full stack

```bash
docker compose up --build
```

Services:

- Java API: `http://localhost:8080/api/v1/notifications/health`
- Node.js health: `http://localhost:3000/api/v1/health`
- Browser client: `http://localhost:3000`
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`

### Stop the stack

```bash
docker compose down
```

## Validation Commands

From the repository root:

```bash
npm run test:java:ps
npm run test:node
npm run test:node:coverage
dotnet build .\notification-worker-dotnet\notification-worker-dotnet.csproj
```

Smoke test after the stack is up:

```powershell
npm run smoke:ps
```

```bash
npm run smoke:sh
```

The smoke tests validate:

- Java API health
- Node.js service health
- push notification acceptance
- email notification acceptance
- sms notification acceptance
- idempotent replay returning the same `notificationId`
- rate limiting returning at least one `HTTP 429`

## Local Development

### Java API

Run with Maven:

```bash
mvn test
mvn spring-boot:run
```

Windows fallback without local Maven installation:

```powershell
npm run test:java:ps
```

Notes:

- The API now defaults to `localhost` for PostgreSQL and Redis when run outside Docker.
- Idempotent replays are resolved before rate limiting, so safe retries do not consume quota.
- Audit timestamps use offset-aware types aligned with PostgreSQL `TIMESTAMP WITH TIME ZONE`.

### Node.js Push Service

```bash
cd notification-service-nodejs
npm install
npm test
npm run dev
```

Notes:

- Local development defaults to Redis on `127.0.0.1`.
- The Jest suite now executes every unit and integration test in `test/**/*.test.js`.

### .NET Worker

```bash
dotnet build .\notification-worker-dotnet\notification-worker-dotnet.csproj
dotnet run --project .\notification-worker-dotnet\notification-worker-dotnet.csproj
```

Notes:

- Offline `NuGetAudit` noise is disabled for cleaner builds in restricted environments.
- Invalid Redis payloads are rejected before template processing to avoid avoidable log noise.

## Sample API Request

```bash
curl -X POST http://localhost:8080/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: portfolio-client-001" \
  -H "Idempotency-Key: request-123" \
  -d '{
    "userId": "user-demo",
    "channel": "push",
    "subject": "Welcome",
    "body": "Your notification pipeline is live"
  }'
```

Expected response:

```json
{
  "notificationId": "generated-id",
  "clientId": "portfolio-client-001",
  "channel": "push",
  "status": "PENDING",
  "createdAt": "2026-04-20T16:00:00Z"
}
```

## What This Project Demonstrates

- Stable cross-service contracts using Redis channels
- Clean separation between synchronous API work and async processing
- Practical QA coverage with Node unit/integration tests and Java controller/use-case tests
- Operational concerns such as idempotency, rate limiting, health checks, and smoke scripts
