import type { Api, Evidence } from './types.js';
import { DomainError } from './domain.js';

export class ApiError extends DomainError {
  constructor(public readonly status: number, code: string) { super(code); }
}
/** Same-origin, HttpOnly session authentication. The CSRF token exists in memory only. */
export class HttpApi implements Api {
  readonly preview = false;
  private token = ''; private tokenHeader = 'X-CSRF-TOKEN';
  private async csrf(): Promise<void> {
    const response = await this.fetch('/auth/csrf');
    const data = await response.json() as { token: string; headerName: string };
    this.token = data.token; this.tokenHeader = data.headerName;
  }
  private async fetch(path: string, init: RequestInit = {}): Promise<Response> {
    let response: Response;
    try { response = await fetch(`/api${path}`, { ...init, credentials: 'same-origin', cache: 'no-store' }); }
    catch { throw new ApiError(0, 'networkError'); }
    if (!response.ok) {
      const data = await response.json().catch(() => ({})) as { code?: string };
      throw new ApiError(response.status, data.code ?? (response.status === 401 ? 'unauthenticated' : 'serverError'));
    }
    return response;
  }
  async get<T>(path: string): Promise<T> { return await (await this.fetch(path)).json() as T; }
  async send<T>(method: 'POST' | 'PUT' | 'PATCH', path: string, body?: unknown): Promise<T> {
    if (!this.token) await this.csrf();
    const response = await this.fetch(path, { method, headers: { 'Content-Type': 'application/json', [this.tokenHeader]: this.token }, body: body === undefined ? undefined : JSON.stringify(body) });
    return (response.status === 204 ? undefined : await response.json()) as T;
  }
  async upload(path: string, file: File): Promise<Evidence> {
    if (!this.token) await this.csrf();
    const form = new FormData(); form.set('file', file);
    const response = await this.fetch(path, { method: 'POST', headers: { [this.tokenHeader]: this.token }, body: form });
    return await response.json() as Evidence;
  }
  async login(email: string, password: string): Promise<void> {
    await this.csrf();
    await this.fetch('/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded', [this.tokenHeader]: this.token }, body: new URLSearchParams({ username: email, password }) });
    // Spring rotates the session and invalidates the previous CSRF token on authentication.
    this.token = ''; await this.csrf();
  }
  async logout(): Promise<void> { await this.send('POST', '/auth/logout'); this.token = ''; }
  async download(path: string): Promise<Blob> { return (await this.fetch(path)).blob(); }
}
