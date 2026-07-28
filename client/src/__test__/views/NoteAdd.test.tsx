import React, { act } from 'react';
import { render, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router';
import { I18nextProvider } from 'react-i18next';
import NoteAdd from '../../views/NoteAdd';
import AuthContext from '../../context/AuthContext';
import i18n from '../../i18n';
import api from '../../api-service/api';
import ApiConfig from '../../api-service/apiConfig';
import { NoteResponse } from '../../types/NoteResponse';
import SidebarContext from '../../context/SidebarContext';

// Mock the entire api module
vi.mock('../../api-service/api', () => ({
  default: {
    postJSON: vi.fn(),
    getJSON: vi.fn(),
    patchJSON: vi.fn(),
  }
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    i18n: {
      changeLanguage: vi.fn(),
      language: 'en',
    },
    t: (key: string) => key,
  }),
  initReactI18next: {
    type: '3rdParty',
    init: vi.fn(),
  },
  I18nextProvider: ({ children }: any) => children,
}));

vi.mock('react-router', async () => {
  const actual = await vi.importActual<any>('react-router');

  return {
    ...actual,
    useSearchParams: vi.fn(),
    useParams: vi.fn()
  };
});

import { useSearchParams, useParams } from 'react-router';

const authContextMock = {
  signed: true,
  user: {
    userId: 1,
    name: 'Ricardo',
    email: 'test@example.com',
    admin: false,
    createdAt: new Date(),
    gravatarImageUrl: 'http://url.com',
    lang: 'en'
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
  setNewPage: vi.fn()
};

// Mock the lang handler
vi.mock('../../lang-service/LangHandler', () => ({
  handleDefaultLang: vi.fn()
}));

// Mock the translator utils
vi.mock('../../utils/TranslatorUtils', () => ({
  translateServerResponse: (message: string) => message
}));

const mockedApi = vi.mocked(api);
const mockedUseParams = vi.mocked(useParams);
const mockedUseSearchParams = vi.mocked(useSearchParams);

describe('NoteAdd Component', () => {
  const renderNoteAdd = () => {
    return render(
      <MemoryRouter>
        <AuthContext.Provider value={authContextMock}>
          <I18nextProvider i18n={i18n}>
            <SidebarContext.Provider value={sidebarContextMock}>
              <NoteAdd />
            </SidebarContext.Provider>
          </I18nextProvider>
        </AuthContext.Provider>
      </MemoryRouter>
    );
  };

  beforeEach(() => {
    // Reset mock between tests
    mockedUseSearchParams.mockReturnValue([new URLSearchParams(), vi.fn()]);
    mockedUseParams.mockReturnValue({});
    vi.clearAllMocks();
  });

  it('should render the NoteAdd component', async () => {
    let result: any;
    await act(async () => {
      result = renderNoteAdd();
    });
    const { getByText } = result;
    expect(getByText('note_form_title_label')).toBeDefined();
    expect(getByText('task_form_url_label')).toBeDefined();
    expect(getByText('note_form_content_label')).toBeDefined();
    expect(getByText('note_form_submit')).toBeDefined();
  });

  it('should show error message when form is invalid', async () => {
    const { getByText, getByRole } = renderNoteAdd();
    const submitButton = getByRole('button', { name: 'note_form_submit' });
    
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(getByText('Please fill in all the fields')).toBeDefined();
    });
  });

  it('should add a new note when form is valid', async () => {
    mockedUseSearchParams.mockReturnValue([
      new URLSearchParams("backTo=home"),
      vi.fn(),
    ]);

    const { getByLabelText, getByTestId, getByRole } = renderNoteAdd();
    const descriptionInput = getByLabelText('note_form_title_label') as HTMLInputElement;
    const noteContentInput = getByTestId('note-content-input-area') as HTMLAreaElement;
    const submitButton = getByRole('button', { name: 'note_form_submit' });

    fireEvent.change(descriptionInput, { target: { value: 'New Note' } });
    fireEvent.change(noteContentInput, { target: { value: 'Note content' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      const newNote: NoteResponse = {
        id: 0,
        title: 'New Note',
        description: 'Note content',
        url: '',
        tags: [],
        lastUpdate: '',
        shared: false,
        shareToken: null,
        archived: false
      }
      expect(api.postJSON).toHaveBeenCalledWith(ApiConfig.notesUrl, newNote);
    });
  });

  it('should render text based on new contentHeader component', async () => {
    let result: any;
    await act(async () => {
      result = renderNoteAdd();
    });
    const { getByText } = result;

    expect(getByText('Add')).toBeDefined();
    expect(getByText('Note')).toBeDefined();
    expect(getByText('Save your notes in plain text or Markdown format')).toBeDefined();
    expect(getByText('Create, Filter, and Easily Find')).toBeDefined();
    expect(getByText('Them')).toBeDefined();
  });

  it('should render a note to edit', async () => {
    mockedUseParams.mockReturnValue({ id: '1' });

    const toEdit: NoteResponse = {
      id: 1,
      title: 'Note one',
      description: 'Description of note one',
      url: 'http://notes.domain.com',
      tags: ['dev'],
      lastUpdate: '3 minutes ago',
      shared: false,
      shareToken: null
    };
  
    vi.spyOn(api, 'getJSON').mockResolvedValue(toEdit);

    const { getByLabelText, getByTestId } = renderNoteAdd();

    await waitFor(() => {
      const noteTitle = getByLabelText('note_form_title_label') as HTMLInputElement;
      const noteUrl = getByLabelText('task_form_url_label') as HTMLInputElement;
      const noteContentInput = getByTestId('note-content-input-area') as HTMLAreaElement;
      expect(noteTitle.value).toBe(toEdit.title);
      expect(noteUrl.value).toBe(toEdit.url);
      expect(noteContentInput.innerHTML).toBe(toEdit.description);
    });
  });

  it('should render a cloned note', async () => {
    window.history.pushState({}, '', '?cloneFrom=123');

    const toClone: NoteResponse = {
      id: 11,
      title: 'Old title',
      description: 'Old description',
      url: 'http://notes.domain.com',
      tags: ['dev'],
      lastUpdate: '1 minute ago',
      shared: false,
      shareToken: null
    };
  
    vi.spyOn(api, 'getJSON').mockResolvedValue(toClone);

    const { getByLabelText, getByTestId } = renderNoteAdd();

    await waitFor(() => {
      const noteTitle = getByLabelText('note_form_title_label') as HTMLInputElement;
      const noteUrl = getByLabelText('task_form_url_label') as HTMLInputElement;
      const noteContentInput = getByTestId('note-content-input-area') as HTMLAreaElement;
      expect(noteTitle.value).toBe(toClone.title);
      expect(noteUrl.value).toBe(toClone.url);
      expect(noteContentInput.innerHTML).toBe(toClone.description);
    });
  });

  it('should not render a cloned note when user is trying to mess up', async () => {
    window.history.pushState({}, '', '?cloneFrom=12345');

    const { getByLabelText, getByTestId } = renderNoteAdd();

    await waitFor(() => {
      const noteTitle = getByLabelText('note_form_title_label') as HTMLInputElement;
      const noteUrl = getByLabelText('task_form_url_label') as HTMLInputElement;
      const noteContentInput = getByTestId('note-content-input-area') as HTMLAreaElement;
      expect(noteTitle.value).toBe('');
      expect(noteUrl.value).toBe('');
      expect(noteContentInput.innerHTML).toBe('');
    });
  });
});
