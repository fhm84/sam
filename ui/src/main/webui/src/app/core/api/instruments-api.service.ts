import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateInstrument,
  Instrument,
  InstrumentFilterRequest,
  PaginatedResponse,
} from '../../model/datamodels';

@Injectable({ providedIn: 'root' })
export class InstrumentsApiService {
  private readonly baseUrl = '/api/instruments';

  constructor(private http: HttpClient) {}

  find(filter: InstrumentFilterRequest = {}): Observable<PaginatedResponse<Instrument>> {
    let params = new HttpParams();
    if (filter.page !== undefined) params = params.set('page', filter.page);
    if (filter.size !== undefined) params = params.set('size', filter.size);
    if (filter.name) params = params.set('name', filter.name);
    return this.http.get<PaginatedResponse<Instrument>>(this.baseUrl, { params });
  }

  load(id: string): Observable<Instrument> {
    return this.http.get<Instrument>(`${this.baseUrl}/${id}`);
  }

  create(data: CreateInstrument): Observable<Instrument> {
    return this.http.post<Instrument>(this.baseUrl, data);
  }

  update(id: string, data: Instrument): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${id}`, data);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
