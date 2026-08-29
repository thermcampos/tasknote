import React from 'react';
import { render, fireEvent, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { I18nextProvider } from 'react-i18next';
import { MemoryRouter } from 'react-router';
import Account from '../../views/Account';
import AuthContext from '../../context/AuthContext';
import i18n from '../../i18n';
import api from '../../api-service/api';
import ApiConfig from '../../api-service/apiConfig';

vi.mock('../../api-service/api');

const changeLanguageMock = vi.fn();

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    i18n: {
      changeLanguage: changeLanguageMock,
      language: 'en'
    },
    t: (key: string) => key,
  }),
  initReactI18next: {
    type: '3rdParty',
    init: vi.fn(),
  },
  I18nextProvider: ({ children }: any) => children,
}));

const authContextMock = {
  signed: true,
  user: {
    userId: 1,
    name: 'Ricardo',
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

describe('Account Component', () => {
  const renderAccount = () => {
    return render(
      <MemoryRouter>
        <AuthContext.Provider value={authContextMock}>
          <I18nextProvider i18n={i18n}>
            <Account />
          </I18nextProvider>
        </AuthContext.Provider>
      </MemoryRouter>
    );
  };

  it('should render the Account component', () => {
    const { getByText } = renderAccount();
    expect(getByText('account_data_update_header')).toBeDefined();
  });

  it('should change language when a language button is clicked', () => {
    const { getByTestId } = renderAccount();
    const languageButton = getByTestId('language-button-pt_br');
    fireEvent.click(languageButton);
    expect(changeLanguageMock).toHaveBeenCalled();
  });

  it('should show alert when delete button is clicked', () => {
    const { getByText } = renderAccount();
    const deleteButton = getByText('account_privacy_delete_btn');
    fireEvent.click(deleteButton);
    expect(getByText('account_delete_title')).toBeDefined();
  })

  it('should call deleteAccount API and signOut when delete is confirmed', async () => {
    const { getByText, getByTestId } = renderAccount();
    const deleteButton = getByText('account_privacy_delete_btn');
    fireEvent.click(deleteButton);

    fireEvent.change(getByTestId('delete-account-password'), { target: { value: 'my-password' } });
    const confirmButton = getByText('account_delete_btn');
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(api.postJSON).toHaveBeenCalledWith(ApiConfig.deleteAccountUrl, { password: 'my-password' });
      expect(authContextMock.signOut).toHaveBeenCalled();
    });
  });

  it('should show inline error and keep dialog open when delete fails', async () => {
    const mockPostJSON = vi.spyOn(api, 'postJSON').mockRejectedValue(new Error('Invalid credentials'));

    const { getByText, getByTestId, queryByTestId } = renderAccount();
    fireEvent.click(getByText('account_privacy_delete_btn'));

    const passwordInput = getByTestId('delete-account-password') as HTMLInputElement;
    fireEvent.change(passwordInput, { target: { value: 'wrong-password' } });
    fireEvent.click(getByText('account_delete_btn'));

    await waitFor(() => {
      expect(getByTestId('delete-account-error')).toBeDefined();
    });

    expect(queryByTestId('delete-account-password')).toBeDefined();
    expect(passwordInput.value).toBe('');
    expect(authContextMock.signOut).not.toHaveBeenCalled();

    mockPostJSON.mockRestore();
  });

  it('should clear password and error when the delete dialog is closed', async () => {
    const mockPostJSON = vi.spyOn(api, 'postJSON').mockRejectedValue(new Error('Invalid credentials'));

    const { getByText, getByTestId, queryByTestId, queryByText } = renderAccount();
    fireEvent.click(getByText('account_privacy_delete_btn'));

    fireEvent.change(getByTestId('delete-account-password'), { target: { value: 'wrong-password' } });
    fireEvent.click(getByText('account_delete_btn'));

    await waitFor(() => {
      expect(getByTestId('delete-account-error')).toBeDefined();
    });

    const closeButton = document.querySelector('.alert .btn-close') as HTMLElement;
    fireEvent.click(closeButton);

    expect(queryByTestId('delete-account-password')).toBeNull();

    fireEvent.click(getByText('account_privacy_delete_btn'));

    const reopenedInput = getByTestId('delete-account-password') as HTMLInputElement;
    expect(reopenedInput.value).toBe('');
    expect(queryByTestId('delete-account-error')).toBeNull();
    expect(queryByText('account_delete_title')).toBeDefined();

    mockPostJSON.mockRestore();
  });

  it('should disable the confirm button when the password field is empty', () => {
    const { getByText, getByTestId } = renderAccount();
    fireEvent.click(getByText('account_privacy_delete_btn'));

    const confirmButton = getByText('account_delete_btn') as HTMLButtonElement;
    expect(confirmButton.disabled).toBe(true);

    fireEvent.change(getByTestId('delete-account-password'), { target: { value: 'x' } });
    expect(confirmButton.disabled).toBe(false);
  });

  it('should submit the form with correct patchPayload', async () => {
    const mockPatchJSON = vi.spyOn(api, 'patchJSON').mockResolvedValue(authContextMock.user);

    const { getByLabelText, getByText, getByTestId } = renderAccount();

    fireEvent.change(getByLabelText('account_form_first_name_label'), { target: { value: 'Jane' } });
    fireEvent.change(getByLabelText('login_email_label'), { target: { value: 'jane.doe@example.com' } });
    fireEvent.change(getByTestId('account-password-one'), { target: { value: 'password123' } });
    fireEvent.change(getByLabelText(/register_password_repeat_label/i), { target: { value: 'password123' } });

    fireEvent.click(getByText('account_form_save'));

    await waitFor(() => {
      expect(mockPatchJSON).toHaveBeenCalledWith(expect.any(String), {
        name: 'Jane',
        email: 'jane.doe@example.com',
        password: 'password123',
        passwordAgain: 'password123',
        lang: ''
      });
    });

    mockPatchJSON.mockRestore();
  });

  it('should render text based on new contentHeader component', () => {
    const { getByText } = renderAccount();

    expect(getByText('account_header_my')).toBeDefined();
    expect(getByText('account_header_account')).toBeDefined();
    expect(getByText('account_my_account_hello')).toBeDefined();
    expect(getByText('account_header_update_manage')).toBeDefined();
    expect(getByText('account_header_data')).toBeDefined();
  });
});
