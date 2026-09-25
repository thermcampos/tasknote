import React from 'react';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useNavigate } from 'react-router';
import AuthContext from '../../context/AuthContext';
import FilterProvider from '../../context/FilterProvider';
import SidebarContext from '../../context/SidebarContext';
import api from '../../api-service/api';
import Home from '../../views/Home';
import {
  cacheHomeItems,
  clearHomeCache,
  getCachedHomeItems
} from '../../utils/HomeCache';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: 'en', changeLanguage: vi.fn() }
  })
}));

vi.mock('../../api-service/api', () => ({
  default: {
    getJSON: vi.fn(),
    postJSON: vi.fn(),
    patchJSON: vi.fn(),
    putJSON: vi.fn(),
    deleteNoContent: vi.fn()
  }
}));

vi.mock('../../lang-service/LangHandler', () => ({
  handleDefaultLang: vi.fn()
}));

vi.mock('../../utils/TranslatorUtils', () => ({
  translateServerResponse: (text: string) => text,
  translateTaskResponse: (tasks: unknown[]) => tasks
}));

const user = {
  userId: 9,
  name: 'Test User',
  email: 'test@example.com',
  admin: false,
  createdAt: new Date(),
  gravatarImageUrl: '',
  lang: 'en',
  lastLogin: '',
  theme: 'light'
};

const authValue = {
  signed: true,
  loading: false,
  user,
  checkCurrentAuthUser: vi.fn(),
  signIn: vi.fn(),
  signOut: vi.fn(),
  register: vi.fn(),
  isAdmin: false,
  updateUser: vi.fn()
};

const task = (id: number, description: string) => ({
  id,
  description,
  completed: false,
  urls: [],
  tags: [],
  lastUpdate: '2026-09-25',
  highPriority: false,
  dueDateFmt: '',
  dueDate: ''
});

function FormRoute() {
  const navigate = useNavigate();

  return (
    <>
      <button onClick={() => navigate('/home')}>Cancel</button>
      <button onClick={() => navigate('/home', { state: { refreshHome: true } })}>Save</button>
    </>
  );
}

function TestApp() {
  return (
    <AuthContext.Provider value={authValue}>
      <FilterProvider>
        <SidebarContext.Provider value={{ currentPage: '/home', setNewPage: vi.fn() }}>
          <MemoryRouter initialEntries={['/home']}>
            <NavigateToForm />
            <Routes>
              <Route path="/home" element={<Home />} />
              <Route path="/tasks/new" element={<FormRoute />} />
            </Routes>
          </MemoryRouter>
        </SidebarContext.Provider>
      </FilterProvider>
    </AuthContext.Provider>
  );
}

function NavigateToForm() {
  const navigate = useNavigate();
  return <button onClick={() => navigate('/tasks/new')}>Add task</button>;
}

const getItemCalls = () => (api.getJSON as ReturnType<typeof vi.fn>).mock.calls
  .filter(([url]) => String(url).includes('/home/items'));

const getTagCalls = () => (api.getJSON as ReturnType<typeof vi.fn>).mock.calls
  .filter(([url]) => String(url).includes('/tasks/tags'));

describe('Home navigation cache', () => {
  beforeEach(() => {
    clearHomeCache();
    localStorage.clear();
    vi.clearAllMocks();
    (api.getJSON as ReturnType<typeof vi.fn>).mockImplementation((url: string) => {
      if (url.includes('/tasks/tags')) {
        return Promise.resolve(['work']);
      }
      if (url.includes('/home/items')) {
        return Promise.resolve({ tasks: [task(1, 'Saved from server')], notes: [] });
      }
      return Promise.resolve([]);
    });
  });

  afterEach(() => {
    clearHomeCache();
  });

  test('keeps cached responses separate from the API and Home view', () => {
    const response = { tasks: [task(1, 'Cached')], notes: [] };
    cacheHomeItems(user.userId, '/home/items', response);

    response.tasks[0].lastUpdate = 'changed by Home';
    response.tasks[0].tags.push('changed by API');

    const firstRead = getCachedHomeItems(user.userId, '/home/items');
    expect(firstRead?.tasks[0].lastUpdate).toBe('2026-09-25');
    expect(firstRead?.tasks[0].tags).toEqual([]);

    firstRead!.tasks[0].lastUpdate = 'changed by Home';
    firstRead!.tasks[0].urls.push('changed by Home');

    const secondRead = getCachedHomeItems(user.userId, '/home/items');
    expect(secondRead?.tasks[0].lastUpdate).toBe('2026-09-25');
    expect(secondRead?.tasks[0].urls).toEqual([]);
  });

  test('returns to cached home data without fetching when a form is cancelled', async () => {
    render(<TestApp />);

    await waitFor(() => expect(getItemCalls()).toHaveLength(1));
    await waitFor(() => expect(getTagCalls()).toHaveLength(1));
    expect(await screen.findByText('Saved from server')).toBeDefined();

    fireEvent.click(screen.getByRole('button', { name: 'Add task' }));
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(await screen.findByText('Saved from server')).toBeDefined();
    expect(getItemCalls()).toHaveLength(1);
    expect(getTagCalls()).toHaveLength(1);
  });

  test('refreshes home items and tags after a form is saved', async () => {
    let itemRequest = 0;
    (api.getJSON as ReturnType<typeof vi.fn>).mockImplementation((url: string) => {
      if (url.includes('/tasks/tags')) {
        return Promise.resolve(itemRequest === 0 ? ['work'] : ['work', 'new']);
      }
      if (url.includes('/home/items')) {
        itemRequest += 1;
        return Promise.resolve({
          tasks: [task(1, itemRequest === 1 ? 'Before save' : 'After save')],
          notes: []
        });
      }
      return Promise.resolve([]);
    });

    render(<TestApp />);
    expect(await screen.findByText('Before save')).toBeDefined();
    await waitFor(() => expect(getTagCalls()).toHaveLength(1));

    fireEvent.click(screen.getByRole('button', { name: 'Add task' }));
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByText('After save')).toBeDefined();
    await waitFor(() => {
      expect(getItemCalls()).toHaveLength(2);
      expect(getTagCalls()).toHaveLength(2);
    });
  });
});
