/** Builds a human-readable instrument label including transposition when present. */
export function instrumentLabel(i: { name: string; displayName?: string; transposition?: string }): string {
  const name = i.displayName || i.name;
  return i.transposition ? `${name} (${i.transposition})` : name;
}

export function formatSize(bytes?: number): string {
  if (bytes == null || bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return (bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0) + ' ' + units[i];
}

/** Formats an ISO 8601 duration string (e.g. "PT3M30S") as "3:30" / "1:03:30". */
export function formatDuration(iso: unknown): string {
  if (!iso || typeof iso !== 'string') return '';
  const match = iso.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?/);
  if (!match) return iso;
  const h = parseInt(match[1] ?? '0', 10);
  const m = parseInt(match[2] ?? '0', 10);
  const s = Math.round(parseFloat(match[3] ?? '0'));
  if (h > 0) {
    return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }
  return `${m}:${String(s).padStart(2, '0')}`;
}
