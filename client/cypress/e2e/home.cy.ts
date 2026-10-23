/**
 * E2E tests for Home page management: searching and filtering tasks and notes.
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

const mockTasks = [
  {
    id: 1,
    description: 'Buy groceries',
    completed: false,
    highPriority: true,
    dueDate: '2026-06-01',
    dueDateFmt: 'Jun 1, 2026',
    lastUpdate: '2026-05-01',
    tags: ['personal'],
    urls: []
  },
  {
    id: 2,
    description: 'Write report',
    completed: false,
    highPriority: false,
    dueDate: '',
    dueDateFmt: '',
    lastUpdate: '2026-05-02',
    tags: ['work'],
    urls: []
  }
];

const mockNotes = [
  {
    id: 1,
    title: 'Meeting notes',
    description: 'Discuss quarterly goals',
    url: null,
    tags: ['work'],
    lastUpdate: '2026-05-01',
    shared: false,
    shareToken: null,
    archived: false
  },
  {
    id: 2,
    title: 'Recipe',
    description: 'Pasta carbonara recipe',
    url: null,
    tags: ['personal'],
    lastUpdate: '2026-05-02',
    shared: false,
    shareToken: null,
    archived: false
  }
];

const mockTags = ['personal', 'work'];

/**
 * Replies to the aggregated /rest/home/items endpoint replicating the
 * server-side search (q), type, and tag filtering semantics.
 *
 * @param {object} req - The intercepted request.
 */
const replyWithFilteredItems = (req: { url: string; reply: (response: object) => void }): void => {
  const url = new URL(req.url);
  const query = (url.searchParams.get('q') ?? '').toLowerCase();
  const type = url.searchParams.get('type');
  const tag = url.searchParams.get('tag');

  let tasks = type === 'notes' ? [] : [...mockTasks];
  let notes = type === 'tasks' ? [] : [...mockNotes];

  if (tag) {
    tasks = tasks.filter(task => task.tags.includes(tag));
    notes = notes.filter(note => note.tags.includes(tag));
  }

  if (query) {
    tasks = tasks.filter(task => task.description.toLowerCase().includes(query));
    notes = notes.filter(note =>
      note.title.toLowerCase().includes(query)
      || note.description.toLowerCase().includes(query));
  }

  req.reply({ statusCode: 200, body: { tasks, notes } });
};

describe('Home Management', () => {
  beforeEach(() => {
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
      body: mockTags
    }).as('getTags');

    cy.intercept('GET', /\/rest\/home\/items/, replyWithFilteredItems).as('getItems');

    cy.visit('/home', {
      onBeforeLoad(win) {
        win.localStorage.setItem('TASKNOTE-TOKEN', 'fake-jwt-token');
        win.localStorage.setItem('TASKNOTE-USER', JSON.stringify(mockUser));
      }
    });
  });

  /**
   * Home page display
   */
  describe('Display', () => {
    it('shows tasks and notes after loading', () => {
      cy.wait('@getItems');

      cy.contains('Buy groceries').should('be.visible');
      cy.contains('Write report').should('be.visible');
      cy.contains('Meeting notes').should('be.visible');
      cy.contains('Recipe').should('be.visible');
    });
  });

  /**
   * Search / text filtering
   */
  describe('Search', () => {
    beforeEach(() => {
      cy.wait('@getItems');
    });

    it('filters items by search text matching a task description', () => {
      cy.get('input[placeholder="Filter tasks & notes"]').type('groceries');
      cy.wait('@getItems');

      cy.contains('Buy groceries').should('be.visible');
      cy.contains('Write report').should('not.exist');
      cy.contains('Meeting notes').should('not.exist');
      cy.contains('Recipe').should('not.exist');
    });

    it('filters items by search text matching a note title', () => {
      cy.get('input[placeholder="Filter tasks & notes"]').type('Meeting');
      cy.wait('@getItems');

      cy.contains('Meeting notes').should('be.visible');
      cy.contains('Buy groceries').should('not.exist');
      cy.contains('Recipe').should('not.exist');
    });

    it('restores all items after clearing the search', () => {
      cy.get('input[placeholder="Filter tasks & notes"]').type('groceries');
      cy.wait('@getItems');
      cy.contains('Write report').should('not.exist');

      cy.get('input[placeholder="Filter tasks & notes"]').clear();
      cy.wait('@getItems');

      cy.contains('Buy groceries').should('be.visible');
      cy.contains('Meeting notes').should('be.visible');
    });
  });

  /**
   * Dropdown filter
   */
  describe('Dropdown Filter', () => {
    beforeEach(() => {
      cy.wait('@getItems');
    });

    it('shows only tasks when Tasks filter is selected', () => {
      cy.get('[data-testid="dropdown-tag-filter"]').click();
      cy.contains('.dropdown-item', 'Tasks').click();
      cy.wait('@getItems');

      cy.contains('Buy groceries').should('be.visible');
      cy.contains('Write report').should('be.visible');
      cy.contains('Meeting notes').should('not.exist');
      cy.contains('Recipe').should('not.exist');
    });

    it('shows only notes when Notes filter is selected', () => {
      cy.get('[data-testid="dropdown-tag-filter"]').click();
      cy.contains('.dropdown-item', 'Notes').click();
      cy.wait('@getItems');

      cy.contains('Meeting notes').should('be.visible');
      cy.contains('Recipe').should('be.visible');
      cy.contains('Buy groceries').should('not.exist');
      cy.contains('Write report').should('not.exist');
    });

    it('shows everything when Everything filter is selected', () => {
      cy.get('[data-testid="dropdown-tag-filter"]').click();
      cy.contains('.dropdown-item', 'Tasks').click();
      cy.wait('@getItems');

      cy.get('[data-testid="dropdown-tag-filter"]').click();
      cy.contains('.dropdown-item', 'Everything').click();
      cy.wait('@getItems');

      cy.contains('Buy groceries').should('be.visible');
      cy.contains('Meeting notes').should('be.visible');
    });

    it('filters by tag when a tag is selected', () => {
      cy.wait('@getTags');

      cy.get('[data-testid="dropdown-tag-filter"]').click();
      cy.contains('.dropdown-item', '#personal').click();
      cy.wait('@getItems');

      cy.contains('Buy groceries').should('be.visible');
      cy.contains('Recipe').should('be.visible');
      cy.contains('Write report').should('not.exist');
      cy.contains('Meeting notes').should('not.exist');
    });
  });
});
