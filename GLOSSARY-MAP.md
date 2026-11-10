# Glossary Map

TaskNote is effectively a single domain context, deployed as two codebases that share one vocabulary and one PostgreSQL schema.

## Contexts

| Context | Path | Role |
|---|---|---|
| TaskNote domain | [`GLOSSARY.md`](./GLOSSARY.md) | Glossary shared by client and server: users, tasks, notes, tags, sharing |
| Client | `client/` | React/Vite SPA; talks to the API via `client/src/api-service/` |
| Server | `server/` | Spring Boot API (controller → service → repository); Flyway-managed schema |

## Relationships

- **Client → Server**: REST over `/auth/**` (unauthenticated), `/rest/**` (JWT) and `/public/**` (shared notes). Path changes must be made on both sides.
- Both contexts use the terms in [`GLOSSARY.md`](./GLOSSARY.md); neither has its own `GLOSSARY.md` yet. Add one under `client/` or `server/` only if their vocabularies diverge.

## Decisions

System-wide ADRs live in [`docs/adr/`](./docs/adr/).

## Tech stack

- **Client**: React, TypeScript, Bootstrap (mobile-first), Vite, Fetch API.
- **Server**: Java, Spring Boot, Spring Security, JPA, GraalVM.
- **Database**: PostgreSQL.
- **Infrastructure**: Docker, Kubernetes, Terraform, VPS.

## Architecture principles

- Keep solutions simple and focused on the problem; prefer readability and maintainability over cleverness or perfection.
- Small functions and modules that do one thing well, with clear descriptive names and clear boundaries between components.
- Avoid premature optimization, over-engineering, speculative abstractions and unnecessary microservices.
- Deliver in small specs, iteratively, as vertical slices with simple APIs and clear documentation.

## Project constraints

### Server

Always:
- Use the `SERIAL` type for `id` columns in migration SQL files.
- Define the table PRIMARY KEY after the table columns, named `<table name>_pk`, on the needed columns.
- Import packages one by one.
- Write JavaDoc for public classes and methods.
- Run `./mvnw -Ptests test` to catch checkstyle issues and failing tests, and fix existing tests if needed.
- Update `docker-compose` and GitHub workflow files when a new variable is added or `application.yml` changes.

Never:
- Use `ON DELETE CASCADE` in SQL scripts or migrations.
- Use star imports.

### Client

Always:
- Type variables according to their data type.
- Put helper functions in the helper directory.
- Update the Dockerfile, `docker-compose` and GitHub workflow files when a new environment variable is added.

Never:
- Use the `any` type.
- Add new dependencies for small helpers or utilities; implement them instead.
