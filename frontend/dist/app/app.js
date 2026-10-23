import { HttpApi, ApiError } from './api.js';
import { DemoApi } from './demo.js';
import { DomainError, escapeHtml as h } from './domain.js';
import { translator } from './i18n.js';
import { dialogView, templateRow } from './dialogs.js';
import { loginView, pageView, pages, shell } from './views.js';
import { icon, logoMark } from './icons.js';
const storage = { get: (key) => { try {
        return localStorage.getItem(key);
    }
    catch {
        return null;
    } }, set: (key, value) => { try {
        localStorage.setItem(key, value);
    }
    catch { /* Preferences may be disabled. */ } } };
export class PruefPilot extends HTMLElement {
    api = new URLSearchParams(location.search).get('demo') === '1' ? new DemoApi() : new HttpApi();
    state;
    passport;
    language = storage.get('pp-language') === 'de' ? 'de' : 'en';
    route = 'overview';
    site = '';
    query = '';
    category = '';
    layout = 'grid';
    modalState;
    busy = false;
    auditPage = 0;
    stream;
    scannerTimer;
    toastTimer;
    requestSequence = 0;
    get t() { return translator(this.language); }
    context() { if (!this.state)
        throw new Error('Workspace is not loaded'); return { state: this.state, t: this.t, language: this.language, route: this.route, site: this.site, query: this.query, category: this.category, layout: this.layout, preview: this.api.preview, ...(this.passport ? { passport: this.passport } : {}) }; }
    connectedCallback() {
        document.documentElement.dataset.theme = storage.get('pp-theme') ?? 'light';
        document.documentElement.lang = this.language;
        this.addEventListener('click', e => void this.onClick(e));
        this.addEventListener('submit', e => void this.onSubmit(e));
        this.addEventListener('change', e => void this.onChange(e));
        this.addEventListener('input', e => this.onInput(e));
        window.addEventListener('hashchange', () => void this.navigate());
        window.addEventListener('keydown', e => { if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k' && this.state) {
            e.preventDefault();
            this.openModal('command');
        } if (e.key === 'Escape')
            document.body.classList.remove('nav-open'); });
        window.addEventListener('offline', () => this.toast(this.t('offline'), true));
        window.addEventListener('beforeunload', e => { if (this.modalState && this.modalDirty()) {
            e.preventDefault();
        } });
        this.loading();
        void this.load();
    }
    loading() { this.innerHTML = `<div class="loading-screen">${logoMark}<span>${h(this.t('loading'))}</span><div class="loader"></div></div>`; }
    render() { document.documentElement.lang = this.language; document.title = `${this.t(this.route.startsWith('asset/') ? 'passport' : this.route)} | Pr\u00fcfPilot`; this.innerHTML = this.state ? shell(this.context(), pageView(this.context())) : loginView(this.t, this.language); }
    async load() {
        try {
            this.state = await this.api.get('/workspace');
            await this.navigate();
        }
        catch (error) {
            if (error instanceof ApiError && error.status === 401) {
                this.state = undefined;
                this.render();
            }
            else {
                this.innerHTML = `<div class="loading-screen">${logoMark}<h1>${h(this.t('connectionFailed'))}</h1><p>${h(this.t('noSilentFallback'))}</p><button class="btn btn-primary" data-action="reload">${h(this.t('retry'))}</button><a href="?demo=1#/overview">${h(this.t('previewLink'))}</a></div>`;
            }
        }
    }
    async navigate() {
        this.closeModal();
        document.body.classList.remove('nav-open');
        this.route = location.hash.replace(/^#\//, '') || 'overview';
        if (!this.state)
            return;
        const sequence = ++this.requestSequence;
        try {
            this.passport = this.route.startsWith('asset/') ? await this.api.get(`/assets/${encodeURIComponent(this.route.split('/')[1] ?? '')}/passport?page=0`) : undefined;
            if (sequence !== this.requestSequence)
                return;
            if (this.passport)
                this.mergePassport(this.passport);
            this.render();
            window.scrollTo({ top: 0 });
        }
        catch (error) {
            this.route = 'assets';
            this.render();
            this.showError(error);
        }
    }
    mergePassport(p) { if (!this.state)
        return; for (const key of ['inspections', 'findings', 'evidence']) {
        const existing = this.state[key];
        const incoming = p[key];
        for (const item of incoming) {
            const idx = existing.findIndex(i => i.id === item.id);
            if (idx >= 0)
                existing[idx] = item;
            else
                existing.push(item);
        }
    } for (const answer of p.answers) {
        const idx = this.state.answers.findIndex(a => a.inspectionId === answer.inspectionId && a.itemId === answer.itemId);
        if (idx >= 0)
            this.state.answers[idx] = answer;
        else
            this.state.answers.push(answer);
    } }
    modalDirty() { return Boolean(this.querySelector('dialog [data-dirty="true"]')); }
    closeModal() { const modal = this.querySelector('#modal'); modal?.close(); this.modalState = undefined; this.stopCamera(); }
    openModal(kind, id = '', restore) {
        if (!this.state)
            return;
        this.modalState = { kind, id };
        const { title, body, wide } = dialogView(this.context(), this.modalState);
        const modal = this.querySelector('#modal');
        if (!modal)
            return;
        modal.className = `modal ${wide ? 'modal-wide' : ''}`;
        modal.innerHTML = `<header class="modal-header"><div><span class="eyebrow">Pr\u00fcfPilot</span><h2 id="modal-title">${h(title)}</h2></div><button type="button" class="icon-btn" data-action="close" aria-label="${h(this.t('close'))}">${icon('close')}</button></header><div class="modal-content">${body}</div>`;
        if (!modal.open)
            modal.showModal();
        modal.oncancel = () => { this.modalState = undefined; this.stopCamera(); };
        modal.onclick = e => { if (e.target === modal && !this.busy) {
            const rect = modal.getBoundingClientRect();
            if (e.clientX < rect.left || e.clientX > rect.right || e.clientY < rect.top || e.clientY > rect.bottom)
                this.closeModal();
        } };
        if (restore)
            modal.querySelectorAll('[name]').forEach(input => { const values = restore.get(input.name); if (!values)
                return; if (input instanceof HTMLInputElement && ['checkbox', 'radio'].includes(input.type))
                input.checked = values.includes(input.value);
            else
                input.value = values[0] ?? ''; });
        if (kind === 'command') {
            this.commandResults('');
            modal.querySelector('#command-query')?.focus();
        }
    }
    captureModal() { const map = new Map(); this.querySelectorAll('dialog form').forEach(form => { for (const [key, value] of new FormData(form))
        if (typeof value === 'string')
            map.set(key, [...(map.get(key) ?? []), value]); }); return map; }
    async refresh(keepModal = false, restore) { const modal = keepModal ? this.modalState : undefined; this.state = await this.api.get('/workspace'); if (this.route.startsWith('asset/')) {
        this.passport = await this.api.get(`/assets/${this.route.split('/')[1]}/passport?page=0`);
        this.mergePassport(this.passport);
    } this.render(); if (modal)
        this.openModal(modal.kind, modal.id, restore);
    else
        this.modalState = undefined; }
    showError(error, form) { const code = error instanceof DomainError ? error.code : 'serverError'; const message = this.t(code); if (form) {
        const box = form.querySelector('.form-error');
        if (box) {
            box.hidden = false;
            box.textContent = message;
            box.scrollIntoView({ block: 'nearest' });
            return;
        }
    } this.toast(message, true); }
    toast(message, error = false) { const toast = this.querySelector('#toast'); if (!toast)
        return; clearTimeout(this.toastTimer); toast.textContent = message; toast.className = `show ${error ? 'error' : ''}`; this.toastTimer = window.setTimeout(() => toast.className = '', 6000); }
    async run(task, form) { if (this.busy)
        return; this.busy = true; const buttons = [...(form ?? this).querySelectorAll('button')]; const enabled = buttons.filter(b => !b.disabled); enabled.forEach(b => b.disabled = true); this.setAttribute('aria-busy', 'true'); try {
        await task();
    }
    catch (error) {
        this.showError(error, form);
    }
    finally {
        this.busy = false;
        enabled.forEach(b => b.disabled = false);
        this.removeAttribute('aria-busy');
    } }
    async onClick(event) {
        const target = event.target.closest('[data-action]');
        if (!target)
            return;
        const action = target.dataset.action, id = target.dataset.id ?? '';
        if (action === 'close') {
            if (!this.busy)
                this.closeModal();
            return;
        }
        if (action === 'theme') {
            const value = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
            document.documentElement.dataset.theme = value;
            storage.set('pp-theme', value);
            return;
        }
        if (action === 'menu') {
            document.body.classList.toggle('nav-open');
            return;
        }
        if (action === 'language') {
            const restore = this.captureModal(), modal = this.modalState;
            this.language = this.language === 'en' ? 'de' : 'en';
            storage.set('pp-language', this.language);
            this.render();
            if (modal)
                this.openModal(modal.kind, modal.id, restore);
            return;
        }
        if (action === 'reload') {
            this.loading();
            await this.load();
            return;
        }
        if (!this.state)
            return;
        if (['new-site', 'new-asset', 'new-template', 'clone-template', 'new-inspection', 'new-schedule', 'new-member', 'template', 'finding', 'password', 'qr', 'command', 'scan'].includes(action)) {
            this.openModal(action, id);
            return;
        }
        if (action === 'inspection') {
            await this.run(async () => { const record = await this.api.get(`/inspections/${id}`); const idx = this.state.inspections.findIndex(i => i.id === id); if (idx >= 0)
                this.state.inspections[idx] = record.inspection;
            else
                this.state.inspections.push(record.inspection); this.state.answers = this.state.answers.filter(a => a.inspectionId !== id).concat(record.answers); this.openModal('inspection', id); });
            return;
        }
        if (action === 'add-item') {
            const container = this.querySelector('#template-items');
            if (container && container.children.length < 100)
                container.insertAdjacentHTML('beforeend', templateRow(this.context(), container.children.length));
            return;
        }
        if (action === 'remove-item') {
            const row = target.closest('[data-template-item]');
            if (row && row.parentElement.children.length > 1)
                row.remove();
            return;
        }
        if (action === 'grid' || action === 'table') {
            this.layout = action;
            this.render();
            return;
        }
        if (action === 'reviews') {
            location.hash = '#/inspections';
            return;
        }
        if (action === 'navigate') {
            this.closeModal();
            location.hash = `#/${id}`;
            return;
        }
        if (action === 'camera') {
            await this.run(() => this.startCamera());
            return;
        }
        if (action === 'print-qr') {
            document.body.classList.add('printing-qr');
            window.print();
            document.body.classList.remove('printing-qr');
            return;
        }
        await this.run(async () => {
            if (action === 'reset' && this.api instanceof DemoApi) {
                this.api.reset();
                await this.refresh();
                this.toast(this.t('previewReset'));
            }
            else if (action === 'logout') {
                await this.api.logout();
                this.closeModal();
                this.state = undefined;
                this.render();
            }
            else if (action === 'start-scheduled') {
                const schedule = this.state.schedules.find(s => s.id === id);
                if (!schedule)
                    return;
                const inspection = await this.api.send('POST', '/inspections', { assetId: schedule.assetId, templateId: schedule.templateId, scheduleId: id, requestKey: crypto.randomUUID() });
                await this.refresh();
                this.openModal('inspection', inspection.id);
            }
            else if (action === 'start-repair') {
                const f = this.state.findings.find(f => f.id === id);
                await this.api.send('POST', `/findings/${id}/start`, { version: f.version });
                await this.refresh(true);
                this.toast(this.t('saved'));
            }
            else if (action === 'pause' || action === 'archive') {
                if (!window.confirm(this.t(action === 'pause' ? 'pauseConfirm' : 'archiveConfirm')))
                    return;
                await this.api.send('POST', action === 'pause' ? `/schedules/${id}/pause` : `/assets/${id}/archive`);
                await this.refresh();
                this.toast(this.t('saved'));
            }
            else if (action === 'toggle-member') {
                const user = this.state.users.find(u => u.id === id);
                await this.api.send('PATCH', `/members/${id}`, { active: user.active === false });
                await this.refresh();
                this.toast(this.t('saved'));
            }
            else if (action === 'download-evidence') {
                const file = this.state.evidence.find(e => e.id === id);
                this.saveBlob(await this.api.download(`/evidence/${id}/download`), file?.filename ?? 'evidence');
            }
            else if (action === 'export')
                await this.export(id, this.language);
            else if (action === 'older-audit') {
                const next = await this.api.get(`/audit?page=${this.auditPage + 1}`);
                if (next.length) {
                    this.auditPage++;
                    this.state.audit.push(...next);
                    this.render();
                }
                else
                    this.toast(this.t('noActivity'));
            }
            else if (action === 'older-inspections' && this.passport) {
                const next = await this.api.get(`/assets/${id}/passport?page=${this.passport.page + 1}`);
                this.passport = { ...next, inspections: [...this.passport.inspections, ...next.inspections], answers: [...this.passport.answers, ...next.answers] };
                this.mergePassport(this.passport);
                this.render();
            }
        });
    }
    async onChange(event) {
        const input = event.target;
        if (input.dataset.upload && input instanceof HTMLInputElement) {
            const file = input.files?.[0];
            if (file) {
                const restore = this.captureModal();
                await this.run(async () => { await this.api.upload(input.dataset.upload, file); await this.refresh(true, restore); this.toast(this.t('saved')); });
            }
            return;
        }
        const kind = input.dataset.change;
        if (kind === 'site') {
            this.site = input.value;
            this.render();
        }
        if (kind === 'category') {
            this.category = input.value;
            this.render();
        }
        if (kind === 'role' && this.api instanceof DemoApi) {
            this.api.setRole(input.value);
            await this.refresh();
        }
        if (kind === 'item-kind') {
            const row = input.closest('[data-template-item]');
            const settings = row?.querySelector('.numeric-settings');
            if (settings)
                settings.hidden = input.value !== 'NUMBER';
        }
    }
    onInput(event) {
        const input = event.target;
        const form = input.closest('form');
        if (form)
            form.dataset.dirty = 'true';
        if (input.dataset.input === 'asset-search') {
            const position = input.selectionStart;
            this.query = input.value;
            this.render();
            const next = this.querySelector('[data-input="asset-search"]');
            next?.focus();
            if (position !== null)
                next?.setSelectionRange(position, position);
        }
        if (input.dataset.input === 'command')
            this.commandResults(input.value);
    }
    commandResults(query) { if (!this.state)
        return; query = query.trim().toLocaleLowerCase(); const matches = pages.filter(([key]) => this.t(key).toLocaleLowerCase().includes(query)).map(([key, glyph]) => `<button class="command-result" data-action="navigate" data-id="${key}">${icon(glyph)}<span>${h(this.t(key))}</span>${icon('arrow')}</button>`); matches.push(...this.state.assets.filter(a => `${a.code} ${a.name}`.toLocaleLowerCase().includes(query)).slice(0, 10).map(a => `<button class="command-result" data-action="navigate" data-id="asset/${a.id}">${icon('box')}<span><strong>${h(a.name)}</strong><small>${h(a.code)}</small></span>${icon('arrow')}</button>`)); const container = this.querySelector('#command-results'); if (container)
        container.innerHTML = matches.join('') || `<p>${h(this.t('noResults'))}</p>`; }
    async onSubmit(event) {
        event.preventDefault();
        const form = event.target;
        const data = new FormData(form);
        const get = (key) => String(data.get(key) ?? '').trim();
        const intent = event.submitter?.value ?? 'save';
        const kind = form.dataset.form, id = form.dataset.id ?? '';
        await this.run(async () => {
            if (form.id === 'login-form') {
                await this.api.login(get('email'), String(data.get('password') ?? ''));
                await this.load();
                return;
            }
            if (!this.state)
                return;
            if (form.id === 'export-form') {
                await this.export(get('assetId'), get('language'));
                return;
            }
            if (kind === 'lookup') {
                this.lookup(get('code'));
                return;
            }
            if (kind === 'new-site')
                await this.api.send('POST', '/sites', { name: get('name'), code: get('code'), address: get('address') });
            if (kind === 'new-asset')
                await this.api.send('POST', '/assets', { siteId: get('siteId'), code: get('code'), name: get('name'), category: get('category'), manufacturer: get('manufacturer'), model: get('model'), serialNumber: get('serialNumber'), locationNote: get('locationNote'), criticality: get('criticality') });
            if (kind === 'new-schedule')
                await this.api.send('POST', '/schedules', { assetId: get('assetId'), templateId: get('templateId'), ownerId: get('ownerId'), intervalDays: Number(get('intervalDays')), nextDue: get('nextDue'), reason: get('reason') });
            if (kind === 'new-template') {
                const items = [...form.querySelectorAll('[data-template-item]')].map(row => { const value = (name) => (row.querySelector(`[name="${name}"]`)?.value ?? '').trim(); const number = (name) => value(name) === '' ? null : Number(value(name)); return { label: value('label'), labelDe: value('labelDe'), kind: value('kind'), required: row.querySelector('[name="required"]').checked, minValue: value('kind') === 'NUMBER' ? number('minValue') : null, maxValue: value('kind') === 'NUMBER' ? number('maxValue') : null, unit: value('kind') === 'NUMBER' ? value('unit') : '' }; });
                await this.api.send('POST', '/templates', { name: get('name'), nameDe: get('nameDe'), description: get('description'), version: Number(get('version')), items });
            }
            if (kind === 'publish-template')
                await this.api.send('POST', `/templates/${id}/review`, { note: get('note') });
            if (kind === 'new-inspection') {
                const inspection = await this.api.send('POST', '/inspections', { assetId: get('assetId'), templateId: get('templateId'), scheduleId: null, requestKey: crypto.randomUUID() });
                await this.refresh();
                this.openModal('inspection', inspection.id);
                return;
            }
            if (kind === 'inspection') {
                const inspection = this.state.inspections.find(i => i.id === id);
                const items = this.state.templateItems.filter(i => i.templateId === inspection.templateId);
                const answers = items.map(item => ({ itemId: item.id, value: get(`value-${item.id}`), note: get(`note-${item.id}`) }));
                await this.api.send(intent === 'draft' ? 'PUT' : 'POST', `/inspections/${id}/${intent === 'draft' ? 'draft' : 'submit'}`, { version: inspection.version, answers });
                await this.refresh(intent === 'draft');
                this.toast(this.t(intent === 'draft' ? 'saved' : 'submitted'));
                return;
            }
            if (kind === 'review-inspection') {
                const inspection = this.state.inspections.find(i => i.id === id);
                await this.api.send('POST', `/inspections/${id}/review`, { version: inspection.version, approved: intent === 'approve', note: get('note') });
            }
            if (kind === 'assign-finding') {
                const f = this.state.findings.find(f => f.id === id);
                await this.api.send('POST', `/findings/${id}/assign`, { version: f.version, assigneeId: get('assigneeId'), dueDate: get('dueDate'), severity: get('severity') });
                await this.refresh(true);
                this.toast(this.t('saved'));
                return;
            }
            if (kind === 'complete-finding') {
                const f = this.state.findings.find(f => f.id === id);
                await this.api.send('POST', `/findings/${id}/complete`, { version: f.version, note: get('note') });
            }
            if (kind === 'verify-finding') {
                const f = this.state.findings.find(f => f.id === id);
                await this.api.send('POST', `/findings/${id}/verify`, { version: f.version, approved: intent === 'approve', note: get('note') });
            }
            if (kind === 'new-member')
                await this.api.send('POST', '/members', { email: get('email'), displayName: get('displayName'), role: get('role'), password: String(data.get('password') ?? '') });
            if (kind === 'password') {
                await this.api.send('POST', '/auth/password', { currentPassword: String(data.get('currentPassword') ?? ''), newPassword: String(data.get('newPassword') ?? '') });
                this.closeModal();
                this.state = undefined;
                this.render();
                this.toast(this.t('passwordChanged'));
                return;
            }
            await this.refresh();
            this.toast(this.t('saved'));
        }, form);
    }
    async export(id, language) { const asset = this.state?.assets.find(a => a.id === id); this.saveBlob(await this.api.download(`/assets/${id}/report?lang=${language}`), `PruefPilot-${asset?.code ?? 'report'}-${language}.zip`); this.toast(this.t('reportReady')); }
    saveBlob(blob, name) { const url = URL.createObjectURL(blob); const anchor = document.createElement('a'); anchor.href = url; anchor.download = name; anchor.click(); setTimeout(() => URL.revokeObjectURL(url), 30000); }
    lookup(raw) { if (!this.state)
        return; let code = raw.trim(); try {
        const url = new URL(code);
        if (url.hash.startsWith('#/asset/'))
            code = url.hash.slice(8);
    }
    catch { /* A printed code is also accepted. */ } const asset = this.state.assets.find(a => a.id === code || a.code.toLowerCase() === code.toLowerCase()); if (!asset)
        throw new DomainError('codeNotFound'); this.closeModal(); location.hash = `#/asset/${asset.id}`; }
    async startCamera() {
        const Detector = window.BarcodeDetector;
        if (!Detector || !navigator.mediaDevices)
            throw new DomainError('cameraUnavailable');
        this.stopCamera();
        this.stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: 'environment' } }, audio: false });
        const video = this.querySelector('#scanner-video');
        if (!video) {
            this.stopCamera();
            return;
        }
        video.hidden = false;
        video.srcObject = this.stream;
        await video.play();
        const detector = new Detector({ formats: ['qr_code'] });
        const detect = async () => { if (!this.stream)
            return; try {
            const codes = await detector.detect(video);
            if (codes[0]) {
                this.lookup(codes[0].rawValue);
                return;
            }
        }
        catch (error) {
            if (error instanceof DomainError)
                this.toast(this.t(error.code), true);
        } this.scannerTimer = window.setTimeout(() => void detect(), 500); };
        await detect();
    }
    stopCamera() { clearTimeout(this.scannerTimer); this.stream?.getTracks().forEach(track => track.stop()); this.stream = undefined; }
}
customElements.define('pruef-pilot', PruefPilot);
//# sourceMappingURL=app.js.map