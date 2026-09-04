# StudyVault

StudyVault is a personal Markdown knowledge base. Authenticated users can create, edit, preview, organize, search, export, import, favorite, trash, restore, and optionally summarize their own notes. Tags are user-owned and can be assigned to notes.

The application is a modular monolith: a Vue single-page application calls a Spring Boot REST API, which stores data in PostgreSQL. In the production-style Compose setup, Nginx serves the Vue build and proxies `/api` requests to the backend.

## Features In This Repository

- Username/email registration and login using an HttpOnly JWT cookie
- Notes with Markdown editing and sanitized preview
- Local draft recovery in the editor; database writes happen on explicit `Save note`
- Embedded photos in note content with manual resize, crop, rename, and move controls
- User-owned tags with color choices and note assignment/removal
- Database-backed search, filtering, pagination, and sorting
- Favorites and soft deletion (`active` -> `trash` -> `active`)
- Markdown import, single-note export, and ZIP export of the current user's notes
- Optional AI summary drafts with explicit save confirmation
- `/api/health` liveness and `/api/ready` dependency readiness endpoints

The dashboard currently provides a welcome view and navigation links. There is no statistics endpoint in the current implementation.

## Tech Stack

- Frontend: Vue 3, TypeScript, Vite, Pinia, Vue Router
- Markdown: `marked` for parsing and `DOMPurify` for sanitization
- Backend: Java 17, Spring Boot 3.3, Spring Web, Spring Security, Spring Data JPA, Bean Validation
- Database: PostgreSQL 16 (production/Compose), H2 for repository tests
- Migrations: Flyway
- Runtime: Docker multi-stage builds, Docker Compose, Nginx
- Tests: JUnit 5, Mockito, MockMvc, Spring Data JPA tests

## Architecture

```text
Browser
  -> Vue Router/views + Pinia auth store
  -> frontend/src/api.ts (same-origin /api requests, credentials included)
  -> Nginx (production) or Vite proxy (development)
  -> Spring Security JWT cookie filter
  -> Controllers (HTTP and DTO mapping)
  -> Services (ownership, validation-related business rules, transactions)
  -> Spring Data repositories (user-scoped queries)
  -> PostgreSQL
```

AI requests follow a separate adapter boundary. `NoteSummaryService` checks note ownership before calling `AiSummaryProvider`; `OpenAiSummaryProvider` calls an OpenAI-compatible endpoint, while the local fallback works without a key.

## Directory Structure

```text
.
├── backend/
│   ├── pom.xml
│   ├── .dockerignore
│   └── src/
│       ├── main/java/com/example/studyvault/
│       │   ├── config/          # security, password encoding, request logging
│       │   ├── controller/      # REST endpoints
│       │   ├── dto/              # request/response records
│       │   ├── entity/           # JPA entities
│       │   ├── exception/        # stable error codes and handler
│       │   ├── repository/       # Spring Data repositories/specifications
│       │   ├── security/         # JWT service and cookie filter
│       │   └── service/          # application and provider services
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/migration/     # Flyway SQL migrations
│       └── test/                 # unit, controller, repository tests
├── frontend/
│   ├── package.json
│   ├── Dockerfile              # standalone frontend build/runtime image
│   ├── nginx.conf              # standalone frontend image config
│   ├── .dockerignore
│   └── src/
│       ├── views/                # login, dashboard, notes, tags, trash
│       ├── stores/auth.ts        # Pinia authentication state
│       ├── api.ts                # fetch client and API methods
│       ├── router.ts             # routes and auth guard
│       └── style.css
├── deploy/
│   ├── docker-compose.yml        # base Compose services
│   ├── docker-compose.prod.yml   # production overrides
│   ├── nginx.Dockerfile
│   ├── nginx.conf
│   └── backup.sh
├── .env.example
├── .env.production.example
└── .dockerignore
```

## Requirements

For local non-container development:

- Java 17+
- Maven 3.9+
- Node.js 20+ and npm
- PostgreSQL 16+ (or a compatible PostgreSQL server)

For the container workflow, Docker Desktop (or Docker Engine) with the `docker compose` command is sufficient.

## Environment Variables

Copy the example file and replace the placeholders. Never commit `.env` or real credentials.

```bash
cp .env.example .env
```

| Variable | Used by | Default/requirement |
| --- | --- | --- |
| `POSTGRES_DB` | Compose/PostgreSQL | Required by Compose; typically `studyvault` |
| `POSTGRES_USER` | Compose/PostgreSQL/backend | Required by Compose |
| `POSTGRES_PASSWORD` | Compose/backend | Required by Compose |
| `DATABASE_URL` | Backend | Defaults to `jdbc:postgresql://localhost:5432/studyvault` outside Compose |
| `DATABASE_USERNAME` | Backend | Defaults to `studyvault` |
| `DATABASE_PASSWORD` | Backend | Required by the backend |
| `JWT_SECRET` | Backend | Required; at least 32 characters |
| `JWT_EXPIRATION_SECONDS` | Backend | `86400` |
| `AUTH_COOKIE_SECURE` | Backend | `false` for local HTTP; `true` behind HTTPS |
| `SERVER_PORT` | Backend | `8080` |
| `APP_PORT` | Compose/Nginx host | `80`; public host port, not backend port |
| `AI_API_KEY` | Backend only | Optional; never sent to the browser |
| `AI_API_URL` | Backend only | `https://api.openai.com/v1/chat/completions` |
| `AI_MODEL` | Backend only | `gpt-4o-mini` |
| `AI_TIMEOUT_SECONDS` | Backend only | `20` |
| `AI_LOCAL_FALLBACK` | Backend only | `true` |

The Compose file passes AI variables to the backend only. An API key is not compiled into the Vue build.

## Docker Compose (Recommended Local Run)

The Compose file starts `postgres`, `backend`, and `nginx`. PostgreSQL has no host port mapping; only Nginx is public.

```bash
cp .env.example .env
# Edit .env and set POSTGRES_PASSWORD and a 32+ character JWT_SECRET.
docker compose --env-file .env -f deploy/docker-compose.yml up -d --build
```

Open [http://localhost](http://localhost), or use another host port:

```bash
APP_PORT=8080 docker compose --env-file .env -f deploy/docker-compose.yml up -d --build
```

With `APP_PORT=8080`, open [http://localhost:8080](http://localhost:8080). Check service status and logs:

```bash
docker compose --env-file .env -f deploy/docker-compose.yml ps
docker compose --env-file .env -f deploy/docker-compose.yml logs -f backend
```

For production overrides:

```bash
cp .env.production.example .env.production
# Edit .env.production with real values.
docker compose --env-file .env.production \
  -f deploy/docker-compose.yml -f deploy/docker-compose.prod.yml \
  up -d --build
```

The `studyvault_app` network connects Nginx to the backend. The `studyvault_database` network is internal and connects the backend to PostgreSQL. The `postgres_data` named volume persists database files across container recreation.

## Local Backend Development

Run PostgreSQL locally (Docker is convenient even when the backend runs from Maven):

```bash
docker run --name studyvault-postgres \
  -e POSTGRES_DB=studyvault \
  -e POSTGRES_USER=studyvault \
  -e POSTGRES_PASSWORD=change-me \
  -p 5432:5432 \
  -d postgres:16-alpine
```

Export backend settings in the same shell. Use a JWT secret of at least 32 characters:

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/studyvault
export DATABASE_USERNAME=studyvault
export DATABASE_PASSWORD=change-me
export JWT_SECRET=replace-with-a-long-random-secret-at-least-32-characters
export JWT_EXPIRATION_SECONDS=86400
export AUTH_COOKIE_SECURE=false
export SERVER_PORT=8080
```

Start the backend from the backend directory:

```bash
cd backend
mvn spring-boot:run
```

Flyway applies migrations automatically when the application starts. Stop the disposable database with `docker stop studyvault-postgres` (and remove it with `docker rm studyvault-postgres` when its data is no longer needed).

## Frontend Development

Install dependencies and start Vite:

```bash
cd frontend
npm install
npm run dev
```

The development server is available at [http://localhost:5173](http://localhost:5173). `frontend/vite.config.ts` proxies `/api` to `http://localhost`, which is the Nginx port in the recommended Compose setup. Therefore, either start Compose with its default `APP_PORT=80`, or change the proxy target locally if Nginx is bound to another host port. A production-like static preview can be built and served with:

```bash
npm run build
npm run preview
```

The browser uses the HttpOnly cookie issued by the backend. API calls use `credentials: 'include'`; no token is stored in browser storage.

## Backend Commands And Tests

From `backend/`:

```bash
mvn spring-boot:run
mvn test
mvn -DskipTests package
```

The test suite uses JUnit 5, Mockito, MockMvc, and Spring Data JPA tests. From the repository root, run the backend tests and frontend production type-check/build as follows:

```bash
(cd backend && mvn test)
(cd frontend && npm run build)
```

There is currently no frontend test runner or `npm test` script in `frontend/package.json`; `npm run build` is the available frontend verification command.

## Database Migrations

Flyway migration files live in `backend/src/main/resources/db/migration` and run on backend startup:

- `V1__init.sql` creates users, notes, tags, note relationships, indexes, and constraints.
- `V2__add_note_revisions.sql` creates note revision history and its index.

Never edit a migration that has already run in a shared database. Add a new sequential file such as `V3__describe_change.sql`, restart the backend, and let Flyway apply it. To inspect applied migrations:

```bash
export PGPASSWORD="$DATABASE_PASSWORD"
psql -h localhost -p 5432 -U "$DATABASE_USERNAME" -d studyvault -c \
  'SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;'
```

`spring.jpa.hibernate.ddl-auto=validate` means Hibernate checks the schema but does not create or alter tables.

## Health And Readiness

The public convenience endpoints are:

```bash
curl -i http://localhost/api/health
curl -i http://localhost/api/ready
```

When `APP_PORT` is set to `8080`, use `http://localhost:8080/api/health` and `http://localhost:8080/api/ready` instead. `/api/health` reports that the application process is alive. `/api/ready` checks Spring Boot health (including the PostgreSQL health indicator) and returns `200` with `READY` only when dependencies are up; otherwise it returns `503` with `NOT_READY`. Nginx deliberately returns `404` for other `/actuator/*` paths, so unnecessary Actuator endpoints are not public.

## API Overview

Successful responses use `{ "success": true, "data": ... }`. Errors use `{ "success": false, "error": { "code", "message", "details", "path", "timestamp" } }`. Protected endpoints require the `STUDYVAULT_TOKEN` HttpOnly cookie created by login or registration.

### Authentication

```text
POST /api/auth/register       JSON: { username, email, password }
POST /api/auth/login          JSON: { usernameOrEmail, password }
POST /api/auth/logout         clears the cookie
GET  /api/auth/me             returns the current user
```

### Notes, search, and lifecycle

```text
GET    /api/notes                         active notes owned by the user
POST   /api/notes                         JSON: { title, content }
GET    /api/notes/{id}
PUT    /api/notes/{id}                    JSON: { title, content }
DELETE /api/notes/{id}                    soft-delete: status becomes trash
POST   /api/notes/{id}/favorite           toggles favorite
POST   /api/notes/{id}/restore            changes trash back to active
GET    /api/notes/trash                   trashed notes owned by the user
GET    /api/search                        q, tag, favorite, status, page, size, sort
```

Search filtering and pagination are expressed as a Spring Data JPA `Specification`, so predicates run in PostgreSQL rather than filtering a complete note list in Java. `sort` accepts `updatedAt` or `createdAt` with `asc` or `desc` (for example, `sort=updatedAt,desc`). Pages are zero-based and `size` is limited to 100.

### Tags

```text
GET    /api/tags
POST   /api/tags                         JSON: { name, color? }
PUT    /api/tags/{id}                    JSON: { name, color? }
DELETE /api/tags/{id}
POST   /api/notes/{id}/tags              JSON: { tagId }
DELETE /api/notes/{id}/tags/{tagId}
```

Tag names are unique per user. Both the note and tag must belong to the authenticated user, and assigning an already assigned tag returns `NOTE_TAG_ALREADY_EXISTS`.

### Markdown files

```text
GET  /api/notes/{id}/export               downloads one Markdown file
GET  /api/notes/export                   downloads a ZIP of the user's notes
POST /api/notes/import                   multipart field `file`, .md, max 5 MB
```

Single-note and ZIP exports are streamed with sanitized filenames. Imports derive the title from a leading `# Heading` or the uploaded filename and create a new note owned by the current user.

### AI summaries

```text
POST /api/notes/{id}/summarize            returns an unsaved summary draft
PUT  /api/notes/{id}/summary              JSON: { summary }, explicitly saves it
```

The note is ownership-checked before its content is sent to the configured provider. Provider failures map to stable codes such as `AI_TIMEOUT`, `AI_RATE_LIMITED`, `AI_PROVIDER_ERROR`, and `AI_NOT_CONFIGURED`.

## Security Design

- Authentication is stateless JWT authentication in the `STUDYVAULT_TOKEN` HttpOnly, SameSite=Lax cookie. The browser never receives the signing secret or stores the token in localStorage.
- Passwords are hashed with BCrypt. JWT and database credentials are supplied through environment variables.
- Every note, tag, relationship, search, import, export, and summary query is scoped to the authenticated `User`. A missing note and another user's note both produce `NOTE_NOT_FOUND`, avoiding an ownership oracle.
- Controllers handle HTTP binding and response mapping; services enforce ownership, validation-related rules, and transactions. DTOs prevent JPA entities (including password hashes and user links) from being serialized.
- Markdown preview is parsed with `marked` and sanitized with `DOMPurify`; uploaded Markdown is treated as text and cannot write arbitrary paths.
- Request logging records path, status, duration, and user ID only. It does not log query strings, request bodies, cookies, passwords, JWTs, or API keys.

## AI Configuration

AI summaries work without a provider key when `AI_LOCAL_FALLBACK=true` (the deterministic local summarizer is used). To call an OpenAI-compatible provider, set these backend-only variables in `.env` or the deployment environment:

```bash
AI_API_KEY=replace-with-provider-key
AI_API_URL=https://api.openai.com/v1/chat/completions
AI_MODEL=gpt-4o-mini
AI_TIMEOUT_SECONDS=20
AI_LOCAL_FALLBACK=true
```

For a compatible provider such as DeepSeek, change only the endpoint and model as appropriate:

```bash
AI_API_URL=https://api.deepseek.com/v1/chat/completions
AI_MODEL=deepseek-chat
```

Restart the backend after changing environment variables:

```bash
docker compose --env-file .env -f deploy/docker-compose.yml up -d --build backend
```

The key is read by the backend adapter and is never sent to Vue. Network timeouts, HTTP 429 responses, invalid provider responses, and rejected credentials are returned as clear application errors; the original note is never overwritten by generating a draft.

## Backups

`deploy/backup.sh` uses `pg_dump` custom format, creates a UTC timestamped file, and removes only matching files directly inside `BACKUP_DIR` older than `RETENTION_DAYS` (14 by default). It never hardcodes a password; PostgreSQL authentication follows standard `PGPASSWORD`, `.pgpass`, or other libpq configuration.

With a PostgreSQL client installed and a database reachable on the host:

```bash
export BACKUP_DIR="$PWD/backups"
export PGHOST=localhost
export PGPORT=5432
export PGDATABASE=studyvault
export PGUSER=studyvault
export PGPASSWORD=change-me
./deploy/backup.sh
```

For the Compose database, run the script in a temporary PostgreSQL client container attached to Compose's internal network. This keeps port 5432 private:

```bash
set -a; . ./.env; set +a
mkdir -p backups
docker compose --env-file .env -f deploy/docker-compose.yml run --rm --no-deps \
  -v "$PWD/backups:/backups" \
  -v "$PWD/deploy/backup.sh:/usr/local/bin/studyvault-backup.sh:ro" \
  -e BACKUP_DIR=/backups -e PGHOST=postgres -e PGPORT=5432 \
  -e PGDATABASE="$POSTGRES_DB" -e PGUSER="$POSTGRES_USER" -e PGPASSWORD="$POSTGRES_PASSWORD" \
  postgres sh /usr/local/bin/studyvault-backup.sh
```

Set `BACKUP_PREFIX` or `RETENTION_DAYS` to customize naming and retention, for example `RETENTION_DAYS=30 ./deploy/backup.sh`. The script exits non-zero and removes a partial file if `pg_dump` fails.

## Restore And Verify

Restore a dump into a temporary database before replacing a production database. The following example uses a local PostgreSQL server and assumes the dump is `backups/studyvault_YYYYMMDDTHHMMSSZ.dump`:

```bash
export PGHOST=localhost
export PGPORT=5432
export PGUSER=studyvault
export PGPASSWORD=change-me
createdb studyvault_restore
pg_restore --exit-on-error --clean --if-exists \
  --dbname=studyvault_restore backups/studyvault_YYYYMMDDTHHMMSSZ.dump
psql --dbname=studyvault_restore -c '\dt'
psql --dbname=studyvault_restore -c 'SELECT COUNT(*) AS users FROM users;'
dropdb studyvault_restore
```

For a Compose database, run `createdb`, `pg_restore`, and the verification queries from a PostgreSQL client container on the same internal network, using `PGHOST=postgres` and the credentials from `.env`. Review the table list and row counts before planning any production restore.

## Building Images

Build the backend image:

```bash
docker build -f backend/Dockerfile -t studyvault-backend ./backend
```

Build the production frontend/Nginx image:

```bash
docker build -f deploy/nginx.Dockerfile -t studyvault-nginx .
```

Both Dockerfiles are multi-stage builds. A Maven or Node builder stage downloads build dependencies and produces an artifact (`.jar` or `dist/`); the final Java runtime or Nginx stage copies only that artifact. Build tools, source caches, and `node_modules` are therefore absent from the production image. `.dockerignore` files exclude local environments, build output, and secrets.

## Troubleshooting

**AI says it is not configured.** Leave `AI_LOCAL_FALLBACK=true` for the built-in local summary, or set `AI_API_KEY` and restart the backend. The key must be set in the backend environment, not in Vue.

**The AI provider returns an error.** Verify `AI_API_URL` is an OpenAI-compatible chat-completions URL, the model name is valid for that provider, and the key has access. A 429 becomes `AI_RATE_LIMITED`; timeouts become `AI_TIMEOUT`.

**The frontend shows “Failed to fetch”.** Confirm Nginx is running and that the browser URL uses its host port (`APP_PORT`). In standalone Vite development, the configured proxy targets `http://localhost` (port 80); start Compose on port 80 or adjust `frontend/vite.config.ts` for another port.

**Login or registration fails.** Check that the backend is running, PostgreSQL is ready, and the browser is using the same origin as Nginx. For local HTTP, keep `AUTH_COOKIE_SECURE=false`; a secure cookie will not be sent over plain HTTP. Duplicate usernames/emails return `USERNAME_ALREADY_EXISTS` or `EMAIL_ALREADY_EXISTS`.

**Readiness returns 503.** Inspect `docker compose ... ps` and PostgreSQL/backend logs. Wait for the PostgreSQL health check and Flyway startup to complete; `/api/ready` remains `NOT_READY` while a dependency is down.

**Flyway validation fails.** Do not edit an applied migration. Restore the original file, or add a new versioned migration for the schema change, then restart the backend.

**A port is already in use.** Set a different host port, such as `APP_PORT=8080`, and open `http://localhost:8080`. `SERVER_PORT` is the backend container/listener port and normally stays `8080`.

**Changes are missing after a Docker rebuild.** Rebuild the affected image with `--build`; the Vue bundle is compiled into the Nginx image. Hard-refresh the browser if an older static bundle is cached.

## Repository Comparison

This README was checked against the current controllers, services, Vite scripts, Dockerfiles, Compose files, migrations, and backup script. The documented routes, environment variables, service names, migration versions, build commands, and backup behavior correspond to those files.

Two planned areas are intentionally not described as implemented: there is no `/api/stats/overview` dashboard statistics endpoint, and the frontend has no test runner or `npm test` command. Local/generated files such as `.env`, `.env.production`, `backend/target`, `frontend/dist`, browser cookies, and database dumps should remain uncommitted.
