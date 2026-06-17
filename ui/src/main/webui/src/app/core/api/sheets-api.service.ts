import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateSheetMusic,
  CoverageResult,
  ExploreShelves,
  PaginatedResponse,
  SheetEnrichment,
  SheetFilterRequest,
  SheetMusic,
  SheetMusicSearchResult,
} from '../../model/datamodels';

@Injectable({ providedIn: 'root' })
export class SheetsApiService {
  private readonly baseUrl = '/api/sheets';

  constructor(private http: HttpClient) {}

  find(filter: SheetFilterRequest = {}): Observable<PaginatedResponse<SheetMusicSearchResult>> {
    let params = new HttpParams();
    if (filter.page !== undefined) params = params.set('page', filter.page);
    if (filter.size !== undefined) params = params.set('size', filter.size);
    if (filter.query) params = params.set('q', filter.query);
    if (filter.title) params = params.set('title', filter.title);
    if (filter.composer) params = params.set('composer', filter.composer);
    if (filter.genre) params = params.set('genre', filter.genre);
    if (filter.titleStartsWith) params = params.set('titleStartsWith', filter.titleStartsWith);
    if (filter.ensemble) params = params.set('ensemble', filter.ensemble);
    if (filter.tag) params = params.set('tag', filter.tag);
    return this.http.get<PaginatedResponse<SheetMusicSearchResult>>(this.baseUrl, { params });
  }

  explore(ensembleId?: string): Observable<ExploreShelves> {
    let params = new HttpParams();
    if (ensembleId) params = params.set('ensemble', ensembleId);
    return this.http.get<ExploreShelves>(`${this.baseUrl}/explore`, { params });
  }

  surprise(ensembleId?: string): Observable<SheetMusicSearchResult | null> {
    let params = new HttpParams();
    if (ensembleId) params = params.set('ensemble', ensembleId);
    return this.http.get<SheetMusicSearchResult | null>(`${this.baseUrl}/explore/surprise`, { params });
  }

  load(id: string): Observable<SheetMusic> {
    return this.http.get<SheetMusic>(`${this.baseUrl}/${id}`);
  }

  create(data: CreateSheetMusic): Observable<SheetMusic> {
    return this.http.post<SheetMusic>(this.baseUrl, data);
  }

  update(id: string, data: SheetMusic): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${id}`, data);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getGenres(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/genres`);
  }

  getAvailableLetters(genre?: string): Observable<string[]> {
    let params = new HttpParams();
    if (genre) params = params.set('genre', genre);
    return this.http.get<string[]>(`${this.baseUrl}/letters`, { params });
  }

  getCoverage(sheetId: string, ensembleId: string): Observable<CoverageResult> {
    const params = new HttpParams().set('ensemble', ensembleId);
    return this.http.get<CoverageResult>(`${this.baseUrl}/${sheetId}/coverage`, { params });
  }

  favorite(id: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${id}/favorite`, null);
  }

  unfavorite(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/favorite`);
  }

  addTags(id: string, tags: string[]): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${id}/tags`, tags);
  }

  removeTags(id: string, tags: string[]): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/tags`, { body: tags });
  }

  enrich(id: string): Observable<SheetEnrichment> {
    return this.http.post<SheetEnrichment>(`${this.baseUrl}/${id}/enrich`, null);
  }

  export(id: string, format: 'ZIP' | 'JSON' | 'CSV' = 'ZIP'): Observable<HttpResponse<Blob>> {
    const params = new HttpParams().set('format', format);
    return this.http.get(`${this.baseUrl}/${id}/export`, {
      params,
      responseType: 'blob',
      observe: 'response',
    });
  }
}
