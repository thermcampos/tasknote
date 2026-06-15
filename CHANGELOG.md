# Changelog

All notable changes to Tasknote will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## api-v29 && app

### Changed
- Bumped Spring Boot to 4.1.0
- CI/CD workflow files updated to run on Gitea.
- Container registry switched to Docker Hub.

## [app-v2026.06.08.22](https://github.com/RMCampos/tasknote/releases/tag/app-v2026.06.08.22) - 2026-06-08

### Added
- Shell Script to confirm new users using docker and sql;

### Changed
- Bumped frontend dependencies to latest versions;
  - `@types/node` from `25.9.1` to `25.9.2`
  - `dompurify` from `3.4.7` to `3.4.8`
  - `i18next` from `26.3.0` to `26.3.1`
  - `react` from `19.2.6` to `19.2.7`
  - `react-dom` from `19.2.6` to `19.2.7`
  - `react-router` from `7.16.0` to `7.17.0`
  - `@types/react` from `19.2.15` to `19.2.17`
  - `eslint-plugin-n` from `18.0.1` to `18.1.0`
  - `typescript-eslint` from `8.60.0` to `8.61.0`
- Ngrok and Dev Docker composer files to run using local users id and group id (`UID` and `GID`);

## [app-v2026.06.08.21](https://github.com/RMCampos/tasknote/releases/tag/app-v2026.06.08.21) - 2026-06-08

### Changed
- The About page to list all current features and tech stack. ([#62](https://github.com/RMCampos/tasknote/issues/62))

## [app-v2026.06.01.20] - 2026-06-01

### Changed
- Bumped backend and frontend dependencies to latest versions. (#61)

## [app-v2026.05.26.19] - 2026-05-26

### Added
- SDD and DDD specification files for AI-assisted development, including Spec 001 implementation. (#60)

## [app-v2026.05.19.18] - 2026-05-19

### Changed
- Auth session refresh now uses server-authoritative current user data instead of stale client state. (#59)

## [app-v2026.05.18.17] - 2026-05-18

### Added
- Last activity date/time is now tracked and displayed for tasks and notes. (#57)

## [app-v2026.05.17.16] - 2026-05-17

### Added
- Users must confirm their email address before they can log in. (#56)
- Migrated app to new domain with Traefik redirect middleware. (#53)

### Fixed
- Mailgun authentication error (401) when sending emails. (#55)
- New domain correctly allowed in CORS and CSP configuration. (#54)

## [app-v2026.05.13.15] - 2026-05-13

### Changed
- Dropped refresh token logic; auth now relies solely on short-lived access tokens, simplifying the session flow. (#51)

## [app-v2026.05.13.14] - 2026-05-13

### Added
- Cypress E2E tests covering Home, Task, and Notes management flows. (#46)
- Cypress E2E tests for authentication flows. (#39)
- Scheduled database backups. (#48)

### Fixed
- Premature route resolution before initial auth check completes. (#45)
- Blocked inline styles in CSP via SHA-256 hash in `style-src`. (#43)
- Frontend test warnings and errors. (#37)

### Changed
- Upgraded Vite to v8. (#35)

## [app-v2026.04.29.4] - 2026-04-29

### Added
- Gemini CLI commands and updated AI agent configuration.

### Changed
- Upgraded TypeScript to v6. (#34)

## [app-v2026.04.29.3] - 2026-04-29

### Changed
- Updated client dependencies to latest minor versions. (#33, #32)

## [app-v2026.04.16.1] - 2026-04-16

### Added
- Dev environment config files and scripts to run the app locally on VPS. (#29)

### Security
- Improved security posture and added protections against XSS attacks. (#30)

## [app-v2026.04.08.20] - 2026-04-08

### Added
- GitHub Actions workflow for building server candidate images.

### Changed
- Bumped Spring Boot to 4.0.5. (#27)

## [app-v2026.04.06.19] - 2026-04-06

### Added
- Kubernetes CD pipeline via GitHub Actions.
- Dev container for Java development.
- Workflow skips deployment when no source changes detected.

## [app-v2026.03.17.17] - 2026-03-17

### Fixed
- Missing email templates and configuration for new domain.

## [app-v2026.03.16.16] - 2026-03-16

### Fixed
- Backend memory leak and state management issues. (#23)
- Error messages not propagating back to client in Spring v4.

### Changed
- Updated error messages and translations for improved user feedback.

## [app-v2026.02.28.15] - 2026-02-28

### Added
- Public note sharing: users can share a note via a public link. (#22)

## [app-v2026.02.27.13] - 2026-02-27

### Added
- Source and Copy buttons in the note markdown preview modal. (#21)

### Changed
- Upgraded backend to Spring Boot 4.0.3 and Java 25. (#20)

## [app-v2026.02.05.5] - 2026-02-05

### Changed
- Improved markdown rendering and added home filter context.
- Added notes tags support throughout the app. (#13)
- Tag suggestion dropdown for Notes and Tasks. (#12)

### Fixed
- Backend null pointer exception.
- Filters now persist between actions in the Home view. (#9)

## [app-v2026.01.18.4] - 2026-01-18

### Added
- CI now uses GHCR (GitHub Container Registry) with unified version tagging for backend images.

### Changed
- Upgraded Spring Boot to 3.5.9.

### Fixed
- Docker image name casing issue causing deployment failures.

## [app-v2026.01.14.3] - 2026-01-14

### Changed
- Dropped Lombok dependency; bumped to Spring 3.5.9.
- Updated backend dependencies to latest versions.

## [app-v2025.12.15.1] - 2025-12-15

### Added
- Initial release with core task and note management features.
- Bruno API collections for local development.

[app-v2026.06.01.20]: https://github.com/RMCampos/tasknote/compare/app-v2026.05.26.19...app-v2026.06.01.20
[app-v2026.05.26.19]: https://github.com/RMCampos/tasknote/compare/app-v2026.05.19.18...app-v2026.05.26.19
[app-v2026.05.19.18]: https://github.com/RMCampos/tasknote/compare/app-v2026.05.18.17...app-v2026.05.19.18
[app-v2026.05.18.17]: https://github.com/RMCampos/tasknote/compare/app-v2026.05.17.16...app-v2026.05.18.17
[app-v2026.05.17.16]: https://github.com/RMCampos/tasknote/compare/app-v2026.05.13.15...app-v2026.05.17.16
[app-v2026.05.13.15]: https://github.com/RMCampos/tasknote/compare/app-v2026.05.13.14...app-v2026.05.13.15
[app-v2026.05.13.14]: https://github.com/RMCampos/tasknote/compare/app-v2026.04.29.4...app-v2026.05.13.14
[app-v2026.04.29.4]: https://github.com/RMCampos/tasknote/compare/app-v2026.04.29.3...app-v2026.04.29.4
[app-v2026.04.29.3]: https://github.com/RMCampos/tasknote/compare/app-v2026.04.16.1...app-v2026.04.29.3
[app-v2026.04.16.1]: https://github.com/RMCampos/tasknote/compare/app-v2026.04.08.20...app-v2026.04.16.1
[app-v2026.04.08.20]: https://github.com/RMCampos/tasknote/compare/app-v2026.04.06.19...app-v2026.04.08.20
[app-v2026.04.06.19]: https://github.com/RMCampos/tasknote/compare/app-v2026.03.17.17...app-v2026.04.06.19
[app-v2026.03.17.17]: https://github.com/RMCampos/tasknote/compare/app-v2026.03.16.16...app-v2026.03.17.17
[app-v2026.03.16.16]: https://github.com/RMCampos/tasknote/compare/app-v2026.02.28.15...app-v2026.03.16.16
[app-v2026.02.28.15]: https://github.com/RMCampos/tasknote/compare/app-v2026.02.27.13...app-v2026.02.28.15
[app-v2026.02.27.13]: https://github.com/RMCampos/tasknote/compare/app-v2026.02.05.5...app-v2026.02.27.13
[app-v2026.02.05.5]: https://github.com/RMCampos/tasknote/compare/app-v2026.01.18.4...app-v2026.02.05.5
[app-v2026.01.18.4]: https://github.com/RMCampos/tasknote/compare/app-v2026.01.14.3...app-v2026.01.18.4
[app-v2026.01.14.3]: https://github.com/RMCampos/tasknote/compare/app-v2025.12.15.1...app-v2026.01.14.3
[app-v2025.12.15.1]: https://github.com/RMCampos/tasknote/releases/tag/app-v2025.12.15.1
