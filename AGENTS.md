# AGENTS.md

## Big picture
- `tasknote` is a monorepo with a React/Vite frontend (`client/`) and Spring Boot API (`server/`) sharing a PostgreSQL schema managed by Flyway migrations (`server/src/main/resources/db/migration/`).
- Request boundary is explicit: unauthenticated endpoints under `/auth/**`, authenticated business endpoints under `/rest/**`, and public shared-note endpoints under `/public/**` (see `server/src/main/java/br/com/tasknoteapp/server/config/SecurityConfig.java`).
- Frontend talks directly to backend URLs from `client/src/api-service/apiConfig.ts` (`VITE_BACKEND_SERVER`), using `fetch` wrapper logic in `client/src/api-service/api.ts`.
- Auth is JWT-in-header + localStorage persistence (`API_TOKEN`), with automatic 2-minute token refresh in `client/src/context/AuthProvider.tsx` via `/rest/user-sessions/refresh`.

## Architecture details that matter
- Frontend route split is auth-state driven in `client/src/App.tsx` + `client/src/routes/ProtectedRoute/index.tsx`; private screens render through `client/src/layout/PrivateLayout/index.tsx`.
- Home view (`client/src/views/Home/index.tsx`) is a key integration surface: loads tasks/notes/tags, applies client-side filtering, and drives note sharing (`/rest/notes/{id}/share` + `/public/notes/{token}`).
- Three React Contexts are active at runtime: `AuthProvider` (JWT lifecycle), `FilterProvider` (search text + option persisted to `localStorage` as `FILTER_TEXT`/`FILTER_OPTION`), and `SidebarProvider` (sidebar open/close state) — all under `client/src/context/`.
- Backend follows controller -> service -> repository layering (`server/src/main/java/br/com/tasknoteapp/server/{controller,service,repository}`).
- `HomeController` (`/rest/home`) exposes aggregated data such as `/rest/home/tasks/tags`; it is separate from `TaskController` and `NoteController`.
- Global API error shape comes from `server/src/main/java/br/com/tasknoteapp/server/controller/RestExceptionController.java`; frontend expects `message` or `fields[].fieldMessage` (`client/src/api-service/api.ts`).
- Email and password-reset flows are Mailgun-backed (`server/src/main/java/br/com/tasknoteapp/server/service/MailgunEmailService.java`) and use templates under `server/src/main/java/br/com/tasknoteapp/server/templates/` + `mailgun-templates/`.
- Backend targets **Java 25** and **Spring Boot 4.x** (`server/pom.xml`).

## Developer workflows (use these first)
- Frontend quality gate: `bash tools/check-frontend.sh` (runs `npm ci`, `lint:fix`, `build`, `test:no-watch`).
- Backend quality gate: `bash tools/check-backend.sh` (runs checkstyle, compile, then `clean verify -P tests`).
- Backend dependency freshness check: `bash tools/check-be-deps.sh` (verifies that `failsafe`, `surefire`, `jacoco`, `checkstyle`, and Spring Boot plugin versions in `pom.xml` match the latest releases on Maven Central; must be run from the repo root or `server/`).
- Important Maven default: tests/checkstyle/jacoco are skipped unless profile `-P tests` is enabled (`server/pom.xml`).
- Dev stack via Docker Compose/Taskfile (`Taskfile.yml`, `docker-compose.dev.yml`): app `5000`, API `8585`, Java debug port `5005`, Postgres `5432`.
- Frontend local dev: run from `client/` with `npm start`; backend local dev: run from `server/` with `./mvnw spring-boot:run`.

## CI/CD and release behavior
- PR workflows (`.github/workflows/drop-ci-pr-frontend.yml`, `.github/workflows/drop-ci-pr-backend.yml`) run checks then push `:candidate` and `:pr-<N>` images to GHCR.
- Main workflows (`.github/workflows/drop-ci-main-frontend.yml`, `.github/workflows/drop-ci-main-backend.yml`) push versioned tags (`app-v<date>.<run>` / `api-v<pom-version>`) + `latest`; backend workflow also increments `server/pom.xml` version.
- Staging deploy workflow (`.github/workflows/drop-cd-pr.yml`) triggers on completion of either PR CI workflow and applies Terraform in `terraform-stg/` using a plan→apply split.
- Production deploy workflow (`.github/workflows/deploy.yml`) triggers on completion of either Main CI workflow and applies Terraform in `terraform/` using a plan→apply split; `apply` can be skipped if there are no Terraform changes.
- Infra wiring (secrets, services, ingress, image vars) is defined in `terraform/main.tf`; an alternative GCP target is under `terraform-gcp/`.

## Project conventions to preserve
- Frontend style is ESLint flat config + stylistic rules (2-space indent, single quotes, semicolons) in `client/eslint.config.mjs`.
- Backend style is Google Checkstyle (`server/.mvn/google_checks.xml`), including 100-char line length and Javadoc requirements on public APIs.
- Integration tests are named `*IntTest.java` and are run by Failsafe; unit tests exclude that suffix (`server/pom.xml`).
- Migration files are versioned `V<timestamp>__description.sql`; never edit old migrations, add new ones in `server/src/main/resources/db/migration/`.

## Agent guardrails
- Prefer editing source, not generated artifacts (`server/target/`, `schemaspy/output/`, `TaskNoteBruno/results.html`).
- Keep API contract compatibility with frontend `ApiConfig` paths; changing endpoint paths requires synchronized client updates.
- When changing auth/session behavior, update both backend filters/controllers and frontend `AuthProvider` token lifecycle.
- If you change CI/build/deploy commands, update both `README.md` and this `AGENTS.md` to keep workflows discoverable.
