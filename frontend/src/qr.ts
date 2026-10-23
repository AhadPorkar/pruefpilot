import { DomainError, escapeHtml } from './domain.js';

/** QR Model 2, version 5, error correction L, byte mode (106 UTF-8 bytes).
 * One Reed-Solomon block, all eight masks evaluated. No remote QR service or tracking.
 * The conservative fixed version keeps the implementation auditable and label size stable.
 */
export function qrMatrix(text: string): boolean[][] {
  const data = [...new TextEncoder().encode(text)];
  if (data.length > 106) throw new DomainError('qrTooLong');
  const size = 37, dataWords = 108, eccWords = 26;
  const bits: number[] = [];
  const append = (value: number, count: number): void => { for (let i = count - 1; i >= 0; i--) bits.push((value >>> i) & 1); };
  append(4, 4); append(data.length, 8); data.forEach(value => append(value, 8));
  append(0, Math.min(4, dataWords * 8 - bits.length));
  while (bits.length % 8) bits.push(0);
  const words: number[] = [];
  for (let i = 0; i < bits.length; i += 8) words.push(bits.slice(i, i + 8).reduce((n, b) => (n << 1) | b, 0));
  for (let pad = 0; words.length < dataWords; pad++) words.push(pad % 2 ? 0x11 : 0xec);
  const multiply = (x: number, y: number): number => {
    let result = 0;
    for (let i = 7; i >= 0; i--) { result = (result << 1) ^ ((result >>> 7) * 0x11d); result ^= ((y >>> i) & 1) * x; }
    return result;
  };
  const divisor: number[] = Array(eccWords).fill(0); divisor[eccWords - 1] = 1;
  let root = 1;
  for (let i = 0; i < eccWords; i++) {
    for (let j = 0; j < eccWords; j++) divisor[j] = multiply(divisor[j]!, root) ^ (j + 1 < eccWords ? divisor[j + 1]! : 0);
    root = multiply(root, 2);
  }
  const remainder: number[] = Array(eccWords).fill(0);
  for (const byte of words) {
    const factor = byte ^ remainder.shift()!; remainder.push(0);
    for (let i = 0; i < eccWords; i++) remainder[i] = remainder[i]! ^ multiply(divisor[i]!, factor);
  }
  const allBits = [...words, ...remainder].flatMap(byte => Array.from({ length: 8 }, (_, i) => (byte >>> (7 - i)) & 1));
  const matrix: boolean[][] = Array.from({ length: size }, () => Array<boolean>(size).fill(false));
  const reserved = matrix.map(row => [...row]);
  const set = (x: number, y: number, dark: boolean): void => {
    if (x >= 0 && y >= 0 && x < size && y < size) { matrix[y]![x] = dark; reserved[y]![x] = true; }
  };
  for (let i = 0; i < size; i++) { set(6, i, i % 2 === 0); set(i, 6, i % 2 === 0); }
  for (const [cx, cy] of [[3, 3], [size - 4, 3], [3, size - 4]] as const) {
    for (let dy = -4; dy <= 4; dy++) for (let dx = -4; dx <= 4; dx++) {
      const d = Math.max(Math.abs(dx), Math.abs(dy)); set(cx + dx, cy + dy, d !== 2 && d !== 4);
    }
  }
  for (let dy = -2; dy <= 2; dy++) for (let dx = -2; dx <= 2; dx++) set(30 + dx, 30 + dy, Math.max(Math.abs(dx), Math.abs(dy)) !== 1);
  const drawFormat = (target: boolean[][], mask: number, mark: boolean): void => {
    const data = (1 << 3) | mask; let rem = data;
    for (let i = 0; i < 10; i++) rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
    const format = ((data << 10) | rem) ^ 0x5412;
    const place = (x: number, y: number, bit: number): void => { target[y]![x] = !!bit; if (mark) reserved[y]![x] = true; };
    for (let i = 0; i <= 5; i++) place(8, i, (format >>> i) & 1);
    place(8, 7, (format >>> 6) & 1); place(8, 8, (format >>> 7) & 1); place(7, 8, (format >>> 8) & 1);
    for (let i = 9; i < 15; i++) place(14 - i, 8, (format >>> i) & 1);
    for (let i = 0; i < 8; i++) place(size - 1 - i, 8, (format >>> i) & 1);
    for (let i = 8; i < 15; i++) place(8, size - 15 + i, (format >>> i) & 1);
    place(8, size - 8, 1);
  };
  drawFormat(matrix, 0, true);
  const masks = [
    (x: number, y: number) => (x + y) % 2 === 0,
    (_x: number, y: number) => y % 2 === 0,
    (x: number, _y: number) => x % 3 === 0,
    (x: number, y: number) => (x + y) % 3 === 0,
    (x: number, y: number) => (Math.floor(x / 3) + Math.floor(y / 2)) % 2 === 0,
    (x: number, y: number) => (x * y) % 2 + (x * y) % 3 === 0,
    (x: number, y: number) => ((x * y) % 2 + (x * y) % 3) % 2 === 0,
    (x: number, y: number) => ((x + y) % 2 + (x * y) % 3) % 2 === 0,
  ];
  const penalty = (target: boolean[][]): number => {
    let score = 0, black = 0;
    for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) {
      if (target[y]![x]) black++;
      if (x && y && target[y]![x] === target[y - 1]![x] && target[y]![x] === target[y]![x - 1] && target[y]![x] === target[y - 1]![x - 1]) score += 3;
    }
    for (let direction = 0; direction < 2; direction++) for (let line = 0; line < size; line++) {
      const row = Array.from({ length: size }, (_, n) => direction ? target[n]![line]! : target[line]![n]!);
      let run = 1;
      for (let i = 1; i < size; i++) { if (row[i] === row[i - 1]) { run++; if (run === 5) score += 3; else if (run > 5) score++; } else run = 1; }
      const sequence = row.map(b => b ? '1' : '0').join('');
      for (let i = 0; i <= size - 11; i++) if (['00001011101', '10111010000'].includes(sequence.slice(i, i + 11))) score += 40;
    }
    return score + Math.floor(Math.abs(black * 100 / (size * size) - 50) / 5) * 10;
  };
  let best = matrix, bestPenalty = Infinity;
  for (let mask = 0; mask < 8; mask++) {
    const target = matrix.map(row => [...row]); let index = 0;
    for (let right = size - 1; right >= 1; right -= 2) {
      if (right === 6) right = 5;
      for (let vertical = 0; vertical < size; vertical++) for (let j = 0; j < 2; j++) {
        const x = right - j, upward = ((right + 1) & 2) === 0, y = upward ? size - 1 - vertical : vertical;
        if (!reserved[y]![x]) { const bit = allBits[index++] ?? 0; target[y]![x] = !!(bit ^ Number(masks[mask]!(x, y))); }
      }
    }
    drawFormat(target, mask, false);
    const score = penalty(target); if (score < bestPenalty) { best = target; bestPenalty = score; }
  }
  return best;
}
export function qrSvg(text: string, title = 'Asset QR code'): string {
  const matrix = qrMatrix(text), size = matrix.length + 8;
  const path = matrix.flatMap((row, y) => row.flatMap((dark, x) => dark ? [`M${x + 4},${y + 4}h1v1h-1z`] : [])).join('');
  return `<svg class="qr-code" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${size} ${size}" role="img" aria-label="${escapeHtml(title)}" shape-rendering="crispEdges"><title>${escapeHtml(title)}</title><path fill="#fff" d="M0 0h${size}v${size}H0z"/><path fill="#102f32" d="${path}"/></svg>`;
}
