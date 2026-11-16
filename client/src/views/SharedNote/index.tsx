import React, { useEffect, useState } from 'react';
import { Card, Col, Container, Row } from 'react-bootstrap';
import { useParams } from 'react-router';
import MarkdownViewer from '../../components/MarkdownViewer';
import { NoteResponse } from '../../types/NoteResponse';
import api from '../../api-service/api';
import ApiConfig from '../../api-service/apiConfig';
import { isSafeUrl } from '../../utils/UrlUtils';

/**
 * SharedNote component for displaying a publicly shared note.
 * Accessible without authentication.
 *
 * @returns {React.ReactNode} The rendered SharedNote component.
 */
function SharedNote(): React.ReactNode {
  const { token } = useParams<{ token: string }>();
  const [note, setNote] = useState<NoteResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    if (!token) {
      setErrorMessage('Invalid share link.');
      setLoading(false);
      return;
    }

    api
      .getJSONNoAuth(`${ApiConfig.publicNotesUrl}/${token}`)
      .then((data: NoteResponse) => {
        setNote(data);
      })
      .catch(() => {
        setErrorMessage('Note not found or no longer shared.');
      })
      .finally(() => {
        setLoading(false);
      });
  }, [token]);

  if (loading) {
    return (
      <Container fluid className="mt-5 text-center">
        <p>Loading...</p>
      </Container>
    );
  }

  if (errorMessage || !note) {
    return (
      <Container fluid className="mt-5">
        <Row className="justify-content-center">
          <Col xs={12} md={8}>
            <Card>
              <Card.Body>
                <Card.Title>Note not found</Card.Title>
                <p className="text-muted">{errorMessage || 'This note is not available.'}</p>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      </Container>
    );
  }

  return (
    <Container fluid className="mt-3">
      <Row className="justify-content-center">
        <Col xs={12} md={10} lg={8}>
          <Card>
            <Card.Header className="d-flex justify-content-between align-items-center">
              <small className="text-muted">TaskNote · Shared Note (Read only)</small>
              {note.tags && note.tags.length > 0 && (
                <small className="text-muted">
                  {note.tags.map(t => `#${t}`).join(' ')}
                </small>
              )}
            </Card.Header>
            <Card.Body>
              <Card.Title>{note.title}</Card.Title>
              {isSafeUrl(note.url) && (
                <p>
                  <a href={note.url!} target="_blank" rel="noopener noreferrer">
                    {note.url}
                  </a>
                </p>
              )}
              <MarkdownViewer markdown={note.description} />
            </Card.Body>
            {note.lastUpdate && (
              <Card.Footer className="text-muted">
                <small>
                  Last updated:
                  {' '}
                  {note.lastUpdate}
                </small>
              </Card.Footer>
            )}
          </Card>
        </Col>
      </Row>
    </Container>
  );
}

export default SharedNote;
