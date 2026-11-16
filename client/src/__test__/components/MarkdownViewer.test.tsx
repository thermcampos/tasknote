import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { defaultValueCtx, editorViewOptionsCtx, rootCtx } from '@milkdown/kit/core';
import { commonmark, linkAttr } from '@milkdown/kit/preset/commonmark';
import { gfm } from '@milkdown/kit/preset/gfm';
import { history } from '@milkdown/kit/plugin/history';
import { listener } from '@milkdown/kit/plugin/listener';
import {
  readOnlyTaskListItemView,
  taskListItemView
} from '../../components/MarkdownEditor/taskListItemView';
import MarkdownViewer from '../../components/MarkdownViewer';

// Real Milkdown under jsdom is brittle (ADR 003), so the viewer is tested by
// recording how it configures the editor. Real rendering is covered by Cypress.
type ConfigFn = (ctx: FakeCtx) => void;

interface FakeCtx {
  set: ReturnType<typeof vi.fn>;
  update: ReturnType<typeof vi.fn>;
}

interface EditorBuild {
  configs: ConfigFn[];
  plugins: unknown[];
}

const builds: EditorBuild[] = [];

vi.mock('@milkdown/kit/core', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@milkdown/kit/core')>();
  return {
    ...actual,
    Editor: {
      make: () => {
        const build: EditorBuild = { configs: [], plugins: [] };
        builds.push(build);
        const chain = {
          config: (fn: ConfigFn) => {
            build.configs.push(fn);
            return chain;
          },
          use: (plugin: unknown) => {
            build.plugins.push(plugin);
            return chain;
          }
        };
        return chain;
      }
    }
  };
});

vi.mock('@milkdown/react', () => ({
  MilkdownProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  Milkdown: () => <div data-testid="milkdown-root" />,
  useEditor: (factory: (root: HTMLElement) => unknown) => {
    const root = document.createElement('div');
    factory(root);
    return { get: () => undefined, loading: false };
  }
}));

/**
 * Runs every recorded config callback of the given build against a fake ctx.
 *
 * @param {EditorBuild} build - The recorded editor build.
 * @returns {FakeCtx} The fake ctx with recorded calls.
 */
const runConfig = (build: EditorBuild): FakeCtx => {
  const ctx: FakeCtx = { set: vi.fn(), update: vi.fn() };
  build.configs.forEach(fn => fn(ctx));
  return ctx;
};

/**
 * Finds the value passed to `ctx.set` for the given slice key.
 *
 * @param {FakeCtx} ctx - The fake ctx.
 * @param {unknown} key - The ctx slice or key.
 * @returns {unknown} The value set for that key.
 */
const valueSetFor = (ctx: FakeCtx, key: unknown): unknown =>
  ctx.set.mock.calls.find(([k]) => k === key)?.[1];

describe('MarkdownViewer', () => {
  beforeEach(() => {
    builds.length = 0;
  });

  afterEach(() => {
    cleanup();
  });

  it('should render the Milkdown surface inside the viewer container', () => {
    render(<MarkdownViewer markdown="# Hello" />);

    const viewer = screen.getByTestId('markdown-viewer');
    expect(viewer.className).toBe('milkdown-viewer');
    expect(viewer.querySelector('[data-testid="milkdown-root"]')).not.toBeNull();
  });

  it('should seed the editor with the provided markdown', () => {
    render(<MarkdownViewer markdown={'# Title\n\nSome **bold** text'} />);

    const ctx = runConfig(builds[0]);
    expect(valueSetFor(ctx, defaultValueCtx)).toBe('# Title\n\nSome **bold** text');
    expect(valueSetFor(ctx, rootCtx)).toBeInstanceOf(HTMLElement);
  });

  it('should apply read-only mode', () => {
    render(<MarkdownViewer markdown="text" />);

    const ctx = runConfig(builds[0]);
    const call = ctx.update.mock.calls.find(([k]) => k === editorViewOptionsCtx);
    expect(call).toBeDefined();
    type Options = { editable: () => boolean; attributes: Record<string, string> };
    const updater = call![1] as (prev: Record<string, unknown>) => Options;
    const options = updater({ handleDOMEvents: {} });
    expect(options.editable()).toBe(false);
    expect(options.attributes).toEqual({ 'aria-readonly': 'true' });
    expect(options).toHaveProperty('handleDOMEvents');
  });

  it('should open links in a new tab without opener access', () => {
    render(<MarkdownViewer markdown="[x](https://example.com)" />);

    const ctx = runConfig(builds[0]);
    const attrs = valueSetFor(ctx, linkAttr.key) as () => Record<string, string>;
    expect(attrs()).toEqual({ target: '_blank', rel: 'noopener noreferrer' });
  });

  it('should use the editor presets with the read-only task-list view and no edit plugins', () => {
    render(<MarkdownViewer markdown="- [ ] task" />);

    const { plugins } = builds[0];
    expect(plugins).toContain(commonmark);
    expect(plugins).toContain(gfm);
    expect(plugins).toContain(readOnlyTaskListItemView);
    expect(plugins).not.toContain(taskListItemView);
    expect(plugins).not.toContain(listener);
    expect(plugins).not.toContain(history);
  });

  it('should rebuild the editor when the markdown changes', () => {
    const { rerender } = render(<MarkdownViewer markdown="first" />);
    rerender(<MarkdownViewer markdown="second" />);

    const lastCtx = runConfig(builds[builds.length - 1]);
    expect(valueSetFor(lastCtx, defaultValueCtx)).toBe('second');
  });
});
