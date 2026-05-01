import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EventLogEntry, EventLogFilterRequest, PaginatedResponse } from '../../model/datamodels';

@Injectable({ providedIn: 'root' })
export class EventLogsApiService {
  private readonly baseUrl = '/api/event-logs';

  constructor(private http: HttpClient) {}

  find(filter: EventLogFilterRequest = {}): Observable<PaginatedResponse<EventLogEntry>> {
    let params = new HttpParams();
    if (filter.page !== undefined) params = params.set('page', filter.page);
    if (filter.size !== undefined) params = params.set('size', filter.size);
    if (filter.eventTypes?.length) {
      filter.eventTypes.forEach(t => (params = params.append('eventTypes', t)));
    }
    if (filter.entityType) params = params.set('entityType', filter.entityType);
    if (filter.userId) params = params.set('userId', filter.userId);
    return this.http.get<PaginatedResponse<EventLogEntry>>(this.baseUrl, { params });
  }
}
