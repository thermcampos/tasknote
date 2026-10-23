/**
 * E2E tests for Notes Management: Create, Update, Delete, and Share notes.
 */

const mockUser = {
  userId: 1,
  name: 'Test User',
  email: 'test@example.com',
  admin: false,
  createdAt: '2026-01-01T00:00:00.000Z',
  gravatarImageUrl: '',
  lang: 'en'
};

const mockNote = {
  id: 1,
  title: 'Meeting notes',
  description: 'Discuss quarterly goals',
  url: null,
  tags: ['work'],
  lastUpdate: '2026-05-01',
  shared: false,
  shareToken: null,
  archived: false
};

describe('Notes Management', () => {
  /**
   * Sets up authentication intercepts and visits the given path.
   *
   * @param {string} path - The path to visit.
   */
  const visitWithAuth = (path: string): void => {
    cy.intercept('GET', /\/rest\/user-sessions\/refresh/, {
      statusCode: 200,
      body: { token: 'fake-jwt-token', ...mockUser }
    }).as('refreshToken');

    cy.intercept('GET', /\/rest\/users\/me/, {
      statusCode: 200,
      body: mockUser
    }).as('getCurrentUser');

    cy.intercept('GET', /\/rest\/home\/tasks\/tags/, {
      statusCode: 200,
      body: ['work', 'personal']
    }).as('getTags');

    cy.visit(path, {
      onBeforeLoad(win) {
        win.localStorage.setItem('TASKNOTE-TOKEN', 'fake-jwt-token');
        win.localStorage.setItem('TASKNOTE-USER', JSON.stringify(mockUser));
      }
    });
  };

  /**
   * Create note
   */
  describe('Create', () => {
    beforeEach(() => {
      visitWithAuth('/notes/new');
    });

    it('displays the add note form', () => {
      cy.contains('Add Note').should('be.visible');
      cy.get('input[name="note_title"]').should('be.visible');
      cy.get('[data-testid="note-markdown-editor"] .ProseMirror').should('be.visible');
      cy.contains('button', 'Save note').should('be.visible');
    });

    it('shows a validation error when submitted without a title', () => {
      cy.contains('button', 'Save note').click();

      cy.get('.alert-danger').should('be.visible');
    });

    it('creates a note and navigates to home on success', () => {
      cy.intercept('POST', /\/rest\/notes/, {
        statusCode: 201,
        body: {
          id: 10,
          title: 'New note',
          description: 'Some content',
          url: null,
          tags: [],
          lastUpdate: '',
          shared: false,
          shareToken: null
        }
      }).as('createNote');

      cy.intercept('GET', /\/rest\/home\/items/, {
        statusCode: 200,
        body: { tasks: [], notes: [] }
      }).as('getItems');

      cy.get('input[name="note_title"]').type('New note');
      cy.get('[data-testid="note-markdown-editor"] .ProseMirror').type('Some content');
      cy.contains('button', 'Save note').click();

      cy.wait('@createNote');
      cy.url().should('include', '/home');
    });

    it('renders markdown live while typing', () => {
      cy.intercept('GET', /\/rest\/home\/items/, {
        statusCode: 200,
        body: { tasks: [], notes: [] }
      }).as('getItems');

      cy.get('input[name="note_title"]').type('Live markdown');
      cy.get('[data-testid="note-markdown-editor"] .ProseMirror')
        .type('# Quarterly goals{enter}Some **bold** text{enter}- first item');

      cy.get('[data-testid="note-markdown-editor"] h1').should('contain.text', 'Quarterly goals');
      cy.get('[data-testid="note-markdown-editor"] strong').should('contain.text', 'bold');
      cy.get('[data-testid="note-markdown-editor"] li').should('contain.text', 'first item');
    });

    it('shows an error alert when the API returns an error on create', () => {
      cy.intercept('POST', /\/rest\/notes/, {
        statusCode: 500,
        body: { message: 'Internal Server Error' }
      }).as('createNoteFail');

      cy.get('input[name="note_title"]').type('Failing note');
      cy.get('[data-testid="note-markdown-editor"] .ProseMirror').type('Some content');
      cy.contains('button', 'Save note').click();

      cy.wait('@createNoteFail');
      cy.get('.alert-danger').should('be.visible');
    });

    it('navigates back to home when Cancel is clicked', () => {
      cy.intercept('GET', /\/rest\/home\/items/, {
        statusCode: 200,
        body: { tasks: [], notes: [] }
      }).as('getItems');

      cy.contains('button', 'Cancel').click();

      cy.url().should('include', '/home');
    });
  });

  /**
   * Update note
   */
  describe('Update', () => {
    beforeEach(() => {
      cy.intercept('GET', /\/rest\/notes\/\d+/, {
        statusCode: 200,
        body: mockNote
      }).as('getNote');

      visitWithAuth('/notes/edit/1');
      cy.wait('@getNote');
    });

    it('pre-fills the form with the existing note data', () => {
      cy.get('input[name="note_title"]').should('have.value', 'Meeting notes');
      cy.get('[data-testid="note-markdown-editor"] .ProseMirror')
        .should('contain.text', 'Discuss quarterly goals');
    });

    it('updates the note and navigates to home on success', () => {
      cy.intercept('PATCH', /\/rest\/notes\/\d+/, {
        statusCode: 200,
        body: { ...mockNote, title: 'Updated meeting notes' }
      }).as('updateNote');

      cy.intercept('GET', /\/rest\/home\/items/, {
        statusCode: 200,
        body: { tasks: [], notes: [] }
      }).as('getItems');

      cy.get('input[name="note_title"]').clear().type('Updated meeting notes');
      cy.contains('button', 'Save note').click();

      cy.wait('@updateNote');
      cy.url().should('include', '/home');
    });

    it('shows an error alert when the update API call fails', () => {
      cy.intercept('PATCH', /\/rest\/notes\/\d+/, {
        statusCode: 500,
        body: { message: 'Internal Server Error' }
      }).as('updateNoteFail');

      cy.get('input[name="note_title"]').clear().type('Updated note');
      cy.contains('button', 'Save note').click();

      cy.wait('@updateNoteFail');
      cy.get('.alert-danger').should('be.visible');
    });
  });

  /**
   * Delete note
   */
  describe('Archive and Delete', () => {
    beforeEach(() => {
      cy.intercept('GET', /\/rest\/home\/items/, {
        statusCode: 200,
        body: { tasks: [], notes: [mockNote] }
      }).as('getItems');

      visitWithAuth('/home');
      cy.wait('@getItems');
    });

    it('archives a note via the note dropdown menu', () => {
      cy.intercept('PUT', /\/rest\/notes\/\d+\/archive/, {
        statusCode: 200,
        body: { ...mockNote, archived: true }
      }).as('archiveNote');

      cy.get('[data-testid="note-dropdown-menu-1"]').click();
      cy.get('[data-testid="note-dropdown-archive-item-1"]').click();

      cy.wait('@archiveNote');
    });

    it('deletes an archived note via the archived note dropdown menu', () => {
      const archivedNote = { ...mockNote, archived: true };

      cy.intercept('GET', /\/rest\/home\/items/, {
        statusCode: 200,
        body: { tasks: [], notes: [archivedNote] }
      }).as('getArchivedItems');

      cy.intercept('DELETE', /\/rest\/notes\/\d+/, {
        statusCode: 204
      }).as('deleteNote');

      cy.visit('/home', {
        onBeforeLoad(win) {
          win.localStorage.setItem('TASKNOTE-TOKEN', 'fake-jwt-token');
          win.localStorage.setItem('TASKNOTE-USER', JSON.stringify(mockUser));
        }
      });

      cy.wait('@getArchivedItems');

      cy.get('[data-testid="archived-note-dropdown-menu-1"]').click();
      cy.get('[data-testid="archived-note-dropdown-delete-item-1"]').click();
      cy.get('[data-testid="confirm-delete-button"]').click();

      cy.wait('@deleteNote');
    });
  });

  /**
   * Share note
   */
  describe('Share', () => {
    beforeEach(() => {
      cy.intercept('GET', /\/rest\/home\/items/, {
        statusCode: 200,
        body: { tasks: [], notes: [mockNote] }
      }).as('getItems');

      visitWithAuth('/home');
      cy.wait('@getItems');
    });

    it('shares a note via the note dropdown menu', () => {
      cy.intercept('PUT', /\/rest\/notes\/\d+\/share/, {
        statusCode: 200,
        body: { ...mockNote, shared: true, shareToken: 'abc123' }
      }).as('shareNote');

      cy.get('[data-testid="note-dropdown-menu-1"]').click();
      cy.get('[data-testid="note-dropdown-share-item-1"]').click();

      cy.wait('@shareNote');
    });

    it('unshares a shared note via the note dropdown menu', () => {
      const sharedNote = { ...mockNote, shared: true, shareToken: 'abc123' };

      cy.intercept('GET', /\/rest\/home\/items/, {
        statusCode: 200,
        body: { tasks: [], notes: [sharedNote] }
      }).as('getSharedItems');

      cy.intercept('PUT', /\/rest\/notes\/\d+\/unshare/, {
        statusCode: 200,
        body: { ...sharedNote, shared: false, shareToken: null }
      }).as('unshareNote');

      cy.visit('/home', {
        onBeforeLoad(win) {
          win.localStorage.setItem('TASKNOTE-TOKEN', 'fake-jwt-token');
          win.localStorage.setItem('TASKNOTE-USER', JSON.stringify(mockUser));
        }
      });

      cy.wait('@getSharedItems');

      cy.get('[data-testid="note-dropdown-menu-1"]').click();
      cy.get('[data-testid="note-dropdown-share-item-1"]').click();

      cy.wait('@unshareNote');
    });
  });
});
