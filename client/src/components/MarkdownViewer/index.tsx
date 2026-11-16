import React from 'react';
import { Milkdown, MilkdownProvider, useEditor } from '@milkdown/react';
import { Editor, defaultValueCtx, editorViewOptionsCtx, rootCtx } from '@milkdown/kit/core';
import { commonmark, linkAttr } from '@milkdown/kit/preset/commonmark';
import { gfm } from '@milkdown/kit/preset/gfm';
import { readOnlyTaskListItemView } from '../MarkdownEditor/taskListItemView';
import '@milkdown/kit/prose/view/style/prosemirror.css';
import '@milkdown/kit/prose/tables/style/tables.css';

interface MarkdownViewerProps {
  markdown: string;
}

const MarkdownViewerInner: React.FC<MarkdownViewerProps> = ({ markdown }) => {
  useEditor(root =>
    Editor.make()
      .config((ctx) => {
        ctx.set(rootCtx, root);
        ctx.set(defaultValueCtx, markdown);
        // Milkdown always gives the view role="textbox"; mark it read-only.
        ctx.update(editorViewOptionsCtx, prev => ({
          ...prev,
          editable: () => false,
          attributes: { 'aria-readonly': 'true' }
        }));
        ctx.set(linkAttr.key, () => ({ target: '_blank', rel: 'noopener noreferrer' }));
      })
      .use(commonmark)
      .use(gfm)
      .use(readOnlyTaskListItemView)
  );

  return <Milkdown />;
};

/**
 * Read-only markdown renderer backed by Milkdown, using the same presets as
 * the note editor so read views render exactly what the editor shows. It has
 * no change callbacks: links open in a new tab and task-list checkboxes are
 * disabled. A new `markdown` value remounts the underlying editor.
 *
 * @param {MarkdownViewerProps} props - The viewer props.
 * @param {string} props.markdown - The markdown source to render.
 * @returns {React.ReactNode} The rendered markdown.
 */
function MarkdownViewer({ markdown }: MarkdownViewerProps): React.ReactNode {
  return (
    <div className="milkdown-viewer" data-testid="markdown-viewer">
      <MilkdownProvider key={markdown}>
        <MarkdownViewerInner markdown={markdown} />
      </MilkdownProvider>
    </div>
  );
}

export default MarkdownViewer;
