import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PaginatedResponse, Person, PersonFilterRequest } from '../../model/datamodels';

@Injectable({ providedIn: 'root' })
export class PersonsApiService {
  private readonly baseUrl = '/api/persons';

  constructor(private http: HttpClient) {}

  find(filter: PersonFilterRequest = {}): Observable<PaginatedResponse<Person>> {
    let params = new HttpParams();
    if (filter.page !== undefined) params = params.set('page', filter.page);
    if (filter.size !== undefined) params = params.set('size', filter.size);
    if (filter.name) params = params.set('name', filter.name);
    return this.http.get<PaginatedResponse<Person>>(this.baseUrl, { params });
  }

  load(id: string): Observable<Person> {
    return this.http.get<Person>(`${this.baseUrl}/${id}`);
  }

  create(data: Person): Observable<Person> {
    return this.http.post<Person>(this.baseUrl, data);
  }

  update(id: string, data: Person): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${id}`, data);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
