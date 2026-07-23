import React from 'react';
import { Col, Row } from 'react-bootstrap';
import './style.css';

interface Props {
  readonly tags?: string[];
  readonly lastUpdate: string;
  readonly taskOrNote: 'task' | 'note';
  readonly onClick?: (e: React.MouseEvent<Element, MouseEvent>) => void;
}

/**
 * Renders the TaskTag component, displaying tags and the last update time.
 *
 * @param {Props} props - The props for the component.
 * @param {string[]} [props.tags] - The tags for the task. If not provided or empty, defaults to '#untagged'.
 * @param {string} props.lastUpdate - The last update time for the task.
 * @returns {React.ReactNode} The rendered TaskTag component.
 */
function TaskTag(props: React.PropsWithChildren<Props>): React.ReactNode {
  const tagContent = props.tags && props.tags.length > 0
    ? props.tags.map(tag => `#${tag}`).join(' ')
    : '#untagged';

  return (
    <Row>
      <Col className="d-inline-block card-tag poppins-regular">
        {tagContent}
        {' '}
        {props.taskOrNote}
        {props.taskOrNote === 'note' && (
          <>
            {' '}
            <a
              href="#"
              onClick={props.onClick}
            >
              Open it
            </a>
          </>
        )}
      </Col>
      <Col className="d-inline-block card-tag ms-5 text-end poppins-regular">
        {props.lastUpdate}
      </Col>
    </Row>
  );
}

export default TaskTag;
