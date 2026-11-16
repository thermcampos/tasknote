import { joinBackward, setBlockType } from '@milkdown/kit/prose/commands';
import type { Command } from '@milkdown/kit/prose/state';
import { $useKeymap } from '@milkdown/kit/utils';

/**
 * Backspace at the very start of a heading. Milkdown's default only lowers the
 * heading level (or turns it into a paragraph) and stops, so the line never
 * goes away in one press (issue #130). Instead:
 * - if there is a block before it, join the heading into that block, like any
 *   other line;
 * - if it is the first block of the document, turn it into a paragraph.
 *
 * Anywhere else (not a heading, caret not at offset 0, or a range selected)
 * the command does nothing and Backspace keeps its default behaviour.
 *
 * @param {EditorState} state - The current editor state.
 * @param {Function} dispatch - Dispatches the resulting transaction.
 * @param {EditorView} view - The editor view.
 * @returns {boolean} Whether the key press was handled.
 */
export const removeHeadingBackward: Command = (state, dispatch, view) => {
  const { $from, empty } = state.selection;
  const heading = $from.parent;
  if (!empty || $from.parentOffset !== 0 || heading.type.name !== 'heading') return false;

  const isFirstBlock = $from.depth === 1 && $from.index(0) === 0;
  if (!isFirstBlock) return joinBackward(state, dispatch, view);

  const paragraph = state.schema.nodes.paragraph;
  return setBlockType(paragraph)(state, dispatch, view);
};

/**
 * Milkdown keymap wiring {@link removeHeadingBackward} to Backspace. Its
 * priority is above the commonmark preset's default (50) so it runs first.
 */
export const headingBackspaceKeymap = $useKeymap('headingBackspace', {
  RemoveHeadingBackward: {
    shortcuts: 'Backspace',
    priority: 100,
    command: () => removeHeadingBackward
  }
});
