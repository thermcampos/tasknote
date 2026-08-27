# Changelog

All notable changes to Tasknote will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## 2026-08-27

### Changed
- Bumped all frontend deps minor and patch target to latest versions.

### Fixed
- Small bug in the build number in dark mode not quite visible.

```bash
# Docker images
- ghcr.io...
```

---

## 2026-08-07

### Added
- Link to the build number to point to the changelog file. (build 201)

### Changed
- All deps to latest version in client for patch target. (build 201)
- All deps to latest version in client for minor target. (build 201)
- Development files for ngrok locally. (build 201)

### Fixed
- Buildx error in build phase in CI. (build 201)

### Removed
- Lingering files from previous CI/CD workflows. (build 201)

```bash
# Docker images
docker pull rmcampos/tasknote-app:app-v2026.08.07.201
```

---

## 2026-07-28

### Added
- Option to archive notes.

### Changed
- Notes should be archived before deleting.

```bash
# Docker images
docker pull rmcampos/tasknote-app:app-v2026.07.28.195
docker pull rmcampos/tasknote-api:api-v2026.07.28.194
```

## 2026-07-23

### Added
- Section for completed tasks in the home page..
- Icons in tasks and notes to differentiate them.
- Modal confirming before delete tasks and notes.

### Changed
- Completed tasks are now kept in the database, unless deleted.
- Buttons in home screen notes view to match the system design.
- Add task form to be easier to see and better structured.
- Delete my account buttons layout to match the system design.
- Loaded tasks now has a light yellow styling.

```bash
# Docker images
docker pull rmcampos/tasknote-app:app-v2026.07.23.190
docker pull rmcampos/tasknote-api:api-v2026.07.23.189
```

### Fixed
- Dropped the untagged tag from loading in the add notes and tasks form.

## 2026-07-22

### Added
- Button to save notes from the preview modal. Closes [#15](https://lightroasted.vps-kinghost.net/rmcampos/tasknote/issues/15)

### Changed
- Removed deployments to staging in PR pipelines. PR only runs CI now. Closes [#14](https://lightroasted.vps-kinghost.net/rmcampos/tasknote/issues/14)

### Removed
- Old files from project and moved scripts to `tools` folder.

```bash
# Docker images
docker pull rmcampos/tasknote-app:app-v2026.07.22.161
docker pull rmcampos/tasknote-api:api-v2026.07.22.169
```

## 2026-07-20

### Changed
- Labels in tasks due date to use the time ago format.
- Bumped all minor deps in the frontend.

### Fixed
- Background image position in landing, login and register pages.

### Removed
- React Date Picker dependency in favor of regular browser input date UI.

```bash
# Docker images
docker pull rmcampos/tasknote-app:app-v2026.07.20.161
```

## 2026-07-01

### Added
- Support for `Draft` notes and tasks.
- Memory for open notes in the home page, if a tab is closed, the app will remember.

### Fixed
- Frontend app build version release getting lost in workflows. 

### Security
- Addressed a list of critical security issues including validations, logging, and passwords.

### Docker images
- `rmcampos/tasknote-app:app-v2026.07.01.140`

### Changed
- Bumped client minor and major dependencies.

### Docker images
- `rmcampos/tasknote-app:app-v2026.06.25.102`

## api-v32 && app-v2026.06.15.97 - 2026-06-15

### Changed
- Bumped Spring Boot to 4.0.7
- CI/CD workflow files updated to run on Gitea.
- Container registry switched to Docker Hub.

### Docker images
- [rmcampos/tasknote-api:32](https://hub.docker.com/layers/rmcampos/tasknote-api/32/images/sha256-4b719a08dbed4a9d4a6eece0059573954ee5193ab8247787fb0e30c037f6b1c6)
- [rmcampos/tasknote-app:app-v2026.06.15.97](https://hub.docker.com/layers/rmcampos/tasknote-app/app-v2026.06.15.97/images/sha256-945a215a7105e34f97ab8e43094092e157156c0b557364260c019c4036cf845d)

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
