import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Attachment,
  CollectionItem,
  CreateCollectionItem,
  PaginatedResponse,
  PaginationRequest,
  SheetCollection,
  SheetCollectionFilterRequest,
} from '../../model/datamodels';

@Injectable({ providedIn: 'root' })
export class CollectionsApiService {
  private readonly baseUrl = '/api/sheet-collections';

  constructor(private http: HttpClient) {}

  // ── Collections ───────────────────────────────────────
  find(
    filter: SheetCollectionFilterRequest = {},
  ): Observable<PaginatedResponse<SheetCollection>> {
    let params = new HttpParams();
    if (filter.page !== undefined) params = params.set('page', filter.page);
    if (filter.size !== undefined) params = params.set('size', filter.size);
    if (filter.name) params = params.set('name', filter.name);
    if (filter.query) params = params.set('query', filter.query);
    if (filter.type) params = params.set('type', filter.type);
    return this.http.get<PaginatedResponse<SheetCollection>>(this.baseUrl, { params });
  }

  load(id: string): Observable<SheetCollection> {
    return this.http.get<SheetCollection>(`${this.baseUrl}/${id}`);
  }

  create(data: SheetCollection): Observable<SheetCollection> {
    return this.http.post<SheetCollection>(this.baseUrl, data);
  }

  update(id: string, data: SheetCollection): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${id}`, data);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  // ── Collection Items ──────────────────────────────────
  listItems(
    collectionId: string,
    pagination: PaginationRequest = {},
  ): Observable<PaginatedResponse<CollectionItem>> {
    let params = new HttpParams();
    if (pagination.page !== undefined) params = params.set('page', pagination.page);
    if (pagination.size !== undefined) params = params.set('size', pagination.size);
    return this.http.get<PaginatedResponse<CollectionItem>>(
      `${this.baseUrl}/${collectionId}/items`,
      { params },
    );
  }

  addItem(collectionId: string, data: CreateCollectionItem): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${collectionId}/items`, data);
  }

  updateItem(collectionId: string, itemId: string, data: CollectionItem): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${collectionId}/items/${itemId}`, data);
  }

  removeItem(collectionId: string, itemId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${collectionId}/items/${itemId}`);
  }

  uploadAttachment(collectionId: string, itemId: string, file: File): Observable<Attachment> {
    const body = new FormData();
    body.append('file', file, file.name);
    return this.http.post<Attachment>(
      `${this.baseUrl}/${collectionId}/items/${itemId}/attachment`,
      body,
    );
  }

  removeAttachment(collectionId: string, itemId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${collectionId}/items/${itemId}/attachment`);
  }

  reorderItems(collectionId: string, orderedIds: string[]): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${collectionId}/items/order`, orderedIds);
  }

  // ── Export ────────────────────────────────────────────
  export(
    id: string,
    format: 'ZIP' | 'JSON' | 'CSV' = 'ZIP',
    includeTextItems = true,
    includeTextAttachments = false,
  ): Observable<HttpResponse<Blob>> {
    const params = new HttpParams()
      .set('format', format)
      .set('includeTextItems', includeTextItems)
      .set('includeTextAttachments', includeTextAttachments);
    return this.http.get(`${this.baseUrl}/${id}/export`, {
      params,
      responseType: 'blob',
      observe: 'response',
    });
  }

  // ── TOC export ────────────────────────────────────────
  downloadToc(collectionId: string, includeTextItems = true): Observable<HttpResponse<Blob>> {
    const params = new HttpParams().set('includeTextItems', includeTextItems);
    return this.http.get(`${this.baseUrl}/${collectionId}/toc`, {
      params,
      responseType: 'blob',
      observe: 'response',
    });
  }

  // ── GEMA setlist export ───────────────────────────────
  downloadGemaSetlist(collectionId: string): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.baseUrl}/${collectionId}/gema-setlist`, {
      responseType: 'blob',
      observe: 'response',
    });
  }

  // ── Reverse lookup ────────────────────────────────────
  getCollectionsForSheet(
    sheetId: string,
    pagination: PaginationRequest = {},
  ): Observable<PaginatedResponse<SheetCollection>> {
    let params = new HttpParams();
    if (pagination.page !== undefined) params = params.set('page', pagination.page);
    if (pagination.size !== undefined) params = params.set('size', pagination.size);
    return this.http.get<PaginatedResponse<SheetCollection>>(
      `/api/sheets/${sheetId}/collections`,
      { params },
    );
  }
}
