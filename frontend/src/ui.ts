import { escapeHtml as h, initials } from './domain.js';
import { icon } from './icons.js';
import type { Translator } from './i18n.js';
import type { Workspace } from './types.js';
export { h, icon };
export function button(label: string, action: string, id = '', variant = 'secondary', glyph = ''): string {
  return `<button type="button" class="btn btn-${h(variant)}" data-action="${h(action)}" data-id="${h(id)}">${glyph ? icon(glyph) : ''}<span>${h(label)}</span></button>`;
}
export function iconButton(label: string, action: string, glyph: string, id = ''): string {
  return `<button type="button" class="icon-btn" title="${h(label)}" aria-label="${h(label)}" data-action="${h(action)}" data-id="${h(id)}">${icon(glyph)}</button>`;
}
export function badge(value: string, t: Translator): string { return `<span class="badge badge-${h(value.toLowerCase())}"><i></i>${h(t(value))}</span>`; }
export function avatar(name: string, size = ''): string { return `<span class="avatar ${h(size)}" aria-hidden="true">${h(initials(name))}</span>`; }
export function userName(state: Workspace, id: string | null): string { return state.users.find(u => u.id === id)?.displayName ?? '\u2014'; }
export function siteName(state: Workspace, id: string): string { return state.sites.find(s => s.id === id)?.name ?? '\u2014'; }
export function empty(title: string, hint = '', glyph = 'box'): string { return `<div class="empty">${icon(glyph)}<h3>${h(title)}</h3>${hint ? `<p>${h(hint)}</p>` : ''}</div>`; }
export function field(label: string, name: string, value: unknown = '', type = 'text', options = ''): string {
  return `<label class="field"><span>${h(label)}</span><input name="${h(name)}" type="${h(type)}" value="${h(value)}" ${options}></label>`;
}
export function area(label: string, name: string, value = '', options = ''): string { return `<label class="field"><span>${h(label)}</span><textarea name="${h(name)}" rows="3" ${options}>${h(value)}</textarea></label>`; }
export function select(label: string, name: string, choices: [string, string][], selected = '', options = ''): string {
  return `<label class="field"><span>${h(label)}</span><select name="${h(name)}" ${options}>${choices.map(([id, text]) => `<option value="${h(id)}" ${selected === id ? 'selected' : ''}>${h(text)}</option>`).join('')}</select></label>`;
}
export function notice(text: string, kind = 'info'): string { return `<div class="notice notice-${h(kind)}">${icon(kind === 'warning' ? 'alert' : 'shield')}<span>${h(text)}</span></div>`; }
