import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PaginatedResponse, PaginationRequest, SheetWithMyParts } from '../../model/datamodels';

@Injectable({ providedIn: 'root' })
export class MyPartsApiService {
  private readonly baseUrl = '/api/me/parts';

  constructor(private http: HttpClient) {}

  find(filter: PaginationRequest = {}): Observable<PaginatedResponse<SheetWithMyParts>> {
    let params = new HttpParams();
    if (filter.page !== undefined) params = params.set('page', filter.page);
    if (filter.size !== undefined) params = params.set('size', filter.size);
    return this.http.get<PaginatedResponse<SheetWithMyParts>>(this.baseUrl, { params });
  }
}
