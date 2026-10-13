import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { InstrumentsApiService } from './instruments-api.service';

describe('InstrumentsApiService', () => {
  let service: InstrumentsApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(InstrumentsApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('find sends paging, name and transposition as query parameters', () => {
    service.find({ page: 2, size: 10, name: 'Horn', transposition: 'Bb' }).subscribe();

    const req = http.expectOne((r) => r.url === '/api/instruments');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.get('name')).toBe('Horn');
    expect(req.request.params.get('transposition')).toBe('Bb');
    req.flush({ data: [], totalCount: 0 });
  });

  it('find omits unset filters', () => {
    service.find({ page: 0, size: 10 }).subscribe();

    const req = http.expectOne((r) => r.url === '/api/instruments');
    expect(req.request.params.has('name')).toBe(false);
    expect(req.request.params.has('transposition')).toBe(false);
    req.flush({ data: [], totalCount: 0 });
  });
});
