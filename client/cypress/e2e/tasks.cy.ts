/**
 * E2E tests for Task Management: Create, Update, and Delete tasks.
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

const mockTask = {
  id: 1,
  description: 'Buy groceries',
  completed: false,
  highPriority: false,
  dueDate: '',
  dueDateFmt: '',
  lastUpdate: '2026-05-01',
  tags: ['personal'],
  urls: []
};

describe('Task Management', () => {
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
      body: ['personal', 'work']
    }).as('getTags');

    cy.visit(path, {
      onBeforeLoad(win) {
        win.localStorage.setItem('TASKNOTE-TOKEN', 'fake-jwt-token');
        win.localStorage.setItem('TASKNOTE-USER', JSON.stringify(mockUser));
      }
    });
  };

  /**
   * Create task
   */
  describe('Create', () => {
    beforeEach(() => {
      visitWithAuth('/tasks/new');
    });

    it('displays the add task form', () => {
      cy.contains('Add Task').should('be.visible');
      cy.get('input[name="description"]').should('be.visible');
      cy.get('input[name="url"]').should('be.visible');
      cy.get('input[name="tag"]').should('be.visible');
      cy.contains('button', 'Save task').should('be.visible');
    });

    it('shows a validation error when submitted without a description', () => {
      cy.contains('button', 'Save task').click();

      cy.get('.alert-danger').should('be.visible');
    });

    it('creates a task and navigates to home on success', () => {
      cy.intercept('POST', /\/rest\/tasks/, {
        statusCode: 201,
        body: {
          id: 10,
          description: 'New task',
          done: false,
          highPriority: false,
          dueDate: '',
          dueDateFmt: '',
          lastUpdate: '',
          tags: [],
          urls: []
        }
      }).as('createTask');

      cy.intercept('GET', /\/rest\/home\/items/, {
        statusCode: 200,
        body: { tasks: [], notes: [] }
      }).as('getItems');

      cy.get('input[name="description"]').type('New task');
      cy.contains('button', 'Save task').click();

      cy.wait('@createTask');
      cy.url().should('include', '/home');
    });

    it('shows an error alert when the API returns an error on create', () => {
      cy.intercept('POST', /\/rest\/tasks/, {
        statusCode: 500,
        body: { message: 'Internal Server Error' }
      }).as('createTaskFail');

      cy.get('input[name="description"]').type('Failing task');
      cy.contains('button', 'Save task').click();

      cy.wait('@createTaskFail');
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
   * Update task
   */
  describe('Update', () => {
    beforeEach(() => {
      cy.intercept('GET', /\/rest\/tasks\/\d+/, {
        statusCode: 200,
        body: mockTask
      }).as('getTask');

      visitWithAuth('/tasks/edit/1');
      cy.wait('@getTask');
    });

    it('pre-fills the form with the existing task data', () => {
      cy.get('input[name="description"]').should('have.value', 'Buy groceries');
    });

    it('updates the task and navigates to home on success', () => {
      cy.intercept('PATCH', /\/rest\/tasks\/\d+/, {
        statusCode: 200,
        body: { ...mockTask, description: 'Buy more groceries' }
      }).as('updateTask');

      cy.intercept('GET', /\/rest\/home\/items/, {
        statusCode: 200,
        body: { tasks: [], notes: [] }
      }).as('getItems');

      cy.get('input[name="description"]').clear().type('Buy more groceries');
      cy.contains('button', 'Save task').click();

      cy.wait('@updateTask');
      cy.url().should('include', '/home');
    });

    it('shows an error alert when the update API call fails', () => {
      cy.intercept('PATCH', /\/rest\/tasks\/\d+/, {
        statusCode: 500,
        body: { message: 'Internal Server Error' }
      }).as('updateTaskFail');

      cy.get('input[name="description"]').clear().type('Updated task');
      cy.contains('button', 'Save task').click();

      cy.wait('@updateTaskFail');
      cy.get('.alert-danger').should('be.visible');
    });
  });

  /**
   * Mark as done / undone
   */
  describe('Mark as Done', () => {
    beforeEach(() => {
      cy.intercept('GET', /\/rest\/home\/items/, {
        statusCode: 200,
        body: { tasks: [mockTask], notes: [] }
      }).as('getItems');

      visitWithAuth('/home');
      cy.wait('@getItems');
    });

    it('marks a task as done via the task dropdown menu', () => {
      cy.intercept('PATCH', /\/rest\/tasks\/\d+/, {
        statusCode: 200,
        body: { ...mockTask, completed: true }
      }).as('markDone');

      cy.get('[data-testid="task-dropdown-menu-1"]').click();
      cy.get('[data-testid="task-dropdown-done-item-1"]').click();

      cy.wait('@markDone')
        .its('request.body')
        .should('deep.equal', { completed: true });
    });

    it('marks a done task as undone via the task dropdown menu', () => {
      cy.intercept('GET', /\/rest\/home\/items/, {
        statusCode: 200,
        body: { tasks: [{ ...mockTask, completed: true }], notes: [] }
      }).as('getDoneItems');

      cy.intercept('PATCH', /\/rest\/tasks\/\d+/, {
        statusCode: 200,
        body: mockTask
      }).as('markUndone');

      cy.visit('/home', {
        onBeforeLoad(win) {
          win.localStorage.setItem('TASKNOTE-TOKEN', 'fake-jwt-token');
          win.localStorage.setItem('TASKNOTE-USER', JSON.stringify(mockUser));
        }
      });

      cy.wait('@getDoneItems');

      cy.get('[data-testid="completed-task-dropdown-menu-1"]').click();
      cy.get('[data-testid="completed-task-dropdown-undone-item-1"]').click();

      cy.wait('@markUndone')
        .its('request.body')
        .should('deep.equal', { completed: false });
    });
  });
});
