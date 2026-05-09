import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Booklet,
  BookletFilterRequest,
  CollectionItem,
  PaginatedResponse,
  PaginationRequest,
} from '../../model/datamodels';

@Injectable({ providedIn: 'root' })
export class BookletsApiService {
  private readonly baseUrl = '/api/booklets';

  constructor(private http: HttpClient) {}

  // ── Booklets ──────────────────────────────────────────
  find(filter: BookletFilterRequest = {}): Observable<PaginatedResponse<Booklet>> {
    let params = new HttpParams();
    if (filter.page !== undefined) params = params.set('page', filter.page);
    if (filter.size !== undefined) params = params.set('size', filter.size);
    if (filter.name) params = params.set('name', filter.name);
    if (filter.query) params = params.set('query', filter.query);
    return this.http.get<PaginatedResponse<Booklet>>(this.baseUrl, { params });
  }

  load(id: string): Observable<Booklet> {
    return this.http.get<Booklet>(`${this.baseUrl}/${id}`);
  }

  create(data: Booklet): Observable<Booklet> {
    return this.http.post<Booklet>(this.baseUrl, data);
  }

  update(id: string, data: Booklet): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${id}`, data);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  // ── Booklet Sheets ────────────────────────────────────
  listSheets(
    bookletId: string,
    pagination: PaginationRequest = {},
  ): Observable<PaginatedResponse<CollectionItem>> {
    let params = new HttpParams();
    if (pagination.page !== undefined) params = params.set('page', pagination.page);
    if (pagination.size !== undefined) params = params.set('size', pagination.size);
    return this.http.get<PaginatedResponse<CollectionItem>>(
      `${this.baseUrl}/${bookletId}/sheets`,
      { params },
    );
  }

  addSheet(bookletId: string, data: CollectionItem): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${bookletId}/sheets`, data);
  }

  updateSheet(bookletId: string, sheetId: string, data: CollectionItem): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${bookletId}/sheets/${sheetId}`, data);
  }

  removeSheet(bookletId: string, sheetId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${bookletId}/sheets/${sheetId}`);
  }
}
