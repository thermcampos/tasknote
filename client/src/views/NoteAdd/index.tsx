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
import { useTranslation } from 'react-i18next';
import { NoteResponse } from '../../types/NoteResponse';
import api from '../../api-service/api';
import ApiConfig from '../../api-service/apiConfig';
import { translateServerResponse } from '../../utils/TranslatorUtils';
import FormInput from '../../components/FormInput';
import ModalMarkdown from '../../components/ModalMarkdown';
import AlertError from '../../components/AlertError';
import ContentHeader from '../../components/ContentHeader';

type NoteAction = 'add' | 'edit';

interface NoteDraft {
  title: string;
  content: string;
  noteUrl: string;
  tags: string[];
}

/**
 * NoteAdd component for adding and editing notes.
 *
 * @returns {React.ReactNode} The rendered NoteAdd component.
 */
function NoteAdd(): React.ReactNode {
  const [validated, setValidated] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [noteId, setNoteId] = useState<number>(0);
  const [noteTitle, setNoteTitle] = useState<string>('');
  const [noteContent, setNoteContent] = useState<string>('');
  const [noteUrl, setNoteUrl] = useState<string>('');
  const [currentTag, setCurrentTag] = useState<string>('');
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [tags, setTags] = useState<string[]>([]);
  const [showTagDropdown, setShowTagDropdown] = useState<boolean>(false);
  const [action, setAction] = useState<NoteAction>('add');
  const [showPreviewMd, setShowPreviewMd] = useState<boolean>(false);
  const [draftBanner, setDraftBanner] = useState<boolean>(false);
  const { i18n, t } = useTranslation();
  const params = useParams();
  const navigate = useNavigate();
  const tagContainerRef = useRef<HTMLDivElement>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const hasUserEdited = useRef<boolean>(false);

  const draftKey = params?.id ? `draft:note:edit:${params.id}` : 'draft:note:new';

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
   * Adds a new note.
   *
   * @param {NoteResponse} payload - The note data to add.
   * @returns {Promise<boolean>} True if the note was added successfully, false otherwise.
   */
  const addNote = async (payload: NoteResponse): Promise<boolean> => {
    try {
      await api.postJSON(ApiConfig.notesUrl, payload);
      return true;
    }
    catch (e) {
      handleError(e);
    }
    return false;
  };

  /**
   * Submits the edited note.
   *
   * @param {NoteResponse} payload - The note data to edit.
   * @returns {Promise<boolean>} True if the note was edited successfully, false otherwise.
   */
  const submitEditNote = async (payload: NoteResponse): Promise<boolean> => {
    try {
      await api.patchJSON(`${ApiConfig.notesUrl}/${payload.id}`, payload);
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
  const resetInputs = () => {
    setNoteId(0);
    setNoteTitle('');
    setNoteUrl('');
    setNoteContent('');
    setCurrentTag('');
    setSelectedTags([]);
    setAction('add');
    setValidated(false);
  };

  const saveDraft = (title: string, content: string, noteUrl: string, draftTags: string[]): void => {
    if (!hasUserEdited.current) return;
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      const draft: NoteDraft = { title, content, noteUrl, tags: draftTags };
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
      const draft: NoteDraft = JSON.parse(raw);
      setNoteTitle(draft.title ?? '');
      setNoteContent(draft.content ?? '');
      setNoteUrl(draft.noteUrl ?? '');
      setSelectedTags(draft.tags ?? []);
      setDraftBanner(true);
    }
    catch {
      localStorage.removeItem(draftKey);
    }
  };

  const handleDiscardDraft = async (): Promise<void> => {
    setDraftBanner(false);
    if (params?.id) {
      try {
        const noteToEdit: NoteResponse = await api.getJSON(`${ApiConfig.notesUrl}/${params.id}`);
        setNoteFromServer(noteToEdit);
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
    saveDraft(noteTitle, noteContent, noteUrl, newTags);
    setCurrentTag('');
    setShowTagDropdown(false);
  };

  const removeTag = (tagToRemove: string): void => {
    const newTags = selectedTags.filter(t => t !== tagToRemove);
    setSelectedTags(newTags);
    hasUserEdited.current = true;
    saveDraft(noteTitle, noteContent, noteUrl, newTags);
  };

  /**
   * Saves the note, either adding a new one or editing an existing one.
   *
   * @returns {Promise<boolean>} True if the note was saved successfully, false otherwise.
   */
  const saveNote = async (): Promise<boolean> => {
    setValidated(true);

    if (!noteTitle.trim() || !noteContent.trim()) {
      setErrorMessage(translateServerResponse('Please fill in all the fields', i18n.language));
      return false;
    }

    const finalTags = [...selectedTags];
    if (currentTag.trim()) {
      const normalized = currentTag.trim().toLowerCase();
      if (!finalTags.includes(normalized)) {
        finalTags.push(normalized);
      }
    }

    const payload: NoteResponse = {
      id: action === 'edit' ? noteId : 0,
      title: noteTitle,
      description: noteContent,
      url: noteUrl,
      tags: finalTags,
      lastUpdate: '',
      shared: false,
      shareToken: null
    };

    const saved = action === 'add'
      ? await addNote(payload)
      : await submitEditNote(payload);

    if (saved) {
      clearDraft();
      resetInputs();
      navigate('/home');
    }

    return saved;
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

    await saveNote();
  };

  /**
   * Checks if the URL is for editing a note and loads the note data if it is.
   */
  const checkEditUrl = async (): Promise<void> => {
    if (params?.id) {
      try {
        const noteToEdit: NoteResponse = await api.getJSON(`${ApiConfig.notesUrl}/${params.id}`);
        setNoteFromServer(noteToEdit);
        setAction('edit');
        applyDraft();
      }
      catch (e) {
        handleError(e);
      }
    }
  };

  const checkCloneUrl = async (): Promise<void> => {
    const { search } = window.location;

    if (!search || !search.startsWith('?') || !search.includes('cloneFrom=')) {
      return;
    }

    const index = search.indexOf('=');
    if (search.length - index > 4) {
      return;
    }

    const idToClone = search.substring(index + 1);
    if (idToClone && !isNaN(parseInt(idToClone))) {
      try {
        const noteToClone: NoteResponse = await api.getJSON(`${ApiConfig.notesUrl}/${idToClone}`);
        setNoteFromServer(noteToClone);
      }
      catch (e) {
        handleError(e);
      }
    }
  };

  const setNoteFromServer = (noteData: NoteResponse) => {
    setNoteId(noteData.id);
    setNoteTitle(noteData.title);
    if (noteData.url) {
      setNoteUrl(noteData.url);
    }
    if (noteData.tags) {
      setSelectedTags(noteData.tags);
    }
    setNoteContent(noteData.description);
  };

  /**
   * Display the Markdown text in Markdown format on a modal.
   *
   * @param {React.MouseEvent<Element, MouseEvent>} e The mouse click event.
   */
  const previewMarkdown = (e: React.MouseEvent<Element, MouseEvent>): void => {
    e.preventDefault();
    e.stopPropagation();
    setShowPreviewMd(noteContent.length > 0);
  };

  /**
   * Closes the Markdown preview modal.
   */
  const handleCloseModal = (): void => setShowPreviewMd(false);

  useEffect(() => {
    loadTags();
    checkEditUrl();
    checkCloneUrl();

    if (!params?.id && !window.location.search.includes('cloneFrom=')) {
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
        h1TextBold="Note"
        subtitle="Save your notes in plain text or Markdown format"
        h2BlackText="Create, Filter, and Easily Find"
        h2GreenText="Them"
      />

      <Row className="main-margin">
        <Col xs={12}>
          <Card>
            <Card.Body>
              <Card.Title>{t('note_form_title')}</Card.Title>

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
                {/* Note title */}
                <FormInput
                  labelText={t('note_form_title_label')}
                  iconName="JournalCheck"
                  required={true}
                  type="text"
                  name="note_title"
                  placeholder={t('note_form_title_placeholder')}
                  value={noteTitle}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                    setNoteTitle(e.target.value);
                    hasUserEdited.current = true;
                    saveDraft(e.target.value, noteContent, noteUrl, selectedTags);
                  }}
                />

                <Row>
                  <Col xs={12} xl={9}>
                    {/* Note URL */}
                    <FormInput
                      labelText={t('task_form_url_label')}
                      iconName="At"
                      required={false}
                      type="text"
                      name="url"
                      placeholder={t('task_form_url_placeholder')}
                      value={noteUrl}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                        setNoteUrl(e.target.value);
                        hasUserEdited.current = true;
                        saveDraft(noteTitle, noteContent, e.target.value, selectedTags);
                      }}
                    />
                  </Col>
                  <Col xs={12} xl={3}>
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

                <Form.Group controlId="form_noteDescription">
                  <Form.Label>
                    {t('note_form_content_label')}
                    <small>
                      <a href="#" onClick={previewMarkdown}>
                        {' '}
                        Preview Markdown
                      </a>
                    </small>
                  </Form.Label>
                  <Form.Control
                    className="font-size-14"
                    as="textarea"
                    required={true}
                    size="lg"
                    rows={15}
                    name="note_description"
                    aria-describedby="noteDescriptionHelper"
                    placeholder={t('note_form_content_placeholder')}
                    value={noteContent}
                    onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => {
                      setNoteContent(e.target.value);
                      hasUserEdited.current = true;
                      saveDraft(noteTitle, e.target.value, noteUrl, selectedTags);
                    }}
                    data-testid="note-content-input-area"
                  />
                </Form.Group>

                <button
                  type="submit"
                  className="home-new-item task-note-btn mt-3"
                >
                  {t('note_form_submit')}
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

      <ModalMarkdown
        show={showPreviewMd}
        onHide={handleCloseModal}
        title={noteTitle}
        markdownText={noteContent}
        onSave={saveNote}
        saveButtonLabel={t('note_form_submit')}
      />
    </Container>
  );
}

export default NoteAdd;
