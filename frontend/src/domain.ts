import type { Asset, Finding, Role, Schedule, TemplateItem, Workspace } from './types.js';

export class DomainError extends Error {
  constructor(public readonly code: string, message = code) { super(message); this.name = 'DomainError'; }
}
export const escapeHtml = (value: unknown): string => String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]!);
export const csvCell = (value: unknown): string => {
  let text = String(value ?? '');
  if (/^[\s]*[=+@-]/.test(text) || /^[\t\r]/.test(text)) text = `'${text}`;
  return `"${text.replaceAll('"', '""')}"`;
};
export function addDays(date: string, days: number): string {
  const parsed = new Date(`${date}T12:00:00Z`);
  if (!Number.isFinite(parsed.getTime()) || !Number.isInteger(days)) throw new DomainError('validation');
  parsed.setUTCDate(parsed.getUTCDate() + days);
  return parsed.toISOString().slice(0, 10);
}
export function dayDistance(start: string, end: string): number {
  return Math.round((Date.parse(`${end}T12:00:00Z`) - Date.parse(`${start}T12:00:00Z`)) / 86400000);
}
export function nextDue(scheduled: string, interval: number, performed: string): string {
  if (!Number.isInteger(interval) || interval < 1 || interval > 3650) throw new DomainError('invalidInterval');
  return addDays(scheduled, Math.max(1, Math.floor(dayDistance(scheduled, performed) / interval) + 1) * interval);
}
export function outcome(item: Pick<TemplateItem, 'kind' | 'required' | 'minValue' | 'maxValue'>, value: string, note: string): 'PASS' | 'FAIL' | 'NA' {
  value = value.trim(); note = note.trim();
  if (value.length > 2000 || note.length > 2000) throw new DomainError('validation');
  if (!value) { if (item.required) throw new DomainError('answerRequired'); return 'NA'; }
  let result: 'PASS' | 'FAIL' | 'NA';
  if (item.kind === 'PASS_FAIL') {
    if (!['PASS', 'FAIL', 'NA'].includes(value)) throw new DomainError('validation');
    result = value as typeof result;
    if (result === 'NA' && note.length < 5) throw new DomainError('noteRequired');
  } else if (item.kind === 'NUMBER') {
    if (!/^[+-]?(\d+(\.\d*)?|\.\d+)([eE][+-]?\d+)?$/.test(value)) throw new DomainError('invalidNumber');
    const number = Number(value);
    if (!Number.isFinite(number) || Math.abs(number) > 999999999999) throw new DomainError('invalidNumber');
    result = (item.minValue !== null && number < item.minValue) || (item.maxValue !== null && number > item.maxValue) ? 'FAIL' : 'PASS';
  } else result = 'PASS';
  if (result === 'FAIL' && note.length < 5) throw new DomainError('noteRequired');
  return result;
}
export function requireVersion(current: number, supplied: number): void { if (current !== supplied) throw new DomainError('conflict'); }
export function requireRole(role: Role, ...allowed: Role[]): void { if (!allowed.includes(role)) throw new DomainError('forbidden'); }
export function canManage(role: Role): boolean { return role === 'ADMIN' || role === 'SUPERVISOR'; }
export function canInspect(role: Role): boolean { return canManage(role) || role === 'INSPECTOR'; }
export function canExport(role: Role): boolean { return canManage(role) || role === 'VIEWER'; }
export function assertComplete(finding: Finding, note: string, evidenceCount: number, actorId: string): void {
  if (finding.status !== 'IN_PROGRESS') throw new DomainError('invalidTransition');
  if (finding.assigneeId !== actorId) throw new DomainError('forbidden');
  if (note.trim().length < 10 || note.length > 4000) throw new DomainError('repairRequired');
  if (!evidenceCount) throw new DomainError('evidenceRequired');
}
export function assertVerify(finding: Finding, actorId: string, note: string, evidenceCount: number): void {
  if (finding.status !== 'AWAITING_VERIFICATION') throw new DomainError('invalidTransition');
  if (finding.completedBy === actorId) throw new DomainError('independentReview');
  if (note.trim().length < 5) throw new DomainError('noteRequired');
  if (!evidenceCount) throw new DomainError('evidenceRequired');
}
export function assetStatus(asset: Asset, state: Workspace): 'ARCHIVED' | 'ACTION_NEEDED' | 'OVERDUE' | 'DUE_SOON' | 'ON_TRACK' | 'UNSCHEDULED' {
  if (asset.archived) return 'ARCHIVED';
  if (state.findings.some(f => f.assetId === asset.id && f.status !== 'CLOSED')) return 'ACTION_NEEDED';
  const schedules = state.schedules.filter(s => s.assetId === asset.id && s.active);
  if (!schedules.length) return 'UNSCHEDULED';
  if (schedules.some(s => s.nextDue < state.today)) return 'OVERDUE';
  if (schedules.some(s => s.nextDue <= addDays(state.today, 7))) return 'DUE_SOON';
  return 'ON_TRACK';
}
export function dueSchedules(state: Workspace, siteId = ''): Schedule[] {
  const ids = new Set(state.assets.filter(a => !a.archived && (!siteId || a.siteId === siteId)).map(a => a.id));
  return state.schedules.filter(s => s.active && ids.has(s.assetId)).sort((a, b) => a.nextDue.localeCompare(b.nextDue));
}
export function filteredAssets(state: Workspace, query: string, siteId: string, category: string): Asset[] {
  query = query.toLocaleLowerCase().trim();
  return state.assets.filter(a => (!siteId || a.siteId === siteId) && (!category || a.category === category) &&
    `${a.name} ${a.code} ${a.serialNumber} ${a.category}`.toLocaleLowerCase().includes(query));
}
export function initials(name: string): string { return name.split(/\s+/).filter(Boolean).slice(0, 2).map(n => n[0]!.toUpperCase()).join(''); }
export function fileMime(bytes: Uint8Array): string {
  if (!bytes.length || bytes.length > 10 * 1024 * 1024) throw new DomainError('fileSize');
  const starts = (prefix: number[]): boolean => prefix.every((n, i) => bytes[i] === n);
  if (starts([137, 80, 78, 71, 13, 10, 26, 10])) return 'image/png';
  if (starts([255, 216, 255])) return 'image/jpeg';
  if (starts([37, 80, 68, 70, 45])) return 'application/pdf';
  throw new DomainError('fileType');
}
export async function sha256(bytes: Uint8Array): Promise<string> {
  const buffer = new Uint8Array(bytes).buffer;
  return [...new Uint8Array(await crypto.subtle.digest('SHA-256', buffer))].map(b => b.toString(16).padStart(2, '0')).join('');
}
export function safeFilename(name: string, mime: string): string {
  let safe = (name.split(/[\\/]/).pop() || 'evidence').replace(/[^a-zA-Z0-9._ -]/g, '_').replace(/^[. ]+/, '').slice(0, 140) || 'evidence';
  const extension = mime === 'image/png' ? '.png' : mime === 'image/jpeg' ? '.jpg' : '.pdf';
  if (!safe.toLowerCase().endsWith(extension)) safe += extension;
  return safe;
}
