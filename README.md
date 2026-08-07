# WrenchLog — Backend

Spring Boot REST API for WrenchLog: user auth (JWT via httpOnly cookies), vehicles, service logs, service reminders, notes, and file attachments.

## Tech stack

- Java 21
- Spring Boot 4 (Web, Data JPA, Security, Validation)
- PostgreSQL
- Flyway (database migrations)
- JJWT (JWT generation/validation)
- Maven

## Prerequisites

- Java 21 (JDK)
- Maven (or use the included wrapper, if present)
- Docker (used to run PostgreSQL locally — see below)

## Environment variables

The app reads these at startup — set them in your IDE run configuration, or export them in your shell before running.

| Variable | Example | Notes |
|---|---|---|
| `DB_USER` | `postgres` | Database username |
| `DB_PASSWORD` | `yourpassword` | Database password |
| `JWT_SECRET` | random 256-bit+ string | Signs JWTs — keep this secret, never commit it |
| `JWT_EXPIRATION_MS` | `86400000` | Token lifetime in milliseconds (86400000 = 24h) |
| `SPRING_PROFILES_ACTIVE` | `dev` | Defaults to `dev` if unset |

Profile-specific settings (DB URL, upload folder, CORS origin, cookie security) live in `application-dev.properties` / `application-prod.properties` — `dev` is used automatically unless `SPRING_PROFILES_ACTIVE=prod` is set.

## Running locally

1. Start PostgreSQL via Docker — this single command creates the database too:
   ```bash
   docker run -d --name wrenchlog-db \
     -e POSTGRES_USER=postgres \
     -e POSTGRES_PASSWORD=yourpassword \
     -e POSTGRES_DB=wrenchlog_dev \
     -p 5432:5432 \
     postgres:15-alpine
   ```
2. Set the environment variables above (via IDE run config is easiest) — make sure `DB_USER`/`DB_PASSWORD` match what you set in the `docker run` command.
3. Run the app:
   ```
   mvn spring-boot:run
   ```
   or run `WrenchLogApplication` directly from your IDE.
4. Flyway applies all migrations automatically on startup — no manual schema setup needed.
5. API is available at `http://localhost:8080`.

**Stopping/removing the local database container:**
```bash
docker stop wrenchlog-db
docker rm wrenchlog-db
```
Since no volume is mounted, this also deletes the data — fine for local dev/testing, but be aware a `stop` alone (without `rm`) preserves the container and its data for next time; only `rm` wipes it.

## Running the tests

```
mvn test
```

Tests use Mockito to mock dependencies — no real database or running server is needed to run the suite.

## Project structure (high level)

```
controller/   REST endpoints — HTTP concerns only, delegates to repositories/services
service/      Business logic (ownership checks, file storage, user auth)
repository/   Spring Data JPA interfaces
model/        JPA entities
dto/          Request/response objects (records where possible)
security/     JWT filter, JWT service, Spring Security config
exception/    Global exception handling
```

## Key architectural notes

- **Auth:** JWT is issued on login and set as an httpOnly cookie (`auth_token`) — never exposed to JS. `/api/auth/me` lets the frontend check current login state.
- **Ownership:** every vehicle-scoped resource (service logs, notes, reminders, files) is checked against the authenticated user via `VehicleAccessService` — never trust a client-supplied user/vehicle ID.
- **Validation:** all write endpoints use dedicated `*CreateDTO` records with Bean Validation (`@Valid`) — entities are never accepted directly as request bodies.
- **Errors:** centralized in `GlobalExceptionHandler` — controllers don't need their own try/catch for standard cases (not found, forbidden, validation failure).

## Deployment

Production runs via Docker (see the root-level deployment notes / `Dockerfile`). Key production env vars: `DATABASE_URL`, `FILE_UPLOAD_DIR`, `CORS_ALLOWED_ORIGIN`, plus the ones listed above. `app.cookie.secure` is `true` in prod (requires real HTTPS), `false` in dev.
