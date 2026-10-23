/** Small ZIP writer using the standard stored method. No compression library or network calls. */
export interface ZipFile { name: string; bytes: Uint8Array }
export function crc32(bytes: Uint8Array): number {
  let crc = 0xffffffff;
  for (const byte of bytes) { crc ^= byte; for (let i = 0; i < 8; i++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0); }
  return (crc ^ 0xffffffff) >>> 0;
}
export function zipFiles(files: ZipFile[]): Blob {
  const local: Uint8Array[] = [], central: Uint8Array[] = []; let offset = 0;
  for (const file of files) {
    if (file.name.startsWith('/') || file.name.split('/').includes('..')) throw new Error('Unsafe archive path');
    const name = new TextEncoder().encode(file.name), crc = crc32(file.bytes), size = file.bytes.length;
    const header = new Uint8Array(30 + name.length), h = new DataView(header.buffer);
    h.setUint32(0, 0x04034b50, true); h.setUint16(4, 20, true); h.setUint16(6, 0x800, true);
    h.setUint16(12, 33, true); h.setUint32(14, crc, true); h.setUint32(18, size, true); h.setUint32(22, size, true); h.setUint16(26, name.length, true); header.set(name, 30);
    const directory = new Uint8Array(46 + name.length), d = new DataView(directory.buffer);
    d.setUint32(0, 0x02014b50, true); d.setUint16(4, 20, true); d.setUint16(6, 20, true); d.setUint16(8, 0x800, true); d.setUint16(14, 33, true);
    d.setUint32(16, crc, true); d.setUint32(20, size, true); d.setUint32(24, size, true); d.setUint16(28, name.length, true); d.setUint32(42, offset, true); directory.set(name, 46);
    local.push(header, file.bytes); central.push(directory); offset += header.length + size;
  }
  const centralSize = central.reduce((total, bytes) => total + bytes.length, 0), end = new Uint8Array(22), e = new DataView(end.buffer);
  e.setUint32(0, 0x06054b50, true); e.setUint16(8, files.length, true); e.setUint16(10, files.length, true); e.setUint32(12, centralSize, true); e.setUint32(16, offset, true);
  return new Blob([...local, ...central, end].map(bytes => new Uint8Array(bytes).buffer), { type: 'application/zip' });
}
