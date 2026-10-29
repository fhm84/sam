import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Member, MemberFilterRequest, PaginatedResponse } from '../../model/datamodels';

@Injectable({ providedIn: 'root' })
export class MembersApiService {
  private readonly baseUrl = '/api/members';

  constructor(private http: HttpClient) {}

  find(filter: MemberFilterRequest = {}): Observable<PaginatedResponse<Member>> {
    let params = new HttpParams();
    if (filter.page !== undefined) params = params.set('page', filter.page);
    if (filter.size !== undefined) params = params.set('size', filter.size);
    if (filter.name) params = params.set('name', filter.name);
    if (filter.status) params = params.set('status', filter.status);
    return this.http.get<PaginatedResponse<Member>>(this.baseUrl, { params });
  }

  load(id: string): Observable<Member> {
    return this.http.get<Member>(`${this.baseUrl}/${id}`);
  }

  create(data: Member): Observable<Member> {
    return this.http.post<Member>(this.baseUrl, data);
  }

  update(id: string, data: Member): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${id}`, data);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  linkUser(memberId: string, userId: string): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${memberId}/user/${userId}`, null);
  }

  unlinkUser(memberId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${memberId}/user`);
  }
}
