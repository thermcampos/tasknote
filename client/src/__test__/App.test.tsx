import React, { act } from 'react';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import App from '../App';
import { render, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AuthContext, { AuthContextData } from '../context/AuthContext';
import authContextMock from './__mocks__/authContextMock';
import SidebarContext from '../context/SidebarContext';
import FilterContext from '../context/FilterContext';
import api from '../api-service/api';
import ApiConfig from '../api-service/apiConfig';
import { THEME_PENDING } from '../app-constants/app-constants';

vi.mock('../api-service/api');

const sidebarContextMock = {
  currentPage: '/home',
  setNewPage: vi.fn()
};

const filterContextMock = {
  filterText: '',
  selectedOption: 'everything',
  setFilterText: vi.fn(),
  setSelectedOption: vi.fn()
};

vi.mock('react-charts', () => ({
  Chart: ({ options }) => <div data-testid="mocked-chart">Mocked Chart</div>
}));

const fakeUser = (theme: string) => ({
  userId: 1,
  name: 'Theme User',
  email: 'theme@example.com',
  admin: false,
  createdAt: new Date(),
  gravatarImageUrl: 'http://dummyimage.com',
  lang: 'en',
  lastLogin: new Date().toISOString(),
  theme
});

const renderApp = async (authValue: Partial<AuthContextData> = {}) => {
  let utils;
  await act(async () => {
    utils = render(
      <AuthContext.Provider value={{ ...authContextMock, ...authValue }}>
        <SidebarContext.Provider value={sidebarContextMock}>
          <FilterContext.Provider value={filterContextMock}>
            <App />
          </FilterContext.Provider>
        </SidebarContext.Provider>
      </AuthContext.Provider>
    );
  });
  return utils!;
};

beforeEach(() => {
  localStorage.clear();
  document.body.removeAttribute('data-bs-theme');
});

test('Renders the app', async () => {
  await renderApp();
});

describe('Theme handling', () => {
  test('Applies the theme from localStorage on load', async () => {
    localStorage.setItem('theme', 'dark');

    await renderApp();

    expect(document.body.getAttribute('data-bs-theme')).toBe('dark');
  });

  test('Toggle when signed out updates only localStorage', async () => {
    const patchSpy = vi.spyOn(api, 'patchJSON');

    const { getByRole } = await renderApp({ signed: false, user: undefined });

    await userEvent.click(getByRole('button', { name: /dark mode/i }));

    expect(document.body.getAttribute('data-bs-theme')).toBe('dark');
    expect(localStorage.getItem('theme')).toBe('dark');
    expect(patchSpy).not.toHaveBeenCalled();
  });

  test('Toggle when signed in updates the UI immediately and saves to the server', async () => {
    const patchSpy = vi.spyOn(api, 'patchJSON').mockResolvedValue(undefined);

    const { getByRole } = await renderApp({ signed: true, user: fakeUser('light') });

    await userEvent.click(getByRole('button', { name: /dark mode/i }));

    expect(document.body.getAttribute('data-bs-theme')).toBe('dark');
    expect(localStorage.getItem('theme')).toBe('dark');
    await waitFor(() =>
      expect(patchSpy).toHaveBeenCalledWith(
        ApiConfig.userUrl,
        expect.objectContaining({ theme: 'dark' })
      )
    );
    expect(localStorage.getItem(THEME_PENDING)).toBeNull();
  });

  test('Failed save keeps the chosen theme and marks it as pending', async () => {
    vi.spyOn(api, 'patchJSON').mockRejectedValue(new Error('Network error'));

    const { getByRole } = await renderApp({ signed: true, user: fakeUser('light') });

    await userEvent.click(getByRole('button', { name: /dark mode/i }));

    await waitFor(() =>
      expect(localStorage.getItem(THEME_PENDING)).toBe('dark')
    );
    expect(document.body.getAttribute('data-bs-theme')).toBe('dark');
    expect(localStorage.getItem('theme')).toBe('dark');
  });

  test('Server theme wins over a stale localStorage value when user data arrives', async () => {
    localStorage.setItem('theme', 'light');
    const patchSpy = vi.spyOn(api, 'patchJSON');

    await renderApp({ signed: true, user: fakeUser('dark') });

    await waitFor(() =>
      expect(document.body.getAttribute('data-bs-theme')).toBe('dark')
    );
    expect(localStorage.getItem('theme')).toBe('dark');
    expect(patchSpy).not.toHaveBeenCalled();
  });

  test('Pending save wins over the server value and is retried on user data arrival', async () => {
    localStorage.setItem('theme', 'dark');
    localStorage.setItem(THEME_PENDING, 'dark');
    const patchSpy = vi.spyOn(api, 'patchJSON').mockResolvedValue(undefined);

    await renderApp({ signed: true, user: fakeUser('light') });

    await waitFor(() =>
      expect(patchSpy).toHaveBeenCalledWith(
        ApiConfig.userUrl,
        expect.objectContaining({ theme: 'dark' })
      )
    );
    expect(document.body.getAttribute('data-bs-theme')).toBe('dark');
    await waitFor(() =>
      expect(localStorage.getItem(THEME_PENDING)).toBeNull()
    );
  });
});
