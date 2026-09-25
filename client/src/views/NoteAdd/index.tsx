import React, { useEffect, useRef, useState } from 'react';
import {
  Alert,
  Card,
  Col,
  Container,
  Row
} from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { NoteResponse } from '../../types/NoteResponse';
import api from '../../api-service/api';
import ApiConfig from '../../api-service/apiConfig';
import { translateServerResponse } from '../../utils/TranslatorUtils';
import { parseNoteDocument } from '../../utils/noteDocumentParser';
import ModalMarkdown from '../../components/ModalMarkdown';
import AlertError from '../../components/AlertError';
import ContentHeader from '../../components/ContentHeader';
import NoteForm from '../../components/NoteForm';

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
  const [title, setTitle] = useState<string>('');
  const [body, setBody] = useState<string>('');
  const [noteUrl, setNoteUrl] = useState<string>('');
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [currentTag, setCurrentTag] = useState<string>('');
  const [tags, setTags] = useState<string[]>([]);
  const [showTagDropdown, setShowTagDropdown] = useState<boolean>(false);
  const [highlightedIndex, setHighlightedIndex] = useState<number>(0);
  const [action, setAction] = useState<NoteAction>('add');
  const [showPreviewMd, setShowPreviewMd] = useState<boolean>(false);
  const [draftBanner, setDraftBanner] = useState<boolean>(false);
  const { i18n, t } = useTranslation();
  const params = useParams();
  const navigate = useNavigate();
  const bodyInputRef = useRef<HTMLTextAreaElement>(null);
  const tagContainerRef = useRef<HTMLDivElement>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const hasUserEdited = useRef<boolean>(false);

  const draftKey = params?.id ? `draft:note:edit:${params.id}` : 'draft:note:new';

  const tagSuggestions = showTagDropdown
    ? tags
        .filter(tag => tag.toLowerCase().includes(currentTag.trim().toLowerCase()))
        .filter(tag => !selectedTags.includes(tag))
        .slice(0, 8)
    : [];

  const loadTags = async (): Promise<void> => {
    try {
      const response: string[] = await api.getJSON(`${ApiConfig.homeUrl}/tasks/tags`);
      setTags(response.filter(tag => tag !== 'untagged'));
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
    setTitle('');
    setBody('');
    setNoteUrl('');
    setSelectedTags([]);
    setCurrentTag('');
    setShowTagDropdown(false);
    setAction('add');
    setValidated(false);
  };

  const saveDraft = (
    draftTitle: string,
    draftBody: string,
    draftUrl: string,
    draftTags: string[]
  ): void => {
    if (!hasUserEdited.current) return;
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      const draft: NoteDraft = {
        title: draftTitle,
        content: draftBody,
        noteUrl: draftUrl,
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
      const draft = JSON.parse(raw);
      let migrated: NoteDraft;
      if (Array.isArray(draft.tags)) {
        migrated = {
          title: typeof draft.title === 'string' ? draft.title : '',
          content: typeof draft.content === 'string' ? draft.content : '',
          noteUrl: typeof draft.noteUrl === 'string' ? draft.noteUrl : '',
          tags: draft.tags.filter((tag: unknown) => typeof tag === 'string')
        };
      }
      else {
        let content = typeof draft.content === 'string' ? draft.content : '';
        if (typeof draft.title === 'string' || typeof draft.noteUrl === 'string') {
          const headerLines = [typeof draft.title === 'string' ? draft.title : ''];
          if (typeof draft.noteUrl === 'string' && draft.noteUrl) {
            headerLines.push(`url: ${draft.noteUrl}`);
          }
          content = `${headerLines.join('\n')}\n\n${content}`;
        }
        const parsed = parseNoteDocument(content);
        migrated = {
          title: parsed.title,
          content: parsed.body,
          noteUrl: parsed.url,
          tags: parsed.tags
        };
      }
      localStorage.setItem(draftKey, JSON.stringify(migrated));
      setTitle(migrated.title);
      setBody(migrated.content);
      setNoteUrl(migrated.noteUrl);
      setSelectedTags(migrated.tags);
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

  const handleTitleChange = (value: string): void => {
    setTitle(value);
    hasUserEdited.current = true;
    saveDraft(value, body, noteUrl, selectedTags);
  };

  const handleBodyChange = (value: string): void => {
    setBody(value);
    hasUserEdited.current = true;
    saveDraft(title, value, noteUrl, selectedTags);
  };

  const handleUrlChange = (value: string): void => {
    setNoteUrl(value);
    hasUserEdited.current = true;
    saveDraft(title, body, value, selectedTags);
  };

  const handleCurrentTagChange = (value: string): void => {
    setCurrentTag(value);
    setShowTagDropdown(true);
    setHighlightedIndex(0);
  };

  const addTag = (tagName: string): void => {
    const normalized = tagName.trim().toLowerCase();
    let newTags = [...selectedTags];
    if (normalized && !selectedTags.includes(normalized)) {
      newTags = [...selectedTags, normalized];
      setSelectedTags(newTags);
    }
    hasUserEdited.current = true;
    saveDraft(title, body, noteUrl, newTags);
    setCurrentTag('');
    setShowTagDropdown(false);
    setHighlightedIndex(0);
  };

  const removeTag = (tagToRemove: string): void => {
    const newTags = selectedTags.filter(tag => tag !== tagToRemove);
    setSelectedTags(newTags);
    hasUserEdited.current = true;
    saveDraft(title, body, noteUrl, newTags);
  };

  const handleTagKeyDown = (e: React.KeyboardEvent<HTMLInputElement>): void => {
    if (e.key === 'Escape') {
      if (showTagDropdown) {
        e.preventDefault();
        setShowTagDropdown(false);
      }
      return;
    }
    if (e.key === 'Enter') {
      if (tagSuggestions.length > 0) {
        e.preventDefault();
        addTag(tagSuggestions[highlightedIndex]);
      }
      else if (currentTag.trim()) {
        e.preventDefault();
        addTag(currentTag);
      }
      return;
    }
    if (tagSuggestions.length === 0) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setHighlightedIndex(prev => (prev + 1) % tagSuggestions.length);
    }
    else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setHighlightedIndex(prev => (prev - 1 + tagSuggestions.length) % tagSuggestions.length);
    }
  };

  /**
   * Saves the note, either adding a new one or editing an existing one.
   *
   * @returns {Promise<boolean>} True if the note was saved successfully, false otherwise.
   */
  const saveNote = async (): Promise<boolean> => {
    setValidated(true);

    if (!title.trim()) {
      setErrorMessage(translateServerResponse('Please fill in all the fields', i18n.language));
      return false;
    }

    const finalTags = [...selectedTags];
    const pendingTag = currentTag.trim().toLowerCase();
    if (pendingTag && !finalTags.includes(pendingTag)) {
      finalTags.push(pendingTag);
    }

    const payload: NoteResponse = {
      id: action === 'edit' ? noteId : 0,
      title: title.trim(),
      description: body,
      url: noteUrl.trim(),
      tags: finalTags,
      lastUpdate: '',
      shared: false,
      shareToken: null,
      archived: false
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
    setTitle(noteData.title);
    setBody(noteData.description);
    setNoteUrl(noteData.url ?? '');
    setSelectedTags(noteData.tags ?? []);
  };

  /**
   * Display the Markdown text in Markdown format on a modal.
   *
   * @param {React.MouseEvent<Element, MouseEvent>} e The mouse click event.
   */
  const previewMarkdown = (e: React.MouseEvent<Element, MouseEvent>): void => {
    e.preventDefault();
    e.stopPropagation();
    setShowPreviewMd(title.length + body.length > 0);
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
              <Card.Title>{title.trim() || t('note_form_untitled')}</Card.Title>

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

              <NoteForm
                validated={validated}
                title={title}
                body={body}
                url={noteUrl}
                selectedTags={selectedTags}
                currentTag={currentTag}
                tagSuggestions={tagSuggestions}
                highlightedIndex={highlightedIndex}
                submitLabel={t('note_form_submit')}
                bodyInputRef={bodyInputRef}
                tagContainerRef={tagContainerRef}
                onTitleChange={handleTitleChange}
                onBodyChange={handleBodyChange}
                onUrlChange={handleUrlChange}
                onCurrentTagChange={handleCurrentTagChange}
                onTagKeyDown={handleTagKeyDown}
                onTagFocus={() => setShowTagDropdown(true)}
                onAddTag={addTag}
                onRemoveTag={removeTag}
                onPreviewMarkdown={previewMarkdown}
                onSubmit={handleSubmit}
                onCancel={() => {
                  clearDraft();
                  navigate('/home');
                }}
              />

            </Card.Body>
          </Card>
        </Col>
      </Row>

      <ModalMarkdown
        show={showPreviewMd}
        onHide={handleCloseModal}
        title={title.trim()}
        markdownText={body}
        tags={selectedTags}
        onSave={saveNote}
        saveButtonLabel={t('note_form_submit')}
      />
    </Container>
  );
}

export default NoteAdd;
