import React from 'react';
import { render } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import AlertError from '../../components/AlertError';

describe('AlertError Component tests', () => {
  const dataTestId: string = "alert-error-msg-tests";
  const renderAlert = (message?: string) => {
    return render(
      <AlertError errorMessage={message} dataTestId={dataTestId} />
    );
  };

  it('should display the message if error', () => {
    const errorMessage = "Oops, something went wrong";
    const { getByTestId } = renderAlert(errorMessage);

    expect(getByTestId(dataTestId)).toBeDefined();
  });

  // Personal notes: queryByTestId should be used to find elements
  // that possibly aren't in the document. getByTestId expect always
  // to find the element.
  it('should not display the message if no error', () => {
    const errorMessage = "";
    const { queryByTestId } = renderAlert(errorMessage);

    expect(queryByTestId(dataTestId)).toBeNull();
  });
});
