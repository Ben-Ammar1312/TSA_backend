# Repository Guidelines

## Project Structure & Module Organization
- Source lives in `src/main/java/org/example/tas_backend` split into `controllers` (REST), `services` (business), `repos` (Spring Data JPA), `entities` (JPA models with Envers auditing), `dtos`/`enums` (API payloads), `configs` (security + Keycloak), and `envers` (revision listener).
- Config and templates sit in `src/main/resources` (`application.properties`, `static/`, `templates/`); override secrets via env vars or a local profile file instead of editing defaults.
- Tests belong in `src/test/java` mirroring the package under test. `target/` holds build artifacts and `uploads/` is git-ignored user storage.

## Build, Test, and Development Commands
- `./mvnw spring-boot:run` — start the app on port 8081 using the Postgres/Keycloak/AI endpoints from `application.properties`.
- `./mvnw test` — run the JUnit + Spring Boot test suite.
- `./mvnw clean package` — produce the runnable jar in `target/` (add `-DskipTests` only for quick local builds).

## Coding Style & Naming Conventions
- Java 21, Spring Boot 3.5.x, Maven wrapper. Use 4-space indents, avoid wildcard imports, and keep methods small with clear side effects.
- Entities typically use Lombok (`@Getter/@Setter`); DTOs may be records or immutable classes; enums are uppercase.
- REST controllers return `ResponseEntity` with validation annotations; keep URI paths kebab-case and scoped by domain (e.g., `/student/...`).

## Testing Guidelines
- JUnit 5 with Spring Boot test starter. Name test classes `*Tests` and mirror the package of the component under test.
- Prefer lightweight slices (`@WebMvcTest` for controllers, Mockito for services) and reserve `@SpringBootTest` for end-to-end flows.
- For persistence changes, use `@DataJpaTest` with an in-memory database and assert Envers/audit fields when relevant.

## Commit & Pull Request Guidelines
- Follow current history: short, imperative messages (`mapping fixed`, `integration done`). Group related edits per commit.
- PRs should state intent, affected endpoints, DB impacts, and configuration changes; include test command output and sample requests/responses (curl/Postman or JSON) for new APIs.
- Link tickets/issues when available and tag reviewers for the touched area (`controllers`, `services`, `repos`).

## Security & Configuration Tips
- Defaults live in `src/main/resources/application.properties`; override via env vars such as `SPRING_DATASOURCE_URL`, `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI`, `AI_BASE_URL`, or a profile-specific properties file.
- Keycloak realm `TAS` and client IDs must match your auth server. AI/LLM calls expect the Django service at `http://localhost:8000`.
- Never commit real credentials, generated uploads, or build outputs; ensure `uploads/` and `target/` stay untracked before pushing.
