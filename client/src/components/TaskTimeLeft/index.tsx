import React from 'react';
import { OverlayTrigger, Tooltip } from 'react-bootstrap';
import { CalendarCheck } from 'react-bootstrap-icons';

interface Props {
  text: string;
  completed: boolean;
  tooltip: string;
}

/**
 * Renders the TaskTimeLeft component if the task is not completed, displaying
 * a calendar icon and the time left for the task.
 *
 * @param {Props} props - Props for the TaskTimeLeft component.
 * @param {string} props.text - The time left for the task.
 * @param {boolean} props.completed - Boolean value indicating if the task is completed.
 * @param {string} props.tooltip - The string representation of a Date instance.
 * @returns
 */
function TaskTimeLeft(props: React.PropsWithChildren<Props>): React.ReactNode | null {
  const tooltipDate = props.tooltip.length > 0
    ? new Intl.DateTimeFormat('en-US', {
        month: 'long',
        day: 'numeric',
        year: 'numeric'
      }).format(new Date(props.tooltip))
    : '';

  return props.completed
    ? null
    : (
        <div className="d-block task-due-date">
          <CalendarCheck />
          {props.tooltip.length > 0 && (
            <OverlayTrigger
              placement="right"
              overlay={(
                <Tooltip id="tooltip-right">
                  {tooltipDate}
                </Tooltip>
              )}
            >
              <span className="ms-2 poppins-regular">
                {props.text}
              </span>
            </OverlayTrigger>
          )}
          {props.tooltip.length === 0 && (
            <span className="ms-2 poppins-regular">
              {props.text}
            </span>
          )}
        </div>
      );
}

export default TaskTimeLeft;
