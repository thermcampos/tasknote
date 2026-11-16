import { fireEvent } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { Node } from '@milkdown/kit/prose/model';
import type { EditorView } from '@milkdown/kit/prose/view';
import { TaskListItemView } from '../../components/MarkdownEditor/taskListItemView';

/**
 * Builds a fake list-item node with the given `checked` attribute.
 *
 * @param {boolean | null} checked - The task state, or null for a plain item.
 * @returns {Node} The fake node.
 */
const fakeNode = (checked: boolean | null): Node =>
  ({
    type: 'list_item',
    attrs: { checked, label: '•', listType: 'bullet', spread: false }
  }) as unknown as Node;

/**
 * Builds a fake editor view that records dispatched transactions.
 *
 * @returns {EditorView} The fake view.
 */
const fakeView = (): EditorView =>
  ({
    dispatch: vi.fn(),
    state: { tr: { setNodeMarkup: vi.fn(() => 'tr') } }
  }) as unknown as EditorView;

const checkboxOf = (view: TaskListItemView): HTMLInputElement =>
  view.dom.querySelector('input[type="checkbox"]') as HTMLInputElement;

describe('TaskListItemView', () => {
  it('should dispatch a transaction when the editable checkbox changes', () => {
    const view = fakeView();
    const nodeView = new TaskListItemView(fakeNode(false), view, () => 3);

    const checkbox = checkboxOf(nodeView);
    expect(checkbox.disabled).toBe(false);
    checkbox.checked = true;
    fireEvent.change(checkbox);

    expect(view.dispatch).toHaveBeenCalledTimes(1);
  });

  it('should render a disabled checkbox in read-only mode', () => {
    const nodeView = new TaskListItemView(fakeNode(true), fakeView(), () => 3, true);

    const checkbox = checkboxOf(nodeView);
    expect(checkbox.disabled).toBe(true);
    expect(checkbox.checked).toBe(true);
    expect(nodeView.dom.dataset.itemType).toBe('task');
    expect(nodeView.dom.dataset.checked).toBe('true');
  });

  it('should never dispatch from a read-only checkbox', () => {
    const view = fakeView();
    const nodeView = new TaskListItemView(fakeNode(false), view, () => 3, true);

    const checkbox = checkboxOf(nodeView);
    fireEvent.click(checkbox);
    checkbox.checked = true;
    fireEvent.change(checkbox);

    expect(view.dispatch).not.toHaveBeenCalled();
  });

  it('should keep a read-only checkbox unchanged on a dispatched click', () => {
    const nodeView = new TaskListItemView(fakeNode(false), fakeView(), () => 3, true);

    const checkbox = checkboxOf(nodeView);
    checkbox.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));

    expect(checkbox.checked).toBe(false);
  });

  it('should render no checkbox for a plain list item', () => {
    const nodeView = new TaskListItemView(fakeNode(null), fakeView(), () => 3, true);

    expect(checkboxOf(nodeView)).toBeNull();
    expect(nodeView.dom.dataset.itemType).toBeUndefined();
  });
});
