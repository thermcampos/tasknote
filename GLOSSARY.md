# TaskNote Context

TaskNote is a mobile-first web app where individuals keep their **tasks** and **notes** in one place, organized with shared **tags** and findable through search. This file is the glossary for the domain; use these terms in code, issues, specs and tests.

## Vision

**Problem**: People lose track of tasks, deadlines and progress in tools that are overwhelming or inflexible, and their notes are scattered across notebooks, sticky notes and digital documents, which makes information hard to find and organize.

**Users**: individuals managing personal tasks and notes; casual users who want a simple, intuitive tool without the complexity of traditional task managers; developers who want to keep coding tasks and notes in one streamlined place.

**Core features**:
- Authentication: accounts and secure login.
- Task management: create, organize and prioritize tasks with deadlines and reminders.
- Note-taking: flexible capture and organization of notes.
- Collaboration: sharing notes with others.
- Search and organization: search and organizational tools across tasks and notes.
- Multi-language support.

**General constraints**:
- GraalVM compatible.
- Mobile-first UI.

**Success criteria**:
- Users manage tasks and notes efficiently, with more productivity and less stress.
- Positive feedback and high engagement.
- Specs stay under control, with a clear roadmap.
- The codebase stays maintainable and scalable.

## Language

**User**:
An authenticated account holder. Owns every task, note and tag; nothing is visible to other users except a publicly shared note.
_Avoid_: Customer, member

**Task**:
A to-do item with a description, completion state, optional due date and high-priority flag.
_Avoid_: Todo, reminder (a reminder is the due-date notification on a task, not a separate thing)

**Completed**:
Whether a task is done. Distinct from a note's archived state.

**High priority**:
Flag on a task marking it as important.

**Due date**:
Optional date a task is due. Can opt in to a notification (**due-date notify**); the system records when it was **sent** so it is not sent twice.

**Note**:
A titled piece of free-form content whose body is stored as plain markdown in `description`.
_Avoid_: Memo, document

**Archived**:
A note hidden from the main view without being deleted.

**Tag**:
A user-owned label. A task or note can have many tags, and one tag can be applied to many tasks and notes.
_Avoid_: Category, label (label is reserved for issue-tracker triage labels)

**Task-note tag**:
The association between a tag and a task or note (`TaskNoteTag`).

**URL**:
A link attached to a task (`TaskUrl`) or note (`NoteUrl`).

**Share token**:
Opaque token that makes a **shared** note readable, without login, at `/public/notes/{token}`. Sharing is toggled via `/rest/notes/{id}/share`.
_Avoid_: Public link id

**Shared note**:
A note whose `shared` flag is on and that has a share token. Read-only for everyone except its owner.

**Filter**:
Client-side search text plus a filter option, persisted in `localStorage` (`FILTER_TEXT` / `FILTER_OPTION`).

**Session / token refresh**:
JWT kept in `localStorage` (`API_TOKEN`) and refreshed every 2 minutes via `/rest/user-sessions/refresh`.

**Password reset**:
Email flow (sent through Resend) that lets a user set a new password; attempts are rate-limited (`UserPwdLimit`).

## Relationships

- A **User** owns many **Tasks**, **Notes** and **Tags**.
- A **Task** or **Note** has zero or more **Tags** (via **Task-note tags**) and zero or more **URLs**.
- A **Note** may become a **Shared note** by getting a **Share token**.

## Boundaries

- `/auth/**`: unauthenticated. `/rest/**`: authenticated. `/public/**`: shared notes only.

## Flagged ambiguities

- "Label" and "tag": **Tag** is the domain term; "label" refers only to issue-tracker triage labels.
