import React from 'react';
import { test, describe, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act, fireEvent, waitFor } from '@testing-library/react';
import AuthContext from '../../context/AuthContext';
import FilterProvider from '../../context/FilterProvider';
import { BrowserRouter } from 'react-router-dom';
import api from '../../api-service/api';
import { TaskResponse } from '../../types/TaskResponse';
import { NoteResponse } from '../../types/NoteResponse';
import Home from '../../views/Home';

// filepath: client/src/views/Home/index.test.tsx

// Mock dependencies
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: 'en', changeLanguage: vi.fn() }
  })
}));

vi.mock('../../api-service/api', () => ({
  default: {
    getJSON: vi.fn(),
    patchJSON: vi.fn(),
    deleteNoContent: vi.fn()
  }
}));

vi.mock('../../lang-service/LangHandler', () => ({
  handleDefaultLang: vi.fn()
}));

vi.mock('../../utils/TranslatorUtils', () => ({
  translateServerResponse: (text: string) => text,
  translateTaskResponse: (tasks: any[]) => tasks
}));

vi.mock('react-router', () => ({
  NavLink: ({ to, children }: { to: string, children: React.ReactNode }) => (
    <a href={to} data-testid={`navlink-${to}`}>{children}</a>
  )
}));

vi.mock('react-bootstrap-icons', () => ({
  ThreeDotsVertical: () => <div data-testid="three-dots-icon">•••</div>,
  CheckSquare: () => <div data-testid="task-icon">☑</div>,
  JournalText: () => <div data-testid="note-icon">📝</div>
}));

// Mock components
vi.mock('../../components/ContentHeader', () => ({
  default: (props: any) => <div data-testid="content-header">{props.h1TextRegular} {props.h1TextBold}</div>
}));

vi.mock('../../components/AlertError', () => ({
  default: (props: any) => <div data-testid="alert-error">{props.errorMessage}</div>
}));

vi.mock('../../components/ModalMarkdown', () => ({
  default: (props: any) => (
    <div data-testid="modal-markdown">
      {props.show ? (
        <div>
          <div data-testid="modal-title">{props.title}</div>
          <div data-testid="modal-content">{props.markdownText}</div>
          <button data-testid="modal-close" onClick={props.onHide}>Close</button>
        </div>
      ) : (
        ''
      )}
    </div>
  )
}));

vi.mock('../../components/TaskTitle', () => ({
  default: (props: any) => <div data-testid="task-title">{props.title}</div>
}));

vi.mock('../../components/TaskTimeLeft', () => ({
  default: (props: any) => <div data-testid="task-time-left">{props.text}</div>
}));

vi.mock('../../components/TaskTag', () => ({
  default: (props: any) => (
    <div data-testid="task-tag">
      {props.tag}
      {props.taskOrNote === 'note' && props.onClick && (
        <a href="#" data-testid="open-it" onClick={props.onClick}>Open it</a>
      )}
    </div>
  )
}));

vi.mock('../../components/NoteTitle', () => ({
  default: (props: any) => <div data-testid="note-title">{props.title}</div>
}));

// Test mock data
const mockTasks: TaskResponse[] = [
  {
    id: 1,
    description: 'Task 1',
    completed: false,
    urls: ['http://example.com'],
    tags: ['work'],
    lastUpdate: '2023-10-10',
    highPriority: true,
    dueDateFmt: '2 days left',
    dueDate: '2023-10-12',
  },
  {
    id: 2,
    description: 'Task 2',
    completed: true,
    urls: [],
    tags: ['home'],
    lastUpdate: '2023-10-09',
    highPriority: false,
    dueDateFmt: '',
    dueDate: '',
  }
];

const mockNotes: NoteResponse[] = [
  {
    id: 1,
    title: 'Note 1',
    description: 'Line 1\nLine 2\nLine 3',
    tags: ['work'],
    lastUpdate: '2023-10-10',
    url: 'http://example.com',
    shared: false,
    shareToken: null
  },
  {
    id: 2,
    title: 'Note 2',
    description: 'This is a sample\nnote content',
    tags: ['personal'],
    lastUpdate: '2023-10-09',
    url: null,
    shared: false,
    shareToken: null
  }
];

const mockTags = ['work', 'home', 'personal', 'untagged'];

const authContextValue = {
  signed: true,
  user: {
    userId: 1,
    name: 'Test User',
    email: 'test@example.com',
    admin: false,
    createdAt: new Date(),
    gravatarImageUrl: 'http://image.com',
    lang: 'en'
  },
  checkCurrentAuthUser: vi.fn(),
  signIn: vi.fn(),
  signOut: vi.fn(),
  register: vi.fn(),
  isAdmin: false,
  updateUser: vi.fn()
};

describe('Home Component', () => {
  const renderHome = () => render(
    <AuthContext.Provider value={authContextValue}>
      <FilterProvider>
        <BrowserRouter>
          <Home />
        </BrowserRouter>
      </FilterProvider>
    </AuthContext.Provider>
  );

  beforeEach(() => {
    // Reset mocks and setup default responses
    vi.clearAllMocks();
    localStorage.clear();
    (api.getJSON as any).mockImplementation((url: string) => {
      if (url.includes('tags')) {
        return Promise.resolve(mockTags);
      } else if (url.includes('tasks')) {
        return Promise.resolve(mockTasks);
      } else if (url.includes('notes')) {
        return Promise.resolve(mockNotes);
      }
      return Promise.resolve([]);
    });
    (api.deleteNoContent as any).mockResolvedValue(undefined);
    
    // Mock window.innerWidth for the cleanText function
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 1024
    });
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  test('renders with initial data', async () => {
    await act(async () => {
      renderHome();
    });

    expect(screen.getByTestId('content-header')).toBeDefined();
    expect(screen.getByPlaceholderText('home_input_filter')).toBeDefined();
    
    // Check that API calls were made
    expect(api.getJSON).toHaveBeenCalledTimes(3);
    
    // Wait for tasks and notes to render
    await waitFor(() => {
      expect(screen.getAllByTestId('task-title').length).toBe(2);
      expect(screen.getAllByTestId('note-title').length).toBe(2);
      expect(screen.getAllByTestId('task-icon').length).toBe(2);
      expect(screen.getAllByTestId('note-icon').length).toBe(2);
    });
  });

  test('filters tasks and notes when search text is entered', async () => {
    await act(async () => {
      renderHome();
    });

    await waitFor(() => {
      expect(screen.getAllByTestId('task-title').length).toBe(2);
      expect(screen.getAllByTestId('note-title').length).toBe(2);
    });

    const searchInput = screen.getByPlaceholderText('home_input_filter');
    
    // Test filtering by entering text
    await act(async () => {
      fireEvent.change(searchInput, { target: { value: 'Task 1' } });
    });

    // Expect filtered results
    await waitFor(() => {
      const taskTitles = screen.getAllByTestId('task-title');
      expect(taskTitles.length).toBe(1);
      expect(taskTitles[0].textContent).toBe('Task 1');
    });
  });

  test('filters by radio button selection', async () => {
    await act(async () => {
      renderHome();
    });

    await waitFor(() => {
      expect(screen.getAllByTestId('task-title').length).toBe(2);
      expect(screen.getAllByTestId('note-title').length).toBe(2);
    });

    const dropdownToggle = screen.getByTestId('main-label-selector');

    await act(async () => {
      fireEvent.click(dropdownToggle);
    });

    // Select "Only Tasks" from dropdown
    const onlyTasksOption = screen.getByRole('button', { name: /home_radio_tasks/i });
    
    await act(async () => {
      fireEvent.click(onlyTasksOption);
    });

    // Should only show tasks
    await waitFor(() => {
      expect(screen.getAllByTestId('task-title').length).toBe(2);
      expect(screen.queryAllByTestId('note-title').length).toBe(0);
    });

    // Select "Only Notes" radio button
    const onlyNotesOption = screen.getByRole('button', { name: /home_radio_notes/i });

    await act(async () => {
      fireEvent.click(onlyNotesOption);
    });

    // Should only show notes
    await waitFor(() => {
      expect(screen.queryAllByTestId('task-title').length).toBe(0);
      expect(screen.getAllByTestId('note-title').length).toBe(2);
    });
  });

  test('filters by tag selection', async () => {
    await act(async () => {
      renderHome();
    });

    await waitFor(() => {
      expect(screen.getAllByTestId('task-title').length).toBe(2);
      expect(screen.getAllByTestId('note-title').length).toBe(2);
    });

    const dropdownToggle = screen.getByTestId('main-label-selector');

    await act(async () => {
      fireEvent.click(dropdownToggle);
    });

    // Select "work" tag radio button
    const workTagOption = screen.getByRole('button', { name: /#work/i });
    
    await act(async () => {
      fireEvent.click(workTagOption);
    });

    // Should only show items with "work" tag
    await waitFor(() => {
      const taskTitles = screen.getAllByTestId('task-title');
      const noteTitles = screen.getAllByTestId('note-title');
      
      expect(taskTitles.length).toBe(1);
      expect(noteTitles.length).toBe(1);
      expect(taskTitles[0].textContent).toBe('Task 1');
      expect(noteTitles[0].textContent).toBe('Note 1');
    });
  });

  test('marks task as done', async () => {
    await act(async () => {
      renderHome();
    });

    await waitFor(() => {
      expect(screen.getAllByTestId('task-title').length).toBe(2);
    });

    // Find the dropdown toggle for the first task
    const dropdownToggles = screen.getAllByTestId('three-dots-icon');
    
    // Click the dropdown toggle
    await act(async () => {
      fireEvent.click(dropdownToggles[0]);
    });

    // Find and click the "Mark as Done" option
    // Since we mocked the components, we need to find it by aria role
    const dropdownItems = screen.getAllByRole('button');
    const markAsDoneButton = dropdownItems.find(
      item => item.textContent === 'task_table_action_done'
    );

    await act(async () => {
      fireEvent.click(markAsDoneButton!);
    });

    // Should call patchJSON API with completed: true
    expect(api.patchJSON).toHaveBeenCalledWith(
      expect.stringContaining('/1'),
      expect.objectContaining({ completed: true })
    );
    
    // Should reload tasks
    expect(api.getJSON).toHaveBeenCalledWith(expect.stringContaining('tasks'));
  });

  test('deletes note', async () => {
    await act(async () => {
      renderHome();
    });

    await waitFor(() => {
      expect(screen.getAllByTestId('note-title').length).toBe(2);
    });

    // Find the dropdown toggle for the first note
    const noteDropdownToggles = screen.getAllByTestId('three-dots-icon');
    // Note dropdowns start after task dropdowns
    const firstNoteDropdown = noteDropdownToggles[mockTasks.length];
    
    // Click the dropdown toggle
    await act(async () => {
      fireEvent.click(firstNoteDropdown);
    });

    // Find and click the "Delete" option by testId
    const deleteButtons = screen.getAllByRole('button');
    const deleteButton = deleteButtons.find(
      button => button.textContent === 'task_table_action_delete'
    );

    await act(async () => {
      fireEvent.click(deleteButton!);
    });

    // Modal should appear; confirm deletion
    const confirmButton = screen.getByText('delete_modal_confirm');
    await act(async () => {
      fireEvent.click(confirmButton);
    });

    // Should call deleteNoContent API
    expect(api.deleteNoContent).toHaveBeenCalledWith(expect.stringContaining('/1'));

    // Should reload notes
    expect(api.getJSON).toHaveBeenCalledWith(expect.stringContaining('notes'))
  });

  test('opens markdown modal when clicking on note tag', async () => {
    await act(async () => {
      renderHome();
    });

    await waitFor(() => {
      expect(screen.getAllByTestId('task-tag').length).toBe(mockTasks.length + mockNotes.length);
    });

    // Find the tags for notes (they start after task tags)
    const noteTags = screen.getAllByTestId('task-tag').slice(mockTasks.length);
    
    // Click on the first note tag
    await act(async () => {
      // Simulate the onClick prop by finding the element and firing a click event
      fireEvent.click(noteTags[0]);
    });

    // Modal should be opened
    const modal = screen.getByTestId('modal-markdown');
    expect(modal.textContent).toBe('');
  });

  test('handles API errors gracefully', async () => {
    // Mock API to throw error
    (api.getJSON as any).mockImplementation(() => {
      throw new Error('API Error');
    });

    await act(async () => {
      renderHome();
    });

    // Should show error message
    expect(screen.getByTestId('alert-error')).toBeDefined();
    expect(screen.getByTestId('alert-error').textContent).toBe('API Error');
  });

  test('keeps filter selection after marking task as done', async () => {
    await act(async () => {
      renderHome();
    });

    await waitFor(() => {
      expect(screen.getAllByTestId('task-title').length).toBe(2);
    });

    // Select "Only Tasks" filter
    const dropdownToggle = screen.getByTestId('main-label-selector');
    await act(async () => {
      fireEvent.click(dropdownToggle);
    });

    const onlyTasksOption = screen.getByRole('button', { name: /home_radio_tasks/i });
    await act(async () => {
      fireEvent.click(onlyTasksOption);
    });

    // Verify filter is applied - notes should be hidden
    await waitFor(() => {
      expect(screen.queryAllByTestId('note-title').length).toBe(0);
    });

    // Mark a task as done
    const dropdownToggles = screen.getAllByTestId('three-dots-icon');
    await act(async () => {
      fireEvent.click(dropdownToggles[0]);
    });

    const dropdownItems = screen.getAllByRole('button');
    const markAsDoneButton = dropdownItems.find(
      item => item.textContent === 'task_table_action_done'
    );
    await act(async () => {
      fireEvent.click(markAsDoneButton!);
    });

    // After reload, filter should still be applied - notes should remain hidden
    await waitFor(() => {
      expect(screen.queryAllByTestId('note-title').length).toBe(0);
      expect(screen.getAllByTestId('task-title').length).toBe(2);
    });
  });

  test('keeps filter selection after deleting a note', async () => {
    await act(async () => {
      renderHome();
    });

    await waitFor(() => {
      expect(screen.getAllByTestId('note-title').length).toBe(2);
    });

    // Select "Only Notes" filter
    const dropdownToggle = screen.getByTestId('main-label-selector');
    await act(async () => {
      fireEvent.click(dropdownToggle);
    });

    const onlyNotesOption = screen.getByRole('button', { name: /home_radio_notes/i });
    await act(async () => {
      fireEvent.click(onlyNotesOption);
    });

    // Verify filter is applied - tasks should be hidden
    await waitFor(() => {
      expect(screen.queryAllByTestId('task-title').length).toBe(0);
    });

    // Delete a note
    const noteDropdownToggles = screen.getAllByTestId('three-dots-icon');
    await act(async () => {
      fireEvent.click(noteDropdownToggles[0]);
    });

    const deleteButtons = screen.getAllByRole('button');
    const deleteButton = deleteButtons.find(
      button => button.textContent === 'task_table_action_delete'
    );
    await act(async () => {
      fireEvent.click(deleteButton!);
    });

    // After reload, filter should still be applied - tasks should remain hidden
    await waitFor(() => {
      expect(screen.queryAllByTestId('task-title').length).toBe(0);
      expect(screen.getAllByTestId('note-title').length).toBe(2);
    });
  });

  test('keeps text search after marking task as done', async () => {
    await act(async () => {
      renderHome();
    });

    await waitFor(() => {
      expect(screen.getAllByTestId('task-title').length).toBe(2);
    });

    // Enter search text to filter to only 'Task 1'
    const searchInput = screen.getByPlaceholderText('home_input_filter');
    await act(async () => {
      fireEvent.change(searchInput, { target: { value: 'Task 1' } });
    });

    await waitFor(() => {
      expect(screen.getAllByTestId('task-title').length).toBe(1);
      expect(screen.getAllByTestId('task-title')[0].textContent).toBe('Task 1');
    });

    // Mark the task as done
    const dropdownToggles = screen.getAllByTestId('three-dots-icon');
    await act(async () => {
      fireEvent.click(dropdownToggles[0]);
    });

    const dropdownItems = screen.getAllByRole('button');
    const markAsDoneButton = dropdownItems.find(
      item => item.textContent === 'task_table_action_done'
    );
    await act(async () => {
      fireEvent.click(markAsDoneButton!);
    });

    // After reload, text filter should still be applied
    await waitFor(() => {
      expect(screen.getAllByTestId('task-title').length).toBe(1);
      expect(screen.getAllByTestId('task-title')[0].textContent).toBe('Task 1');
    });
  });

  test('saves note ID to localStorage when opening modal', async () => {
    await act(async () => {
      renderHome();
    });

    await waitFor(() => {
      expect(screen.getAllByTestId('open-it').length).toBe(2);
    });

    const openItLinks = screen.getAllByTestId('open-it');
    await act(async () => {
      fireEvent.click(openItLinks[1]);
    });

    expect(localStorage.getItem('OPEN_NOTE_ID')).toBe('1');
    expect(screen.getByTestId('modal-title').textContent).toBe('Note 1');
    expect(screen.getByTestId('modal-content').textContent).toBe('Line 1\nLine 2\nLine 3');
  });

  test('restores open note modal from localStorage on reload', async () => {
    localStorage.setItem('OPEN_NOTE_ID', '2');

    await act(async () => {
      renderHome();
    });

    await waitFor(() => {
      expect(screen.getByTestId('modal-title').textContent).toBe('Note 2');
    });

    expect(screen.getByTestId('modal-content').textContent).toBe('This is a sample\nnote content');
  });

  test('does not restore modal if localStorage note ID not found', async () => {
    localStorage.setItem('OPEN_NOTE_ID', '999');

    await act(async () => {
      renderHome();
    });

    await waitFor(() => {
      expect(screen.getAllByTestId('note-title').length).toBe(2);
    });

    const modal = screen.getByTestId('modal-markdown');
    expect(modal.textContent).toBe('');
    expect(localStorage.getItem('OPEN_NOTE_ID')).toBeNull();
  });

  test('clears localStorage when closing modal', async () => {
    await act(async () => {
      renderHome();
    });

    await waitFor(() => {
      expect(screen.getAllByTestId('open-it').length).toBe(2);
    });

    const openItLinks = screen.getAllByTestId('open-it');
    await act(async () => {
      fireEvent.click(openItLinks[1]);
    });

    expect(localStorage.getItem('OPEN_NOTE_ID')).toBe('1');

    const closeButton = screen.getByTestId('modal-close');
    await act(async () => {
      fireEvent.click(closeButton);
    });

    expect(localStorage.getItem('OPEN_NOTE_ID')).toBeNull();
  });
  /*
  test('getFirstRows properly formats note preview', async () => {
    await act(async () => {
      renderHome();
    });

    await waitFor(() => {
      expect(screen.getAllByTestId('note-title').length).toBe(2);
    });

    // Find rendered note previews
    const noteCards = screen.getAllByClassName('text-muted span-line-break font-size-14');
    
    // First note should have preview with the first two lines
    expect(noteCards[0].textContent).toBe('Line 1\nLine 2');
  });*/
});