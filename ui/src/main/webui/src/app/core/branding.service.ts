import { HttpClient, HttpContext } from '@angular/common/http';
import { computed, inject, Injectable } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { SKIP_ERROR_TOAST } from './error.interceptor';

/** `BrandingResource.BrandingDto`: both null when the installation has no branding. */
interface Branding {
  name: string | null;
  logoVersion: string | null;
}

const NONE: Branding = { name: null, logoVersion: null };

/**
 * The band's name and logo (server config `sam.branding.*`), shown next to SAM's own logo.
 * Fetched once from the public API, so the share page gets it without a login. If the request
 * fails, the UI simply looks unbranded.
 */
@Injectable({ providedIn: 'root' })
export class BrandingService {
  private readonly http = inject(HttpClient);

  private readonly branding = toSignal(
    this.http
      .get<Branding>('/api/public/branding', {
        context: new HttpContext().set(SKIP_ERROR_TOAST, true),
      })
      .pipe(catchError(() => of(NONE))),
    { initialValue: NONE },
  );

  readonly name = computed(() => this.branding().name);

  /** Versioned, so the browser may cache it for good and still sees a new file. */
  readonly logoUrl = computed(() => {
    const version = this.branding().logoVersion;
    return version ? `/api/public/branding/logo?v=${encodeURIComponent(version)}` : null;
  });

  readonly active = computed(() => !!this.name() || !!this.logoUrl());
}
