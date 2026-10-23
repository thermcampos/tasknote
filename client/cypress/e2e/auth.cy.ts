/**
 * E2E tests for authentication flows: User Signup and Login.
 *
 * These tests cover the primary user journeys for account creation
 * and logging in to the TaskNote application.
 */
describe('Authentication', () => {
  /**
   * User Signup (Registration) flow
   */
  describe('Signup', () => {
    beforeEach(() => {
      cy.visit('/register');
    });

    it('displays the registration form', () => {
      cy.contains('h2', 'Create account').should('be.visible');
      cy.get('input[name="email"]').should('be.visible');
      cy.get('input[name="password"]').should('be.visible');
      cy.get('input[name="passwordAgain"]').should('be.visible');
      cy.get('button[type="submit"]').should('be.visible').and('contain', 'Create account');
    });

    it('shows a validation error when the form is submitted empty', () => {
      cy.get('button[type="submit"]').click();
      cy.get('.alert-danger').should('be.visible');
    });

    it('shows a validation error when passwords do not match', () => {
      cy.get('input[name="email"]').type('testuser@example.com');
      cy.get('input[name="password"]').type('Password1!');
      cy.get('input[name="passwordAgain"]').type('DifferentPass1!');
      cy.get('button[type="submit"]').click();
      cy.get('.alert-danger').should('be.visible');
    });

    it('navigates to the login page via the login link', () => {
      cy.contains('a', 'Login').first().click();
      cy.url().should('include', '/login');
    });

    it('navigates back to the landing page via the back-to-home link', () => {
      cy.contains('a', 'Back to home').click();
      cy.url().should('eq', `${Cypress.config('baseUrl')}/`);
    });

    it('submits the signup form and shows a confirmation message on success', () => {
      cy.intercept({ method: 'PUT', url: /\/auth\/sign-up/ }, {
        statusCode: 201,
        body: { message: 'User created' }
      }).as('signUp');

      cy.get('input[name="email"]').type(`newuser${Date.now()}@example.com`);
      cy.get('input[name="password"]').type('Password1!');
      cy.get('input[name="passwordAgain"]').type('Password1!');
      cy.get('button[type="submit"]').click();

      cy.wait('@signUp');
      cy.get('.alert-success').should('be.visible');
    });

    it('displays an error alert when the API returns an error on signup', () => {
      cy.intercept({ method: 'PUT', url: /\/auth\/sign-up/ }, {
        statusCode: 409,
        body: { message: 'Email already in use' }
      }).as('signUpFail');

      cy.get('input[name="email"]').type(`existing${Date.now()}@example.com`);
      cy.get('input[name="password"]').type('Password1!');
      cy.get('input[name="passwordAgain"]').type('Password1!');
      cy.get('button[type="submit"]').click();

      cy.wait('@signUpFail');
      cy.get('.alert-danger').should('be.visible');
    });
  });

  /**
   * User Login flow
   */
  describe('Login', () => {
    beforeEach(() => {
      cy.visit('/login');
    });

    it('displays the login form', () => {
      cy.contains('h2', 'Login').should('be.visible');
      cy.get('input[name="email"]').should('be.visible');
      cy.get('input[name="password"]').should('be.visible');
      cy.get('button[type="submit"]').should('be.visible').and('contain', 'Login');
    });

    it('shows a validation error when the form is submitted empty', () => {
      cy.get('button[type="submit"]').click();
      cy.get('.alert-danger').should('be.visible');
    });

    it('navigates to the register page via the create account link', () => {
      cy.contains('a', 'Create account').click();
      cy.url().should('include', '/register');
    });

    it('navigates back to the landing page via the back-to-home link', () => {
      cy.contains('a', 'Back to home').click();
      cy.url().should('eq', `${Cypress.config('baseUrl')}/`);
    });

    it('navigates to the reset password page via the forgot password link', () => {
      cy.contains('a', 'Forgot your password?').click();
      cy.url().should('include', '/reset-password');
    });

    it('redirects to home after a successful login', () => {
      cy.intercept({ method: 'POST', url: /\/auth\/sign-in/ }, {
        statusCode: 200,
        body: { token: 'fake-jwt-token', email: 'user@example.com' }
      }).as('signIn');

      cy.intercept('GET', /\/rest\/users\/me/, {
        statusCode: 200,
        body: {
          userId: 1,
          name: 'Test User',
          email: 'user@example.com',
          admin: false,
          createdAt: '2026-01-01T00:00:00.000Z',
          gravatarImageUrl: '',
          lang: 'en'
        }
      }).as('getCurrentUser');

      cy.intercept('GET', /\/rest\/home\/tasks\/tags/, {
        statusCode: 200,
        body: []
      }).as('getTags');

      cy.intercept('GET', /\/rest\/home\/items/, {
        statusCode: 200,
        body: { tasks: [], notes: [] }
      }).as('getItems');

      cy.get('input[name="email"]').type('user@example.com');
      cy.get('input[name="password"]').type('Password1!');
      cy.get('button[type="submit"]').click();

      cy.wait('@signIn');
      cy.url().should('include', '/home');
    });

    it('displays an error alert when the API returns an error on login', () => {
      cy.intercept({ method: 'POST', url: /\/auth\/sign-in/ }, {
        statusCode: 401,
        body: { message: 'Invalid credentials' }
      }).as('signInFail');

      cy.get('input[name="email"]').type('user@example.com');
      cy.get('input[name="password"]').type('wrongpassword');
      cy.get('button[type="submit"]').click();

      cy.wait('@signInFail');
      cy.get('.alert-danger').should('be.visible');
    });
  });
});
