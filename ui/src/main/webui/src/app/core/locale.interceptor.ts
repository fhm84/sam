import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TranslationService } from './translation.service';

/**
 * Tells SAM's API the language chosen in the app (which can differ from the browser's), so
 * server-made texts such as download file names ("… – Partitur.pdf") match the UI.
 */
export const localeInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api/')) {
    return next(req);
  }
  const locale = inject(TranslationService).locale();
  return next(req.clone({ setHeaders: { 'Accept-Language': locale } }));
};
