# StudyVault

StudyVault is a full-stack personal Markdown knowledge base for capturing, organizing, and revisiting study notes. It is built as a modular monolith with a Vue single-page application, a Spring Boot REST API, and PostgreSQL persistence.

The project demonstrates practical application engineering: authenticated user flows, ownership-scoped data access, safe Markdown rendering, database-backed search, file transfer, revision history, study-task tracking, and an optional AI integration that remains useful without an API key.

## Key Features

- Registration, login, logout, and session restoration with an HttpOnly JWT cookie
- Markdown note editor with preview, local draft recovery, and sanitized rendering
- Search, filtering, pagination, and sorting backed by Spring Data JPA specifications
- User-owned tags with color choices and note assignment/removal
- Favorites, soft deletion, trash recovery, and explicit permanent deletion
- Note revision history with ownership-scoped preview and restore
- Markdown import plus single-note and ZIP export
- Embedded note images with resize, crop, rename, and move controls
- Study tasks with due dates, status tracking, and links to notes
- Optional note summaries from an OpenAI-compatible provider or a deterministic local fallback
- Light/dark themes, responsive layouts, and keyboard shortcuts
- Health and readiness endpoints for deployment monitoring

## Tech Stack

| Area | Technologies |
| --- | --- |
| Frontend | Vue 3, TypeScript, Vite, Pinia, Vue Router |
| Markdown | `marked`, `DOMPurify` |
| Backend | Java 17, Spring Boot 3.3, Spring Web, Spring Security |
| Persistence | Spring Data JPA, PostgreSQL 16, Flyway |
| Testing | JUnit 5, Mockito, MockMvc, Spring Data JPA tests, H2 |
| Delivery | Docker multi-stage builds, Docker Compose, Nginx |

## Architecture

```text
Browser
  -> Vue views + Pinia stores + Vue Router
  -> frontend/src/api.ts (same-origin /api requests)
  -> Nginx (production) or Vite proxy (development)
  -> Spring Security JWT cookie filter
  -> REST controllers and DTOs
  -> Services (ownership, validation, transactions)
  -> Spring Data repositories and specifications
  -> PostgreSQL
```

The application is intentionally a modular monolith. Controllers map HTTP requests to DTOs, services own business rules and transaction boundaries, and repositories expose user-scoped persistence queries. The frontend uses the same `/api` contract in development and production.

Search predicates and pagination are executed by the database rather than by filtering a complete in-memory note list. Flyway migrations provide the schema history, while Hibernate validates the deployed schema at startup.

## AI Implementation

AI is isolated behind the `AiSummaryProvider` interface:

1. `NoteSummaryService` verifies that the authenticated user owns the note.
2. `OpenAiSummaryProvider` can call any OpenAI-compatible chat-completions endpoint with configured timeout handling.
3. `LocalSummaryProvider` supplies a deterministic fallback when no provider key is configured.
4. Provider failures are translated into stable application errors for the UI.

Generated summaries are persisted through a dedicated summary endpoint. They do not overwrite the note title or Markdown content, and the API key is read only by the backend.

## Project Structure

```text
.
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/studyvault/
│       │   ├── config/          # security and application configuration
│       │   ├── controller/      # REST endpoints and DTO mapping
│       │   ├── dto/             # request and response models
│       │   ├── entity/          # JPA entities
│       │   ├── exception/       # stable errors and global handler
│       │   ├── repository/      # user-scoped data access
│       │   ├── security/        # JWT service and cookie filter
│       │   └── service/         # business logic and AI providers
│       ├── main/resources/
│       │   └── db/migration/    # Flyway migrations
│       └── test/                # unit, controller, and repository tests
├── frontend/
│   ├── package.json
│   └── src/
│       ├── views/               # auth, dashboard, notes, tags, trash, tasks
│       ├── stores/              # Pinia state
│       ├── api.ts               # API client
│       └── router.ts            # routes and auth guard
├── deploy/
│   ├── docker-compose.yml
│   ├── docker-compose.prod.yml
│   ├── nginx.Dockerfile
│   ├── nginx.conf
│   └── backup.sh
├── .env.example
├── .env.production.example
└── README.md
```

## Security

- Authentication uses a stateless JWT in the `STUDYVAULT_TOKEN` HttpOnly, SameSite=Lax cookie.
- Passwords are hashed with BCrypt; secrets and database credentials come from environment variables.
- Every note, tag, search, import, export, revision, task, and summary operation is scoped to the authenticated user.
- Missing or unauthorized notes resolve to stable not-found errors rather than revealing ownership.
- DTOs prevent JPA entities and password hashes from being serialized directly.
- Markdown is parsed with `marked` and sanitized with `DOMPurify` before preview.
- Uploaded Markdown is treated as text, with filename sanitization and size limits.
- Request logging records operational metadata only; it does not log cookies, tokens, passwords, request bodies, or API keys.

## Testing

The backend test suite covers authentication, authorization boundaries, controller responses, validation errors, note lifecycle behavior, search specifications, revisions, tags, file transfer, study tasks, and AI provider behavior.

Run the checks from the repository root:

```bash
(cd backend && mvn test)
(cd frontend && npm run build)
```

`npm run build` performs the frontend TypeScript check and production Vite build. The frontend does not currently define a separate `npm test` script.

## Setup

### Prerequisites

- Docker Desktop or Docker Engine with `docker compose` (recommended)
- Java 17+ and Maven 3.9+ for local backend development
- Node.js 20+ and npm for local frontend development

### Docker Compose

1. Create the local environment file:

   ```bash
   cp .env.example .env
   ```

2. Set `POSTGRES_PASSWORD` and a `JWT_SECRET` with at least 32 characters.

3. Start the stack:

   ```bash
   docker compose --env-file .env -f deploy/docker-compose.yml up -d --build
   ```

4. Open [http://localhost](http://localhost). To use another host port, set `APP_PORT`, for example:

   ```bash
   APP_PORT=8080 docker compose --env-file .env -f deploy/docker-compose.yml up -d --build
   ```

The Compose stack runs PostgreSQL, the Spring Boot backend, and Nginx. Only Nginx is exposed publicly; PostgreSQL stays on the internal Docker network.

### Local Development

Start a PostgreSQL 16 instance, then export the backend settings shown in `.env.example` and run:

```bash
(cd backend && mvn spring-boot:run)
(cd frontend && npm install && npm run dev)
```

The Vite server runs on [http://localhost:5173](http://localhost:5173) and proxies `/api` to `http://localhost:8080` by default. Flyway applies migrations when the backend starts.

### Essential Configuration

| Variable | Purpose |
| --- | --- |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | PostgreSQL and backend connection |
| `JWT_SECRET` | JWT signing secret; minimum 32 characters |
| `JWT_EXPIRATION_SECONDS` | Cookie token lifetime; defaults to `86400` |
| `AUTH_COOKIE_SECURE` | Set `true` when serving over HTTPS |
| `APP_PORT` | Public Nginx host port; defaults to `80` |
| `AI_API_KEY` | Optional backend-only provider key |
| `AI_API_URL`, `AI_MODEL` | Optional OpenAI-compatible provider settings |
| `AI_LOCAL_FALLBACK` | Keep `true` to use summaries without a provider key |

Never commit `.env`, real credentials, cookies, or database dumps. Use the example files as templates and provide secrets through the runtime environment.

## Operational Endpoints

```text
GET /api/health  -> process liveness
GET /api/ready   -> dependency readiness (including PostgreSQL)
```

Both endpoints are available through the Nginx public port in the Compose deployment.
