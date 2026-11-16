import React, { act } from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { BrowserRouter } from 'react-router';
import { fireEvent, render, screen } from '@testing-library/react';
import LoginForm from '../../components/LoginForm';
import '../../i18n';
import AuthContext from '../../context/AuthContext';

const authContextMock = {
  signed: true,
  user: {
    userId: 1,
    name: 'Ricardo',
    email: 'test@example.com',
    admin: false,
    createdAt: new Date(),
    gravatarImageUrl: 'http://image.com'
  },
  checkCurrentAuthUser: vi.fn(),
  signIn: vi.fn(),
  signOut: vi.fn(),
  register: vi.fn(),
  isAdmin: false,
  updateUser: vi.fn()
};

describe('LoginForm Component test', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  const renderFn = (prefix: string) => {
    return render(
      <BrowserRouter>
        <AuthContext.Provider value={authContextMock}>
          <LoginForm prefix={prefix} />
        </AuthContext.Provider>
      </BrowserRouter>
    );
  };

  it('should render the Login Form component correctly', () => {
    renderFn("login");

    const loginEmailInput: HTMLInputElement = screen.getByTestId('login_email_input');
    expect(loginEmailInput).toBeDefined();
    expect(loginEmailInput.required).toBe(true);
  });

  it('should render the Register Form component correctly', () => {
    renderFn("register");

    const loginEmailInput: HTMLInputElement = screen.getByTestId('register_email_input');
    expect(loginEmailInput).toBeDefined();
    expect(loginEmailInput.required).toBe(true);
  });

  it('should render the Login Form correctly for "login" prefix', () => {
    renderFn("login");

    const emailInput = screen.getByTestId('login_email_input');
    const passwordInput = screen.getByTestId('account-password-login');
    const submitButton = screen.getByRole('button', { name: /login/i });

    expect(emailInput).toBeDefined();
    expect(passwordInput).toBeDefined();
    expect(submitButton).toBeDefined();
  });

  it('should render the Register Form correctly for "register" prefix', () => {
    renderFn("register");

    const emailInput = screen.getByTestId('register_email_input');
    const passwordInput = screen.getByTestId('account-password-login');
    const repeatPasswordInput = screen.getByTestId('account-repeat-password-register');
    const submitButton = screen.getByRole('button', { name: /create account/i });

    expect(emailInput).toBeDefined();
    expect(passwordInput).toBeDefined();
    expect(repeatPasswordInput).toBeDefined();
    expect(submitButton).toBeDefined();
  });

  it('should render the Reset Password Form correctly for "reset" prefix', () => {
    renderFn("reset");

    const emailInput = screen.getByTestId('reset_email_input');
    const submitButton = screen.getByRole('button', { name: /Send confirmation email/i });

    expect(emailInput).toBeDefined();
    expect(submitButton).toBeDefined();
  });

  it('should display error message when form is invalid', () => {
    renderFn("login");

    const submitButton = screen.getByRole('button', { name: /login/i });
    fireEvent.click(submitButton);

    const errorMessage = screen.getByText(/please fill in your username and password/i);
    expect(errorMessage).toBeDefined();
  });

  it('should display success message for "register" prefix after successful submission', async () => {
    renderFn("register");

    const emailInput = screen.getByTestId('register_email_input');
    const passwordInput = screen.getByTestId('account-password-login');
    const repeatPasswordInput = screen.getByTestId('account-repeat-password-register');
    const submitButton = screen.getByRole('button', { name: /create account/i });

    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'password123' } });
    fireEvent.change(repeatPasswordInput, { target: { value: 'password123' } });
    fireEvent.click(submitButton);

    const successMessage = await screen.findByText(/please confirm your email before proceeding/i);
    expect(successMessage).toBeDefined();
  });

  it('should disable resend button and show countdown for "register" prefix', async () => {
    vi.useFakeTimers();
    renderFn("register");

    const emailInput = screen.getByTestId('register_email_input');
    const passwordInput = screen.getByTestId('account-password-login');
    const repeatPasswordInput = screen.getByTestId('account-repeat-password-register');
    const submitButton = screen.getByRole('button', { name: /create account/i });

    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'password123' } });
    fireEvent.change(repeatPasswordInput, { target: { value: 'password123' } });
    
    await act(async () => {
      fireEvent.click(submitButton);
    });

    const resendButton = screen.queryByRole('button', { name: /resend confirmation email/i }) as HTMLButtonElement;
    expect(resendButton).toBeNull();
    
    vi.useRealTimers();
  });

  it('should display resend button when login fails with "Email not confirmed!"', async () => {
    authContextMock.signIn.mockRejectedValue(new Error('Email not confirmed!'));
    renderFn("login");

    const emailInput = screen.getByTestId('login_email_input');
    const passwordInput = screen.getByTestId('account-password-login');
    const submitButton = screen.getByRole('button', { name: /login/i });

    fireEvent.change(emailInput, { target: { value: 'test@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'password123' } });
    
    await act(async () => {
      fireEvent.click(submitButton);
    });

    const errorMessage = await screen.findByText(/email not confirmed/i);
    expect(errorMessage).toBeDefined();

    const resendButton = screen.getByRole('button', { name: /resend confirmation email/i });
    expect(resendButton).toBeDefined();
  });
});
