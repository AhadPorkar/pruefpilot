import { escapeHtml as h, initials } from './domain.js';
import { icon } from './icons.js';
export { h, icon };
export function button(label, action, id = '', variant = 'secondary', glyph = '') {
    return `<button type="button" class="btn btn-${h(variant)}" data-action="${h(action)}" data-id="${h(id)}">${glyph ? icon(glyph) : ''}<span>${h(label)}</span></button>`;
}
export function iconButton(label, action, glyph, id = '') {
    return `<button type="button" class="icon-btn" title="${h(label)}" aria-label="${h(label)}" data-action="${h(action)}" data-id="${h(id)}">${icon(glyph)}</button>`;
}
export function badge(value, t) { return `<span class="badge badge-${h(value.toLowerCase())}"><i></i>${h(t(value))}</span>`; }
export function avatar(name, size = '') { return `<span class="avatar ${h(size)}" aria-hidden="true">${h(initials(name))}</span>`; }
export function userName(state, id) { return state.users.find(u => u.id === id)?.displayName ?? '\u2014'; }
export function siteName(state, id) { return state.sites.find(s => s.id === id)?.name ?? '\u2014'; }
export function empty(title, hint = '', glyph = 'box') { return `<div class="empty">${icon(glyph)}<h3>${h(title)}</h3>${hint ? `<p>${h(hint)}</p>` : ''}</div>`; }
export function field(label, name, value = '', type = 'text', options = '') {
    return `<label class="field"><span>${h(label)}</span><input name="${h(name)}" type="${h(type)}" value="${h(value)}" ${options}></label>`;
}
export function area(label, name, value = '', options = '') { return `<label class="field"><span>${h(label)}</span><textarea name="${h(name)}" rows="3" ${options}>${h(value)}</textarea></label>`; }
export function select(label, name, choices, selected = '', options = '') {
    return `<label class="field"><span>${h(label)}</span><select name="${h(name)}" ${options}>${choices.map(([id, text]) => `<option value="${h(id)}" ${selected === id ? 'selected' : ''}>${h(text)}</option>`).join('')}</select></label>`;
}
export function notice(text, kind = 'info') { return `<div class="notice notice-${h(kind)}">${icon(kind === 'warning' ? 'alert' : 'shield')}<span>${h(text)}</span></div>`; }
//# sourceMappingURL=ui.js.map