import { inject, Injectable } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { map, Observable } from 'rxjs';
import { DocumentsApiService } from './api/documents-api.service';

/**
 * Loads a document as a sanitized blob URL suitable for use in
 * an <iframe> or <img> [src] binding.
 *
 * The caller is responsible for revoking the raw URL via
 * `URL.revokeObjectURL(rawUrl)` when it is no longer needed.
 */
export interface PreviewBlob {
  safeUrl: SafeResourceUrl;
  rawUrl: string;
}

@Injectable({ providedIn: 'root' })
export class DocumentPreviewService {
  private readonly documentsApi = inject(DocumentsApiService);
  private readonly sanitizer = inject(DomSanitizer);

  load(documentId: string): Observable<PreviewBlob> {
    return this.documentsApi
      .download(this.documentsApi.forTopLevel(), documentId)
      .pipe(
        map((response) => {
          const rawUrl = URL.createObjectURL(response.body!);
          return {
            rawUrl,
            safeUrl: this.sanitizer.bypassSecurityTrustResourceUrl(rawUrl),
          };
        }),
      );
  }
}
