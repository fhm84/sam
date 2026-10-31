import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';

import { localeInterceptor } from './locale.interceptor';
import { TranslationService } from './translation.service';

describe('localeInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  const locale = signal<'en' | 'de'>('de');

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([localeInterceptor])),
        provideHttpClientTesting(),
        { provide: TranslationService, useValue: { locale } },
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('sends the app language to the API', () => {
    locale.set('de');
    http.get('/api/sheets').subscribe();

    expect(controller.expectOne('/api/sheets').request.headers.get('Accept-Language')).toBe('de');
  });

  it('follows a language switch', () => {
    locale.set('en');
    http.get('/api/sheets').subscribe();

    expect(controller.expectOne('/api/sheets').request.headers.get('Accept-Language')).toBe('en');
  });

  it('leaves other hosts alone', () => {
    http.get('http://localhost:1411/.well-known/openid-configuration').subscribe();

    expect(
      controller.expectOne('http://localhost:1411/.well-known/openid-configuration').request.headers.has('Accept-Language'),
    ).toBe(false);
  });
});
