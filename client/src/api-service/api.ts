import { API_TOKEN } from '../app-constants/app-constants';

type UnauthorizedHandler = () => void;

let unauthorizedHandler: UnauthorizedHandler | undefined;

/**
 * Registers a callback invoked when an authenticated request is rejected with 401 or 403.
 * Pass `undefined` to unregister.
 *
 * @param {UnauthorizedHandler | undefined} handler The callback to invoke on auth failures.
 */
export function setUnauthorizedHandler(handler: UnauthorizedHandler | undefined): void {
  unauthorizedHandler = handler;
}

/**
 * Retrieves the API token from local storage.
 *
 * @returns {string} The API token if it exists in local storage, otherwise an empty string.
 */
function getToken(): string {
  const tokenState = localStorage.getItem(API_TOKEN);
  return tokenState ?? '';
}

/**
 * Retrieves all the headers for the app.
 *
 * @returns {Headers} the headers.
 */
function getHeaders(addAuth: boolean = true): Headers {
  const headers = new Headers();
  headers.append('Content-Type', 'application/json');

  if (addAuth) {
    headers.append('Authorization', `Bearer ${getToken()}`);
  }

  return headers;
}

function getRequestInit(method: string, payload: object | undefined, addAuth: boolean = true): RequestInit {
  const body: string | undefined = method !== 'GET' ? JSON.stringify(payload) : undefined;

  return {
    method: method,
    mode: 'cors',
    credentials: 'include',
    headers: getHeaders(addAuth),
    body
  };
};

/**
 * Handle errors for the AJAX HTTPS requests.
 *
 * @param {number} httpStatusCode The HTTP response status code.
 */
function handleError(httpStatusCode: number) {
  if (httpStatusCode === 500) {
    throw new Error('Internal Server Error!');
  }
  throw new Error('Unknown error');
}

async function handleResponse(response: Response, authenticated: boolean = true) {
  // Successful responses
  if (response?.ok) {
    const codesToIgnore: number[] = [204];
    if (codesToIgnore.includes(response.status)) {
      return;
    }
    return await response.json();
  }

  // Error responses
  if (response) {
    if (authenticated && (response.status === 401 || response.status === 403)) {
      unauthorizedHandler?.();
    }
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      const data = await response.json();
      if ('message' in data && typeof data.message === 'string') {
        throw new Error(data.message);
      }
      if ('fields' in data && Array.isArray(data.fields)) {
        const firstError = data.fields[0].fieldMessage as string;
        throw new Error(firstError);
      }
    }
    handleError(response.status);
  }
}

function isAddAuth(url: string): boolean {
  return !url.includes('sign-in') && !url.includes('sign-up');
}

const api = {
  getJSON: async (url: string) => {
    const response = await fetch(url, getRequestInit('GET', {}));
    return handleResponse(response);
  },

  getJSONNoAuth: async (url: string) => {
    const response = await fetch(url, getRequestInit('GET', {}, false));
    return handleResponse(response, false);
  },

  postJSON: async (url: string, payload: object) => {
    const response = await fetch(url, getRequestInit('POST', payload, isAddAuth(url)));
    return handleResponse(response, isAddAuth(url));
  },

  patchJSON: async (url: string, payload: object) => {
    const response = await fetch(url, getRequestInit('PATCH', payload));
    return handleResponse(response);
  },

  putJSON: async (url: string, payload: object) => {
    const response = await fetch(url, getRequestInit('PUT', payload, isAddAuth(url)));
    return handleResponse(response, isAddAuth(url));
  },

  deleteNoContent: async (url: string) => {
    const response = await fetch(url, getRequestInit('DELETE', {}));
    return handleResponse(response);
  }
};

export default api;
