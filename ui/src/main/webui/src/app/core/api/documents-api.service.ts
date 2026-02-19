import { Injectable } from '@angular/core';
import { HttpClient, HttpEventType, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import {
  AttachmentType,
  DocumentDownload,
  DocumentFilterRequest,
  PaginatedResponse,
} from '../../model/datamodels';

export interface UploadProgress {
  type: 'progress' | 'complete';
  progress?: number;
  identifier?: string;
}

@Injectable({ providedIn: 'root' })
export class DocumentsApiService {
  constructor(private http: HttpClient) {}

  list(
    basePath: string,
    filter: DocumentFilterRequest = {},
  ): Observable<PaginatedResponse<DocumentDownload>> {
    let params = new HttpParams();
    if (filter.page !== undefined) params = params.set('page', filter.page);
    if (filter.size !== undefined) params = params.set('size', filter.size);
    return this.http.get<PaginatedResponse<DocumentDownload>>(basePath, { params });
  }

  download(basePath: string, docIdentifier: string): Observable<HttpResponse<Blob>> {
    return this.http.get(`${basePath}/${docIdentifier}`, {
      observe: 'response',
      responseType: 'blob',
    });
  }

  upload(basePath: string, file: File, type?: AttachmentType): Observable<UploadProgress> {
    const formData = new FormData();
    formData.append('file', file);
    if (type) formData.append('type', type);

    return this.http
      .post<{ identifier: string }>(basePath, formData, {
        observe: 'events',
        reportProgress: true,
      })
      .pipe(
        map((event) => {
          if (event.type === HttpEventType.UploadProgress) {
            return {
              type: 'progress' as const,
              progress: Math.round((event.loaded / (event.total || 1)) * 100),
            };
          }
          if (event.type === HttpEventType.Response) {
            return { type: 'complete' as const, identifier: (event.body as any)?.identifier };
          }
          return { type: 'progress' as const, progress: 0 };
        }),
      );
  }

  // Convenience methods for common base paths
  forSheets(sheetId: string) {
    return `/api/sheets/${sheetId}/documents`;
  }

  forInstrumentations(sheetId: string, instrumentationId: string) {
    return `/api/sheets/${sheetId}/instrumentations/${instrumentationId}/documents`;
  }

  forTopLevel() {
    return '/api/documents';
  }
}
