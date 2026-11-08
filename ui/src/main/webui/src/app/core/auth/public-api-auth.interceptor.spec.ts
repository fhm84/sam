import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpInterceptorFn, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { withoutTokenForPublicApi } from './public-api-auth.interceptor';

describe('withoutTokenForPublicApi', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  // Stands in for angular-auth-oidc-client's interceptor: adds the token to every request it sees
  const auth = vi.fn<HttpInterceptorFn>((req, next) =>
    next(req.clone({ setHeaders: { Authorization: 'Bearer stale' } })),
  );

  beforeEach(() => {
    auth.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([withoutTokenForPublicApi(auth)])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('sends share links without the token', () => {
    http.get('/api/public/shares/abc').subscribe();

    expect(controller.expectOne('/api/public/shares/abc').request.headers.has('Authorization')).toBe(false);
    expect(auth).not.toHaveBeenCalled();
  });

  it('keeps the token on the rest of the API', () => {
    http.get('/api/shares').subscribe();

    expect(controller.expectOne('/api/shares').request.headers.get('Authorization')).toBe('Bearer stale');
  });

  it('keeps it on paths that only start like the public API', () => {
    http.get('/api/publications').subscribe();

    expect(controller.expectOne('/api/publications').request.headers.get('Authorization')).toBe('Bearer stale');
  });
});
