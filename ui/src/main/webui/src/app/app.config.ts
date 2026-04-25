import { ApplicationConfig, provideBrowserGlobalErrorListeners, APP_INITIALIZER, inject } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { providePrimeNG } from 'primeng/config';
import { MessageService } from 'primeng/api';
import Aura from '@primeuix/themes/aura';
import { AbstractSecurityStorage, DefaultLocalStorageService, authInterceptor, provideAuth, StsConfigLoader, withAppInitializerAuthCheck } from 'angular-auth-oidc-client';

import { routes } from './app.routes';
import { TranslationService } from './core/translation.service';
import { errorInterceptor } from './core/error.interceptor';
import { authConfigLoader } from './core/auth/auth.config';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideAnimationsAsync(),
    provideHttpClient(withInterceptors([authInterceptor(), errorInterceptor])),
    MessageService,
    providePrimeNG({
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
    { provide: AbstractSecurityStorage, useClass: DefaultLocalStorageService },
    {
      provide: APP_INITIALIZER,
      useFactory: () => {
        const i18n = inject(TranslationService);
        return () => i18n.initialize();
      },
      multi: true,
    },
  ],
};
