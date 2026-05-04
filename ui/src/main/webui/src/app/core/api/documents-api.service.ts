import { Injectable } from '@angular/core';
import { HttpClient, HttpEventType, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import {
  Attachment,
  AttachmentType,
  ClassificationApplyRequest,
  ClassificationApplyResult,
  DocumentDownload,
  DocumentFilterRequest,
  DownloadFormat,
  PaginatedResponse,
  SheetClassification,
} from '../../model/datamodels';

export interface UploadProgress {
  type: 'progress' | 'complete';
  progress?: number;
  attachment?: Attachment;
}

@Injectable({ providedIn: 'root' })
export class DocumentsApiService {
  constructor(private http: HttpClient) {}

  list(
    basePath: string,
    filter: DocumentFilterRequest = {},
  ): Observable<PaginatedResponse<Attachment>> {
    let params = new HttpParams();
    if (filter.page !== undefined) params = params.set('page', filter.page);
    if (filter.size !== undefined) params = params.set('size', filter.size);
    return this.http.get<PaginatedResponse<Attachment>>(basePath, { params });
  }

  listUnlinked(filter: DocumentFilterRequest = {}): Observable<PaginatedResponse<DocumentDownload>> {
    let params = new HttpParams();
    if (filter.page !== undefined) params = params.set('page', filter.page);
    if (filter.size !== undefined) params = params.set('size', filter.size);
    return this.http.get<PaginatedResponse<DocumentDownload>>(`${this.forTopLevel()}/unlinked`, { params });
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
      .post<Attachment>(basePath, formData, {
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
            return { type: 'complete' as const, attachment: event.body ?? undefined };
          }
          return { type: 'progress' as const, progress: 0 };
        }),
      );
  }

  /**
   * Link an existing (unlinked) document to a sheet and optionally an instrumentation.
   * Expects: POST /api/documents/{documentId}/link  { sheetId, instrumentationId?, type? }
   */
  linkToSheet(
    documentId: string,
    sheetId: string,
    instrumentationId?: string,
    type?: AttachmentType,
  ): Observable<void> {
    const body: Record<string, string> = { sheetId };
    if (instrumentationId) body['instrumentationId'] = instrumentationId;
    if (type) body['attachmentType'] = type;
    return this.http.post<void>(`${this.forTopLevel()}/${documentId}/link`, body);
  }

  downloadBatch(
    basePath: string,
    type?: AttachmentType,
    includeInstrumentations = false,
    format: DownloadFormat = 'ZIP',
  ): Observable<HttpResponse<Blob>> {
    let params = new HttpParams();
    if (type) params = params.set('type', type);
    if (includeInstrumentations) params = params.set('includeInstrumentations', 'true');
    if (format !== 'ZIP') params = params.set('format', format);
    return this.http.get(`${basePath}/batch`, {
      observe: 'response',
      responseType: 'blob',
      params,
    });
  }

  downloadBatchByIds(
    ids: string[],
    format: DownloadFormat = 'ZIP',
    baseName?: string,
  ): Observable<HttpResponse<Blob>> {
    return this.http.post(`${this.forTopLevel()}/batch`, { ids, format, baseName }, {
      observe: 'response',
      responseType: 'blob',
    });
  }

  classify(documentId: string): Observable<SheetClassification> {
    return this.http.post<SheetClassification>(
      `${this.forTopLevel()}/${documentId}/classify`,
      null,
    );
  }

  apply(documentId: string, request: ClassificationApplyRequest): Observable<ClassificationApplyResult> {
    return this.http.post<ClassificationApplyResult>(
      `${this.forTopLevel()}/${documentId}/apply`,
      request,
    );
  }

  delete(basePath: string, docIdentifier: string): Observable<void> {
    return this.http.delete<void>(`${basePath}/${docIdentifier}`);
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
