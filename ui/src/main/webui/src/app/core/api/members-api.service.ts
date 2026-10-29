import { Injectable } from '@angular/core';
import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Member, MemberFilterRequest, OwnMemberData, PaginatedResponse } from '../../model/datamodels';
import { SKIP_ERROR_TOAST } from '../error.interceptor';

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

  /** The caller's own member data (404 when no member is linked to the login). */
  own(): Observable<OwnMemberData> {
    // A login without a member profile is a normal case the page explains, not an error toast
    return this.http.get<OwnMemberData>('/api/me/member', { context: new HttpContext().set(SKIP_ERROR_TOAST, true) });
  }
}
