import React, { act } from 'react';
import { render, fireEvent, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router';
import { I18nextProvider } from 'react-i18next';
import TaskAdd from '../../views/TaskAdd';
import AuthContext from '../../context/AuthContext';
import i18n from '../../i18n';
import api from '../../api-service/api';
import ApiConfig from '../../api-service/apiConfig';
import TaskNoteRequest from '../../types/TaskNoteRequest';
import SidebarContext from '../../context/SidebarContext';

vi.mock('../../api-service/api');

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
    init:  vi.fn(),
  },
  I18nextProvider: ({ children }: any) => children,
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<any>("react-router-dom");

  return {
    ...actual,
    useSearchParams: vi.fn(),
  };
});

import { useSearchParams } from "react-router-dom";

const authContextMock = {
  signed: true,
  user: {
    userId: 1,
    name: 'Ricardo',
    email: 'test@example.com',
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

describe('TaskAdd Component', () => {
  const renderTaskAdd = () => {
    return render(
      <MemoryRouter>
        <AuthContext.Provider value={authContextMock}>
          <I18nextProvider i18n={i18n}>
            <SidebarContext.Provider value={sidebarContextMock}>
              <TaskAdd />
            </SidebarContext.Provider>
          </I18nextProvider>
        </AuthContext.Provider>
      </MemoryRouter>
    );
  };

  beforeEach(() => {
    // Reset mock between tests
    (useSearchParams as unknown as ReturnType<typeof vi.fn>).mockReset();
  });

  it('should render the TaskAdd component', async () => {
    let result: any;
    await act(async () => {
      result = renderTaskAdd();
    });
    const { getByText } = result;
    expect(getByText('task_form_title')).toBeDefined();
    expect(getByText('task_form_desc_label')).toBeDefined();
    expect(getByText('task_form_url_label')).toBeDefined();
    expect(getByText('task_form_duedate_label')).toBeDefined();
    expect(getByText('task_form_submit')).toBeDefined();
  });

  it('should show error message when form is invalid', async () => {
    let result: any;
    await act(async () => {
      result = renderTaskAdd();
    });
    const { getByText, getByRole } = result;
    const submitButton = getByRole('button', { name: 'task_form_submit' });
    fireEvent.click(submitButton);
    await waitFor(() => {
      expect(getByText('Please fill in all the fields')).toBeDefined();
    });
  });

  it('should add a new task when form is valid', async () => {
    (useSearchParams as unknown as ReturnType<typeof vi.fn>).mockReturnValue([
      new URLSearchParams("backTo=home"),
    ]);

    let result: any;
    await act(async () => {
      result = renderTaskAdd();
    });
    const { getByLabelText, getByRole } = result;
    const descriptionInput = getByLabelText('task_form_desc_label') as HTMLInputElement;
    const submitButton = getByRole('button', { name: 'task_form_submit' });

    fireEvent.change(descriptionInput, { target: { value: 'New Task' } });
    expect(descriptionInput.value).toBe('New Task');
    fireEvent.click(submitButton);

    await waitFor(() => {
      const newTask: TaskNoteRequest = {
        description: 'New Task',
        dueDate: '',
        highPriority: false,
        tags: [],
        urls: []
      }
      expect(api.postJSON).toHaveBeenCalledWith(ApiConfig.tasksUrl, newTask);
    });
  });

  it('should render text based on new contentHeader component', async () => {
    (useSearchParams as unknown as ReturnType<typeof vi.fn>).mockReturnValue([
      new URLSearchParams("backTo=home"),
    ]);

    let result: any;
    await act(async () => {
      result = renderTaskAdd();
    });
    const { getByText } = result;

    expect(getByText('Add')).toBeDefined();
    expect(getByText('Task')).toBeDefined();
    expect(getByText('Be on top of your TODO list')).toBeDefined();
    expect(getByText('Create, Filter, and Easily Find')).toBeDefined();
    expect(getByText('Them')).toBeDefined();
  });
});
