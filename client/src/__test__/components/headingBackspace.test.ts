import { afterEach, beforeAll, describe, expect, it } from 'vitest';
import { Editor, defaultValueCtx, editorViewCtx, rootCtx, serializerCtx } from '@milkdown/kit/core';
import { commonmark } from '@milkdown/kit/preset/commonmark';
import { gfm } from '@milkdown/kit/preset/gfm';
import { history } from '@milkdown/kit/plugin/history';
import { TextSelection } from '@milkdown/kit/prose/state';
import type { EditorView } from '@milkdown/kit/prose/view';
import { headingBackspaceKeymap } from '../../components/MarkdownEditor/headingBackspace';

beforeAll(() => {
  // jsdom has no layout engine; stub the geometry APIs ProseMirror calls.
  const rect = {
    x: 0, y: 0, top: 0, left: 0, bottom: 0, right: 0, width: 0, height: 0,
    toJSON: () => ({})
  } as DOMRect;
  Range.prototype.getBoundingClientRect = () => rect;
  Range.prototype.getClientRects = () =>
    ({ item: () => null, length: 0, [Symbol.iterator]: [][Symbol.iterator] }) as unknown as DOMRectList;
});

let editor: Editor | null = null;

afterEach(async () => {
  await editor?.destroy();
  editor = null;
  document.body.innerHTML = '';
});

const createEditor = async (markdown: string) => {
  const root = document.createElement('div');
  document.body.appendChild(root);
  editor = await Editor.make()
    .config((ctx) => {
      ctx.set(rootCtx, root);
      ctx.set(defaultValueCtx, markdown);
    })
    .use(commonmark)
    .use(gfm)
    .use(history)
    .use(headingBackspaceKeymap)
    .create();
  const ctx = editor.ctx;
  const view = ctx.get(editorViewCtx);
  return {
    view,
    markdown: () => ctx.get(serializerCtx)(view.state.doc),
    blockTypes: () => view.state.doc.content.content.map(node => node.type.name)
  };
};

/** Puts the caret at offset `offset` inside the top-level block at `index`. */
const placeCaret = (view: EditorView, index: number, offset = 0) => {
  let pos = 0;
  for (let i = 0; i < index; i++) pos += view.state.doc.child(i).nodeSize;
  view.dispatch(view.state.tr.setSelection(TextSelection.create(view.state.doc, pos + 1 + offset)));
};

const pressBackspace = (view: EditorView): boolean => {
  const event = new KeyboardEvent('keydown', { key: 'Backspace', bubbles: true, cancelable: true });
  view.dom.dispatchEvent(event);
  return event.defaultPrevented;
};

describe('headingBackspaceKeymap', () => {
  it('should merge a heading into the previous block with a single Backspace', async () => {
    const { view, markdown, blockTypes } = await createEditor('Intro\n\n# Title');
    placeCaret(view, 1);

    expect(pressBackspace(view)).toBe(true);

    expect(blockTypes()).toEqual(['paragraph']);
    expect(markdown()).toBe('IntroTitle\n');
  });

  it('should merge deeper headings in one press instead of stepping down levels', async () => {
    const { view, markdown } = await createEditor('Intro\n\n### Title');
    placeCaret(view, 1);

    pressBackspace(view);

    expect(markdown()).toBe('IntroTitle\n');
  });

  it('should remove an empty heading line in one press', async () => {
    const { view, markdown, blockTypes } = await createEditor('Intro\n\n# Title');
    placeCaret(view, 1);
    const { from } = view.state.selection;
    view.dispatch(view.state.tr.delete(from, from + 'Title'.length));
    expect(blockTypes()).toEqual(['paragraph', 'heading']);

    pressBackspace(view);

    expect(blockTypes()).toEqual(['paragraph']);
    expect(markdown()).toBe('Intro\n');
  });

  it('should turn a heading on the first line into a paragraph', async () => {
    const { view, markdown, blockTypes } = await createEditor('## Title\n\nBody');
    placeCaret(view, 0);

    expect(pressBackspace(view)).toBe(true);

    expect(blockTypes()).toEqual(['paragraph', 'paragraph']);
    expect(markdown()).toBe('Title\n\nBody\n');
  });

  it('should leave Backspace inside the heading text to the default behaviour', async () => {
    const { view, markdown } = await createEditor('Intro\n\n# Title');
    placeCaret(view, 1, 3);

    expect(pressBackspace(view)).toBe(false);

    expect(markdown()).toBe('Intro\n\n# Title\n');
  });
});
