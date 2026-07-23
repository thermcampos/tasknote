import React, { useState } from 'react';
import Modal from 'react-bootstrap/Modal';
import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import './style.css';

type Props = {
  show: boolean;
  title: string;
  markdownText: string;
  onHide: () => void;
  onSave?: () => Promise<boolean>;
  saveButtonLabel?: string;
};

/**
 * Renders a modal with Markdown format text.
 *
 * @param {Props} props The ModalMarkdown props with show, title and text.
 * @param {boolean} props.show Defines when to display the modal.
 * @param {string} props.title The modal title to be displayed.
 * @param {string} props.markdownText The Markdown text to be rendered.
 * @param {Function} props.onHide The function to be called when closing the modal.
 * @returns {React.ReactNode} the Markdown component rendered.
 */
const ModalMarkdown: React.FC<Props> = (props: Props): React.ReactNode => {
  const [showSource, setShowSource] = useState<boolean>(false);
  const [copied, setCopied] = useState<boolean>(false);

  const handleToggleSource = () => setShowSource(prev => !prev);

  const handleCopy = () => {
    navigator.clipboard.writeText(props.markdownText).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }).catch(() => {
      // clipboard write failed silently; no state change
    });
  };

  const handleHide = () => {
    setShowSource(false);
    setCopied(false);
    props.onHide();
  };

  return props.show
    ? (
        <Modal
          show={props.show}
          onHide={handleHide}
          backdrop="static"
          keyboard={false}
          size="xl"
          aria-labelledby="markdown-modal-content"
        >
          <Modal.Header closeButton>
            <Modal.Title
              id="markdown-modal-content"
              data-testid="modal-header-title"
            >
              {props.title === '' ? 'No title' : props.title}
            </Modal.Title>
          </Modal.Header>
          <Modal.Body className="markdown-modal">
            {showSource
              ? (
                  <pre className="markdown-source" data-testid="markdown-source-view">
                    {props.markdownText}
                  </pre>
                )
              : (
                  <Markdown remarkPlugins={[remarkGfm]}>{props.markdownText}</Markdown>
                )}
          </Modal.Body>
          <Modal.Footer className="d-flex flex-wrap gap-2 justify-content-end">
            <button
              type="button"
              onClick={handleHide}
              className="home-new-item-secondary task-note-btn"
            >
              Close
            </button>
            <button
              type="button"
              onClick={handleToggleSource}
              data-testid="modal-source-button"
              className={`${showSource ? 'home-new-item' : 'home-new-item-secondary'} task-note-btn`}
            >
              Source
            </button>
            <button
              type="button"
              onClick={handleCopy}
              data-testid="modal-copy-button"
              className="home-new-item-secondary task-note-btn"
            >
              {copied ? 'Copied!' : 'Copy'}
            </button>
            {props.onSave && (
              <button
                type="button"
                onClick={async () => {
                  handleHide();
                  await props.onSave!();
                }}
                data-testid="modal-save-button"
                className="home-new-item task-note-btn"
              >
                {props.saveButtonLabel ?? 'Save note'}
              </button>
            )}
          </Modal.Footer>
        </Modal>
      )
    : null;
};

export default ModalMarkdown;
