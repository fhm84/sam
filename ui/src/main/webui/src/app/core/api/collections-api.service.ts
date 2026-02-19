import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CollectionSheet,
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

  // ── Collection Sheets ─────────────────────────────────
  listSheets(
    collectionId: string,
    pagination: PaginationRequest = {},
  ): Observable<PaginatedResponse<CollectionSheet>> {
    let params = new HttpParams();
    if (pagination.page !== undefined) params = params.set('page', pagination.page);
    if (pagination.size !== undefined) params = params.set('size', pagination.size);
    return this.http.get<PaginatedResponse<CollectionSheet>>(
      `${this.baseUrl}/${collectionId}/sheets`,
      { params },
    );
  }

  addSheet(collectionId: string, data: CollectionSheet): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${collectionId}/sheets`, data);
  }

  updateSheet(collectionId: string, sheetId: string, data: CollectionSheet): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${collectionId}/sheets/${sheetId}`, data);
  }

  removeSheet(collectionId: string, sheetId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${collectionId}/sheets/${sheetId}`);
  }
}
