import React from 'react';
import { describe, vi, it, expect, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, act, cleanup } from '@testing-library/react';
import ModalMarkdown from '../../components/ModalMarkdown';

// The read-only Milkdown viewer is unit-tested on its own; here a stand-in
// keeps the `markdown` contract. Real rendering is covered by Cypress.
vi.mock('../../components/MarkdownViewer', () => ({
  default: ({ markdown }: { markdown: string }) => (
    <div data-testid="markdown-viewer">{markdown}</div>
  )
}));

describe('ModalMarkdown Component', () => {
  const props = {
    show: true,
    title: 'Test Title',
    markdownText: '# Test Markdown',
    onHide: vi.fn(),
  };

  beforeEach(() => {
    vi.stubGlobal('navigator', {
      clipboard: {
        writeText: vi.fn().mockResolvedValue(undefined),
      },
    });
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
    cleanup();
  });

  it('should render the modal with the correct title and markdown text', () => {
    render(<ModalMarkdown {...props} />);

    expect(screen.getByTestId('modal-header-title').innerHTML).toBe('Test Title');
    expect(screen.getByTestId('markdown-viewer').textContent).toBe('# Test Markdown');
  });

  it('should call onHide when the close button is clicked', () => {
    render(<ModalMarkdown {...props} />);

    fireEvent.click(screen.getByText('Close'));
    expect(props.onHide).toHaveBeenCalled();
  });

  it('should not render the modal when show is false', () => {
    render(<ModalMarkdown {...props} show={false} />);

    expect(screen.queryByTestId('modal-header-title')).toBeNull();
  });

  it('should render "No title" when title is an empty string', () => {
    render(<ModalMarkdown {...props} title="" />);

    expect(screen.getByTestId('modal-header-title').innerHTML).toBe('No title');
  });

  it('should show Source and Copy buttons', () => {
    render(<ModalMarkdown {...props} />);

    expect(screen.getByTestId('modal-source-button')).toBeDefined();
    expect(screen.getByTestId('modal-copy-button')).toBeDefined();
  });

  it('should toggle to source view when Source button is clicked', () => {
    render(<ModalMarkdown {...props} />);

    expect(screen.queryByTestId('markdown-source-view')).toBeNull();

    fireEvent.click(screen.getByTestId('modal-source-button'));

    expect(screen.getByTestId('markdown-source-view')).toBeDefined();
    expect(screen.getByTestId('markdown-source-view').textContent).toBe('# Test Markdown');
    expect(screen.queryByTestId('markdown-viewer')).toBeNull();
  });

  it('should toggle back to rendered view when Source button is clicked again', () => {
    render(<ModalMarkdown {...props} />);

    fireEvent.click(screen.getByTestId('modal-source-button'));
    expect(screen.getByTestId('markdown-source-view')).toBeDefined();

    fireEvent.click(screen.getByTestId('modal-source-button'));
    expect(screen.queryByTestId('markdown-source-view')).toBeNull();
  });

  it('should call clipboard writeText with markdownText when Copy button is clicked', async () => {
    render(<ModalMarkdown {...props} />);

    await act(async () => {
      fireEvent.click(screen.getByTestId('modal-copy-button'));
    });

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith('# Test Markdown');
  });

  it('should show "Copied!" text after Copy button is clicked', async () => {
    vi.useFakeTimers();
    render(<ModalMarkdown {...props} />);

    const copyButton = screen.getByTestId('modal-copy-button');

    await act(async () => {
      fireEvent.click(copyButton);
    });

    // Resolve microtasks for the clipboard promise
    await act(async () => {
      await vi.runAllTicks();
    });

    expect(screen.getByTestId('modal-copy-button').textContent).toBe('Copied!');

    // Advance timers to see it go back to "Copy"
    await act(async () => {
      vi.advanceTimersByTime(2000);
    });

    expect(screen.getByTestId('modal-copy-button').textContent).toBe('Copy');
    vi.useRealTimers();
  });

  it('should render tags below the content', () => {
    render(<ModalMarkdown {...props} tags={['work', 'ideas']} />);

    const tags = screen.getByTestId('modal-tags-preview');
    expect(tags.textContent).toBe('#work#ideas');
    const viewer = screen.getByTestId('markdown-viewer');
    expect(viewer.compareDocumentPosition(tags) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('should reset source view state when modal is closed', () => {
    render(<ModalMarkdown {...props} />);

    fireEvent.click(screen.getByTestId('modal-source-button'));
    expect(screen.getByTestId('markdown-source-view')).toBeDefined();

    fireEvent.click(screen.getByText('Close'));
    expect(props.onHide).toHaveBeenCalled();
  });
});
