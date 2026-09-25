import React from 'react';
import { Badge, Form, ListGroup } from 'react-bootstrap';
import { useTranslation } from 'react-i18next';

interface NoteFormProps {
  validated: boolean;
  title: string;
  body: string;
  url: string;
  selectedTags: string[];
  currentTag: string;
  tagSuggestions: string[];
  highlightedIndex: number;
  submitLabel: string;
  bodyInputRef: React.RefObject<HTMLTextAreaElement | null>;
  tagContainerRef: React.RefObject<HTMLDivElement | null>;
  onTitleChange: (value: string) => void;
  onBodyChange: (value: string) => void;
  onUrlChange: (value: string) => void;
  onCurrentTagChange: (value: string) => void;
  onTagKeyDown: (e: React.KeyboardEvent<HTMLInputElement>) => void;
  onTagFocus: () => void;
  onAddTag: (tag: string) => void;
  onRemoveTag: (tag: string) => void;
  onPreviewMarkdown: (e: React.MouseEvent<Element, MouseEvent>) => void;
  onSubmit: (e: React.SubmitEvent<HTMLFormElement>) => void;
  onCancel: () => void;
}

/**
 * Hybrid note form: a heading-styled title input on top, the note body in the
 * middle, and URL/tags inputs below. Label-less with subtle placeholders so
 * the form reads as a single document.
 *
 * @param {NoteFormProps} props - The form state and callbacks.
 * @returns {React.ReactNode} The rendered note form.
 */
function NoteForm({
  validated,
  title,
  body,
  url,
  selectedTags,
  currentTag,
  tagSuggestions,
  highlightedIndex,
  submitLabel,
  bodyInputRef,
  tagContainerRef,
  onTitleChange,
  onBodyChange,
  onUrlChange,
  onCurrentTagChange,
  onTagKeyDown,
  onTagFocus,
  onAddTag,
  onRemoveTag,
  onPreviewMarkdown,
  onSubmit,
  onCancel
}: NoteFormProps): React.ReactNode {
  const { t } = useTranslation();

  return (
    <Form
      noValidate
      validated={validated}
      onSubmit={onSubmit}
      autoComplete="off"
    >
      <Form.Group controlId="form_noteTitle">
        <Form.Control
          className="note-form-input note-form-title-input"
          type="text"
          required={true}
          maxLength={100}
          name="note_title"
          placeholder={t('note_form_untitled')}
          value={title}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => onTitleChange(e.target.value)}
          onKeyDown={(e: React.KeyboardEvent<HTMLInputElement>) => {
            if (e.key === 'Enter') {
              e.preventDefault();
              bodyInputRef.current?.focus();
            }
          }}
          data-testid="note-title-input"
        />
      </Form.Group>

      <Form.Group controlId="form_noteDescription">
        <div className="d-flex justify-content-end">
          <small>
            <a href="#" onClick={onPreviewMarkdown}>
              Preview Markdown
            </a>
          </small>
        </div>
        <Form.Control
          className="note-form-input note-content-input"
          as="textarea"
          rows={15}
          name="note_description"
          placeholder={t('note_form_content_placeholder')}
          value={body}
          ref={bodyInputRef}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => onBodyChange(e.target.value)}
          data-testid="note-content-input-area"
        />
      </Form.Group>

      <Form.Group controlId="form_noteUrl" className="mt-2">
        <Form.Control
          className="note-form-input note-form-meta-input"
          type="text"
          name="note_url"
          placeholder={t('note_form_url_placeholder')}
          value={url}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => onUrlChange(e.target.value)}
          data-testid="note-url-input"
        />
      </Form.Group>

      <Form.Group
        controlId="form_noteTags"
        className="mt-2"
        ref={tagContainerRef}
        style={{ position: 'relative' }}
      >
        <Form.Control
          className="note-form-input note-form-meta-input"
          type="text"
          name="note_tags"
          placeholder={t('note_form_tags_placeholder')}
          value={currentTag}
          autoComplete="off"
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => onCurrentTagChange(e.target.value)}
          onKeyDown={onTagKeyDown}
          onFocus={onTagFocus}
          data-testid="note-tags-input"
        />
        {selectedTags.length > 0 && (
          <div className="mb-2 d-flex flex-wrap gap-1 mt-2" data-testid="note-tags-preview">
            {selectedTags.map(tag => (
              <Badge
                key={tag}
                bg="warning"
                text="dark"
                className="p-2"
                style={{ cursor: 'pointer' }}
                onClick={() => onRemoveTag(tag)}
              >
                #
                {tag}
                {' '}
                &times;
              </Badge>
            ))}
          </div>
        )}
        {tagSuggestions.length > 0 && (
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
            {tagSuggestions.map((suggestion, index) => (
              <ListGroup.Item
                key={suggestion}
                action
                variant="warning"
                active={index === highlightedIndex}
                className="d-flex align-items-center gap-2"
                onMouseDown={(e: React.MouseEvent) => {
                  e.preventDefault();
                  onAddTag(suggestion);
                }}
              >
                <i className="bi bi-tag"></i>
                #
                {suggestion}
              </ListGroup.Item>
            ))}
          </ListGroup>
        )}
      </Form.Group>

      <div className="d-flex justify-content-end gap-2 mt-3">
        <button
          type="submit"
          className="home-new-item task-note-btn"
        >
          {submitLabel}
        </button>

        <button
          type="button"
          className="home-new-item-secondary task-note-btn"
          onClick={onCancel}
        >
          Cancel
        </button>
      </div>
    </Form>
  );
}

export default NoteForm;
