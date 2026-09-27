import React, { useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Badge,
  Button,
  Card,
  Col,
  Container,
  Dropdown,
  Form,
  InputGroup,
  Modal,
  Row
} from 'react-bootstrap';
import { TaskResponse } from '../../types/TaskResponse';
import { NoteResponse } from '../../types/NoteResponse';
import { HomeItemsResponse } from '../../types/HomeItemsResponse';
import api from '../../api-service/api';
import ApiConfig from '../../api-service/apiConfig';
import { handleDefaultLang } from '../../lang-service/LangHandler';
import { translateServerResponse, translateTaskResponse } from '../../utils/TranslatorUtils';
import AuthContext from '../../context/AuthContext';
import FilterContext from '../../context/FilterContext';
import ContentHeader from '../../components/ContentHeader';
import AlertError from '../../components/AlertError';
import { CheckSquare, JournalText, ThreeDotsVertical } from 'react-bootstrap-icons';
import { NavLink, useLocation, useNavigate } from 'react-router';
import ModalMarkdown from '../../components/ModalMarkdown';
import TaskTitle from '../../components/TaskTitle';
import TaskTimeLeft from '../../components/TaskTimeLeft';
import TaskTag from '../../components/TaskTag';
import NoteTitle from '../../components/NoteTitle';
import {
  cacheHomeItems,
  cacheHomeTags,
  getCachedHomeItems,
  getCachedHomeTags
} from '../../utils/HomeCache';

const OPEN_NOTE_ID_KEY = 'OPEN_NOTE_ID';
const SEARCH_DEBOUNCE_MS = 300;

/**
 * Home page component.
 *
 * This component displays the home page of the application,
 *
 * @returns {React.ReactNode} The Home page component.
 */
function Home(): React.ReactNode {
  const { user } = useContext(AuthContext);
  const { filterText, selectedOption, setFilterText, setSelectedOption } = useContext(FilterContext);
  const { i18n, t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  const [refreshAfterSave] = useState(
    () => (location.state as { refreshHome?: boolean } | null)?.refreshHome === true
  );
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [tags, setTags] = useState<string[]>([]);
  const [name, setName] = useState<string>(user?.name ? user?.name : 'User');
  const [showMarkdownView, setShowMarkdownView] = useState<boolean>(false);
  const [modalTitle, setModalTitle] = useState<string>('');
  const [modalContent, setModalContent] = useState<string>('');
  const [tasks, setTasks] = useState<TaskResponse[]>([]);
  const [completedTasks, setCompletedTasks] = useState<TaskResponse[]>([]);
  const [notes, setNotes] = useState<NoteResponse[]>([]);
  const [archivedNotes, setArchivedNotes] = useState<NoteResponse[]>([]);
  const [showDeleteModal, setShowDeleteModal] = useState<boolean>(false);
  const [deleteTarget, setDeleteTarget] = useState<{ type: 'task' | 'note'; id: number } | null>(null);

  /**
   * Handles the error by setting the error message.
   *
   * @param {unknown} e - The error to handle.
   */
  const handleError = (e: unknown): void => {
    if (typeof e === 'string') {
      setErrorMessage(translateServerResponse(e, i18n.language));
    }
    else if (e instanceof Error) {
      setErrorMessage(translateServerResponse(e.message, i18n.language));
    }
  };

  const loadTags = async (forceRefresh = false): Promise<void> => {
    if (!forceRefresh && user) {
      const cachedTags = getCachedHomeTags(user.userId);
      if (cachedTags) {
        setTags(cachedTags);
        return;
      }
    }

    try {
      const response: string[] = await api.getJSON(`${ApiConfig.homeUrl}/tasks/tags`);
      setTags(response);
      if (user) {
        cacheHomeTags(user.userId, response);
      }
    }
    catch (e) {
      handleError(e);
    }
  };

  /**
   * Toggle a task's completed status.
   *
   * @param {TaskResponse} task The task to be marked as completed or uncompleted.
   */
  const toggleTaskCompleted = async (task: TaskResponse): Promise<void> => {
    try {
      await api.patchJSON(`${ApiConfig.tasksUrl}/${task.id}`, { completed: !task.completed });
      await loadItems(filterText, selectedOption);
    }
    catch (e) {
      handleError(e);
    }
  };

  /**
   * Delete a task.
   *
   * @param {number} taskIdParam The task ID to be deleted.
   */
  const deleteTask = async (taskIdParam: number) => {
    try {
      await api.deleteNoContent(`${ApiConfig.tasksUrl}/${taskIdParam}`);
      await loadItems(filterText, selectedOption);
      await loadTags(true);
    }
    catch (e) {
      handleError(e);
    }
  };

  /**
   * Delete a note.
   *
   * @param {number} noteIdParam The note ID to be deleted.
   */
  const deleteNote = async (noteIdParam: number) => {
    try {
      await api.deleteNoContent(`${ApiConfig.notesUrl}/${noteIdParam}`);
      await loadItems(filterText, selectedOption);
      await loadTags(true);
    }
    catch (e) {
      handleError(e);
    }
  };

  /**
   * Filter notes into active and archived sets.
   *
   * @param {NoteResponse[]} allNotes The full list of notes to partition.
   * @returns {{ active: NoteResponse[]; archived: NoteResponse[] }} Active and archived notes.
   */
  const partitionNotes = (allNotes: NoteResponse[]): { active: NoteResponse[]; archived: NoteResponse[] } => {
    return allNotes.reduce<{ active: NoteResponse[]; archived: NoteResponse[] }>(
      (acc, note) => {
        if (note.archived) {
          acc.archived.push(note);
        }
        else {
          acc.active.push(note);
        }
        return acc;
      },
      { active: [], archived: [] }
    );
  };

  /**
   * Opens the delete confirmation modal for a task or note.
   *
   * @param {object} target The target to delete with type and id.
   */
  const confirmDelete = (target: { type: 'task' | 'note'; id: number }) => {
    setDeleteTarget(target);
    setShowDeleteModal(true);
  };

  /**
   * Confirms and executes the delete action.
   */
  const handleConfirmDelete = async () => {
    if (!deleteTarget) {
      return;
    }

    if (deleteTarget.type === 'task') {
      await deleteTask(deleteTarget.id);
    }
    else {
      await deleteNote(deleteTarget.id);
    }

    setShowDeleteModal(false);
    setDeleteTarget(null);
  };

  /**
   * Archive a note, moving it to the archived notes section.
   *
   * @param {number} noteId The note ID to be archived.
   */
  const archiveNote = async (noteId: number): Promise<void> => {
    try {
      await api.putJSON(`${ApiConfig.notesUrl}/${noteId}/archive`, {});
      await loadItems(filterText, selectedOption);
    }
    catch (e) {
      handleError(e);
    }
  };

  /**
   * Restore an archived note back to active notes.
   *
   * @param {number} noteId The note ID to be restored.
   */
  const restoreNote = async (noteId: number): Promise<void> => {
    try {
      await api.putJSON(`${ApiConfig.notesUrl}/${noteId}/restore`, {});
      await loadItems(filterText, selectedOption);
    }
    catch (e) {
      handleError(e);
    }
  };

  const handleArchiveNote = (noteId: number): void => {
    void archiveNote(noteId);
  };

  /**
   * Share or unshare a note.
   *
   * @param {NoteResponse} note The note to share or unshare.
   */
  const toggleShareNote = async (note: NoteResponse): Promise<void> => {
    try {
      const action = note.shared ? 'unshare' : 'share';
      await api.putJSON(`${ApiConfig.notesUrl}/${note.id}/${action}`, {});
      await loadItems(filterText, selectedOption);
    }
    catch (e) {
      handleError(e);
    }
  };

  /**
   * Copy share link to clipboard.
   *
   * @param {NoteResponse} note The shared note.
   */
  const copyShareLink = (note: NoteResponse): void => {
    const link = `${window.location.origin}/public/notes/${note.shareToken}`;
    navigator.clipboard.writeText(link).catch(() => {
      setErrorMessage('Failed to copy link to clipboard.');
    });
  };

  /**
   * Build the home items URL for the given search text and filter option.
   *
   * @param {string} text - The search text.
   * @param {string | undefined} option - The selected filter option.
   * @returns {string} The items URL with query params, if any.
   */
  const buildItemsUrl = (text: string, option: string | undefined): string => {
    const params = new URLSearchParams();
    if (text && text.trim().length > 0) {
      params.append('q', text.trim());
    }
    if (option === 'onlyTasks') {
      params.append('type', 'tasks');
    }
    else if (option === 'onlyNotes') {
      params.append('type', 'notes');
    }
    else if (option && option.startsWith('radio_')) {
      params.append('tag', option.substring(6));
    }
    const queryString = params.toString();
    return queryString
      ? `${ApiConfig.homeUrl}/items?${queryString}`
      : `${ApiConfig.homeUrl}/items`;
  };

  /**
   * Load home items from the server for the current view: the default window,
   * or the active search/tag query.
   *
   * @param {string} text - The search text.
   * @param {string | undefined} option - The selected filter option.
   */
  const loadItems = async (
    text: string,
    option: string | undefined,
    forceRefresh = true
  ): Promise<void> => {
    const itemsUrl = buildItemsUrl(text, option);
    if (!forceRefresh && user) {
      const cachedItems = getCachedHomeItems(user.userId, itemsUrl);
      if (cachedItems) {
        updateHomeItems(cachedItems);
        return;
      }
    }

    try {
      const response: HomeItemsResponse = await api.getJSON(itemsUrl);
      if (user) {
        cacheHomeItems(user.userId, itemsUrl, response);
      }

      updateHomeItems(response);
    }
    catch (e) {
      handleError(e);
    }
  };

  const updateHomeItems = (response: HomeItemsResponse): void => {
    const translatedTasks = translateTaskResponse(response.tasks ?? [], i18n.language);
    translatedTasks.sort((t1, t2) => {
      if (t1.completed === t2.completed) {
        if (t1.highPriority === t2.highPriority) {
          return 0;
        }
        if (t1.highPriority) {
          return -1;
        }
        return 1;
      }
      if (t1.completed) {
        return -1;
      }
      return 1;
    });

    const fetchedNotes = [...(response.notes ?? [])];
    fetchedNotes.sort((n1, n2) => (n1.id > n2.id) ? -1 : 1);

    setTasks(translatedTasks.filter((task: TaskResponse) => !task.completed));
    setCompletedTasks(translatedTasks.filter((task: TaskResponse) => task.completed));

    const { active, archived } = partitionNotes(fetchedNotes);
    setNotes(active);
    setArchivedNotes(archived);
  };

  const cleanText = (text: string): string => {
    if (!text) {
      return text;
    }

    let cleaned = text.trim();
    if (cleaned.startsWith('- ')) {
      cleaned = cleaned.substring(2);
    }
    if (cleaned.startsWith('#')) {
      cleaned = text.replaceAll('#', '').trim();
    }
    cleaned = cleaned.replaceAll('`', '');
    cleaned = cleaned.replaceAll('**', '');

    // check for url pattern
    if (cleaned.includes('[') && cleaned.includes('](') && cleaned.includes('(')) {
      cleaned = cleaned.replaceAll(' [', ' ');
      cleaned = cleaned.replaceAll('](', ' - ');
      if (cleaned.endsWith(')')) {
        cleaned = cleaned.substring(0, cleaned.length - 1);
      }
      if (cleaned.endsWith('/')) {
        cleaned = cleaned.substring(0, cleaned.length - 1);
      }
    }
    if (cleaned.endsWith(';')) {
      cleaned = cleaned.substring(0, cleaned.length - 1);
    }
    if (cleaned.endsWith(':')) {
      cleaned = cleaned.substring(0, cleaned.length - 1);
    }

    // check for dot line
    if (cleaned.startsWith('.')) {
      cleaned = cleaned.substring(0, cleaned.length - 1);
    }

    // shorten the line up to 100
    const max = window.innerWidth / 15;
    if (cleaned.length > max) {
      cleaned = cleaned.substring(0, max);
    }
    return cleaned;
  };

  const getFirstRows = (content: string): string => {
    const lines = content.split('\n');
    const preview: string[] = [];
    const maxLines = 2;
    for (let i = 0; i < 10; i++) {
      const clean = cleanText(lines[i]);
      if (clean !== '') {
        preview.push(clean);
        if (preview.length === maxLines) {
          break;
        }
      }
    }
    return preview.join('\n');
  };

  const handleCloseModal = () => {
    setShowMarkdownView(false);
    localStorage.removeItem(OPEN_NOTE_ID_KEY);
  };

  const getSelectedLabel = (): string => {
    if (selectedOption === 'everything') return t('home_radio_everything');
    if (selectedOption === 'onlyTasks') return t('home_radio_tasks');
    if (selectedOption === 'onlyNotes') return t('home_radio_notes');

    // Handle tag selections
    const tagName = selectedOption.replace('radio_', '');
    if (tags.includes(tagName)) {
      return `#${tagName}`;
    }

    return t('home_radio_everything');
  };

  const isUnboundedView = (): boolean => {
    return filterText.trim().length > 0 || selectedOption.startsWith('radio_');
  };

  const getWindowHintKey = (): string => {
    if (selectedOption === 'onlyTasks') {
      return 'home_window_hint_tasks';
    }
    if (selectedOption === 'onlyNotes') {
      return 'home_window_hint_notes';
    }
    return 'home_window_hint';
  };

  const getSelectedVariant = (): string => {
    if (selectedOption === 'everything') {
      return 'secondary';
    }
    if (selectedOption === 'onlyTasks') {
      return 'primary';
    }
    if (selectedOption === 'onlyNotes') {
      return 'info';
    }
    return 'warning'; // for tags
  };

  const handleOptionChange = (value: string): void => {
    setSelectedOption(value);
  };

  useEffect(() => {
    if (refreshAfterSave) {
      navigate(location.pathname, { replace: true, state: null });
    }
  }, [refreshAfterSave, navigate, location.pathname]);

  useEffect(() => {
    handleDefaultLang(user?.lang);
    setName(user?.name ?? 'User');
    void loadTags(refreshAfterSave);
  }, [user, refreshAfterSave]);

  useEffect(() => {
    const timer = setTimeout(() => {
      void loadItems(filterText, selectedOption, refreshAfterSave);
    }, SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [filterText, selectedOption, user, refreshAfterSave]);

  useEffect(() => {
    const openNoteId = localStorage.getItem(OPEN_NOTE_ID_KEY);
    if (openNoteId && notes.length > 0) {
      const noteId = Number(openNoteId);
      const foundNote = notes.find(n => n.id === noteId);
      if (foundNote) {
        setModalTitle(foundNote.title);
        setModalContent(foundNote.description);
        setShowMarkdownView(true);
      }
      else {
        localStorage.removeItem(OPEN_NOTE_ID_KEY);
      }
    }
  }, [notes]);

  return (
    <Container fluid>
      <ContentHeader
        h1TextRegular={t('home_welcome_title')}
        h1TextBold={name}
        subtitle={t('home_welcome_subtitle')}
        h2BlackText={t('home_welcome_start')}
        h2GreenText={t('home_welcome_productive')}
        isHomeComponent
        newTaskI18n={t('task_form_title')}
        newNoteI18n={t('note_form_title')}
      />

      <AlertError
        errorMessage={errorMessage}
        onClose={() => {
          setErrorMessage('');
        }}
      />

      <Row>
        <Col xs={12}>
          <InputGroup size="lg" className="shadow-sm">
            <Form.Control
              type="text"
              placeholder={t('home_input_filter')}
              value={filterText}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                setFilterText(e.target.value)}
              className="border-0"
              style={{
                borderTopRightRadius: 0,
                borderBottomRightRadius: 0,
                fontSize: '16px'
              }}
            />

            <Dropdown
              onSelect={eventKey => eventKey && handleOptionChange(eventKey)}
            >
              <Dropdown.Toggle
                variant="success"
                id="filter-dropdown"
                className="d-flex align-items-center gap-2 px-3"
                data-testid="dropdown-tag-filter"
                style={{
                  borderTopLeftRadius: 0,
                  borderBottomLeftRadius: 0,
                  minWidth: '160px',
                  justifyContent: 'space-between'
                }}
              >
                <Badge
                  bg={getSelectedVariant()}
                  className="text-white"
                  style={{ fontSize: '0.85rem' }}
                  data-testid="main-label-selector"
                >
                  {getSelectedLabel()}
                </Badge>
              </Dropdown.Toggle>

              <Dropdown.Menu
                className="shadow-lg border-0"
                style={{ minWidth: '200px' }}
              >
                <Dropdown.Header className="text-muted small">
                  <i className="bi bi-funnel me-2"></i>
                  Filter Options
                </Dropdown.Header>

                <Dropdown.Item
                  eventKey="everything"
                  active={selectedOption === 'everything'}
                  className="d-flex align-items-center gap-2"
                >
                  <Badge bg="secondary" className="badge-sm">
                    {t('home_radio_everything')}
                  </Badge>
                </Dropdown.Item>

                <Dropdown.Item
                  eventKey="onlyTasks"
                  active={selectedOption === 'onlyTasks'}
                  className="d-flex align-items-center gap-2"
                >
                  <Badge bg="primary" className="badge-sm">
                    {t('home_radio_tasks')}
                  </Badge>
                </Dropdown.Item>

                <Dropdown.Item
                  eventKey="onlyNotes"
                  active={selectedOption === 'onlyNotes'}
                  className="d-flex align-items-center gap-2"
                >
                  <Badge bg="info" className="badge-sm">
                    {t('home_radio_notes')}
                  </Badge>
                </Dropdown.Item>

                {tags.length > 0 && (
                  <>
                    <Dropdown.Divider />
                    <Dropdown.Header className="text-muted small">
                      <i className="bi bi-tags me-2"></i>
                      Tags
                    </Dropdown.Header>
                    {tags.map((tag: string) => (
                      <Dropdown.Item
                        key={tag}
                        eventKey={`radio_${tag}`}
                        active={selectedOption === `radio_${tag}`}
                        className="d-flex align-items-center gap-2"
                      >
                        <Badge bg="warning" text="dark" className="badge-sm">
                          #
                          {tag}
                        </Badge>
                      </Dropdown.Item>
                    ))}
                  </>
                )}
              </Dropdown.Menu>
            </Dropdown>
          </InputGroup>
          <Form.Text
            className="text-muted search-help-text"
            data-testid="home-view-hint"
          >
            {isUnboundedView() ? t('home_search_hint') : t(getWindowHintKey())}
          </Form.Text>
        </Col>
      </Row>

      <Row className="mt-3">
        {tasks.map((task: TaskResponse) => (
          <Col xs={12} key={task.id.toString()}>
            <Card
              key={task.id.toString()}
              className={`task-card ${task.highPriority ? 'high-importance' : ''}`}
            >
              <Card.Body>
                <Row>
                  <Col xs={10}>
                    <Card.Title>
                      <span className="home-item-icon">
                        <CheckSquare />
                      </span>
                      <TaskTitle
                        title={task.description}
                        completed={task.completed}
                        taskUrl={task.urls}
                      />
                    </Card.Title>
                  </Col>
                  <Col xs={2} className="text-end">
                    <Dropdown>
                      <Dropdown.Toggle
                        variant="success"
                        data-testid={`task-dropdown-menu-${task.id}`}
                      >
                        <ThreeDotsVertical />
                      </Dropdown.Toggle>
                      <Dropdown.Menu>
                        {!task.completed && (
                          <NavLink to={`/tasks/edit/${task.id}`}>
                            <Dropdown.Item as="span">
                              {t('task_table_action_edit')}
                            </Dropdown.Item>
                          </NavLink>
                        )}
                        <Dropdown.Item
                          as="button"
                          onClick={() => toggleTaskCompleted(task)}
                          data-testid={`task-dropdown-done-item-${task.id}`}
                        >
                          {task.completed
                            ? t('task_table_action_undone')
                            : t('task_table_action_done')}
                        </Dropdown.Item>
                        <Dropdown.Item
                          as="button"
                          onClick={() =>
                            confirmDelete({ type: 'task', id: task.id })}
                          data-testid={`task-dropdown-delete-item-${task.id}`}
                        >
                          {t('task_table_action_delete')}
                        </Dropdown.Item>
                      </Dropdown.Menu>
                    </Dropdown>
                  </Col>
                </Row>

                {task.dueDateFmt && (
                  <TaskTimeLeft
                    text={task.dueDateFmt}
                    completed={task.completed}
                    tooltip={task.dueDate}
                  />
                )}
              </Card.Body>
              <Card.Footer className="task-card-footer">
                <TaskTag
                  tags={task.tags}
                  lastUpdate={task.lastUpdate}
                  taskOrNote="task"
                />
              </Card.Footer>
            </Card>
          </Col>
        ))}
      </Row>

      <Row>
        {notes.map((note: NoteResponse) => (
          <Col xs={12} key={note.id.toString()}>
            <Card key={note.id.toString()} className="mb-3">
              <Card.Body>
                <Row>
                  <Col xs={10}>
                    <Card.Title>
                      <span className="home-item-icon">
                        <JournalText />
                      </span>
                      <NoteTitle title={note.title} noteUrl={note.url} />
                    </Card.Title>
                  </Col>
                  <Col xs={2} className="text-end">
                    <Dropdown>
                      <Dropdown.Toggle
                        variant="success"
                        data-testid={`note-dropdown-menu-${note.id}`}
                      >
                        <ThreeDotsVertical />
                      </Dropdown.Toggle>
                      <Dropdown.Menu>
                        <NavLink to={`/notes/edit/${note.id}`}>
                          <Dropdown.Item as="span">
                            {t('task_table_action_edit')}
                          </Dropdown.Item>
                        </NavLink>
                        <NavLink to={`/notes/new?cloneFrom=${note.id}`}>
                          <Dropdown.Item as="span">
                            {t('task_table_action_clone')}
                          </Dropdown.Item>
                        </NavLink>
                        <Dropdown.Item
                          as="button"
                          onClick={() => toggleShareNote(note)}
                          data-testid={`note-dropdown-share-item-${note.id}`}
                        >
                          {note.shared
                            ? t('note_action_unshare')
                            : t('note_action_share')}
                        </Dropdown.Item>
                        {note.shared && note.shareToken && (
                          <Dropdown.Item
                            as="button"
                            onClick={() => copyShareLink(note)}
                            data-testid={`note-dropdown-copy-link-${note.id}`}
                          >
                            {t('note_action_copy_link')}
                          </Dropdown.Item>
                        )}
                        <Dropdown.Item
                          as="button"
                          onClick={() => handleArchiveNote(note.id)}
                          data-testid={`note-dropdown-archive-item-${note.id}`}
                        >
                          {t('note_action_archive')}
                        </Dropdown.Item>
                      </Dropdown.Menu>
                    </Dropdown>
                  </Col>
                </Row>

                <span className="text-muted span-line-break font-size-14">
                  {getFirstRows(note.description)}
                </span>
              </Card.Body>
              <Card.Footer className="task-card-footer">
                <TaskTag
                  tags={note.tags}
                  lastUpdate={note.lastUpdate}
                  taskOrNote="note"
                  onClick={(e: React.MouseEvent<Element, MouseEvent>) => {
                    e.preventDefault();
                    e.stopPropagation();
                    setModalTitle(note.title);
                    setModalContent(note.description);
                    setShowMarkdownView(true);
                    localStorage.setItem(OPEN_NOTE_ID_KEY, note.id.toString());
                  }}
                />
              </Card.Footer>
            </Card>
          </Col>
        ))}
      </Row>

      {completedTasks.length > 0 && (
        <Row className="mt-4">
          <Col xs={12}>
            <h5 className="text-muted">{t('home_completed_tasks_title')}</h5>
          </Col>
          {completedTasks.map((task: TaskResponse) => (
            <Col xs={12} key={`completed-${task.id.toString()}`}>
              <Card
                className={`task-card task-completed ${task.highPriority ? 'high-importance' : ''}`}
              >
                <Card.Body>
                  <Row>
                    <Col xs={10}>
                      <Card.Title>
                        <span className="home-item-icon">
                          <CheckSquare />
                        </span>
                        <TaskTitle
                          title={task.description}
                          completed={task.completed}
                          taskUrl={task.urls}
                        />
                      </Card.Title>
                    </Col>
                    <Col xs={2} className="text-end">
                      <Dropdown>
                        <Dropdown.Toggle
                          variant="success"
                          data-testid={`completed-task-dropdown-menu-${task.id}`}
                        >
                          <ThreeDotsVertical />
                        </Dropdown.Toggle>
                        <Dropdown.Menu>
                          <Dropdown.Item
                            as="button"
                            onClick={() => toggleTaskCompleted(task)}
                            data-testid={`completed-task-dropdown-undone-item-${task.id}`}
                          >
                            {t('task_table_action_undone')}
                          </Dropdown.Item>
                          <Dropdown.Item
                            as="button"
                            onClick={() =>
                              confirmDelete({ type: 'task', id: task.id })}
                            data-testid={`completed-task-dropdown-delete-item-${task.id}`}
                          >
                            {t('task_table_action_delete')}
                          </Dropdown.Item>
                        </Dropdown.Menu>
                      </Dropdown>
                    </Col>
                  </Row>
                </Card.Body>
                <Card.Footer className="task-card-footer">
                  <TaskTag
                    tags={task.tags}
                    lastUpdate={task.lastUpdate}
                    taskOrNote="task"
                  />
                </Card.Footer>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      {archivedNotes.length > 0 && (
        <Row className="mt-4">
          <Col xs={12}>
            <h5 className="text-muted">{t('home_archived_notes_title')}</h5>
          </Col>
          {archivedNotes.map((note: NoteResponse) => (
            <Col xs={12} key={`archived-${note.id.toString()}`}>
              <Card className="task-card task-completed mb-3">
                <Card.Body>
                  <Row>
                    <Col xs={10}>
                      <Card.Title>
                        <span className="home-item-icon">
                          <JournalText />
                        </span>
                        <NoteTitle title={note.title} noteUrl={note.url} />
                      </Card.Title>
                    </Col>
                    <Col xs={2} className="text-end">
                      <Dropdown>
                        <Dropdown.Toggle
                          variant="success"
                          data-testid={`archived-note-dropdown-menu-${note.id}`}
                        >
                          <ThreeDotsVertical />
                        </Dropdown.Toggle>
                        <Dropdown.Menu>
                          <Dropdown.Item
                            as="button"
                            onClick={() => restoreNote(note.id)}
                            data-testid={`archived-note-dropdown-restore-item-${note.id}`}
                          >
                            {t('note_action_restore')}
                          </Dropdown.Item>
                          <Dropdown.Item
                            as="button"
                            onClick={() =>
                              confirmDelete({ type: 'note', id: note.id })}
                            data-testid={`archived-note-dropdown-delete-item-${note.id}`}
                          >
                            {t('note_action_delete_permanently')}
                          </Dropdown.Item>
                        </Dropdown.Menu>
                      </Dropdown>
                    </Col>
                  </Row>

                  <span className="text-muted span-line-break font-size-14">
                    {getFirstRows(note.description)}
                  </span>
                </Card.Body>
                <Card.Footer className="task-card-footer">
                  <TaskTag
                    tags={note.tags}
                    lastUpdate={note.lastUpdate}
                    taskOrNote="note"
                    onClick={(e: React.MouseEvent<Element, MouseEvent>) => {
                      e.preventDefault();
                      e.stopPropagation();
                      setModalTitle(note.title);
                      setModalContent(note.description);
                      setShowMarkdownView(true);
                      localStorage.setItem(
                        OPEN_NOTE_ID_KEY,
                        note.id.toString()
                      );
                    }}
                  />
                </Card.Footer>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      <ModalMarkdown
        show={showMarkdownView}
        onHide={handleCloseModal}
        title={modalTitle}
        markdownText={modalContent}
      />

      <Modal
        show={showDeleteModal}
        onHide={() => setShowDeleteModal(false)}
        centered
        backdrop="static"
      >
        <Modal.Header closeButton className="bg-danger-subtle">
          <Modal.Title className="d-flex align-items-center gap-2">
            <i className="bi bi-exclamation-triangle-fill text-danger"></i>
            {t('delete_modal_title')}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>{t('delete_modal_body')}</Modal.Body>
        <Modal.Footer className="d-flex flex-wrap gap-2 justify-content-end">
          <Button
            variant="outline-secondary"
            onClick={() => {
              setShowDeleteModal(false);
            }}
            className="task-note-btn"
          >
            {t('delete_modal_cancel')}
          </Button>
          <Button
            variant="danger"
            onClick={handleConfirmDelete}
            className="task-note-btn"
            data-testid="confirm-delete-button"
          >
            {t('delete_modal_confirm')}
          </Button>
        </Modal.Footer>
      </Modal>
    </Container>
  );
}

export default Home;
