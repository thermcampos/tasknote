import React from 'react';
import { render, waitFor } from '@testing-library/react';
import { beforeAll, describe, expect, it, vi } from 'vitest';
import MarkdownEditor from '../../components/MarkdownEditor';

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

describe('MarkdownEditor', () => {
  it('should render initial markdown as rich content', async () => {
    const { container } = render(
      <MarkdownEditor
        defaultValue={'# Title\n\nSome **bold** text\n\n- one\n- two'}
        placeholder="Write something..."
        onChange={vi.fn()}
      />
    );

    await waitFor(() => {
      expect(container.querySelector('h1')?.textContent).toBe('Title');
    });

    expect(container.querySelector('strong')?.textContent).toBe('bold');
    expect(container.querySelectorAll('li').length).toBe(2);
  });

  it('should serialize edits back to markdown on change', async () => {
    const onChange = vi.fn();
    const { container } = render(
      <MarkdownEditor defaultValue="Hello" placeholder="Write something..." onChange={onChange} />
    );

    await waitFor(() => {
      expect(container.querySelector('.ProseMirror')).toBeDefined();
    });

    const editor = container.querySelector('.ProseMirror') as HTMLElement;
    editor.focus();

    await waitFor(() => {
      expect(editor.textContent).toContain('Hello');
    });
  });

  it('should mark the editor as empty when there is no content', async () => {
    const { getByTestId } = render(
      <MarkdownEditor defaultValue="" placeholder="Write something..." onChange={vi.fn()} />
    );

    await waitFor(() => {
      expect(getByTestId('note-markdown-editor').className).toContain('editor-empty');
    });
  });
});
