import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { StorageIntegrityReport } from '../../model/datamodels';

@Injectable({ providedIn: 'root' })
export class AdminStorageApiService {
  private readonly http = inject(HttpClient);

  /** Re-hashes every stored file against the database; read-only, may take a while. */
  checkIntegrity(): Observable<StorageIntegrityReport> {
    return this.http.post<StorageIntegrityReport>('/api/admin/storage/integrity-check', null);
  }
}
