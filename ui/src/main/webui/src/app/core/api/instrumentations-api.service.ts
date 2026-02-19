import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateInstrumentation, Instrumentation } from '../../model/datamodels';

@Injectable({ providedIn: 'root' })
export class InstrumentationsApiService {
  constructor(private http: HttpClient) {}

  private url(sheetId: string) {
    return `/api/sheets/${sheetId}/instrumentations`;
  }

  list(sheetId: string): Observable<Instrumentation[]> {
    return this.http.get<Instrumentation[]>(this.url(sheetId));
  }

  load(sheetId: string, instrumentationId: string): Observable<Instrumentation> {
    return this.http.get<Instrumentation>(`${this.url(sheetId)}/${instrumentationId}`);
  }

  create(sheetId: string, data: CreateInstrumentation): Observable<void> {
    return this.http.post<void>(this.url(sheetId), data);
  }

  createBulk(sheetId: string, data: CreateInstrumentation[]): Observable<void> {
    return this.http.post<void>(`${this.url(sheetId)}/bulk`, data);
  }

  update(sheetId: string, instrumentationId: string, data: Instrumentation): Observable<void> {
    return this.http.put<void>(`${this.url(sheetId)}/${instrumentationId}`, data);
  }

  delete(sheetId: string, instrumentationId: string): Observable<void> {
    return this.http.delete<void>(`${this.url(sheetId)}/${instrumentationId}`);
  }
}
