import React, { useEffect, useRef, useState } from 'react';
import {
  Alert,
  Badge,
  Card,
  Col,
  Container,
  Form,
  ListGroup,
  Row
} from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router';
import { useTranslation } from 'react-i18next';
import { NoteResponse } from '../../types/NoteResponse';
import api from '../../api-service/api';
import ApiConfig from '../../api-service/apiConfig';
import { translateServerResponse } from '../../utils/TranslatorUtils';
import ModalMarkdown from '../../components/ModalMarkdown';
import AlertError from '../../components/AlertError';
import ContentHeader from '../../components/ContentHeader';

type NoteAction = 'add' | 'edit';

interface NoteDraft {
  content: string;
}

interface LegacyNoteDraft {
  title?: string;
  content: string;
  noteUrl?: string;
}

interface ParsedNoteDocument {
  title: string;
  url: string;
  body: string;
}

interface ParsedTagsFooter {
  tags: string[];
  bodyWithoutFooter: string;
  footerLineIndex: number;
}

interface FooterCaretContext {
  tokenStart: number;
  query: string;
}

const TAGS_FOOTER_PATTERN = /^(tags:\s*)(.*)$/i;

const parseTagsFooter = (content: string): ParsedTagsFooter | null => {
  const lines = content.split('\n');
  for (let i = lines.length - 1; i >= 0; i -= 1) {
    const line = lines[i].replace(/\r$/, '');
    if (line.trim() === '') continue;
    const match = line.match(TAGS_FOOTER_PATTERN);
    if (!match) return null;
    const tags = match[2]
      .split(',')
      .map(token => token.trim().toLowerCase())
      .filter((token, index, all) => token.length > 0 && all.indexOf(token) === index);
    const bodyWithoutFooter = lines.slice(0, i).join('\n').replace(/\s+$/, '');
    return { tags, bodyWithoutFooter, footerLineIndex: i };
  }
  return null;
};

const synthesizeTagsFooter = (content: string, tags: string[]): string => {
  if (parseTagsFooter(content) || tags.length === 0) return content;
  return `${content.replace(/\s+$/, '')}\n\ntags: ${tags.join(', ')}`;
};

const URL_LINE_PATTERN = /^url:\s*(.*)$/i;

const normalizeTitleLine = (line: string): string => line.replace(/^#+\s+/, '').trim();

const parseNoteDocument = (content: string): ParsedNoteDocument => {
  const lines = content.split('\n');
  const title = lines.length > 0 ? normalizeTitleLine(lines[0].replace(/\r$/, '')) : '';
  const footer = parseTagsFooter(content);
  const footerLineIndex = footer ? footer.footerLineIndex : lines.length;
  let url = '';
  let urlLineIndex = -1;
  for (let i = 1; i < footerLineIndex; i += 1) {
    const match = lines[i].replace(/\r$/, '').match(URL_LINE_PATTERN);
    if (match) {
      urlLineIndex = i;
      url = match[1].split(/[\s,]+/).filter(token => token.length > 0)[0] ?? '';
      break;
    }
  }
  const body = lines
    .slice(1, footerLineIndex)
    .filter((_, index) => index + 1 !== urlLineIndex)
    .join('\n')
    .replace(/^\s+/, '')
    .replace(/\s+$/, '');
  return { title, url, body };
};

const synthesizeNoteDocument = (noteData: NoteResponse): string => {
  const headerLines = [noteData.title];
  if (noteData.url) {
    headerLines.push(`url: ${noteData.url}`);
  }
  const body = synthesizeTagsFooter(noteData.description, noteData.tags ?? []);
  return `${headerLines.join('\n')}\n\n${body}`;
};

const getFooterCaretContext = (content: string, caret: number): FooterCaretContext | null => {
  const parsed = parseTagsFooter(content);
  if (!parsed) return null;
  const lines = content.split('\n');
  let lineStart = 0;
  for (let i = 0; i < parsed.footerLineIndex; i += 1) {
    lineStart += lines[i].length + 1;
  }
  const line = lines[parsed.footerLineIndex].replace(/\r$/, '');
  if (caret < lineStart || caret > lineStart + line.length) return null;
  const match = line.match(TAGS_FOOTER_PATTERN);
  if (!match) return null;
  const caretInLine = caret - lineStart;
  if (caretInLine < match[1].length) return null;
  const beforeCaret = line.substring(match[1].length, caretInLine);
  const tokenOffset = beforeCaret.lastIndexOf(',') + 1;
  return {
    tokenStart: lineStart + match[1].length + tokenOffset,
    query: beforeCaret.substring(tokenOffset).trim().toLowerCase()
  };
};

/**
 * NoteAdd component for adding and editing notes.
 *
 * @returns {React.ReactNode} The rendered NoteAdd component.
 */
function NoteAdd(): React.ReactNode {
  const [validated, setValidated] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [noteId, setNoteId] = useState<number>(0);
  const [noteContent, setNoteContent] = useState<string>('');
  const [tags, setTags] = useState<string[]>([]);
  const [footerCtx, setFooterCtx] = useState<FooterCaretContext | null>(null);
  const [showFooterDropdown, setShowFooterDropdown] = useState<boolean>(false);
  const [highlightedIndex, setHighlightedIndex] = useState<number>(0);
  const [action, setAction] = useState<NoteAction>('add');
  const [showPreviewMd, setShowPreviewMd] = useState<boolean>(false);
  const [draftBanner, setDraftBanner] = useState<boolean>(false);
  const { i18n, t } = useTranslation();
  const params = useParams();
  const navigate = useNavigate();
  const contentAreaRef = useRef<HTMLDivElement>(null);
  const contentInputRef = useRef<HTMLTextAreaElement>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const hasUserEdited = useRef<boolean>(false);

  const draftKey = params?.id ? `draft:note:edit:${params.id}` : 'draft:note:new';

  const parsedDocument = parseNoteDocument(noteContent);
  const parsedFooter = parseTagsFooter(noteContent);
  const footerTags = parsedFooter ? parsedFooter.tags : [];
  const footerSuggestions = showFooterDropdown && footerCtx
    ? tags
        .filter(tag => tag.toLowerCase().includes(footerCtx.query))
        .filter(tag => !footerTags.includes(tag.toLowerCase()))
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
    setNoteContent('');
    setFooterCtx(null);
    setShowFooterDropdown(false);
    setAction('add');
    setValidated(false);
  };

  const saveDraft = (content: string): void => {
    if (!hasUserEdited.current) return;
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      const draft: NoteDraft = { content };
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
      const draft: LegacyNoteDraft = JSON.parse(raw);
      let content = draft.content ?? '';
      if (typeof draft.title === 'string' || typeof draft.noteUrl === 'string') {
        const headerLines = [draft.title ?? ''];
        if (draft.noteUrl) {
          headerLines.push(`url: ${draft.noteUrl}`);
        }
        content = `${headerLines.join('\n')}\n\n${content}`;
        const migrated: NoteDraft = { content };
        localStorage.setItem(draftKey, JSON.stringify(migrated));
      }
      setNoteContent(content);
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

  const refreshFooterAutocomplete = (content: string, caret: number): void => {
    const ctx = getFooterCaretContext(content, caret);
    setFooterCtx(ctx);
    setShowFooterDropdown(ctx !== null);
    setHighlightedIndex(0);
  };

  const acceptSuggestion = (suggestion: string): void => {
    if (!footerCtx) return;
    const rest = noteContent.substring(footerCtx.tokenStart);
    const boundaries = [rest.indexOf(','), rest.indexOf('\n')].filter(index => index >= 0);
    const tokenEnd = boundaries.length > 0
      ? footerCtx.tokenStart + Math.min(...boundaries)
      : noteContent.length;
    const leadingWhitespace = noteContent
      .substring(footerCtx.tokenStart, tokenEnd)
      .match(/^\s*/)?.[0] ?? '';
    const replacement = `${leadingWhitespace}${suggestion}`;
    const newContent = noteContent.substring(0, footerCtx.tokenStart)
      + replacement
      + noteContent.substring(tokenEnd);
    const newCaret = footerCtx.tokenStart + replacement.length;
    hasUserEdited.current = true;
    setNoteContent(newContent);
    saveDraft(newContent);
    setTimeout(() => {
      if (contentInputRef.current) {
        contentInputRef.current.focus();
        contentInputRef.current.setSelectionRange(newCaret, newCaret);
        refreshFooterAutocomplete(newContent, newCaret);
      }
    }, 0);
  };

  const handleContentKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>): void => {
    if (e.key === 'Escape') {
      if (showFooterDropdown) {
        e.preventDefault();
        setShowFooterDropdown(false);
      }
      return;
    }
    if (!showFooterDropdown || footerSuggestions.length === 0) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setHighlightedIndex(prev => (prev + 1) % footerSuggestions.length);
    }
    else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setHighlightedIndex(prev => (prev - 1 + footerSuggestions.length) % footerSuggestions.length);
    }
    else if (e.key === 'Enter') {
      e.preventDefault();
      acceptSuggestion(footerSuggestions[highlightedIndex]);
    }
  };

  /**
   * Saves the note, either adding a new one or editing an existing one.
   *
   * @returns {Promise<boolean>} True if the note was saved successfully, false otherwise.
   */
  const saveNote = async (): Promise<boolean> => {
    setValidated(true);

    if (!parsedDocument.title || !noteContent.trim()) {
      setErrorMessage(translateServerResponse('Please fill in all the fields', i18n.language));
      return false;
    }

    const payload: NoteResponse = {
      id: action === 'edit' ? noteId : 0,
      title: parsedDocument.title,
      description: parsedDocument.body,
      url: parsedDocument.url,
      tags: parsedFooter?.tags ?? [],
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
    setNoteContent(synthesizeNoteDocument(noteData));
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
      if (contentAreaRef.current && !contentAreaRef.current.contains(event.target as Node)) {
        setShowFooterDropdown(false);
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
              <Card.Title>{parsedDocument.title || t('note_form_untitled')}</Card.Title>

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
                  <div ref={contentAreaRef} style={{ position: 'relative' }}>
                    <Form.Control
                      className="note-content-input"
                      as="textarea"
                      required={true}
                      size="lg"
                      rows={15}
                      name="note_description"
                      aria-describedby="noteDescriptionHelper"
                      placeholder={t('note_form_content_placeholder')}
                      value={noteContent}
                      ref={contentInputRef}
                      onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => {
                        setNoteContent(e.target.value);
                        hasUserEdited.current = true;
                        saveDraft(e.target.value);
                        refreshFooterAutocomplete(
                          e.target.value,
                          e.target.selectionStart ?? e.target.value.length
                        );
                      }}
                      onKeyDown={handleContentKeyDown}
                      onClick={(e: React.MouseEvent<HTMLTextAreaElement>) => {
                        refreshFooterAutocomplete(noteContent, e.currentTarget.selectionStart ?? 0);
                      }}
                      onKeyUp={(e: React.KeyboardEvent<HTMLTextAreaElement>) => {
                        const caretKeys = ['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Home', 'End'];
                        if (caretKeys.includes(e.key)) {
                          refreshFooterAutocomplete(noteContent, e.currentTarget.selectionStart ?? 0);
                        }
                      }}
                      data-testid="note-content-input-area"
                    />
                    {footerSuggestions.length > 0 && (
                      <ListGroup
                        data-testid="tag-suggestion-dropdown"
                        style={{
                          position: 'absolute',
                          zIndex: 1000,
                          width: '100%',
                          maxHeight: '200px',
                          overflowY: 'auto',
                          left: 0,
                          top: '100%'
                        }}
                      >
                        {footerSuggestions.map((suggestion, index) => (
                          <ListGroup.Item
                            key={suggestion}
                            action
                            variant="warning"
                            active={index === highlightedIndex}
                            className="d-flex align-items-center gap-2"
                            onMouseDown={(e: React.MouseEvent) => {
                              e.preventDefault();
                              acceptSuggestion(suggestion);
                            }}
                          >
                            <i className="bi bi-tag"></i>
                            #
                            {suggestion}
                          </ListGroup.Item>
                        ))}
                      </ListGroup>
                    )}
                  </div>
                  <Form.Text className="text-muted" id="noteDescriptionHelper">
                    {'The first line is the note title. To link a URL to this note, add a line `url: <url>` before the tags. Add a final line `tags: a, b` to tag this note.'}
                  </Form.Text>
                  {footerTags.length > 0 && (
                    <div className="mb-2 d-flex flex-wrap gap-1" data-testid="note-tags-preview">
                      {footerTags.map(tag => (
                        <Badge
                          key={tag}
                          bg="warning"
                          text="dark"
                          className="p-2 mt-3"
                        >
                          #
                          {tag}
                        </Badge>
                      ))}
                    </div>
                  )}
                </Form.Group>

                <div className="d-flex justify-content-end gap-2 mt-3">
                  <button
                    type="submit"
                    className="home-new-item task-note-btn"
                  >
                    {t('note_form_submit')}
                  </button>

                  <button
                    type="button"
                    className="home-new-item-secondary task-note-btn"
                    onClick={() => {
                      clearDraft();
                      navigate('/home');
                    }}
                  >
                    Cancel
                  </button>
                </div>
              </Form>

            </Card.Body>
          </Card>
        </Col>
      </Row>

      <ModalMarkdown
        show={showPreviewMd}
        onHide={handleCloseModal}
        title={parsedDocument.title}
        markdownText={parsedDocument.body}
        tags={footerTags}
        onSave={saveNote}
        saveButtonLabel={t('note_form_submit')}
      />
    </Container>
  );
}

export default NoteAdd;
