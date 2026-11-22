import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { BrandingService } from './branding.service';
import { SKIP_ERROR_TOAST } from './error.interceptor';

describe('BrandingService', () => {
  let service: BrandingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(BrandingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('builds a versioned logo URL from the public endpoint', () => {
    const req = http.expectOne('/api/public/branding');
    expect(req.request.context.get(SKIP_ERROR_TOAST)).toBe(true);
    req.flush({ name: 'Test Band', logoVersion: 'abc123' });

    expect(service.name()).toBe('Test Band');
    expect(service.logoUrl()).toBe('/api/public/branding/logo?v=abc123');
    expect(service.active()).toBe(true);
  });

  it('stays unbranded without configuration', () => {
    http.expectOne('/api/public/branding').flush({ name: null, logoVersion: null });

    expect(service.logoUrl()).toBeNull();
    expect(service.active()).toBe(false);
  });

  it('stays unbranded when the request fails', () => {
    http.expectOne('/api/public/branding').flush('down', { status: 503, statusText: 'Unavailable' });

    expect(service.name()).toBeNull();
    expect(service.active()).toBe(false);
  });
});
