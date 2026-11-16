# ADR 004: Render all markdown with Milkdown

## Status

Accepted. Supersedes the "Read views" row of
[ADR 003](003-live-markdown-editor.md) and its acceptance criterion that saved
markdown "renders identically in the Home modal and SharedNote page
(react-markdown)". Tracked in issue #129.

---

## Context

After ADR 003, TaskNote rendered note markdown through two engines:

- the note editor used **Milkdown** (commonmark + gfm presets);
- the Home note modal and the public SharedNote page used
  **react-markdown + remark-gfm**.

Both parsed the same GFM source but produced different DOM, so a note could
look different depending on where it was read. The editor normalises markdown
on save while react-markdown rendered whatever the stored string contained.
Two engines meant two sets of CSS, two sets of quirks, and two dependencies;
`fix: checkbox rendering (#97)` and `fix: url rendering (#96)` were each fixed
in one engine only.

---

## Decision

| Decision | Choice |
|---|---|
| Renderer | Milkdown is the only markdown renderer in the client |
| Read surface | New `MarkdownViewer` component (`markdown: string`, no callbacks), separate from `MarkdownEditor` |
| Presets | Same as the editor: commonmark + gfm. No `listener`, `history`, link input rule or heading keymap |
| Read-only mode | `editorViewOptionsCtx` with `editable: () => false`, plus `aria-readonly="true"` (Milkdown always sets `role="textbox"`) |
| Task lists | Read-only variant of the editor's task-list node view: same DOM, disabled checkbox, never dispatches |
| Links | `linkAttr` sets `target="_blank"` and `rel="noopener noreferrer"` in the viewer only; `href` sanitised by Milkdown's `sanitizeLinkHref` |
| Consumers | Home note modal (`ModalMarkdown`) and SharedNote page; their surrounding chrome is unchanged |
| Styling | Typography rules shared by `.milkdown-editor .ProseMirror` and `.milkdown-viewer .ProseMirror`; react-markdown-specific modal CSS removed |
| Loading | Eager; route-level code splitting is tracked in #136 |
| Dependencies | `react-markdown` and the direct `remark-gfm` dependency removed (remark stays transitively via Milkdown) |

Alternatives rejected: keeping react-markdown for reads and only sharing config
(still two render paths); a `readOnly` prop on the editor (pollutes its
`onChange`/`focus()` contract and keeps the dispatching checkbox reachable).

---

## Consequences

- The editor, the Home modal and the SharedNote page render a note from the
  same schema, so what the editor writes is what readers see.
- **Raw HTML now shows as source text in read views.** Milkdown keeps it as an
  `html` node rendered as `span[data-type="html"]`, and the editor round-trips
  it verbatim, so editing the note does not change this. Accepted.
- Footnotes are supported by the gfm preset; definition lists are unsupported
  by both the old and new engines.
- Opening the Home modal mounts one ProseMirror instance (cards keep their
  plain-text preview).
- Bundle (`vite build`, main JS chunk): 4,670,202 B → 4,624,869 B
  (690,448 B → 676,604 B gzip). The change is justified by correctness and
  maintenance, not size.
- Read views are unit-tested with Milkdown mocked; real rendering is covered by
  Cypress (`client/cypress/e2e/markdown-read-views.cy.ts`).
