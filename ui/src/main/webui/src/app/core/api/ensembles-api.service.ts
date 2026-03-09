import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateEnsemble,
  CreateEnsembleVoice,
  CreateVoiceOption,
  Ensemble,
  EnsembleCoverageStatus,
  EnsembleFilterRequest,
  EnsembleVoice,
  PaginatedResponse,
  VoiceOption,
} from '../../model/datamodels';

@Injectable({ providedIn: 'root' })
export class EnsemblesApiService {
  private readonly baseUrl = '/api/ensembles';

  constructor(private http: HttpClient) {}

  // ── Ensembles ─────────────────────────────────────────
  find(filter: EnsembleFilterRequest = {}): Observable<PaginatedResponse<Ensemble>> {
    let params = new HttpParams();
    if (filter.page !== undefined) params = params.set('page', filter.page);
    if (filter.size !== undefined) params = params.set('size', filter.size);
    if (filter.name) params = params.set('name', filter.name);
    return this.http.get<PaginatedResponse<Ensemble>>(this.baseUrl, { params });
  }

  load(id: string): Observable<Ensemble> {
    return this.http.get<Ensemble>(`${this.baseUrl}/${id}`);
  }

  create(data: CreateEnsemble): Observable<Ensemble> {
    return this.http.post<Ensemble>(this.baseUrl, data);
  }

  update(id: string, data: Ensemble): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${id}`, data);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  // ── Coverage ──────────────────────────────────────────
  computeCoverage(ensembleId: string): Observable<EnsembleCoverageStatus> {
    return this.http.post<EnsembleCoverageStatus>(
      `${this.baseUrl}/${ensembleId}/coverage/compute`,
      null,
    );
  }

  getCoverageStatus(ensembleId: string): Observable<EnsembleCoverageStatus> {
    return this.http.get<EnsembleCoverageStatus>(
      `${this.baseUrl}/${ensembleId}/coverage/status`,
    );
  }

  // ── Voices ────────────────────────────────────────────
  listVoices(ensembleId: string): Observable<EnsembleVoice[]> {
    return this.http.get<EnsembleVoice[]>(`${this.baseUrl}/${ensembleId}/voices`);
  }

  loadVoice(ensembleId: string, voiceId: string): Observable<EnsembleVoice> {
    return this.http.get<EnsembleVoice>(`${this.baseUrl}/${ensembleId}/voices/${voiceId}`);
  }

  createVoice(ensembleId: string, data: CreateEnsembleVoice): Observable<EnsembleVoice> {
    return this.http.post<EnsembleVoice>(`${this.baseUrl}/${ensembleId}/voices`, data);
  }

  updateVoice(ensembleId: string, voiceId: string, data: EnsembleVoice): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${ensembleId}/voices/${voiceId}`, data);
  }

  deleteVoice(ensembleId: string, voiceId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${ensembleId}/voices/${voiceId}`);
  }

  // ── Voice Options ─────────────────────────────────────
  listVoiceOptions(ensembleId: string, voiceId: string): Observable<VoiceOption[]> {
    return this.http.get<VoiceOption[]>(
      `${this.baseUrl}/${ensembleId}/voices/${voiceId}/options`,
    );
  }

  createVoiceOption(
    ensembleId: string,
    voiceId: string,
    data: CreateVoiceOption,
  ): Observable<VoiceOption> {
    return this.http.post<VoiceOption>(
      `${this.baseUrl}/${ensembleId}/voices/${voiceId}/options`,
      data,
    );
  }

  updateVoiceOption(
    ensembleId: string,
    voiceId: string,
    optionId: string,
    data: VoiceOption,
  ): Observable<void> {
    return this.http.put<void>(
      `${this.baseUrl}/${ensembleId}/voices/${voiceId}/options/${optionId}`,
      data,
    );
  }

  deleteVoiceOption(
    ensembleId: string,
    voiceId: string,
    optionId: string,
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/${ensembleId}/voices/${voiceId}/options/${optionId}`,
    );
  }
}
