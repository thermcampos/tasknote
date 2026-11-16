/**
 * E2E tests for the read-only markdown views (Home note modal and the public
 * SharedNote page). Both render through the same read-only Milkdown viewer as
 * the note editor (issue #129, ADR 004).
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

const richDescription = [
  '# Quarterly goals',
  '',
  '## Details',
  '',
  'Some **bold** text and a [docs link](https://example.com/docs).',
  '',
  'A [bad link](javascript:alert(1)) and raw <b>html</b> here.',
  '',
  '- first item',
  '- second item',
  '',
  '- [ ] open task',
  '- [x] done task'
].join('\n');

const richNote = {
  id: 1,
  title: 'Rich note',
  description: richDescription,
  url: null,
  tags: ['work'],
  lastUpdate: '2026-05-01',
  shared: true,
  shareToken: 'share-token-1',
  archived: false
};

const VIEWER = '[data-testid="markdown-viewer"]';

/**
 * Asserts the shared rendering contract of the read-only viewer: headings keep
 * their levels, inline marks, lists and task-list checkboxes render, checkboxes
 * cannot be toggled, links open safely in a new tab, unsafe hrefs are dropped,
 * and raw HTML shows as literal text.
 */
const assertReadOnlyRendering = (): void => {
  cy.get(`${VIEWER} .ProseMirror`).should('have.attr', 'contenteditable', 'false');
  cy.get(`${VIEWER} .ProseMirror`).should('have.attr', 'aria-readonly', 'true');

  cy.get(`${VIEWER} h1`).should('have.text', 'Quarterly goals');
  cy.get(`${VIEWER} h2`).should('have.text', 'Details');
  cy.get(`${VIEWER} strong`).should('have.text', 'bold');
  cy.get(`${VIEWER} li`).should('contain.text', 'first item');
  cy.get(`${VIEWER} li`).should('contain.text', 'second item');

  cy.get(`${VIEWER} li[data-item-type="task"] input[type="checkbox"]`).should('have.length', 2);
  cy.get(`${VIEWER} li[data-item-type="task"] input[type="checkbox"]`)
    .first()
    .should('not.be.checked')
    .and('be.disabled')
    .click({ force: true });
  cy.get(`${VIEWER} li[data-item-type="task"] input[type="checkbox"]`)
    .first()
    .should('not.be.checked');
  cy.get(`${VIEWER} li[data-item-type="task"]`).first().should('have.attr', 'data-checked', 'false');
  cy.get(`${VIEWER} li[data-item-type="task"] input[type="checkbox"]`)
    .last()
    .should('be.checked')
    .click({ force: true });
  cy.get(`${VIEWER} li[data-item-type="task"] input[type="checkbox"]`)
    .last()
    .should('be.checked');

  cy.contains(`${VIEWER} a`, 'docs link')
    .should('have.attr', 'href', 'https://example.com/docs')
    .and('have.attr', 'target', '_blank')
    .and('have.attr', 'rel', 'noopener noreferrer');
  cy.contains(`${VIEWER} a`, 'docs link').focus();
  cy.focused().should('have.text', 'docs link');

  cy.contains(`${VIEWER} a`, 'bad link').then(($a) => {
    const href = $a.attr('href');
    expect(href === undefined || href === '').to.equal(true);
  });

  cy.get(`${VIEWER} span[data-type="html"]`).should('have.length', 2);
  cy.get(`${VIEWER} p`).should('contain.text', 'raw <b>html</b> here.');
  cy.get(`${VIEWER} b`).should('not.exist');
};

describe('Markdown read views', () => {
  describe('Home note modal', () => {
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
        body: ['work']
      }).as('getTags');

      cy.intercept('GET', /\/rest\/home\/items/, {
        statusCode: 200,
        body: { tasks: [], notes: [richNote] }
      }).as('getItems');

      cy.visit('/home', {
        onBeforeLoad(win) {
          win.localStorage.setItem('TASKNOTE-TOKEN', 'fake-jwt-token');
          win.localStorage.setItem('TASKNOTE-USER', JSON.stringify(mockUser));
        }
      });
      cy.wait('@getItems');

      cy.contains('a', 'Open it').click();
      cy.get('[data-testid="modal-header-title"]').should('have.text', 'Rich note');
    });

    it('renders the note with the read-only Milkdown viewer', () => {
      cy.get('.modal[aria-labelledby="markdown-modal-content"]').should('be.visible');
      assertReadOnlyRendering();
    });

    it('shows the raw stored markdown verbatim in the Source view', () => {
      cy.get('[data-testid="modal-source-button"]').click();

      cy.get('[data-testid="markdown-source-view"]').invoke('text').should('eq', richDescription);
      cy.get(VIEWER).should('not.exist');

      cy.get('[data-testid="modal-source-button"]').click();
      cy.get(`${VIEWER} h1`).should('have.text', 'Quarterly goals');
    });

    it('copies the raw stored markdown', () => {
      let writeText: sinon.SinonStub | undefined;
      cy.window().then((win) => {
        writeText = cy.stub(win.navigator.clipboard, 'writeText').resolves();
      });

      cy.get('[data-testid="modal-copy-button"]').click();

      cy.then(() => {
        expect(writeText).to.have.been.calledOnceWith(richDescription);
      });
      cy.get('[data-testid="modal-copy-button"]').should('have.text', 'Copied!');
    });
  });

  describe('Public SharedNote page', () => {
    beforeEach(() => {
      // The page itself lives at the same path, so only alias the API fetch.
      cy.intercept('GET', `**/public/notes/${richNote.shareToken}`, (req) => {
        if (req.resourceType === 'document') return;
        req.alias = 'getSharedNote';
        req.reply({ statusCode: 200, body: richNote });
      });

      cy.visit(`/public/notes/${richNote.shareToken}`);
      cy.wait('@getSharedNote');
    });

    it('renders the shared note with the read-only Milkdown viewer', () => {
      cy.contains('Rich note').should('be.visible');
      cy.contains('Last updated:').should('be.visible');
      assertReadOnlyRendering();
    });
  });
});
