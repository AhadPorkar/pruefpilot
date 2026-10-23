import { DomainError } from './domain.js';
export class ApiError extends DomainError {
    status;
    constructor(status, code) {
        super(code);
        this.status = status;
    }
}
/** Same-origin, HttpOnly session authentication. The CSRF token exists in memory only. */
export class HttpApi {
    preview = false;
    token = '';
    tokenHeader = 'X-CSRF-TOKEN';
    async csrf() {
        const response = await this.fetch('/auth/csrf');
        const data = await response.json();
        this.token = data.token;
        this.tokenHeader = data.headerName;
    }
    async fetch(path, init = {}) {
        let response;
        try {
            response = await fetch(`/api${path}`, { ...init, credentials: 'same-origin', cache: 'no-store' });
        }
        catch {
            throw new ApiError(0, 'networkError');
        }
        if (!response.ok) {
            const data = await response.json().catch(() => ({}));
            throw new ApiError(response.status, data.code ?? (response.status === 401 ? 'unauthenticated' : 'serverError'));
        }
        return response;
    }
    async get(path) { return await (await this.fetch(path)).json(); }
    async send(method, path, body) {
        if (!this.token)
            await this.csrf();
        const response = await this.fetch(path, { method, headers: { 'Content-Type': 'application/json', [this.tokenHeader]: this.token }, body: body === undefined ? undefined : JSON.stringify(body) });
        return (response.status === 204 ? undefined : await response.json());
    }
    async upload(path, file) {
        if (!this.token)
            await this.csrf();
        const form = new FormData();
        form.set('file', file);
        const response = await this.fetch(path, { method: 'POST', headers: { [this.tokenHeader]: this.token }, body: form });
        return await response.json();
    }
    async login(email, password) {
        await this.csrf();
        await this.fetch('/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded', [this.tokenHeader]: this.token }, body: new URLSearchParams({ username: email, password }) });
        // Spring rotates the session and invalidates the previous CSRF token on authentication.
        this.token = '';
        await this.csrf();
    }
    async logout() { await this.send('POST', '/auth/logout'); this.token = ''; }
    async download(path) { return (await this.fetch(path)).blob(); }
}
//# sourceMappingURL=api.js.map