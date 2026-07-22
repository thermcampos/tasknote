import React, { useState } from 'react';
import Button from 'react-bootstrap/Button';
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
            <Button
              variant="outline-secondary"
              onClick={handleHide}
              className="modal-action-btn"
            >
              Close
            </Button>
            <Button
              variant={showSource ? 'info' : 'outline-info'}
              onClick={handleToggleSource}
              data-testid="modal-source-button"
              className="modal-action-btn"
            >
              Source
            </Button>
            <Button
              variant="outline-primary"
              onClick={handleCopy}
              data-testid="modal-copy-button"
              className="modal-action-btn"
            >
              {copied ? 'Copied!' : 'Copy'}
            </Button>
            {props.onSave && (
              <Button
                variant="primary"
                onClick={async () => {
                  handleHide();
                  await props.onSave!();
                }}
                data-testid="modal-save-button"
                className="home-new-item modal-action-btn"
              >
                {props.saveButtonLabel ?? 'Save note'}
              </Button>
            )}
          </Modal.Footer>
        </Modal>
      )
    : null;
};

export default ModalMarkdown;
