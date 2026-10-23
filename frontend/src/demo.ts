import type { Api, Answer, Asset, Evidence, Finding, Inspection, Passport, Role, Schedule, Template, TemplateItem, User, Workspace } from './types.js';
import { assertComplete, assertVerify, csvCell, DomainError, escapeHtml, fileMime, nextDue, outcome, requireRole, requireVersion, safeFilename, sha256 } from './domain.js';
import { createDemoState } from './seed.js';
import { zipFiles } from './zip.js';
import type { ZipFile } from './zip.js';

type Body = Record<string, unknown>;
const text = (body: Body, key: string): string => String(body[key] ?? '').trim();
const uid = (): string => crypto.randomUUID();
const now = (): string => new Date().toISOString();
const decode = (base64: string): Uint8Array => Uint8Array.from(atob(base64), c => c.charCodeAt(0));
const requireState = (current: string, expected: string): void => { if (current !== expected) throw new DomainError('invalidTransition'); };
/** Explicit preview adapter. Never selected after a network failure, never shipped as a server authority.
 * Preview data is kept in this tab's memory and discarded on reload. */
export class DemoApi implements Api {
  readonly preview = true;
  private state: Workspace;
  constructor(state = createDemoState()) { this.state = structuredClone(state); }
  reset(): void { this.state = createDemoState(); }
  setRole(role: Role): void {
    const user = this.state.users.find(u => u.role === role && u.active !== false);
    if (!user) throw new DomainError('notFound'); this.state.user = user;
  }
  private asset(id: string): Asset { const row = this.state.assets.find(a => a.id === id); if (!row) throw new DomainError('notFound'); return row; }
  private inspection(id: string): Inspection { const row = this.state.inspections.find(a => a.id === id); if (!row) throw new DomainError('notFound'); return row; }
  private finding(id: string): Finding { const row = this.state.findings.find(a => a.id === id); if (!row) throw new DomainError('notFound'); return row; }
  private template(id: string): Template { const row = this.state.templates.find(a => a.id === id); if (!row) throw new DomainError('notFound'); return row; }
  private record(entityType: string, entityId: string, action: string, detail = ''): void {
    this.state.audit.unshift({ id: uid(), actorId: this.state.user.id, actorName: this.state.user.displayName, entityType, entityId, action, detail, occurredAt: now() });
  }
  private member(id: string): User {
    const row = this.state.users.find(u => u.id === id && u.active !== false);
    if (!row) throw new DomainError('notFound'); return row;
  }
  async get<T>(path: string): Promise<T> {
    const url = new URL(path, 'http://preview.invalid'), pieces = url.pathname.split('/').filter(Boolean);
    let result: unknown;
    if (pieces[0] === 'workspace') {
      for (const file of this.state.evidence) if (!file.sha256 && file.previewData) { file.sha256 = await sha256(decode(file.previewData)); file.size = decode(file.previewData).length; }
      result = this.state;
    } else if (pieces[0] === 'auth' && pieces[1] === 'me') result = this.state.user;
    else if (pieces[0] === 'members') { requireRole(this.state.user.role, 'ADMIN'); result = this.state.users; }
    else if (pieces[0] === 'audit') { const page = Number(url.searchParams.get('page') ?? 0); result = this.state.audit.slice(page * 100, (page + 1) * 100); }
    else if (pieces[0] === 'inspections' && pieces[1]) { const row = this.inspection(pieces[1]); result = { inspection: row, answers: this.state.answers.filter(a => a.inspectionId === row.id) }; }
    else if (pieces[0] === 'assets' && pieces[2] === 'passport') {
      const asset = this.asset(pieces[1]!), page = Math.max(0, Number(url.searchParams.get('page') ?? 0));
      const history = this.state.inspections.filter(i => i.assetId === asset.id).sort((a, b) => b.createdAt.localeCompare(a.createdAt));
      const inspections = history.slice(page * 100, (page + 1) * 100), ids = new Set(inspections.map(i => i.id));
      const findings = this.state.findings.filter(f => f.assetId === asset.id), evidence = this.state.evidence.filter(e => e.assetId === asset.id);
      const related = new Set([asset.id, ...history.map(i => i.id), ...findings.map(f => f.id), ...evidence.map(e => e.id)]);
      result = { asset, inspections, answers: this.state.answers.filter(a => ids.has(a.inspectionId)), findings, evidence, audit: this.state.audit.filter(a => related.has(a.entityId)), page, hasMore: history.length > (page + 1) * 100 } satisfies Passport;
    } else throw new DomainError('notFound');
    return structuredClone(result) as T;
  }
  async login(email: string, _password: string): Promise<void> {
    const user = this.state.users.find(u => u.email?.toLowerCase() === email.toLowerCase() && u.active !== false);
    if (!user) throw new DomainError('invalidCredentials'); this.state.user = user;
  }
  async logout(): Promise<void> { /* Preview has no real authentication session. */ }
  async send<T>(_method: 'POST' | 'PUT' | 'PATCH', path: string, input?: unknown): Promise<T> {
    // Snapshot rollback gives preview commands transactional behavior too.
    const snapshot = structuredClone(this.state);
    try { return structuredClone(this.mutate(path, (input ?? {}) as Body)) as T; }
    catch (error) { this.state = snapshot; throw error; }
  }
  private mutate(path: string, body: Body): unknown {
    const parts = path.split('/').filter(Boolean), [resource, id, action] = parts;
    const actor = this.state.user;
    if (resource === 'sites' && !id) {
      requireRole(actor.role, 'ADMIN', 'SUPERVISOR');
      const code = text(body, 'code').toUpperCase(); if (this.state.sites.some(s => s.code === code)) throw new DomainError('conflict');
      const row = { id: uid(), name: text(body, 'name'), code, address: text(body, 'address') };
      this.state.sites.push(row); this.record('SITE', row.id, 'SITE_CREATED', code); return row;
    }
    if (resource === 'assets' && !id) {
      requireRole(actor.role, 'ADMIN', 'SUPERVISOR');
      if (this.state.assets.filter(a => !a.archived).length >= 250) throw new DomainError('assetLimit');
      if (!this.state.sites.some(s => s.id === body.siteId)) throw new DomainError('notFound');
      const code = text(body, 'code').toUpperCase(); if (this.state.assets.some(a => a.code === code)) throw new DomainError('conflict');
      const row: Asset = { id: uid(), siteId: text(body, 'siteId'), name: text(body, 'name'), code, category: text(body, 'category'), manufacturer: text(body, 'manufacturer'), model: text(body, 'model'), serialNumber: text(body, 'serialNumber'), locationNote: text(body, 'locationNote'), criticality: text(body, 'criticality') as Asset['criticality'], archived: false, createdAt: now() };
      this.state.assets.push(row); this.record('ASSET', row.id, 'ASSET_CREATED', code); return row;
    }
    if (resource === 'assets' && id && action === 'archive') {
      requireRole(actor.role, 'ADMIN', 'SUPERVISOR'); const row = this.asset(id);
      if (this.state.schedules.some(s => s.assetId === id && s.active) || this.state.findings.some(f => f.assetId === id && f.status !== 'CLOSED') || this.state.inspections.some(i => i.assetId === id && ['DRAFT', 'SUBMITTED'].includes(i.status))) throw new DomainError('openWork');
      row.archived = true; this.record('ASSET', id, 'ASSET_ARCHIVED'); return;
    }
    if (resource === 'templates' && !id) {
      requireRole(actor.role, 'ADMIN', 'SUPERVISOR');
      const items = body.items as Omit<TemplateItem, 'id' | 'templateId' | 'position'>[];
      if (!items?.length || items.length > 100) throw new DomainError('validation');
      if (items.some(i => i.minValue !== null && i.maxValue !== null && i.minValue > i.maxValue)) throw new DomainError('validation');
      const row: Template = { id: uid(), name: text(body, 'name'), nameDe: text(body, 'nameDe'), description: text(body, 'description'), version: Number(body.version), status: 'DRAFT', reviewedBy: null, reviewedAt: null, createdAt: now() };
      this.state.templates.unshift(row); this.state.templateItems.push(...items.map((item, position) => ({ ...item, id: uid(), templateId: row.id, position })));
      this.record('TEMPLATE', row.id, 'TEMPLATE_CREATED', row.name); return row;
    }
    if (resource === 'templates' && id && action === 'review') {
      requireRole(actor.role, 'ADMIN', 'SUPERVISOR'); const row = this.template(id); requireState(row.status, 'DRAFT');
      if (text(body, 'note').length < 5) throw new DomainError('noteRequired');
      row.status = 'REVIEWED'; row.reviewedBy = actor.id; row.reviewedAt = now(); this.record('TEMPLATE', id, 'TEMPLATE_REVIEWED', text(body, 'note')); return;
    }
    if (resource === 'schedules' && !id) {
      requireRole(actor.role, 'ADMIN', 'SUPERVISOR'); const assetId = text(body, 'assetId'), templateId = text(body, 'templateId');
      if (this.asset(assetId).archived) throw new DomainError('archived'); requireState(this.template(templateId).status, 'REVIEWED'); this.member(text(body, 'ownerId'));
      const days = Number(body.intervalDays); if (!Number.isInteger(days) || days < 1 || days > 3650) throw new DomainError('invalidInterval');
      if (text(body, 'reason').length < 10) throw new DomainError('scheduleReason');
      if (this.state.schedules.some(s => s.assetId === assetId && s.templateId === templateId && s.active)) throw new DomainError('conflict');
      const row: Schedule = { id: uid(), assetId, templateId, ownerId: text(body, 'ownerId'), intervalDays: days, nextDue: text(body, 'nextDue'), reason: text(body, 'reason'), reviewedBy: actor.id, active: true, createdAt: now() };
      this.state.schedules.push(row); this.record('SCHEDULE', row.id, 'SCHEDULE_CREATED', row.reason); return row;
    }
    if (resource === 'schedules' && id && action === 'pause') {
      requireRole(actor.role, 'ADMIN', 'SUPERVISOR'); const row = this.state.schedules.find(s => s.id === id); if (!row) throw new DomainError('notFound');
      if (this.state.inspections.some(i => i.scheduleId === id && ['DRAFT', 'SUBMITTED'].includes(i.status))) throw new DomainError('openWork');
      row.active = false; this.record('SCHEDULE', id, 'SCHEDULE_PAUSED'); return;
    }
    if (resource === 'inspections' && !id) {
      requireRole(actor.role, 'ADMIN', 'SUPERVISOR', 'INSPECTOR');
      const assetId = text(body, 'assetId'), templateId = text(body, 'templateId'), scheduleId = body.scheduleId ? String(body.scheduleId) : null, requestKey = text(body, 'requestKey');
      const previous = this.state.inspections.find(i => i.requestKey === requestKey);
      if (previous) { if (previous.assetId !== assetId || previous.templateId !== templateId || previous.scheduleId !== scheduleId || previous.inspectorId !== actor.id) throw new DomainError('conflict'); return previous; }
      if (this.asset(assetId).archived) throw new DomainError('archived'); requireState(this.template(templateId).status, 'REVIEWED');
      const schedule = scheduleId ? this.state.schedules.find(s => s.id === scheduleId && s.assetId === assetId && s.templateId === templateId && s.active) : null;
      if (scheduleId && !schedule) throw new DomainError('validation');
      if (scheduleId && this.state.inspections.some(i => i.scheduleId === scheduleId && ['DRAFT', 'SUBMITTED'].includes(i.status))) throw new DomainError('conflict');
      const row: Inspection = { id: uid(), assetId, templateId, scheduleId, requestKey, inspectorId: actor.id, status: 'DRAFT', plannedDue: schedule?.nextDue ?? null, performedAt: null, submittedAt: null, reviewedBy: null, reviewNote: '', version: 0, createdAt: now() };
      this.state.inspections.unshift(row); this.record('INSPECTION', row.id, 'INSPECTION_STARTED'); return row;
    }
    if (resource === 'inspections' && id) {
      const row = this.inspection(id); requireVersion(row.version, Number(body.version));
      if (action === 'draft' || action === 'submit') {
        requireRole(actor.role, 'ADMIN', 'SUPERVISOR', 'INSPECTOR'); requireState(row.status, 'DRAFT'); if (row.inspectorId !== actor.id) throw new DomainError('forbidden');
        const items = this.state.templateItems.filter(i => i.templateId === row.templateId), answers = body.answers as Answer[];
        if (!Array.isArray(answers) || new Set(answers.map(a => a.itemId)).size !== answers.length || answers.some(a => !items.some(i => i.id === a.itemId))) throw new DomainError('validation');
        if (action === 'submit' && answers.length !== items.length) throw new DomainError('answerRequired');
        const validated = answers.map(a => ({ inspectionId: id, itemId: a.itemId, value: a.value.trim(), note: (a.note ?? '').trim(), outcome: action === 'draft' ? 'NA' as const : outcome(items.find(i => i.id === a.itemId)!, a.value, a.note ?? '') }));
        for (const answer of validated) {
          this.state.answers = this.state.answers.filter(a => !(a.inspectionId === id && a.itemId === answer.itemId)); this.state.answers.push(answer);
          if (action === 'submit' && answer.outcome === 'FAIL') {
            const item = items.find(i => i.id === answer.itemId)!;
            const finding: Finding = { id: uid(), assetId: row.assetId, inspectionId: id, itemId: item.id, title: item.label, description: answer.note, severity: 'MEDIUM', assigneeId: null, dueDate: null, status: 'OPEN', repairNote: '', completedBy: null, verifiedBy: null, verifyNote: '', resolvedAt: null, version: 0, createdAt: now() };
            this.state.findings.unshift(finding); this.record('FINDING', finding.id, 'FINDING_CREATED', item.label);
          }
        }
        row.version++; if (action === 'submit') { row.status = 'SUBMITTED'; row.performedAt = now(); row.submittedAt = now(); }
        this.record('INSPECTION', id, action === 'draft' ? 'DRAFT_SAVED' : 'INSPECTION_SUBMITTED'); return row;
      }
      if (action === 'review') {
        requireRole(actor.role, 'ADMIN', 'SUPERVISOR'); requireState(row.status, 'SUBMITTED');
        if (actor.id === row.inspectorId) throw new DomainError('independentReview'); if (text(body, 'note').length < 5) throw new DomainError('noteRequired');
        row.status = body.approved ? 'VERIFIED' : 'REJECTED'; row.reviewedBy = actor.id; row.reviewNote = text(body, 'note'); row.version++;
        if (body.approved && row.scheduleId) {
          const schedule = this.state.schedules.find(s => s.id === row.scheduleId)!;
          if (schedule.nextDue !== row.plannedDue) throw new DomainError('conflict');
          schedule.nextDue = nextDue(schedule.nextDue, schedule.intervalDays, row.performedAt!.slice(0, 10)); this.record('SCHEDULE', schedule.id, 'SCHEDULE_ADVANCED', schedule.nextDue);
        }
        this.record('INSPECTION', id, body.approved ? 'INSPECTION_VERIFIED' : 'INSPECTION_REJECTED', row.reviewNote); return row;
      }
    }
    if (resource === 'findings' && id) {
      const row = this.finding(id); requireVersion(row.version, Number(body.version));
      if (action === 'assign') {
        requireRole(actor.role, 'ADMIN', 'SUPERVISOR'); if (!['OPEN', 'IN_PROGRESS'].includes(row.status)) throw new DomainError('invalidTransition');
        const assignee = this.member(text(body, 'assigneeId')); requireRole(assignee.role, 'ADMIN', 'SUPERVISOR', 'TECHNICIAN');
        row.assigneeId = assignee.id; row.dueDate = text(body, 'dueDate'); row.severity = text(body, 'severity') as Finding['severity']; row.version++;
        this.record('FINDING', id, 'FINDING_ASSIGNED', assignee.displayName); return row;
      }
      if (action === 'start') {
        requireRole(actor.role, 'ADMIN', 'SUPERVISOR', 'TECHNICIAN'); if (row.assigneeId !== actor.id) throw new DomainError('forbidden'); requireState(row.status, 'OPEN');
        row.status = 'IN_PROGRESS'; row.version++; this.record('FINDING', id, 'REPAIR_STARTED'); return row;
      }
      const count = this.state.evidence.filter(e => e.findingId === id).length;
      if (action === 'complete') {
        requireRole(actor.role, 'ADMIN', 'SUPERVISOR', 'TECHNICIAN'); assertComplete(row, text(body, 'note'), count, actor.id);
        row.status = 'AWAITING_VERIFICATION'; row.repairNote = text(body, 'note'); row.completedBy = actor.id; row.version++;
        this.record('FINDING', id, 'REPAIR_DOCUMENTED', row.repairNote); return row;
      }
      if (action === 'verify') {
        requireRole(actor.role, 'ADMIN', 'SUPERVISOR'); assertVerify(row, actor.id, text(body, 'note'), count);
        row.status = body.approved ? 'CLOSED' : 'IN_PROGRESS'; row.verifiedBy = actor.id; row.verifyNote = text(body, 'note'); row.resolvedAt = body.approved ? now() : null; row.version++;
        this.record('FINDING', id, body.approved ? 'CLOSURE_VERIFIED' : 'REPAIR_REJECTED', row.verifyNote); return row;
      }
    }
    if (resource === 'members' && !id) {
      requireRole(actor.role, 'ADMIN'); const email = text(body, 'email').toLowerCase();
      if (this.state.users.some(u => u.email?.toLowerCase() === email)) throw new DomainError('conflict');
      if (text(body, 'password').length < 14 || new TextEncoder().encode(String(body.password)).length > 72) throw new DomainError('passwordPolicy');
      const row: User = { id: uid(), displayName: text(body, 'displayName'), email, role: text(body, 'role') as Role, active: true };
      this.state.users.push(row); this.record('MEMBER', row.id, 'MEMBER_CREATED', row.role); return row;
    }
    if (resource === 'members' && id) {
      requireRole(actor.role, 'ADMIN'); if (id === actor.id) throw new DomainError('selfDisable');
      const row = this.state.users.find(u => u.id === id); if (!row) throw new DomainError('notFound'); row.active = Boolean(body.active);
      this.record('MEMBER', id, row.active ? 'MEMBER_ENABLED' : 'MEMBER_DISABLED'); return;
    }
    if (resource === 'auth' && id === 'logout') return;
    if (resource === 'auth' && id === 'password') { throw new DomainError('noPermission'); }
    throw new DomainError('notFound');
  }
  async upload(path: string, file: File): Promise<Evidence> {
    const [resource, parentId] = path.split('/').filter(Boolean), actor = this.state.user;
    let assetId: string, inspectionId: string, findingId: string | null = null;
    if (resource === 'findings') {
      const finding = this.finding(parentId!); requireRole(actor.role, 'ADMIN', 'SUPERVISOR', 'TECHNICIAN'); requireState(finding.status, 'IN_PROGRESS');
      if (finding.assigneeId !== actor.id) throw new DomainError('forbidden'); assetId = finding.assetId; inspectionId = finding.inspectionId; findingId = finding.id;
    } else {
      const inspection = this.inspection(parentId!); requireRole(actor.role, 'ADMIN', 'SUPERVISOR', 'INSPECTOR'); requireState(inspection.status, 'DRAFT');
      if (inspection.inspectorId !== actor.id) throw new DomainError('forbidden'); assetId = inspection.assetId; inspectionId = inspection.id;
    }
    const bytes = new Uint8Array(await file.arrayBuffer()), mime = fileMime(bytes);
    const total = this.state.evidence.filter(e => e.assetId === assetId).reduce((n, e) => n + e.size, 0);
    if (total + bytes.length > 50 * 1024 * 1024) throw new DomainError('storageLimit');
    let binary = ''; for (let i = 0; i < bytes.length; i += 8192) binary += String.fromCharCode(...bytes.slice(i, i + 8192));
    const row: Evidence = { id: uid(), assetId, inspectionId, findingId, uploadedBy: actor.id, filename: safeFilename(file.name, mime), mime, size: bytes.length, sha256: await sha256(bytes), createdAt: now(), previewData: btoa(binary) };
    this.state.evidence.unshift(row); this.record('EVIDENCE', row.id, 'EVIDENCE_ATTACHED', row.filename); return structuredClone(row);
  }
  async download(path: string): Promise<Blob> {
    const url = new URL(path, 'http://preview.invalid'), pieces = url.pathname.split('/').filter(Boolean);
    if (pieces[0] === 'evidence') {
      const row = this.state.evidence.find(e => e.id === pieces[1]); if (!row?.previewData) throw new DomainError('notFound');
      return new Blob([new Uint8Array(decode(row.previewData)).buffer], { type: row.mime });
    }
    requireRole(this.state.user.role, 'ADMIN', 'SUPERVISOR', 'VIEWER');
    const asset = this.asset(pieces[1]!), de = url.searchParams.get('lang') === 'de';
    const inspections = this.state.inspections.filter(i => i.assetId === asset.id), ids = new Set(inspections.map(i => i.id));
    const answers = this.state.answers.filter(a => ids.has(a.inspectionId)), findings = this.state.findings.filter(f => f.assetId === asset.id), evidence = this.state.evidence.filter(e => e.assetId === asset.id);
    const encode = (text: string): Uint8Array => new TextEncoder().encode(text);
    const notice = de ? 'VORSCHAU: Fiktive Demonstrationsdaten. Kein Pruefzertifikat. Keine elektronische Signatur.' : 'PREVIEW: fictional demonstration data. Not a certificate or electronic signature.';
    const data = { schemaVersion: 1, generatedAt: now(), notice, asset, inspections, answers, findings, evidence: evidence.map(({ previewData: _, ...rest }) => rest) };
    const title = de ? 'Anlagenpass' : 'Asset passport';
    const rows = inspections.map(i => `<tr><td>${escapeHtml(i.id)}</td><td>${escapeHtml(i.status)}</td><td>${escapeHtml(i.performedAt)}</td><td>${escapeHtml(i.reviewNote)}</td></tr>`).join('');
    const html = `<!doctype html><html lang="${de ? 'de' : 'en'}"><meta charset="utf-8"><title>PruefPilot / ${escapeHtml(asset.code)}</title><style>body{font:15px system-ui;margin:50px;color:#123c36}table{border-collapse:collapse;width:100%}td,th{padding:12px;border-bottom:1px solid #ddd;text-align:left}aside{background:#fff1d4;padding:20px}</style><h1>PruefPilot / ${title}</h1><aside>${escapeHtml(notice)}</aside><h2>${escapeHtml(asset.name)}</h2><p>${escapeHtml(asset.code)}</p><table><tr><th>ID</th><th>Status</th><th>UTC</th><th>${de ? 'Kontrollvermerk' : 'Review note'}</th></tr>${rows}</table><p>${de ? 'Vollstaendige Datensaetze in data.json und den CSV-Dateien.' : 'Complete records are in data.json and the CSV files.'}</p></html>`;
    const files: ZipFile[] = [{ name: 'report.html', bytes: encode(html) }, { name: 'data.json', bytes: encode(JSON.stringify(data, null, 2)) }];
    const csv = (rows: Record<string, unknown>[], columns: string[]): Uint8Array => encode('\ufeff' + columns.join(',') + '\r\n' + rows.map(row => columns.map(c => csvCell(row[c])).join(',')).join('\r\n'));
    files.push({ name: 'inspections.csv', bytes: csv(inspections as unknown as Record<string, unknown>[], ['id', 'status', 'performedAt', 'reviewNote']) });
    files.push({ name: 'corrective-actions.csv', bytes: csv(findings as unknown as Record<string, unknown>[], ['id', 'title', 'severity', 'status', 'repairNote', 'verifyNote']) });
    files.push({ name: 'answers.csv', bytes: csv(answers as unknown as Record<string, unknown>[], ['inspectionId', 'itemId', 'value', 'note', 'outcome']) });
    for (const file of evidence) if (file.previewData) files.push({ name: `evidence/${file.id}_${file.filename}`, bytes: decode(file.previewData) });
    const entries = await Promise.all(files.map(async f => ({ path: f.name, bytes: f.bytes.length, sha256: await sha256(f.bytes) })));
    files.push({ name: 'manifest.json', bytes: encode(JSON.stringify({ algorithm: 'SHA-256', files: entries, notice: 'Unsigned manifest. Excludes itself.' }, null, 2)) });
    this.record('ASSET', asset.id, 'REPORT_EXPORTED', de ? 'de' : 'en'); return zipFiles(files);
  }
}
