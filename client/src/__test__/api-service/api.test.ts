// api.test.ts
import { describe, it, expect, vi, beforeEach } from 'vitest';
import api, { setUnauthorizedHandler } from '../../api-service/api';

function jsonErrorResponse(status: number): Response {
  return {
    ok: false,
    status,
    headers: new Headers({ 'content-type': 'application/json' }),
    json: async () => ({ message: 'Unauthorized' })
  } as unknown as Response;
}

describe('api unauthorized handling', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setUnauthorizedHandler(undefined);
  });

  it('invokes the unauthorized handler on 401 for authenticated requests', async () => {
    const handler = vi.fn();
    setUnauthorizedHandler(handler);
    vi.mocked(global.fetch).mockResolvedValue(jsonErrorResponse(401));

    await expect(api.getJSON('/api/rest/home')).rejects.toThrow('Unauthorized');

    expect(handler).toHaveBeenCalledTimes(1);
  });

  it('invokes the unauthorized handler on 403 for authenticated requests', async () => {
    const handler = vi.fn();
    setUnauthorizedHandler(handler);
    vi.mocked(global.fetch).mockResolvedValue(jsonErrorResponse(403));

    await expect(api.getJSON('/api/rest/home')).rejects.toThrow('Unauthorized');

    expect(handler).toHaveBeenCalledTimes(1);
  });

  it('does not invoke the handler for unauthenticated requests', async () => {
    const handler = vi.fn();
    setUnauthorizedHandler(handler);
    vi.mocked(global.fetch).mockResolvedValue(jsonErrorResponse(401));

    await expect(api.getJSONNoAuth('/api/public/notes/abc')).rejects.toThrow('Unauthorized');

    expect(handler).not.toHaveBeenCalled();
  });

  it('does not invoke the handler for sign-in failures', async () => {
    const handler = vi.fn();
    setUnauthorizedHandler(handler);
    vi.mocked(global.fetch).mockResolvedValue(jsonErrorResponse(401));

    await expect(api.postJSON('/api/auth/sign-in', {})).rejects.toThrow('Unauthorized');

    expect(handler).not.toHaveBeenCalled();
  });
});
