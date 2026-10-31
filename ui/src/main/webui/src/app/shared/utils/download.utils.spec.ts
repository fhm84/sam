import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { filenameFromDisposition, saveDownload } from './download.utils';

describe('filenameFromDisposition', () => {
  it('decodes the RFC 5987 form the document download sends', () => {
    expect(filenameFromDisposition("inline; filename*=utf-8''Hardening%20Test.pdf")).toBe('Hardening Test.pdf');
  });

  it('decodes umlauts', () => {
    expect(filenameFromDisposition("attachment; filename*=UTF-8''Fl%C3%B6te%201.pdf")).toBe('Flöte 1.pdf');
  });

  it('accepts a language tag', () => {
    expect(filenameFromDisposition("attachment; filename*=utf-8'de'Noten.zip")).toBe('Noten.zip');
  });

  it('reads the quoted plain form the exports send', () => {
    expect(filenameFromDisposition('attachment; filename="1000 Traeume weit.zip"')).toBe('1000 Traeume weit.zip');
  });

  it('reads an unquoted plain form', () => {
    expect(filenameFromDisposition('attachment; filename=setlist.xlsx')).toBe('setlist.xlsx');
  });

  it('prefers the encoded form when both are present', () => {
    expect(filenameFromDisposition('attachment; filename="Flote.pdf"; filename*=utf-8\'\'Fl%C3%B6te.pdf')).toBe(
      'Flöte.pdf',
    );
  });

  it('falls back to the plain form when the encoding is broken', () => {
    expect(filenameFromDisposition('attachment; filename="safe.pdf"; filename*=utf-8\'\'bad%E0%A4%A')).toBe(
      'safe.pdf',
    );
  });

  it('returns null without a file name', () => {
    expect(filenameFromDisposition('inline')).toBeNull();
    expect(filenameFromDisposition(null)).toBeNull();
    expect(filenameFromDisposition(undefined)).toBeNull();
  });
});

describe('saveDownload', () => {
  let anchor: HTMLAnchorElement;

  beforeEach(() => {
    anchor = document.createElement('a');
    vi.spyOn(anchor, 'click').mockImplementation(() => {});
    vi.spyOn(document, 'createElement').mockReturnValue(anchor);
    URL.createObjectURL = vi.fn().mockReturnValue('blob:x');
    URL.revokeObjectURL = vi.fn();
  });

  afterEach(() => vi.restoreAllMocks());

  function response(disposition?: string): HttpResponse<Blob> {
    const headers = disposition ? new HttpHeaders({ 'Content-Disposition': disposition }) : new HttpHeaders();
    return new HttpResponse({ body: new Blob(['x']), headers });
  }

  it('uses the name from the response', () => {
    saveDownload(response("inline; filename*=utf-8''Klarinette%201.pdf"), 'download');

    expect(anchor.download).toBe('Klarinette 1.pdf');
    expect(anchor.click).toHaveBeenCalled();
  });

  it('uses the fallback when the response names no file', () => {
    saveDownload(response(), 'Klarinette 1');

    expect(anchor.download).toBe('Klarinette 1');
  });
});
