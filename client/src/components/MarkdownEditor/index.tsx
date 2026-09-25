import React, { forwardRef, useImperativeHandle, useState } from 'react';
import { Milkdown, MilkdownProvider, useEditor } from '@milkdown/react';
import { Editor, defaultValueCtx, editorViewCtx, rootCtx } from '@milkdown/kit/core';
import { commonmark } from '@milkdown/kit/preset/commonmark';
import { gfm } from '@milkdown/kit/preset/gfm';
import { history } from '@milkdown/kit/plugin/history';
import { listener, listenerCtx } from '@milkdown/kit/plugin/listener';
import '@milkdown/kit/prose/view/style/prosemirror.css';
import '@milkdown/kit/prose/tables/style/tables.css';

export interface MarkdownEditorHandle {
  focus: () => void;
}

interface MarkdownEditorProps {
  defaultValue: string;
  placeholder: string;
  onChange: (markdown: string) => void;
}

const MarkdownEditorInner = forwardRef<MarkdownEditorHandle, MarkdownEditorProps>(
  ({ defaultValue, placeholder, onChange }, ref) => {
    const [isEmpty, setIsEmpty] = useState<boolean>(defaultValue.trim() === '');

    const { get } = useEditor(root =>
      Editor.make()
        .config((ctx) => {
          ctx.set(rootCtx, root);
          ctx.set(defaultValueCtx, defaultValue);
          ctx.get(listenerCtx).markdownUpdated((_ctx, markdown) => {
            setIsEmpty(markdown.trim() === '');
            onChange(markdown);
          });
        })
        .use(commonmark)
        .use(gfm)
        .use(listener)
        .use(history)
    );

    useImperativeHandle(ref, () => ({
      focus: () => {
        get()?.action((ctx) => {
          ctx.get(editorViewCtx).focus();
        });
      }
    }));

    return (
      <div
        className={`milkdown-editor${isEmpty ? ' editor-empty' : ''}`}
        data-placeholder={placeholder}
        data-testid="note-markdown-editor"
      >
        <Milkdown />
      </div>
    );
  }
);

MarkdownEditorInner.displayName = 'MarkdownEditorInner';

/**
 * Notion-style live markdown editor backed by Milkdown. Content is kept as
 * plain markdown: `defaultValue` seeds the editor and `onChange` reports the
 * serialized markdown after every edit. Remount (via `key`) to replace the
 * content externally.
 *
 * @param {MarkdownEditorProps} props - The editor props.
 * @param {React.Ref<MarkdownEditorHandle>} ref - Exposes `focus()`.
 * @returns {React.ReactNode} The rendered editor.
 */
const MarkdownEditor = forwardRef<MarkdownEditorHandle, MarkdownEditorProps>((props, ref) => (
  <MilkdownProvider>
    <MarkdownEditorInner {...props} ref={ref} />
  </MilkdownProvider>
));

MarkdownEditor.displayName = 'MarkdownEditor';

export default MarkdownEditor;
