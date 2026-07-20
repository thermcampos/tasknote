import React, { useEffect, useRef, useState } from 'react';
import {
  Alert,
  Badge,
  Card,
  Col,
  Container,
  Form,
  InputGroup,
  ListGroup,
  Row
} from 'react-bootstrap';
import { Hash } from 'react-bootstrap-icons';
import { useNavigate, useParams } from 'react-router';
import TaskNoteRequest from '../../types/TaskNoteRequest';
import { TaskResponse } from '../../types/TaskResponse';
import { useTranslation } from 'react-i18next';
import api from '../../api-service/api';
import ApiConfig from '../../api-service/apiConfig';
import { translateServerResponse } from '../../utils/TranslatorUtils';
import FormInput from '../../components/FormInput';
import ContentHeader from '../../components/ContentHeader';
import AlertError from '../../components/AlertError';

type TaskAction = 'add' | 'edit';

interface TaskDraft {
  description: string;
  taskUrl: string;
  dueDate: string | null;
  highPriority: boolean;
  tags: string[];
}

/**
 * TaskAdd component for adding and editing tasks.
 *
 * @returns {React.ReactNode} The rendered TaskAdd component.
 */
function TaskAdd(): React.ReactNode {
  const [validated, setValidated] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [taskId, setTaskId] = useState<number>(0);
  const [taskDescription, setTaskDescription] = useState<string>('');
  const [taskUrl, setTaskUrl] = useState<string>('');
  const [taskDone, setTaskDone] = useState<boolean>(false);
  const [action, setAction] = useState<TaskAction>('add');
  const [dueDate, setDueDate] = useState<string>('');
  const [highPriority, setHighPriority] = useState<boolean>(false);
  const [currentTag, setCurrentTag] = useState<string>('');
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [tags, setTags] = useState<string[]>([]);
  const [showTagDropdown, setShowTagDropdown] = useState<boolean>(false);
  const [draftBanner, setDraftBanner] = useState<boolean>(false);
  const { i18n, t } = useTranslation();
  const params = useParams();
  const navigate = useNavigate();
  const tagContainerRef = useRef<HTMLDivElement>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const hasUserEdited = useRef<boolean>(false);

  const draftKey = params?.id ? `draft:task:edit:${params.id}` : 'draft:task:new';

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
   * Handles errors by setting the error message and form invalid state.
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

  /**
   * Adds a new task.
   *
   * @param {TaskNoteRequest} payload - The task data to add.
   * @returns {Promise<boolean>} True if the task was added successfully, false otherwise.
   */
  const addTask = async (payload: TaskNoteRequest): Promise<boolean> => {
    try {
      await api.postJSON(ApiConfig.tasksUrl, payload);
      return true;
    }
    catch (e) {
      handleError(e);
    }
    return false;
  };

  /**
   * Submits the edited task.
   *
   * @param {TaskResponse} payload - The task data to edit.
   * @returns {Promise<boolean>} True if the task was edited successfully, false otherwise.
   */
  const submitEditTask = async (payload: TaskResponse): Promise<boolean> => {
    try {
      await api.patchJSON(`${ApiConfig.tasksUrl}/${payload.id}`, payload);
      return true;
    }
    catch (e) {
      handleError(e);
    }
    return false;
  };

  /**
   * Resets the input fields to their default values.
   */
  const resetInputs = (): void => {
    setTaskId(0);
    setTaskDescription('');
    setTaskDone(false);
    setTaskUrl('');
    setDueDate('');
    setHighPriority(false);
    setCurrentTag('');
    setSelectedTags([]);
    setAction('add');
    setValidated(false);
  };

  const saveDraft = (
    description: string,
    taskUrl: string,
    due: string,
    priority: boolean,
    draftTags: string[]
  ): void => {
    if (!hasUserEdited.current) return;
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      const draft: TaskDraft = {
        description,
        taskUrl,
        dueDate: due || null,
        highPriority: priority,
        tags: draftTags
      };
      localStorage.setItem(draftKey, JSON.stringify(draft));
    }, 1500);
  };

  const clearDraft = (): void => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    localStorage.removeItem(draftKey);
  };

  const applyDraft = (): void => {
    const raw = localStorage.getItem(draftKey);
    if (!raw) return;
    try {
      const draft: TaskDraft = JSON.parse(raw);
      setTaskDescription(draft.description ?? '');
      setTaskUrl(draft.taskUrl ?? '');
      const parsedDate = draft.dueDate ? draft.dueDate : '';
      setDueDate(parsedDate);
      setHighPriority(draft.highPriority ?? false);
      setSelectedTags(draft.tags ?? []);
      setDraftBanner(true);
    }
    catch {
      localStorage.removeItem(draftKey);
    }
  };

  const setTaskFromServer = (task: TaskResponse): void => {
    setTaskId(task.id);
    setTaskDescription(task.description);
    setTaskUrl(task.urls.length ? task.urls[0] : '');
    setTaskDone(task.done);
    if (task.dueDateFmt) {
      setDueDate(task.dueDate);
    }
    setHighPriority(task.highPriority);
    if (task.tags) {
      setSelectedTags(task.tags);
    }
  };

  const handleDiscardDraft = async (): Promise<void> => {
    setDraftBanner(false);
    if (params?.id) {
      try {
        const taskToEdit: TaskResponse = await api.getJSON(`${ApiConfig.tasksUrl}/${params.id}`);
        setTaskFromServer(taskToEdit);
      }
      catch (e) {
        handleError(e);
      }
      finally {
        clearDraft();
      }
    }
    else {
      clearDraft();
      resetInputs();
    }
  };

  const addTag = (tagName: string): void => {
    const normalized = tagName.trim().toLowerCase();
    let newTags = [...selectedTags];
    if (normalized && !selectedTags.includes(normalized)) {
      newTags = [...selectedTags, normalized];
      setSelectedTags(newTags);
    }
    hasUserEdited.current = true;
    saveDraft(taskDescription, taskUrl, dueDate, highPriority, newTags);
    setCurrentTag('');
    setShowTagDropdown(false);
  };

  const removeTag = (tagToRemove: string): void => {
    const newTags = selectedTags.filter(t => t !== tagToRemove);
    setSelectedTags(newTags);
    hasUserEdited.current = true;
    saveDraft(taskDescription, taskUrl, dueDate, highPriority, newTags);
  };

  /**
   * Handles the form submission.
   *
   * @param {React.SubmitEvent<HTMLFormElement>} event - The form submission event.
   */
  const handleSubmit = async (event: React.SubmitEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault();
    event.stopPropagation();
    setValidated(true);

    const form = event.currentTarget;
    if (!form.checkValidity()) {
      setErrorMessage(translateServerResponse('Please fill in all the fields', i18n.language));
      return;
    }

    const dueDateFormatted: string = dueDate;

    const finalTags = [...selectedTags];
    if (currentTag.trim()) {
      const normalized = currentTag.trim().toLowerCase();
      if (!finalTags.includes(normalized)) {
        finalTags.push(normalized);
      }
    }

    if (action === 'add') {
      const addPayload: TaskNoteRequest = {
        description: taskDescription.trim(),
        highPriority: highPriority,
        dueDate: dueDateFormatted,
        tags: finalTags,
        urls: taskUrl ? [taskUrl] : []
      };

      const added: boolean = await addTask(addPayload);
      if (added) {
        clearDraft();
        form.reset();
        resetInputs();
        navigate('/home');
      }
    }
    else if (action === 'edit') {
      const editPayload: TaskResponse = {
        id: taskId,
        description: taskDescription.trim(),
        done: taskDone,
        highPriority: highPriority,
        dueDate: dueDateFormatted,
        dueDateFmt: '',
        lastUpdate: '',
        tags: finalTags,
        urls: taskUrl ? [taskUrl] : []
      };

      const edited: boolean = await submitEditTask(editPayload);
      if (edited) {
        clearDraft();
        form.reset();
        resetInputs();
        navigate('/home');
      }
    }
  };

  /**
   * Checks if the URL is for editing a task and loads the task data if it is.
   */
  const checkEditUrl = async (): Promise<void> => {
    if (params.id) {
      try {
        const taskToEdit: TaskResponse = await api.getJSON(`${ApiConfig.tasksUrl}/${params.id}`);
        setTaskFromServer(taskToEdit);
        setAction('edit');
        applyDraft();
      }
      catch (e) {
        handleError(e);
      }
    }
  };

  useEffect(() => {
    loadTags();
    checkEditUrl();

    if (!params?.id) {
      applyDraft();
    }

    const handleClickOutside = (event: MouseEvent): void => {
      if (tagContainerRef.current && !tagContainerRef.current.contains(event.target as Node)) {
        setShowTagDropdown(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, []);

  return (
    <Container fluid>
      <ContentHeader
        h1TextRegular="Add"
        h1TextBold="Task"
        subtitle="Be on top of your TODO list"
        h2BlackText="Create, Filter, and Easily Find"
        h2GreenText="Them"
      />

      {/* Form to add, edit and save tasks */}
      <Row className="main-margin">
        <Col xs={12}>
          <Card>
            <Card.Body>
              <Card.Title>{t('task_form_title')}</Card.Title>

              <AlertError
                errorMessage={errorMessage}
                onClose={() => setErrorMessage('')}
              />

              {draftBanner && (
                <Alert variant="warning" dismissible onClose={() => setDraftBanner(false)}>
                  Draft restored from a previous session.
                  {' '}
                  <Alert.Link
                    href="#"
                    onClick={(e: React.MouseEvent) => {
                      e.preventDefault();
                      void handleDiscardDraft();
                    }}
                  >
                    Discard draft
                  </Alert.Link>
                </Alert>
              )}

              <Form
                noValidate
                validated={validated}
                onSubmit={handleSubmit}
                autoComplete="off"
              >
                {/* Description */}
                <FormInput
                  labelText={t('task_form_desc_label')}
                  iconName="PencilFill"
                  required={true}
                  type="text"
                  name="description"
                  placeholder={t('task_form_desc_placeholder')}
                  value={taskDescription}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                    setTaskDescription(e.target.value);
                    hasUserEdited.current = true;
                    saveDraft(e.target.value, taskUrl, dueDate, highPriority, selectedTags);
                  }}
                />

                <Row>
                  <Col xs={12} sm={12} xxl={6}>
                    {/* Task URL */}
                    <FormInput
                      labelText={t('task_form_url_label')}
                      iconName="At"
                      required={false}
                      type="text"
                      name="url"
                      placeholder={t('task_form_url_placeholder')}
                      value={taskUrl}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                        setTaskUrl(e.target.value);
                        hasUserEdited.current = true;
                        saveDraft(taskDescription, e.target.value, dueDate, highPriority, selectedTags);
                      }}
                    />
                  </Col>
                  <Col xs={12} sm={6} xxl={6}>
                    {/* Due date */}
                    <FormInput
                      labelText={t('task_form_duedate_label')}
                      iconName="CalendarCheck"
                      required={false}
                      type="date"
                      name="dueDate"
                      placeholder={t('task_form_duedate_placeholder')}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                        setDueDate(e.target.value);
                        hasUserEdited.current = true;
                        saveDraft(taskDescription, taskUrl, e.target.value, highPriority, selectedTags);
                      }}
                    />
                  </Col>
                  <Col xs={12} sm={12} xxl={12}>
                    {/* Tag with suggestion dropdown */}
                    <Form.Group className="mb-3" ref={tagContainerRef} style={{ position: 'relative' }}>
                      <Form.Label>Tags</Form.Label>
                      <InputGroup className="mb-3">
                        <InputGroup.Text>
                          <Hash />
                        </InputGroup.Text>
                        <Form.Control
                          type="text"
                          name="tag"
                          placeholder="my-tag (Optional)"
                          value={currentTag}
                          onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                            setCurrentTag(e.target.value);
                            setShowTagDropdown(true);
                          }}
                          onKeyDown={(e: React.KeyboardEvent<HTMLInputElement>) => {
                            if (e.key === 'Enter' && currentTag.trim()) {
                              e.preventDefault();
                              addTag(currentTag);
                            }
                          }}
                          onFocus={() => setShowTagDropdown(true)}
                          autoComplete="off"
                        />
                      </InputGroup>
                      <div className="mb-2 d-flex flex-wrap gap-1">
                        {selectedTags.map(t => (
                          <Badge
                            key={t}
                            bg="warning"
                            text="dark"
                            className="p-2"
                            style={{ cursor: 'pointer' }}
                            onClick={() => removeTag(t)}
                          >
                            #
                            {t}
                            {' '}
                            &times;
                          </Badge>
                        ))}
                      </div>
                      {showTagDropdown && tags.filter(t => t.toLowerCase().includes(currentTag.toLowerCase())).length > 0 && (
                        <ListGroup
                          style={{
                            position: 'absolute',
                            zIndex: 1000,
                            width: '100%',
                            maxHeight: '200px',
                            overflowY: 'auto',
                            left: 0
                          }}
                        >
                          {tags
                            .filter(t => t.toLowerCase().includes(currentTag.toLowerCase()))
                            .map(t => (
                              <ListGroup.Item
                                key={t}
                                action
                                onMouseDown={(e: React.MouseEvent) => {
                                  e.preventDefault();
                                  addTag(t);
                                }}
                              >
                                #
                                {t}
                              </ListGroup.Item>
                            ))}
                        </ListGroup>
                      )}
                    </Form.Group>
                  </Col>
                </Row>

                <Form.Check
                  type="switch"
                  id="high-priority-input"
                  label="High priority"
                  className="mb-3"
                  name="highPriority"
                  checked={highPriority}
                  onChange={() => {
                    setHighPriority(!highPriority);
                    hasUserEdited.current = true;
                    saveDraft(taskDescription, taskUrl, dueDate, !highPriority, selectedTags);
                  }}
                />

                <button
                  type="submit"
                  className="home-new-item task-note-btn"
                >
                  {t('task_form_submit')}
                </button>

                <button
                  type="button"
                  className="ms-2 home-new-item-secondary task-note-btn"
                  onClick={() => {
                    clearDraft();
                    navigate('/home');
                  }}
                >
                  Cancel
                </button>
              </Form>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
}

export default TaskAdd;
