import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateSheetMusic,
  CoverageResult,
  PaginatedResponse,
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
    if (filter.query) params = params.set('query', filter.query);
    if (filter.title) params = params.set('title', filter.title);
    if (filter.composer) params = params.set('composer', filter.composer);
    if (filter.genre) params = params.set('genre', filter.genre);
    return this.http.get<PaginatedResponse<SheetMusicSearchResult>>(this.baseUrl, { params });
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

  getCoverage(sheetId: string, ensembleId: string): Observable<CoverageResult> {
    const params = new HttpParams().set('ensemble', ensembleId);
    return this.http.get<CoverageResult>(`${this.baseUrl}/${sheetId}/coverage`, { params });
  }
}
