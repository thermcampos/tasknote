import React, { useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Badge,
  Card,
  Col,
  Container,
  Dropdown,
  Form,
  InputGroup,
  Row
} from 'react-bootstrap';
import { TaskResponse } from '../../types/TaskResponse';
import { NoteResponse } from '../../types/NoteResponse';
import api from '../../api-service/api';
import ApiConfig from '../../api-service/apiConfig';
import { handleDefaultLang } from '../../lang-service/LangHandler';
import { translateServerResponse, translateTaskResponse } from '../../utils/TranslatorUtils';
import AuthContext from '../../context/AuthContext';
import FilterContext from '../../context/FilterContext';
import ContentHeader from '../../components/ContentHeader';
import AlertError from '../../components/AlertError';
import { CheckSquare, JournalText, ThreeDotsVertical } from 'react-bootstrap-icons';
import { NavLink } from 'react-router';
import ModalMarkdown from '../../components/ModalMarkdown';
import TaskTitle from '../../components/TaskTitle';
import TaskTimeLeft from '../../components/TaskTimeLeft';
import TaskTag from '../../components/TaskTag';
import NoteTitle from '../../components/NoteTitle';

const OPEN_NOTE_ID_KEY = 'OPEN_NOTE_ID';

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
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [tags, setTags] = useState<string[]>([]);
  const [name, setName] = useState<string>(user?.name ? user?.name : 'User');
  const [showMarkdownView, setShowMarkdownView] = useState<boolean>(false);
  const [modalTitle, setModalTitle] = useState<string>('');
  const [modalContent, setModalContent] = useState<string>('');
  const [tasks, setTasks] = useState<TaskResponse[]>([]);
  const [notes, setNotes] = useState<NoteResponse[]>([]);
  const [savedNotes, setSavedNotes] = useState<NoteResponse[]>([]);
  const [savedTasks, setSavedTasks] = useState<TaskResponse[]>([]);

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

  const loadTags = async (): Promise<void> => {
    try {
      const response: string[] = await api.getJSON(`${ApiConfig.homeUrl}/tasks/tags`);
      setTags(response);
    }
    catch (e) {
      handleError(e);
    }
  };

  /**
   * Mark a task as done or undone.
   *
   * @param {TaskResponse} task The task to be marked as done or undone.
   */
  const markAsDone = async (task: TaskResponse): Promise<void> => {
    try {
      await api.deleteNoContent(`${ApiConfig.tasksUrl}/${task.id}`);
      await loadAllTasks();
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
      await loadAllNotes();
    }
    catch (e) {
      handleError(e);
    }
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
      await loadAllNotes();
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
   * Apply filters to a given set of tasks and notes, updating displayed state.
   *
   * @param {string} text - The text to filter by.
   * @param {string | undefined} radioFilter - The radio filter option.
   * @param {TaskResponse[]} allTasks - The full list of tasks to filter from.
   * @param {NoteResponse[]} allNotes - The full list of notes to filter from.
   */
  const applyFilter = (text: string, radioFilter: string | undefined, allTasks: TaskResponse[], allNotes: NoteResponse[]): void => {
    if (!text && (!radioFilter || radioFilter === 'everything')) {
      setNotes([...allNotes]);
      setTasks([...allTasks]);
      return;
    }

    const tagToFilter = radioFilter?.startsWith('radio_') ? radioFilter.substring(6) : '';

    if (radioFilter && radioFilter === 'onlyTasks') {
      setNotes([]);
    }
    else {
      let filteredNotes = allNotes.filter((note: NoteResponse) => {
        const anyTitleMatch = note.title.toLowerCase().includes(text.toLowerCase());
        const anyContentMatch = note.description.toLowerCase().includes(text.toLowerCase());
        const anyUrlMatch = note.url?.includes(text.toLowerCase());
        const anyTagMatch = note.tags?.some(tag => tag.toLowerCase().includes(text.toLowerCase()));
        return anyTitleMatch || anyContentMatch || anyUrlMatch || anyTagMatch;
      });

      if (tagToFilter === 'untagged') {
        filteredNotes = filteredNotes.filter((note: NoteResponse) => !note.tags || note.tags.length === 0);
      }
      else if (tagToFilter) {
        filteredNotes = filteredNotes.filter((note: NoteResponse) => note.tags && note.tags.includes(tagToFilter));
      }

      setNotes([...filteredNotes]);
    }

    if (radioFilter && radioFilter === 'onlyNotes') {
      setTasks([]);
    }
    else {
      let filteredTasks = allTasks.filter((task: TaskResponse) => {
        return task.description.toLowerCase().includes(text.toLowerCase())
          || task.tags?.some(tag => tag.toLowerCase().includes(text.toLowerCase()))
          || task.urls.filter((url: string) => url.includes(text.toLowerCase())).length > 0;
      });

      if (tagToFilter === 'untagged') {
        filteredTasks = filteredTasks.filter((task: TaskResponse) => !task.tags || task.tags.length === 0);
      }
      else if (tagToFilter) {
        filteredTasks = filteredTasks.filter((task: TaskResponse) => task.tags && task.tags.includes(tagToFilter));
      }

      setTasks([...filteredTasks]);
    }
  };

  /**
   * Filter notes by a given text.
   */
  const filterTasksAndNotes = (text: string, radioFilter?: string): void => {
    setFilterText(text);
    applyFilter(text, radioFilter, savedTasks, savedNotes);
  };

  /**
   * Load tasks from the server.
   */
  const loadAllTasks = async () => {
    try {
      const tasksFetched: TaskResponse[] = await api.getJSON(ApiConfig.tasksUrl);
      const translated = translateTaskResponse(tasksFetched, i18n.language);
      translated.sort((t1, t2) => {
        if (t1.highPriority === t2.highPriority) {
          return 0;
        }
        if (t1.highPriority) {
          return -1;
        }
        return 1;
      });
      setSavedTasks([...translated]);
    }
    catch (e) {
      handleError(e);
    }
  };

  /**
   * Load notes from the server.
   */
  const loadAllNotes = async () => {
    try {
      const notesFetched: NoteResponse[] = await api.getJSON(ApiConfig.notesUrl);
      notesFetched.sort((n1, n2) => (n1.id > n2.id) ? -1 : 1);
      setSavedNotes([...notesFetched]);
    }
    catch (e) {
      handleError(e);
    }
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
    filterTasksAndNotes(filterText, value);
  };

  useEffect(() => {
    handleDefaultLang(user?.lang);
    setName(user?.name ?? 'User');
    loadTags();
    loadAllTasks();
    loadAllNotes();
  }, [user]);

  useEffect(() => {
    applyFilter(filterText, selectedOption, savedTasks, savedNotes);
  }, [savedTasks, savedNotes, filterText, selectedOption]);

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
                filterTasksAndNotes(e.target.value, selectedOption)}
              className="border-0"
              style={{
                borderTopRightRadius: 0,
                borderBottomRightRadius: 0,
                fontSize: '16px'
              }}
            />

            <Dropdown onSelect={eventKey => eventKey && handleOptionChange(eventKey)}>
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

              <Dropdown.Menu className="shadow-lg border-0" style={{ minWidth: '200px' }}>
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
        </Col>
      </Row>

      <Row className="mt-3">
        {tasks.map((task: TaskResponse) => (
          <Col xs={12} key={task.id.toString()}>
            <Card key={task.id.toString()} className={`task-card ${task.highPriority ? 'high-importance' : ''}`}>
              <Card.Body>
                <Row>
                  <Col xs={10}>
                    <Card.Title>
                      <span className="home-item-icon">
                        <CheckSquare />
                      </span>
                      <TaskTitle
                        title={task.description}
                        done={task.done}
                        taskUrl={task.urls}
                      />
                    </Card.Title>
                  </Col>
                  <Col xs={2} className="text-end">
                    <Dropdown>
                      <Dropdown.Toggle variant="success" data-testid={`task-dropdown-menu-${task.id}`}>
                        <ThreeDotsVertical />
                      </Dropdown.Toggle>
                      <Dropdown.Menu>
                        {!task.done && (
                          <NavLink to={`/tasks/edit/${task.id}`}>
                            <Dropdown.Item as="span">
                              {t('task_table_action_edit')}
                            </Dropdown.Item>
                          </NavLink>
                        )}
                        <Dropdown.Item
                          as="button"
                          onClick={() => markAsDone(task)}
                          data-testid={`task-dropdown-done-item-${task.id}`}
                        >
                          {task.done ? t('task_table_action_undone') : t('task_table_action_done')}
                        </Dropdown.Item>
                      </Dropdown.Menu>
                    </Dropdown>
                  </Col>
                </Row>

                {task.dueDateFmt && (
                  <TaskTimeLeft
                    text={task.dueDateFmt}
                    done={task.done}
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
                      <NoteTitle
                        title={note.title}
                        noteUrl={note.url}
                      />
                    </Card.Title>
                  </Col>
                  <Col xs={2} className="text-end">
                    <Dropdown>
                      <Dropdown.Toggle variant="success" data-testid={`note-dropdown-menu-${note.id}`}>
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
                          {note.shared ? t('note_action_unshare') : t('note_action_share')}
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
                          onClick={() => deleteNote(note.id)}
                          data-testid={`note-dropdown-delete-item-${note.id}`}
                        >
                          {t('task_table_action_delete')}
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

      <ModalMarkdown
        show={showMarkdownView}
        onHide={handleCloseModal}
        title={modalTitle}
        markdownText={modalContent}
      />
    </Container>
  );
}

export default Home;
