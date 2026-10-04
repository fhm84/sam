import { ApplicationConfig, provideBrowserGlobalErrorListeners, APP_INITIALIZER, inject } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { HttpClient, provideHttpClient, withInterceptors, withXhr } from '@angular/common/http';
import { provideOptimus } from '@openng/optimus-ui/config';
import { MessageService } from '@openng/optimus-ui/api';
import Aura from '@openng/optimus-ui-themes/aura';
import { ColorSchemeService } from './core/color-scheme.service';
import { AbstractSecurityStorage, DefaultSessionStorageService, authInterceptor, provideAuth, StsConfigLoader, withAppInitializerAuthCheck } from 'angular-auth-oidc-client';

import { routes } from './app.routes';
import { TranslationService } from './core/translation.service';
import { errorInterceptor } from './core/error.interceptor';
import { authConfigLoader } from './core/auth/auth.config';
import { removeLegacyLocalStorageTokens } from './core/auth/legacy-token-cleanup';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideAnimationsAsync(),
    provideHttpClient(withXhr(), withInterceptors([authInterceptor(), errorInterceptor])),
    MessageService,
    provideOptimus({
      theme: {
        preset: Aura,
        options: {
          darkModeSelector: '.dark',
        },
      },
    }),
    provideAuth(
      {
        loader: {
          provide: StsConfigLoader,
          useFactory: authConfigLoader,
          deps: [HttpClient],
        },
      },
      withAppInitializerAuthCheck(),
    ),
    // sessionStorage, not localStorage: tokens (incl. the refresh token) don't outlive the tab and
    // aren't shared with every other tab, which narrows what an XSS bug could exfiltrate. A new tab
    // logs in again silently through the Keycloak SSO session (see authGuard for deep links).
    { provide: AbstractSecurityStorage, useClass: DefaultSessionStorageService },
    {
      provide: APP_INITIALIZER,
      useFactory: () => () => removeLegacyLocalStorageTokens(),
      multi: true,
    },
    {
      provide: APP_INITIALIZER,
      useFactory: () => {
        const i18n = inject(TranslationService);
        return () => i18n.initialize();
      },
      multi: true,
    },
    {
      provide: APP_INITIALIZER,
      useFactory: () => {
        const colorScheme = inject(ColorSchemeService);
        return () => colorScheme.initialize();
      },
      multi: true,
    },
  ],
};
