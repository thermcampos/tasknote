import { listItemSchema } from '@milkdown/kit/preset/commonmark';
import type { Node } from '@milkdown/kit/prose/model';
import type { EditorView, NodeView, ViewMutationRecord } from '@milkdown/kit/prose/view';
import { $view } from '@milkdown/kit/utils';

export class TaskListItemView implements NodeView {
  dom: HTMLLIElement;
  contentDOM: HTMLDivElement;
  private checkbox: HTMLInputElement | null = null;

  constructor(
    private node: Node,
    private readonly view: EditorView,
    private readonly getPos: () => number | undefined,
    private readonly readOnly: boolean = false
  ) {
    this.dom = document.createElement('li');
    this.contentDOM = document.createElement('div');
    this.dom.appendChild(this.contentDOM);
    this.render();
  }

  update(node: Node): boolean {
    if (node.type !== this.node.type) return false;
    this.node = node;
    this.render();
    return true;
  }

  ignoreMutation(mutation: ViewMutationRecord): boolean {
    return this.checkbox !== null && mutation.target === this.checkbox;
  }

  destroy(): void {
    this.checkbox?.removeEventListener('change', this.handleToggle);
  }

  private render(): void {
    const { checked, label, listType, spread } = this.node.attrs;
    this.dom.dataset.label = label;
    this.dom.dataset.listType = listType;
    this.dom.dataset.spread = String(spread);

    if (checked === null || checked === undefined) {
      delete this.dom.dataset.itemType;
      delete this.dom.dataset.checked;
      if (this.checkbox) {
        this.checkbox.removeEventListener('change', this.handleToggle);
        this.checkbox.remove();
        this.checkbox = null;
      }
      return;
    }

    this.dom.dataset.itemType = 'task';
    this.dom.dataset.checked = String(checked);

    if (!this.checkbox) {
      const checkbox = document.createElement('input');
      checkbox.type = 'checkbox';
      checkbox.className = 'task-list-item-checkbox';
      checkbox.setAttribute('contenteditable', 'false');
      if (this.readOnly) {
        // Read views must not dispatch: `editable: false` does not block
        // programmatic transactions, so the checkbox gets no change handler.
        checkbox.disabled = true;
        checkbox.setAttribute('aria-label', 'Task');
        // `disabled` stops real user clicks, but a dispatched click event still
        // runs the checkbox's toggle default action; cancel it so the read view
        // can never show a state that differs from the stored markdown.
        checkbox.addEventListener('click', event => event.preventDefault());
      }
      else {
        checkbox.setAttribute('aria-label', 'Toggle task');
        checkbox.addEventListener('mousedown', event => event.preventDefault());
        checkbox.addEventListener('change', this.handleToggle);
      }
      this.dom.insertBefore(checkbox, this.contentDOM);
      this.checkbox = checkbox;
    }
    this.checkbox.checked = Boolean(checked);
  }

  private handleToggle = (event: Event): void => {
    if (this.readOnly) return;
    const pos = this.getPos();
    if (pos === undefined) return;
    const checked = (event.target as HTMLInputElement).checked;
    this.view.dispatch(
      this.view.state.tr.setNodeMarkup(pos, undefined, { ...this.node.attrs, checked })
    );
  };
}

/**
 * Milkdown node view that renders GFM task list items (`- [ ]`/`- [x]`) with a
 * clickable checkbox instead of a plain bullet. Toggling the checkbox updates
 * the `checked` attribute so the serialized markdown stays in sync.
 */
export const taskListItemView = $view(listItemSchema.node, () => {
  return (node, view, getPos) => new TaskListItemView(node, view, getPos);
});

/**
 * Read-only variant of {@link taskListItemView} for read views: renders the
 * same DOM, but the checkbox is disabled and never dispatches a transaction.
 */
export const readOnlyTaskListItemView = $view(listItemSchema.node, () => {
  return (node, view, getPos) => new TaskListItemView(node, view, getPos, true);
});
