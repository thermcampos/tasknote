import React from 'react';
import ExternalLinkIcon from '../../assets/icons8-external-link-30.png';
import { isSafeUrl } from '../../utils/UrlUtils';
import './style.css';

interface Props {
  readonly title: string;
  readonly completed: boolean;
  readonly taskUrl: string[];
}

/**
 * Renders the TaskTitle component, displaying an icon and the task title.
 *
 * @param {Props} props - The props for the component.
 * @param {string} [props.title] - The title for the task.
 * @param {boolean} [props.completed] - Define if the task is completed.
 * @returns {React.ReactNode} The rendered TaskTitle component.
 */
function TaskTitle(props: React.PropsWithChildren<Props>): React.ReactNode {
  return (
    <span className="task-title-icon" data-testid={`task-title-container-${props.title}`}>
      <span
        className={`${props.completed ? 'ms-2 text-strike' : ''} poppins-semibold`}
        data-testid={`task-title-text-${props.title}`}
      >
        {props.title}
        {props.taskUrl && props.taskUrl.length > 0 && isSafeUrl(props.taskUrl[0]) && (
          <a href={props.taskUrl[0]} target="_blank" rel="noreferrer" className="task-note-external-link">
            <img src={ExternalLinkIcon} width={20} alt="external link" />
          </a>
        )}
      </span>
    </span>
  );
}

export default TaskTitle;
