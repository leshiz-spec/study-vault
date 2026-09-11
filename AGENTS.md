# StudyVault Agent Instructions

## Project Purpose

StudyVault is a one-month learning project for building and deploying a personal Markdown knowledge base. The goal is to practice the complete engineering process—requirements, implementation, testing, deployment, and review—not to maximize the number of features.

Favor simple, understandable solutions that the student can explain. Keep the application a modular monolith; do not introduce microservices or other major infrastructure unless the user explicitly asks for it.

## Source of Truth and Scope

- Inspect the repository before making changes. Follow the existing structure, naming, dependency versions, and scripts when they differ from examples in the design document.
- Preserve working behavior and unrelated user changes.
- Implement only the requested feature. Do not silently add post-MVP features or refactor unrelated modules.
- Complete the MVP before proposing version history, backlinks, study tasks, image upload, dark mode, keyboard shortcuts, a knowledge graph, Redis, queues, or microservices.
- When requirements are ambiguous and different choices would materially change data, security, or architecture, ask before implementing.

## Technology and Architecture

- Frontend: Vue 3, TypeScript, Vite, Pinia, and Vue Router.
- Backend: Java 17 or 21, Spring Boot 3, Maven, Spring Web, Spring Security, Spring Data JPA, and Bean Validation.
- Database: PostgreSQL for production and integration testing. Local development may use SQLite, H2, or Docker PostgreSQL only if the existing configuration supports it.
- Database migrations: Flyway under `backend/src/main/resources/db/migration`.
- Deployment: Docker Compose and Nginx. Only Nginx should expose public HTTP/HTTPS ports in production; PostgreSQL must remain on the internal Docker network.
- Keep frontend requests on same-origin `/api` paths in production.

Expected repository areas:

```text
frontend/   Vue application
backend/    Spring Boot application
deploy/     Docker Compose, Nginx, and backup configuration
```

Do not create missing areas merely because they appear above unless the current task requires them.

## Backend Rules

- `controller`: HTTP routing, request validation, status codes, and DTO mapping only. Do not place complex business logic here.
- `service`: business rules and transaction boundaries. Use `@Transactional` where a use case performs related reads/writes.
- `repository`: persistence queries. Every user-owned note or tag operation must be scoped to the authenticated user.
- `entity`: JPA persistence models. Do not expose entities directly from REST endpoints.
- `dto`: request and response models with validation annotations.
- `exception`: stable application error codes and global exception handling.
- `security`: authentication and current-user resolution.

Security requirements:

- Hash passwords with BCrypt; never log or return passwords, password hashes, tokens, cookies, API keys, or database credentials.
- Prefer JWT access tokens in `HttpOnly` cookies when JWT is already selected; otherwise preserve the existing Spring Session approach.
- Never trust a client-supplied user ID for authorization.
- A user must never be able to read, modify, restore, export, or delete another user's notes or tags.
- Return stable errors such as `NOTE_NOT_FOUND`, `FORBIDDEN`, `VALIDATION_ERROR`, and `AI_TIMEOUT` through the project's unified error format.
- Keep production stack traces and debug details out of API responses.

Data and migration requirements:

- Make schema changes through a new Flyway migration; never edit a migration that may already have been applied unless the user explicitly confirms it is safe.
- Preserve soft deletion for notes. Treat permanent deletion as an explicit, separate operation.
- Add indexes deliberately based on query patterns, including user ownership and common filtering/sorting fields.

## Frontend Rules

- Use Vue 3 Composition API with TypeScript and follow the style already used in the repository.
- Keep reusable UI in `components`, page-level UI in `views`, global state in Pinia `stores`, routing in `router`, and HTTP request code in `api`.
- Do not call backend endpoints directly from unrelated components when an API module already exists.
- Every asynchronous screen must handle loading, empty, success, and error states.
- Validate user input in the UI for usability, but rely on backend validation and authorization for security.
- Implement autosave carefully: debounce writes, show save status, prevent stale responses from overwriting newer edits, and surface failures.
- Render Markdown safely; do not introduce raw HTML/XSS behavior without explicit sanitization.

## API and Behavior Conventions

- Keep REST endpoints under `/api` and preserve the existing API contract.
- Use the unified response shape when the project already follows it:

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

- Support validation, ownership checks, predictable pagination/sorting, and clear empty results.
- Avoid breaking response fields or endpoint paths without updating the frontend, tests, and documentation together.

## AI Feature Rules

- AI features are optional; StudyVault must remain usable without an API key.
- Implement at most one or two AI features for the MVP: note summary, five review questions, or related-note recommendations.
- Keep provider-specific code behind an adapter/service boundary.
- Handle missing configuration, timeout, rate limiting, and provider errors with clear messages.
- Generated content must be shown as a draft and must never silently overwrite the user's note.
- Keep AI keys in server-side environment variables only. Never expose them to the frontend, source control, logs, or images.

## Testing and Verification

Before changing code, locate the real build and test commands in `pom.xml`, `package.json`, Compose files, and README. After changes, run the smallest relevant checks first, then broader checks when practical.

Typical commands, only when supported by the repository:

```bash
cd backend && ./mvnw test
cd backend && mvn test
cd frontend && npm test
cd frontend && npm run build
docker compose -f deploy/docker-compose.yml config
```

- Do not claim tests passed unless they were actually run.
- If a command cannot run, report the exact command, the relevant error, and what remains unverified.
- Add or update tests for changed behavior, especially authentication failures, cross-user access, missing IDs, duplicate tags, blank titles, search with no results, autosave failures, and database errors.
- Prefer JUnit 5, Mockito, MockMvc, and Testcontainers according to the layer being tested. Use Vitest for frontend logic when it is configured.
- For bug fixes, add a regression test when practical.

## Deployment and Operations

- Never commit `.env`, `.env.local`, `.env.production`, private keys, tokens, or real credentials. Update `.env.example` with placeholder names when configuration changes.
- Inject database credentials, JWT secrets, and AI keys through environment variables.
- Keep database and user files on persistent volumes so container rebuilds do not erase data.
- Preserve `/api/health` for liveness and `/api/ready` for dependency readiness.
- Logs may include request path, status, duration, and non-sensitive user ID, but never secrets.
- Deployment-related changes must consider backup and restore. Do not delete volumes, production data, migrations, or backups without explicit user approval and a verified target.
- Validate Compose configuration before suggesting deployment. Do not state that production deployment succeeded unless health checks and a basic user-flow smoke test were performed.

## Working Style for Codex

- Explain the intended change briefly before editing, especially for a learner-facing task.
- Make small, focused changes that are easy to review and commit.
- When asked for a hint, explanation, diagnosis, or review, do not implement the solution unless the user asks you to.
- When implementing, show the important data flow and security boundary in plain language after the change.
- Do not hide complexity behind generated code the user cannot reasonably explain.
- Avoid adding dependencies when the standard library or an existing project dependency is sufficient. Explain any new dependency and its tradeoff.
- Update README or relevant documentation when setup steps, environment variables, endpoints, or deployment behavior change.

## Completion Checklist

Before finishing a coding task, confirm the applicable items:

- The requested behavior is implemented without unrelated scope expansion.
- Authentication and per-user authorization remain enforced.
- Input validation and stable error handling cover failure paths.
- Relevant tests/build checks were run and their results are reported honestly.
- No secrets or sensitive data were added to code, logs, fixtures, or documentation.
- Database changes use a new migration and deployment changes preserve persistent data.
- Documentation and `.env.example` reflect any user-facing setup changes.

