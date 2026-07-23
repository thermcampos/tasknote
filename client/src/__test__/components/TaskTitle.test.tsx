import React from 'react';
import { render } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import TaskTitle from '../../components/TaskTitle';

describe('TaskTitle Component', () => {
  it('should render the TaskTitle component with high priority and completed', () => {
    const { getByText, container } = render(<TaskTitle title="Test Task" highPriority={true} completed={true} />);
    expect(container.querySelector('.task-title-icon')).toBeDefined();
    expect(container.querySelector('.text-strike')).toBeDefined();
    expect(getByText('Test Task')).toBeDefined();
    expect(container.querySelector('svg')).toBeDefined(); // Check2Circle icon
  });

  it('should render the TaskTitle component with high priority and not completed', () => {
    const { getByText, container } = render(<TaskTitle title="Test Task" highPriority={true} completed={false} />);
    expect(container.querySelector('.task-title-icon')).toBeDefined();
    expect(container.querySelector('.text-strike')).toBeNull();
    expect(getByText('Test Task')).toBeDefined();
    expect(container.querySelector('svg')).toBeDefined(); // Bell icon
  });

  it('should render the TaskTitle component with not high priority and completed', () => {
    const { getByText, container } = render(<TaskTitle title="Test Task" highPriority={false} completed={true} />);
    expect(container.querySelector('.task-title-icon')).toBeDefined();
    expect(container.querySelector('.text-strike')).toBeDefined();
    expect(getByText('Test Task')).toBeDefined();
    expect(container.querySelector('svg')).toBeDefined(); // Check2Circle icon
  });

  it('should render the TaskTitle component with not high priority and not completed', () => {
    const { getByText, container } = render(<TaskTitle title="Test Task" highPriority={false} completed={false} />);
    expect(container.querySelector('.task-title-icon')).toBeDefined();
    expect(container.querySelector('.text-strike')).toBeNull();
    expect(getByText('Test Task')).toBeDefined();
    expect(container.querySelector('svg')).toBeNull(); // No icon
  });
});
