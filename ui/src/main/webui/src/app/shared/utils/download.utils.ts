import { HttpResponse } from '@angular/common/http';

/**
 * The file name a `Content-Disposition` header asks for. The RFC 5987 form
 * (`filename*=utf-8''Kla%CC%88rinette%201.pdf`, sent for documents) wins over the plain one
 * (`filename="export.zip"`, sent for exports); `null` when the header names no file.
 */
export function filenameFromDisposition(disposition: string | null | undefined): string | null {
  if (!disposition) return null;
  const encoded = disposition.match(/filename\*\s*=\s*[^']*'[^']*'([^;]+)/i);
  if (encoded) {
    try {
      const name = decodeURIComponent(encoded[1].trim());
      if (name) return name;
    } catch {
      // malformed percent-encoding: fall back to the plain form
    }
  }
  const plain = disposition.match(/filename\s*=\s*(?:"([^"]*)"|([^;]+))/i);
  const name = (plain?.[1] ?? plain?.[2] ?? '').trim();
  return name || null;
}

/**
 * Saves a downloaded file under the name the server gave it, or `fallbackName` when the response
 * names none.
 */
export function saveDownload(response: HttpResponse<Blob>, fallbackName: string): void {
  const filename = filenameFromDisposition(response.headers.get('Content-Disposition')) ?? fallbackName;
  const url = URL.createObjectURL(response.body!);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
