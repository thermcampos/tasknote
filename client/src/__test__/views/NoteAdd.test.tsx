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

  it('should render the hybrid form with title, body, url and tags inputs and no labels', async () => {
    let result: any;
    await act(async () => {
      result = renderNoteAdd();
    });
    const { getByTestId, getByText, queryByLabelText } = result;
    expect(getByText('note_form_untitled')).toBeDefined();
    expect(getByTestId('note-title-input')).toBeDefined();
    expect(getByTestId('note-content-input-area')).toBeDefined();
    expect(getByTestId('note-url-input')).toBeDefined();
    expect(getByTestId('note-tags-input')).toBeDefined();
    expect(getByText('note_form_submit')).toBeDefined();
    expect(queryByLabelText('note_form_title_label')).toBeNull();
    expect(queryByLabelText('note_form_content_label')).toBeNull();
    expect(queryByLabelText('task_form_url_label')).toBeNull();
  });

  it('should show error message when form is invalid', async () => {
    const { getByText, getByRole } = renderNoteAdd();
    const submitButton = getByRole('button', { name: 'note_form_submit' });

    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(getByText('Please fill in all the fields')).toBeDefined();
    });
  });

  it('should block save when the title is blank even with a body', async () => {
    const { getByText, getByTestId, getByRole } = renderNoteAdd();
    const noteContentInput = getByTestId('note-content-input-area') as HTMLTextAreaElement;
    const submitButton = getByRole('button', { name: 'note_form_submit' });

    fireEvent.change(noteContentInput, { target: { value: 'Body without a title' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(getByText('Please fill in all the fields')).toBeDefined();
    });
    expect(api.postJSON).not.toHaveBeenCalled();
  });

  it('should add a new note from the split fields', async () => {
    mockedUseSearchParams.mockReturnValue([
      new URLSearchParams('backTo=home'),
      vi.fn(),
    ]);

    const { getByTestId, getByRole } = renderNoteAdd();
    const submitButton = getByRole('button', { name: 'note_form_submit' });

    fireEvent.change(getByTestId('note-title-input'), { target: { value: 'New Note' } });
    fireEvent.change(getByTestId('note-content-input-area'), { target: { value: 'Note content' } });
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

  it('should use the title verbatim without stripping markdown markers', async () => {
    const { getByTestId, getByRole } = renderNoteAdd();
    const submitButton = getByRole('button', { name: 'note_form_submit' });

    fireEvent.change(getByTestId('note-title-input'), { target: { value: '# My Title' } });
    fireEvent.change(getByTestId('note-content-input-area'), { target: { value: 'Body' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(api.postJSON).toHaveBeenCalledWith(ApiConfig.notesUrl, expect.objectContaining({
        title: '# My Title',
        description: 'Body'
      }));
    });
  });

  it('should send the url input value with the note', async () => {
    const { getByTestId, getByRole } = renderNoteAdd();
    const submitButton = getByRole('button', { name: 'note_form_submit' });

    fireEvent.change(getByTestId('note-title-input'), { target: { value: 'Titled' } });
    fireEvent.change(getByTestId('note-content-input-area'), { target: { value: 'Body' } });
    fireEvent.change(getByTestId('note-url-input'), { target: { value: 'https://example.com/page' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(api.postJSON).toHaveBeenCalledWith(ApiConfig.notesUrl, expect.objectContaining({
        title: 'Titled',
        url: 'https://example.com/page',
        description: 'Body'
      }));
    });
  });

  it('should update the card title live with the title input', async () => {
    const { getByText, getByTestId } = renderNoteAdd();

    expect(getByText('note_form_untitled')).toBeDefined();

    fireEvent.change(getByTestId('note-title-input'), { target: { value: 'Live Title' } });

    await waitFor(() => {
      expect(getByText('Live Title')).toBeDefined();
    });
  });

  it('should move focus to the body when pressing Enter in the title', async () => {
    const { getByTestId } = renderNoteAdd();
    const titleInput = getByTestId('note-title-input') as HTMLInputElement;
    const bodyInput = getByTestId('note-content-input-area') as HTMLTextAreaElement;

    titleInput.focus();
    fireEvent.keyDown(titleInput, { key: 'Enter' });

    await waitFor(() => {
      expect(document.activeElement).toBe(bodyInput);
    });
  });

  it('should hide the url and tags from the markdown preview', async () => {
    const { getByText, getByTestId } = renderNoteAdd();

    fireEvent.change(getByTestId('note-title-input'), { target: { value: 'My Title' } });
    fireEvent.change(getByTestId('note-content-input-area'), { target: { value: 'Body text' } });
    fireEvent.change(getByTestId('note-url-input'), { target: { value: 'https://example.com' } });
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

  it('should render a note to edit with title, url and tags in their own fields', async () => {
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
      expect((getByTestId('note-title-input') as HTMLInputElement).value).toBe('Note one');
      expect((getByTestId('note-content-input-area') as HTMLTextAreaElement).value).toBe(
        'Description of note one'
      );
      expect((getByTestId('note-url-input') as HTMLInputElement).value).toBe(
        'http://notes.domain.com'
      );
    });

    await waitFor(() => {
      expect(getByTestId('note-tags-preview').textContent).toContain('#dev');
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
      expect((getByTestId('note-title-input') as HTMLInputElement).value).toBe('Note one');
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
      expect((getByTestId('note-title-input') as HTMLInputElement).value).toBe('Old title');
      expect((getByTestId('note-content-input-area') as HTMLTextAreaElement).value).toBe(
        'Old description'
      );
      expect((getByTestId('note-url-input') as HTMLInputElement).value).toBe(
        'http://notes.domain.com'
      );
    });

    window.history.pushState({}, '', '/');
  });

  it('should not render a cloned note when user is trying to mess up', async () => {
    window.history.pushState({}, '', '?cloneFrom=12345');

    const { getByTestId } = renderNoteAdd();

    await waitFor(() => {
      expect((getByTestId('note-title-input') as HTMLInputElement).value).toBe('');
      expect((getByTestId('note-content-input-area') as HTMLTextAreaElement).value).toBe('');
    });

    window.history.pushState({}, '', '/');
  });

  it('should commit a tag chip with Enter and send tags on save', async () => {
    const { getByTestId, getByRole } = renderNoteAdd();
    const submitButton = getByRole('button', { name: 'note_form_submit' });

    fireEvent.change(getByTestId('note-title-input'), { target: { value: 'Tagged Note' } });
    fireEvent.change(getByTestId('note-content-input-area'), { target: { value: 'Note content' } });

    const tagsInput = getByTestId('note-tags-input') as HTMLInputElement;
    fireEvent.change(tagsInput, { target: { value: 'Foo' } });
    fireEvent.keyDown(tagsInput, { key: 'Enter' });
    fireEvent.change(tagsInput, { target: { value: 'bar' } });
    fireEvent.keyDown(tagsInput, { key: 'Enter' });
    fireEvent.change(tagsInput, { target: { value: 'foo' } });
    fireEvent.keyDown(tagsInput, { key: 'Enter' });

    await waitFor(() => {
      const preview = getByTestId('note-tags-preview');
      expect(preview.textContent).toContain('#foo');
      expect(preview.textContent).toContain('#bar');
      expect(preview.textContent).not.toContain('#Foo');
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

  it('should commit a pending tag left in the input on save', async () => {
    const { getByTestId, getByRole } = renderNoteAdd();
    const submitButton = getByRole('button', { name: 'note_form_submit' });

    fireEvent.change(getByTestId('note-title-input'), { target: { value: 'Pending Tag' } });
    fireEvent.change(getByTestId('note-content-input-area'), { target: { value: 'Body' } });
    fireEvent.change(getByTestId('note-tags-input'), { target: { value: 'dev' } });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(api.postJSON).toHaveBeenCalledWith(ApiConfig.notesUrl, expect.objectContaining({
        tags: ['dev']
      }));
    });
  });

  it('should remove a tag chip when clicking it', async () => {
    const { getByTestId, getByText, queryByTestId } = renderNoteAdd();

    const tagsInput = getByTestId('note-tags-input') as HTMLInputElement;
    fireEvent.change(tagsInput, { target: { value: 'dev' } });
    fireEvent.keyDown(tagsInput, { key: 'Enter' });

    await waitFor(() => {
      expect(getByTestId('note-tags-preview').textContent).toContain('#dev');
    });

    fireEvent.click(getByText(/#dev/));

    await waitFor(() => {
      expect(queryByTestId('note-tags-preview')).toBeNull();
    });
  });

  it('should show tag suggestions and accept one on click', async () => {
    vi.spyOn(api, 'getJSON').mockResolvedValue(['dev', 'design']);

    const { getByTestId, getByText } = renderNoteAdd();

    await waitFor(() => {
      expect(api.getJSON).toHaveBeenCalled();
    });

    const tagsInput = getByTestId('note-tags-input') as HTMLInputElement;
    fireEvent.focus(tagsInput);
    fireEvent.change(tagsInput, { target: { value: 'de' } });

    await waitFor(() => {
      expect(getByTestId('tag-suggestion-dropdown')).toBeDefined();
    });

    fireEvent.mouseDown(getByText('#dev'));

    await waitFor(() => {
      expect(getByTestId('note-tags-preview').textContent).toContain('#dev');
      expect(tagsInput.value).toBe('');
    });
  });

  it('should restore a legacy content-only draft into the split fields', async () => {
    localStorage.setItem('draft:note:new', JSON.stringify({ content: 'Draft Title\n\nDraft body' }));

    const { getByTestId, getByText } = renderNoteAdd();

    await waitFor(() => {
      expect((getByTestId('note-title-input') as HTMLInputElement).value).toBe('Draft Title');
      expect((getByTestId('note-content-input-area') as HTMLTextAreaElement).value).toBe('Draft body');
    });
    expect(getByText(/Draft restored/)).toBeDefined();
  });

  it('should migrate an old-shaped draft into the split fields once', async () => {
    localStorage.setItem(
      'draft:note:new',
      JSON.stringify({ title: 'Old Title', content: 'Old body', noteUrl: 'http://x.com' })
    );

    const { getByTestId } = renderNoteAdd();

    await waitFor(() => {
      expect((getByTestId('note-title-input') as HTMLInputElement).value).toBe('Old Title');
      expect((getByTestId('note-content-input-area') as HTMLTextAreaElement).value).toBe('Old body');
      expect((getByTestId('note-url-input') as HTMLInputElement).value).toBe('http://x.com');
    });

    const stored = JSON.parse(localStorage.getItem('draft:note:new')!);
    expect(stored).toEqual({
      title: 'Old Title',
      content: 'Old body',
      noteUrl: 'http://x.com',
      tags: []
    });
  });

  it('should restore a new-shaped draft as-is', async () => {
    localStorage.setItem(
      'draft:note:new',
      JSON.stringify({ title: 'Draft', content: 'Body', noteUrl: '', tags: ['dev'] })
    );

    const { getByTestId } = renderNoteAdd();

    await waitFor(() => {
      expect((getByTestId('note-title-input') as HTMLInputElement).value).toBe('Draft');
      expect((getByTestId('note-content-input-area') as HTMLTextAreaElement).value).toBe('Body');
      expect(getByTestId('note-tags-preview').textContent).toContain('#dev');
    });
  });
});
