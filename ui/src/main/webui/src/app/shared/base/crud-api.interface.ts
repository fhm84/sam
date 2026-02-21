import { Observable } from 'rxjs';
import { PaginatedResponse, PaginationRequest } from '../../model/datamodels';

export interface CrudApi<T, F extends PaginationRequest> {
  find(filter: F): Observable<PaginatedResponse<T>>;
  delete(id: string): Observable<void>;
}
