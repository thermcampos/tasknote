import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router';
import { I18nextProvider } from 'react-i18next';
import Sidebar from '../../components/Sidebar';
import AuthContext from '../../context/AuthContext';
import SidebarContext from '../../context/SidebarContext';
import i18n from '../../i18n';

const authContextMock = {
  signed: true,
  user: {
    userId: 1,
    name: 'Ricardo',
    email: 'ricardo@campos.com',
    admin: false,
    createdAt: new Date(),
    gravatarImageUrl: 'http://url.com'
  },
  checkCurrentAuthUser: vi.fn(),
  signIn: vi.fn(),
  signOut: vi.fn(),
  register: vi.fn(),
  isAdmin: false,
  updateUser: vi.fn(),
};

const sidebarContextMock = {
  currentPage: '/home',
  setNewPage: vi.fn(),
  isCollapsed: false,
  toggleCollapsed: vi.fn()
};

describe('Sidebar Component', () => {
  const renderSidebar = (sidebarValue = sidebarContextMock) => {
    return render(
      <MemoryRouter>
        <AuthContext.Provider value={authContextMock}>
          <I18nextProvider i18n={i18n}>
            <SidebarContext.Provider value={sidebarValue}>
              <Sidebar isMobileOpen={false} setIsMobileOpen={vi.fn()} />
            </SidebarContext.Provider>
          </I18nextProvider>
        </AuthContext.Provider>
      </MemoryRouter>
    );
  };

  it('should render the Sidebar component', () => {
    const { getByText } = renderSidebar();
    expect(getByText('Ricardo')).toBeDefined();
  });

  it('should call signOut when logout is clicked', () => {
    const { getByText } = renderSidebar();
    fireEvent.click(getByText('Logout'));
    expect(authContextMock.signOut).toHaveBeenCalled();
  });

  it('should highlight the selected menu item', () => {
    const { getByText } = renderSidebar();
    fireEvent.click(getByText('Home'));
    const dashboardElement = getByText('Home').closest('.sidebar-nav');
    expect(dashboardElement).not.toBeNull();
    expect(dashboardElement!.classList.contains('selected')).toBe(true);
  });

  it('should call toggleCollapsed when the collapse button is clicked', () => {
    const toggleCollapsed = vi.fn();
    const { getByLabelText } = renderSidebar({ ...sidebarContextMock, toggleCollapsed });
    fireEvent.click(getByLabelText('Collapse sidebar'));
    expect(toggleCollapsed).toHaveBeenCalled();
  });
});

describe('Sidebar collapsed rail', () => {
  const collapsedContextMock = {
    ...sidebarContextMock,
    isCollapsed: true
  };

  const renderCollapsedSidebar = () => {
    return render(
      <MemoryRouter>
        <AuthContext.Provider value={authContextMock}>
          <I18nextProvider i18n={i18n}>
            <SidebarContext.Provider value={collapsedContextMock}>
              <Sidebar isMobileOpen={false} setIsMobileOpen={vi.fn()} />
            </SidebarContext.Provider>
          </I18nextProvider>
        </AuthContext.Provider>
      </MemoryRouter>
    );
  };

  it('should render the rail without labels when collapsed', () => {
    const { container, queryByText } = renderCollapsedSidebar();
    expect(container.querySelector('.sidebar-collapsed')).not.toBeNull();
    expect(queryByText('Ricardo')).toBeNull();
    expect(queryByText('Logout')).toBeNull();
  });

  it('should expose native title tooltips on nav items when collapsed', () => {
    const { getByTitle } = renderCollapsedSidebar();
    expect(getByTitle('Home')).toBeDefined();
    expect(getByTitle('Logout')).toBeDefined();
  });

  it('should label the toggle as expand when collapsed', () => {
    const { getByLabelText } = renderCollapsedSidebar();
    expect(getByLabelText('Expand sidebar').getAttribute('aria-expanded')).toBe('false');
  });
});

describe('Build link visibility', () => {
  it('build link keeps footer-link class for theme-aware styling', () => {
    const { getByTestId } = render(
      <MemoryRouter>
        <AuthContext.Provider value={authContextMock}>
          <I18nextProvider i18n={i18n}>
            <SidebarContext.Provider value={sidebarContextMock}>
              <Sidebar isMobileOpen={false} setIsMobileOpen={vi.fn()} />
            </SidebarContext.Provider>
          </I18nextProvider>
        </AuthContext.Provider>
      </MemoryRouter>
    );
    const link = getByTestId('footer-text');
    expect(link.classList.contains('footer-link')).toBe(true);
    expect(link.getAttribute('href')).toContain('CHANGELOG.md');
  });
});
