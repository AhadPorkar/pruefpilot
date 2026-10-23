/** Original, lightweight outline icon set. Decorative icons never replace accessible labels. */
const paths: Record<string, string> = {
  grid: '<rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/>',
  box: '<path d="m12 3 9 5v8l-9 5-9-5V8l9-5Z"/><path d="m3 8 9 5 9-5M12 13v8M7 5.8l10 5.4"/>',
  clipboard: '<rect x="5" y="4" width="14" height="18" rx="2"/><rect x="9" y="2" width="6" height="4" rx="1"/><path d="m8 13 3 3 5-6"/>',
  calendar: '<rect x="3" y="5" width="18" height="16" rx="3"/><path d="M7 2v6m10-6v6M3 11h18m-14 4h2m4 0h2m-8 3h2"/>',
  wrench: '<path d="M14 6a5 5 0 0 0-6 6L3 17a2.8 2.8 0 0 0 4 4l6-6a5 5 0 0 0 6-6l-3 3-4-4 3-3"/>',
  report: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6Z"/><path d="M14 2v6h6M8 12h8m-8 4h8"/>',
  layers: '<path d="m12 3 10 6-10 6L2 9l10-6Zm-10 11 10 6 10-6M2 18l10 6 10-6" transform="translate(0 -1) scale(1 .94)"/>',
  users: '<circle cx="9" cy="7" r="3"/><path d="M3 21v-3a6 6 0 0 1 12 0v3M17 4a3 3 0 0 1 0 6m2 5a5 5 0 0 1 2 4v2"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 6v6l4 2"/>',
  help: '<circle cx="12" cy="12" r="9"/><path d="M9.5 8a2.6 2.6 0 1 1 4 2.2c-1 .6-1.5 1-1.5 2.8m0 4h.01"/>',
  search: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/>',
  bell: '<path d="M18 8a6 6 0 0 0-12 0c0 7-3 8-3 9h18c0-1-3-2-3-9m-12 9h12m-8 3a2 2 0 0 0 4 0"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  arrow: '<path d="M4 12h16m-6-6 6 6-6 6"/>',
  back: '<path d="M20 12H4m6-6-6 6 6 6"/>',
  chevron: '<path d="m9 5 7 7-7 7"/>',
  down: '<path d="m6 9 6 6 6-6"/>',
  check: '<path d="m5 12 4 4 10-10"/>',
  shield: '<path d="m12 2 8 4v6c0 5-5 9-8 10-3-1-8-5-8-10V6l8-4Z"/><path d="m8 12 3 3 5-6"/>',
  alert: '<path d="m10 4-8 14a2 2 0 0 0 2 3h16a2 2 0 0 0 2-3L14 4a2.3 2.3 0 0 0-4 0Z"/><path d="M12 9v5m0 3h.01"/>',
  pin: '<path d="M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/>',
  qr: '<path d="M3 8V3h5m8 0h5v5m0 8v5h-5M8 21H3v-5"/><rect x="7" y="7" width="3" height="3"/><rect x="14" y="7" width="3" height="3"/><rect x="7" y="14" width="3" height="3"/><path d="M14 14h3v3h-3v3m6-6v6"/>',
  download: '<path d="M12 3v12m-5-5 5 5 5-5M4 16v5h16v-5"/>',
  upload: '<path d="M12 16V4m-5 5 5-5 5 5M4 16v5h16v-5"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 1v2m0 18v2M1 12h2m18 0h2M4 4l2 2m12 12 2 2M4 20l2-2M18 6l2-2"/>',
  moon: '<path d="M21 13.3A9 9 0 0 1 10.7 3 9 9 0 1 0 21 13.3Z"/>',
  close: '<path d="m6 6 12 12M6 18 18 6"/>',
  list: '<path d="M8 5h13M8 12h13M8 19h13M3 5h.01M3 12h.01M3 19h.01"/>',
  file: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6Z"/><path d="M14 2v6h6"/>',
  image: '<rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="8" cy="8" r="1.5"/><path d="m3 17 6-6 4 4 3-3 5 5"/>',
  logout: '<path d="M10 3H4v18h6m-2-9h13m-5-5 5 5-5 5"/>',
  menu: '<path d="M4 6h16M4 12h16M4 18h16"/>',
  external: '<path d="M14 3h7v7m0-7L10 14m0-11H3v18h18v-7"/>',
  play: '<path d="m8 4 12 8-12 8V4Z"/>',
  lock: '<rect x="4" y="10" width="16" height="11" rx="3"/><path d="M8 10V6a4 4 0 0 1 8 0v4m-4 4v3"/>',
  refresh: '<path d="M21 10a9 9 0 0 0-15-6L3 7m0-5v5h5M3 14a9 9 0 0 0 15 6l3-3m0 5v-5h-5"/>',
  settings: '<path d="M4 7h16M4 17h16"/><circle cx="8" cy="7" r="3"/><circle cx="16" cy="17" r="3"/>',
  copy: '<rect x="8" y="8" width="13" height="13" rx="2"/><path d="M16 8V3H3v13h5"/>',
  trash: '<path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7m4-7v7"/>',
  spark: '<path d="m12 2 3 7 7 3-7 3-3 7-3-7-7-3 7-3 3-7Z"/>',
};
export function icon(name: string, className = ''): string {
  return `<svg class="icon ${className}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">${paths[name] ?? paths.box}</svg>`;
}
export const logoMark = '<svg viewBox="0 0 40 40" fill="none" aria-hidden="true"><path d="M20 2 36 11v18L20 38 4 29V11L20 2Z" fill="currentColor"/><path d="m12 20 6 6 11-13" stroke="var(--logo-ink,#123f39)" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/></svg>';
export function assetArt(): string {
  return `<svg class="asset-art" viewBox="0 0 400 250" fill="none" aria-hidden="true"><defs><linearGradient id="metal" x1="70" y1="80" x2="330" y2="210" gradientUnits="userSpaceOnUse"><stop stop-color="#c8e9dd"/><stop offset="1" stop-color="#659b8b"/></linearGradient></defs><path d="m40 193 160-86 160 86-160 50-160-50Z" stroke="currentColor" opacity=".15"/><path d="m66 190 135-65 135 65-135 43-135-43Z" fill="currentColor" opacity=".07"/><path d="m94 152 115-53 105 38-115 55-105-40Z" fill="url(#metal)"/><path d="m94 152 105 40v14L94 165v-13Z" fill="#548d7e"/><path d="m199 192 115-55v13l-115 56v-14Z" fill="#87b6a4"/><path d="m113 153 33 13v-95l-33-12v94Z" fill="#78a895"/><path d="m146 166 14-8V65l-14 6v95Z" fill="#47786b"/><path d="m113 59 14-7 33 13-14 6-33-12Z" fill="#c8e3d4"/><path d="m266 144 30-15V41l-30 14v89Z" fill="#77ab96"/><path d="m254 139 12 5V55l-12-4v88Z" fill="#466f65"/><path d="m254 51 30-14 12 4-30 14-12-4Z" fill="#c8e3d4"/><path d="m145 94 110-51 1 13-110 51V94Z" fill="#9ac3ae"/><path d="m160 153 49-24 49 19-49 25-49-20Z" fill="#dbeade"/><path d="m160 153 49 20v11l-49-20v-11Z" fill="#709681"/><path d="m209 173 49-25v11l-49 25v-11Z" fill="#91b7a0"/><path d="M132 85v48m146-61v43" stroke="#d9f2df" stroke-width="2" opacity=".8"/><path d="m66 222 21-10m254-54 20-9M79 91l25 10m199-62 28-13" stroke="currentColor" opacity=".4" stroke-dasharray="3 4"/><circle cx="331" cy="26" r="4" fill="#b9ec9e"/><path d="M325 198h33m-16-16v32" stroke="currentColor" opacity=".25"/></svg>`;
}
