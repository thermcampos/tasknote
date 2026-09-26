// AuthProvider.test.tsx
import React, { useContext } from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, act, waitFor, cleanup } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SidebarProvider from '../../context/SidebarProvider';
import SidebarContext, { SidebarContextData } from '../../context/SidebarContext';

// Create a helper component to consume AuthContext for testing.
const ConsumerComponent: React.FC = () => {
  const {
    currentPage,
    setNewPage,
    isCollapsed,
    toggleCollapsed
  } = useContext<SidebarContextData>(SidebarContext);

  return (
    <div>
      <div data-testid="page">{currentPage}</div>
      <div data-testid="collapsed">{String(isCollapsed)}</div>
      <button
        data-testid="setPage"
        onClick={() => {
          setNewPage('/another');
        }}
      >
        Change page
      </button>
      <button
        data-testid="toggleCollapsed"
        onClick={() => {
          toggleCollapsed();
        }}
      >
        Toggle collapsed
      </button>
    </div>
  );
};

describe('SidebarProvider', () => {
  // Reset DOM and mocks for each test.
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  afterEach(() => {
    cleanup();
  });

  it('should render the default context values', () => {
    const { getByTestId } = render(
      <SidebarProvider>
        <ConsumerComponent />
      </SidebarProvider>
    );

    expect(getByTestId('page').textContent).toBe('/home');
  });

  it('should set a new page after click', async () => {
    const { getByTestId } = render(
      <SidebarProvider>
        <ConsumerComponent />
      </SidebarProvider>
    );

    await act(async () => {
      userEvent.click(getByTestId('setPage'));
    });

    await waitFor(() =>
      expect(getByTestId('page').textContent).toBe('/home')
    );
  });

  it('should default to expanded when nothing is persisted', () => {
    const { getByTestId } = render(
      <SidebarProvider>
        <ConsumerComponent />
      </SidebarProvider>
    );

    expect(getByTestId('collapsed').textContent).toBe('false');
  });

  it('should initialize collapsed state from localStorage', () => {
    localStorage.setItem('SIDEBAR_COLLAPSED', 'true');
    const { getByTestId } = render(
      <SidebarProvider>
        <ConsumerComponent />
      </SidebarProvider>
    );

    expect(getByTestId('collapsed').textContent).toBe('true');
  });

  it('should toggle collapsed state and persist it to localStorage', async () => {
    const { getByTestId } = render(
      <SidebarProvider>
        <ConsumerComponent />
      </SidebarProvider>
    );

    await act(async () => {
      userEvent.click(getByTestId('toggleCollapsed'));
    });

    await waitFor(() => {
      expect(getByTestId('collapsed').textContent).toBe('true');
      expect(localStorage.getItem('SIDEBAR_COLLAPSED')).toBe('true');
    });

    await act(async () => {
      userEvent.click(getByTestId('toggleCollapsed'));
    });

    await waitFor(() => {
      expect(getByTestId('collapsed').textContent).toBe('false');
      expect(localStorage.getItem('SIDEBAR_COLLAPSED')).toBe('false');
    });
  });
});
