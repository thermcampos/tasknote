# Spec: Live Markdown Editor

## Goal

Replace the plain-textarea note editor and the "Preview Markdown" toggle with a
Notion-style live markdown editor (Milkdown). As the user types markdown syntax
(`#`, `-`, `**bold**`, `*italic*`, etc.), it renders inline immediately. The note
title, URL, and tags move out of the document body into dedicated, label-less
form fields that visually read as one continuous document.

---

## User Value

- No mode switching between writing and previewing; what you see is what you get.
- Notes stay stored as plain markdown: zero data migration, existing notes and
  public shared-note links keep working unchanged.

---

## Decisions (agreed during design review)

| Decision | Choice |
|---|---|
| Interaction model | Notion-style WYSIWYG; markdown syntax is hidden once applied |
| Library | Milkdown (`@milkdown/kit`, `@milkdown/react`) with CommonMark + GFM presets |
| Storage format | Plain markdown in `notes.description`; serializer normalization accepted |
| Form layout | Hybrid: title input on top (heading-styled), URL + tags inputs below the body, placeholders only, no labels, visually one document |
| Markdown scope | Full GFM: headers, bullets, numbered lists, bold, italic, inline code, fenced code, links, strikethrough, task lists, tables, blockquotes |
| Paste | Rich content converts to GFM; pasted markdown text auto-renders |
| Read views | Home note modal and SharedNote page keep `react-markdown` (v1) |
| Preview | "Preview Markdown" removed from the note editor; `ModalMarkdown` remains for Home |
| Size limit | Live character count past 45k; save blocked client-side at 50k |
| Theming | Hand-rolled editor CSS on `--bs-*` variables; light and dark themes |
| Mobile | Desktop-first; must not break on mobile, polish is follow-up |
| Delivery | PR 1: hybrid form; PR 2: Milkdown swap |

---

## Requirements

### PR 1: Hybrid note form

- Title, URL, and tags become dedicated inputs; the body stays a plain textarea.
- Title input is styled as a document heading with placeholder `Untitled note`;
  pressing Enter moves focus into the body.
- URL input (`Add a URL`) and tags input (`Add tags`) sit below the body with
  muted, label-less styling; tags keep the existing autocomplete dropdown and
  Enter-to-commit behavior.
- The in-document `title` / `url:` / `tags:` line syntax is removed from the
  editing path; the API payload (`title`, `description`, `url`, `tags`) is
  unchanged.
- Draft autosave (1.5s localStorage debounce) covers all fields.

### PR 2: Milkdown live editor

- Body textarea is replaced by a Milkdown editor component with input rules for
  the full GFM set.
- Markdown is serialized on change (for autosave and save) and parsed on load.
- "Preview Markdown" link, its handler, and the editor's `ModalMarkdown` usage
  are removed.
- Live character count appears past 45k chars; saving is blocked with an inline
  error at 50k (server limit stays the source of truth).
- Editor styling maps to Bootstrap theme variables for both light and dark.

### Explicitly out of scope

- Backend or database changes (none required).
- Formatting toolbar (supersedes deleted spec 002).
- Mobile-specific editor polish.
- Migrating read views (Home modal, SharedNote) to Milkdown.
- Image embedding.

---

## Acceptance Criteria

### PR 1

- [ ] Title, URL, tags are separate inputs; body textarea unchanged.
- [ ] Enter in the title input focuses the body.
- [ ] Tag autocomplete works from the tags input below the body.
- [ ] Saving a note sends the same API payload shape as before.
- [ ] Draft autosave restores title/body/URL/tags after reload.
- [ ] Existing notes load correctly into the split fields.

### PR 2

- [ ] Typing `# `, `- `, `**bold**`, `*italic*`, `` `code` ``, `> `, `[]` renders
      live without showing raw syntax.
- [ ] Pasted markdown and pasted rich content render as GFM.
- [ ] Saved `description` is valid markdown that renders identically in the Home
      modal and SharedNote page (react-markdown).
- [ ] "Preview Markdown" no longer exists in the editor.
- [ ] Character counter appears past 45k; save blocked at 50k.
- [ ] Editor is readable and correctly themed in light and dark modes.
- [ ] Cypress e2e: create a note with header, bold, and a list; verify saved
      markdown via API and rendering in the Home modal.

---

## Dependencies

- None (spec 002, markdown toolbar, was deleted; this spec supersedes it).

---

## Risks

- **Markdown normalization**: Milkdown's serializer normalizes formatting
  (e.g. `** bold **` becomes `**bold**`), so merely opening and saving an old
  note may reformat untouched sections. Accepted; mention in changelog.
- **Round-trip fidelity**: syntax outside the enabled presets (raw inline HTML)
  can be dropped by the editor schema on save. Accepted for v1.
- **Mobile quirks**: ProseMirror/contentEditable issues with virtual keyboards
  and autocorrect. Mitigated by desktop-first scope.
- **jsdom testing**: keystroke-level input-rule simulation is brittle; editor
  is tested through its markdown in/out API instead, with one Cypress flow for
  real-browser coverage.

---

## Notes

- Library facts (checked 2026-09): Milkdown 7.22.x, actively maintained, small
  issue backlog, first-class React 19 support, markdown-native via remark-based
  transformer (`getMarkdown()`, `markdownUpdated` events). TipTap rejected
  because markdown round-tripping relies on a deprecated community package or a
  young official one.
- Do not adopt Crepe (Milkdown's prebuilt UI); its theming conflicts with
  Bootstrap and the design has no toolbar.
- `ModalMarkdown` and `react-markdown` remain dependencies for Home/SharedNote.
