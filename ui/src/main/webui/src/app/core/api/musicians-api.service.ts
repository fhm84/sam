import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Musician, MusicianFilterRequest, PaginatedResponse } from '../../model/datamodels';

@Injectable({ providedIn: 'root' })
export class MusiciansApiService {
  private readonly baseUrl = '/api/musicians';

  constructor(private http: HttpClient) {}

  find(filter: MusicianFilterRequest = {}): Observable<PaginatedResponse<Musician>> {
    let params = new HttpParams();
    if (filter.page !== undefined) params = params.set('page', filter.page);
    if (filter.size !== undefined) params = params.set('size', filter.size);
    if (filter.name) params = params.set('name', filter.name);
    return this.http.get<PaginatedResponse<Musician>>(this.baseUrl, { params });
  }

  load(id: string): Observable<Musician> {
    return this.http.get<Musician>(`${this.baseUrl}/${id}`);
  }

  create(data: Musician): Observable<Musician> {
    return this.http.post<Musician>(this.baseUrl, data);
  }

  update(id: string, data: Musician): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${id}`, data);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
