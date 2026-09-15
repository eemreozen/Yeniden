import { API_BASE_URL, REQUEST_TIMEOUT_MS } from './config';
import { ApiEnvelope, ApiError, ApiProblem } from './types';

let accessToken: string | null = null;
let refreshHandler: (() => Promise<boolean>) | null = null;
let refreshInFlight: Promise<boolean> | null = null;

export function setAccessToken(token: string | null): void { accessToken = token; }
export function setRefreshHandler(handler: (() => Promise<boolean>) | null): void { refreshHandler = handler; }

async function parseError(response: Response): Promise<ApiError> {
  let body: ApiProblem = {};
  try { body = await response.json(); } catch { /* non-json gateway failure */ }
  return new ApiError(response.status, body.code ?? 'request_failed', body.detail ?? body.message ?? body.title ?? 'İstek başarısız.');
}

export async function request<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
  try {
    const response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers, signal: controller.signal });
    const isRefreshRequest = path === '/auth/refresh';
    if (response.status === 401 && retry && !isRefreshRequest && refreshHandler) {
      refreshInFlight ??= refreshHandler().finally(() => { refreshInFlight = null; });
      if (await refreshInFlight) return request<T>(path, init, false);
    }
    if (!response.ok) throw await parseError(response);
    if (response.status === 204) return undefined as T;
    const envelope = await response.json() as ApiEnvelope<T>;
    return envelope && Object.prototype.hasOwnProperty.call(envelope, 'data') ? envelope.data : envelope as unknown as T;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    if ((error as { name?: string }).name === 'AbortError') throw new ApiError(408, 'timeout', 'Sunucu zaman aşımına uğradı.');
    throw new ApiError(0, 'network_error', 'Sunucuya bağlanılamadı.');
  } finally { clearTimeout(timeout); }
}

export const get = <T>(path: string) => request<T>(path);
export const post = <T>(path: string, body?: unknown, headers?: Record<string, string>) => request<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body), headers });
export const patch = <T>(path: string, body: unknown) => request<T>(path, { method: 'PATCH', body: JSON.stringify(body) });
