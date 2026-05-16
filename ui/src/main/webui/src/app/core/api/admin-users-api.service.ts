import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserInfo } from '../../model/datamodels';

@Injectable({ providedIn: 'root' })
export class AdminUsersApiService {
  private readonly baseUrl = '/api/admin/users';

  constructor(private http: HttpClient) {}

  search(query: string): Observable<UserInfo[]> {
    const params = new HttpParams().set('search', query);
    return this.http.get<UserInfo[]>(this.baseUrl, { params });
  }

  getById(id: string): Observable<UserInfo> {
    return this.http.get<UserInfo>(`${this.baseUrl}/${id}`);
  }
}
