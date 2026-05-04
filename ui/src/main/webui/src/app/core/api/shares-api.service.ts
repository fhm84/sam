import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateShareRequest,
  PaginatedResponse,
  PublicShareInfo,
  ShareResponse,
} from '../../model/datamodels';

@Injectable({ providedIn: 'root' })
export class SharesApiService {
  private readonly baseUrl = '/api/shares';
  private readonly publicBase = '/api/public/shares';

  constructor(private http: HttpClient) {}

  create(request: CreateShareRequest): Observable<ShareResponse> {
    return this.http.post<ShareResponse>(this.baseUrl, request);
  }

  list(): Observable<PaginatedResponse<ShareResponse>> {
    return this.http.get<PaginatedResponse<ShareResponse>>(this.baseUrl);
  }

  revoke(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getPublicInfo(token: string): Observable<PublicShareInfo> {
    return this.http.get<PublicShareInfo>(`${this.publicBase}/${token}`);
  }

  instrDownloadUrl(token: string): string {
    return `${this.publicBase}/${token}/download`;
  }

  collectionTocUrl(token: string): string {
    return `${this.publicBase}/${token}/toc`;
  }

  collectionPartUrl(token: string, instrumentationId: string): string {
    return `${this.publicBase}/${token}/download/${instrumentationId}`;
  }

  sheetDownloadAllUrl(token: string, type = 'PART'): string {
    return `${this.publicBase}/${token}/download?type=${type}`;
  }

  collectionDownloadAllUrl(token: string, type = 'PART'): string {
    return `${this.publicBase}/${token}/download?type=${type}`;
  }

  sheetPartUrl(token: string, instrumentationId: string): string {
    return `${this.publicBase}/${token}/download/${instrumentationId}`;
  }

  sheetAttachmentUrl(token: string, attachmentId: string): string {
    return `${this.publicBase}/${token}/sheet/${attachmentId}`;
  }
}
