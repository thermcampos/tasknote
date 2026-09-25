import React, { act } from 'react';
import { render, fireEvent, waitFor } from '@testing-library/react';
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
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('should render the NoteAdd component without title and url inputs', async () => {
    let result: any;
    await act(async () => {
      result = renderNoteAdd();
    });
    const { getByText, queryByLabelText } = result;
    expect(getByText('note_form_untitled')).toBeDefined();
    expect(getByText('note_form_content_label')).toBeDefined();
    expect(getByText('note_form_submit')).toBeDefined();
    expect(queryByLabelText('note_form_title_label')).toBeNull();
    expect(queryByLabelText('task_form_url_label')).toBeNull();
  });

  it('should show the helper text mentioning title line, url line and tags footer', async () => {
    const { getByText } = renderNoteAdd();
    expect(getByText(/The first line is the note title/)).toBeDefined();
    expect(getByText(/url: <url>/)).toBeDefined();
    expect(getByText(/tags: a, b/)).toBeDefined();
  });

  it('should show error message when form is invalid', async () => {
    const { getByText, getByRole } = renderNoteAdd();
    const submitButton = getByRole('button', { name: 'note_form_submit' });

    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(getByText('Please fill in all the fields')).toBeDefined();
    });
  });

  it('should block save when the first line is blank', async () => {
    const { getByText, getByTestId, getByRole } = renderNoteAdd();
    const noteContentInput = getByTestId('note-content-input-area') as HTMLTextAreaElement;
    const submitButton = getByRole('button', { name: 'note_form_submit' });

    fireEvent.change(noteContentInput, { target: { value: '\nBody without a title' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(getByText('Please fill in all the fields')).toBeDefined();
    });
    expect(api.postJSON).not.toHaveBeenCalled();
  });

  it('should add a new note deriving title from the first line', async () => {
    mockedUseSearchParams.mockReturnValue([
      new URLSearchParams('backTo=home'),
      vi.fn(),
    ]);

    const { getByTestId, getByRole } = renderNoteAdd();
    const noteContentInput = getByTestId('note-content-input-area') as HTMLTextAreaElement;
    const submitButton = getByRole('button', { name: 'note_form_submit' });

    fireEvent.change(noteContentInput, { target: { value: 'New Note\n\nNote content' } });
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
      };
      expect(api.postJSON).toHaveBeenCalledWith(ApiConfig.notesUrl, newNote);
    });
  });

  it('should strip markdown heading markers from the title line', async () => {
    const { getByTestId, getByRole } = renderNoteAdd();
    const noteContentInput = getByTestId('note-content-input-area') as HTMLTextAreaElement;
    const submitButton = getByRole('button', { name: 'note_form_submit' });

    fireEvent.change(noteContentInput, { target: { value: '# My Title\n\nBody' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(api.postJSON).toHaveBeenCalledWith(ApiConfig.notesUrl, expect.objectContaining({
        title: 'My Title',
        description: 'Body'
      }));
    });
  });

  it('should parse the url line case-insensitively and strip it from the description', async () => {
    const { getByTestId, getByRole } = renderNoteAdd();
    const noteContentInput = getByTestId('note-content-input-area') as HTMLTextAreaElement;
    const submitButton = getByRole('button', { name: 'note_form_submit' });

    fireEvent.change(noteContentInput, {
      target: { value: 'Titled\nURL: https://example.com/page, extra\n\nBody\n\ntags: dev' }
    });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(api.postJSON).toHaveBeenCalledWith(ApiConfig.notesUrl, expect.objectContaining({
        title: 'Titled',
        url: 'https://example.com/page',
        description: 'Body',
        tags: ['dev']
      }));
    });
  });

  it('should use the first url line when multiple are present', async () => {
    const { getByTestId, getByRole } = renderNoteAdd();
    const noteContentInput = getByTestId('note-content-input-area') as HTMLTextAreaElement;
    const submitButton = getByRole('button', { name: 'note_form_submit' });

    fireEvent.change(noteContentInput, {
      target: { value: 'Titled\nurl: https://first\nurl: https://second\n\nBody' }
    });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(api.postJSON).toHaveBeenCalledWith(ApiConfig.notesUrl, expect.objectContaining({
        url: 'https://first',
        description: 'url: https://second\n\nBody'
      }));
    });
  });

  it('should update the card title live with the normalized first line', async () => {
    const { getByText, getByTestId } = renderNoteAdd();
    const noteContentInput = getByTestId('note-content-input-area') as HTMLTextAreaElement;

    expect(getByText('note_form_untitled')).toBeDefined();

    fireEvent.change(noteContentInput, { target: { value: '## Live Title\n\nBody' } });

    await waitFor(() => {
      expect(getByText('Live Title')).toBeDefined();
    });
  });

  it('should hide the title, url and tags lines from the markdown preview', async () => {
    const { getByText, getByTestId } = renderNoteAdd();
    const noteContentInput = getByTestId('note-content-input-area') as HTMLTextAreaElement;

    fireEvent.change(noteContentInput, {
      target: { value: 'My Title\nurl: https://example.com\n\nBody text\n\ntags: dev' }
    });
    fireEvent.click(getByText('Preview Markdown'));

    await waitFor(() => {
      expect(getByTestId('modal-header-title').textContent).toBe('My Title');
    });

    fireEvent.click(getByTestId('modal-source-button'));

    await waitFor(() => {
      expect(getByTestId('markdown-source-view').textContent).toBe('Body text');
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

  it('should render a note to edit with title and url synthesized into the body', async () => {
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

    const { getByTestId } = renderNoteAdd();

    await waitFor(() => {
      const noteContentInput = getByTestId('note-content-input-area') as HTMLTextAreaElement;
      expect(noteContentInput.innerHTML).toBe(
        'Note one\nurl: http://notes.domain.com\n\nDescription of note one\n\ntags: dev'
      );
    });
  });

  it('should round-trip title, url, body and tags when saving an edited note', async () => {
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

    const { getByTestId, getByRole } = renderNoteAdd();

    await waitFor(() => {
      const noteContentInput = getByTestId('note-content-input-area') as HTMLTextAreaElement;
      expect(noteContentInput.innerHTML).toContain('Note one');
    });

    fireEvent.click(getByRole('button', { name: 'note_form_submit' }));

    await waitFor(() => {
      expect(api.patchJSON).toHaveBeenCalledWith(`${ApiConfig.notesUrl}/1`, {
        id: 1,
        title: 'Note one',
        description: 'Description of note one',
        url: 'http://notes.domain.com',
        tags: ['dev'],
        lastUpdate: '',
        shared: false,
        shareToken: null,
        archived: false
      });
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

    const { getByTestId } = renderNoteAdd();

    await waitFor(() => {
      const noteContentInput = getByTestId('note-content-input-area') as HTMLTextAreaElement;
      expect(noteContentInput.innerHTML).toBe(
        'Old title\nurl: http://notes.domain.com\n\nOld description\n\ntags: dev'
      );
    });

    window.history.pushState({}, '', '/');
  });

  it('should not render a cloned note when user is trying to mess up', async () => {
    window.history.pushState({}, '', '?cloneFrom=12345');

    const { getByTestId } = renderNoteAdd();

    await waitFor(() => {
      const noteContentInput = getByTestId('note-content-input-area') as HTMLTextAreaElement;
      expect(noteContentInput.innerHTML).toBe('');
    });

    window.history.pushState({}, '', '/');
  });

  it('should parse the tags footer, strip it from the description and send tags on save', async () => {
    const { getByTestId, getByRole } = renderNoteAdd();
    const noteContentInput = getByTestId('note-content-input-area') as HTMLTextAreaElement;
    const submitButton = getByRole('button', { name: 'note_form_submit' });

    fireEvent.change(noteContentInput, {
      target: { value: 'Tagged Note\n\nNote content\n\nTags: Foo, bar , foo,,' }
    });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(api.postJSON).toHaveBeenCalledWith(ApiConfig.notesUrl, {
        id: 0,
        title: 'Tagged Note',
        description: 'Note content',
        url: '',
        tags: ['foo', 'bar'],
        lastUpdate: '',
        shared: false,
        shareToken: null,
        archived: false
      });
    });
  });

  it('should save an untagged note when the footer has an empty value', async () => {
    const { getByTestId, getByRole } = renderNoteAdd();
    const noteContentInput = getByTestId('note-content-input-area') as HTMLTextAreaElement;
    const submitButton = getByRole('button', { name: 'note_form_submit' });

    fireEvent.change(noteContentInput, { target: { value: 'Untagged Note\n\nNote content\n\ntags:' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(api.postJSON).toHaveBeenCalledWith(ApiConfig.notesUrl, {
        id: 0,
        title: 'Untagged Note',
        description: 'Note content',
        url: '',
        tags: [],
        lastUpdate: '',
        shared: false,
        shareToken: null,
        archived: false
      });
    });
  });

  it('should show live tag chips while typing the footer', async () => {
    const { getByTestId, queryByTestId } = renderNoteAdd();
    const noteContentInput = getByTestId('note-content-input-area') as HTMLTextAreaElement;

    expect(queryByTestId('note-tags-preview')).toBeNull();

    fireEvent.change(noteContentInput, { target: { value: 'Content\n\ntags: dev, react' } });

    await waitFor(() => {
      const preview = getByTestId('note-tags-preview');
      expect(preview.textContent).toContain('#dev');
      expect(preview.textContent).toContain('#react');
    });
  });

  it('should keep the server footer over note.tags when editing', async () => {
    mockedUseParams.mockReturnValue({ id: '1' });

    const toEdit: NoteResponse = {
      id: 1,
      title: 'Note one',
      description: 'Description of note one\n\ntags: body-tag',
      url: 'http://notes.domain.com',
      tags: ['server-tag'],
      lastUpdate: '3 minutes ago',
      shared: false,
      shareToken: null
    };

    vi.spyOn(api, 'getJSON').mockResolvedValue(toEdit);

    const { getByTestId } = renderNoteAdd();

    await waitFor(() => {
      const noteContentInput = getByTestId('note-content-input-area') as HTMLTextAreaElement;
      expect(noteContentInput.innerHTML).toBe(
        'Note one\nurl: http://notes.domain.com\n\nDescription of note one\n\ntags: body-tag'
      );
    });

    await waitFor(() => {
      const preview = getByTestId('note-tags-preview');
      expect(preview.textContent).toContain('#body-tag');
      expect(preview.textContent).not.toContain('#server-tag');
    });
  });

  it('should restore a content-only draft', async () => {
    localStorage.setItem('draft:note:new', JSON.stringify({ content: 'Draft Title\n\nDraft body' }));

    const { getByTestId, getByText } = renderNoteAdd();

    await waitFor(() => {
      const noteContentInput = getByTestId('note-content-input-area') as HTMLTextAreaElement;
      expect(noteContentInput.value).toBe('Draft Title\n\nDraft body');
    });
    expect(getByText(/Draft restored/)).toBeDefined();
  });

  it('should migrate an old-shaped draft into the content once', async () => {
    localStorage.setItem(
      'draft:note:new',
      JSON.stringify({ title: 'Old Title', content: 'Old body', noteUrl: 'http://x.com' })
    );

    const { getByTestId } = renderNoteAdd();

    await waitFor(() => {
      const noteContentInput = getByTestId('note-content-input-area') as HTMLTextAreaElement;
      expect(noteContentInput.value).toBe('Old Title\nurl: http://x.com\n\nOld body');
    });

    const stored = JSON.parse(localStorage.getItem('draft:note:new')!);
    expect(stored).toEqual({ content: 'Old Title\nurl: http://x.com\n\nOld body' });
  });
});
