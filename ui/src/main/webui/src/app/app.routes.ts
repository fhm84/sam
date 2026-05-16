import { Routes } from '@angular/router';
import { AppLayout } from './layout/component/app-layout';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'share/:token',
    loadComponent: () =>
      import('./features/public-share/public-share-page').then((m) => m.PublicSharePage),
  },
  {
    path: '',
    component: AppLayout,
    canActivate: [authGuard],
    children: [
      {
        path: 'sheets',
        loadChildren: () =>
          import('./features/sheets/sheets.routes').then((m) => m.SHEETS_ROUTES),
      },
      {
        path: 'collections',
        loadChildren: () =>
          import('./features/collections/collections.routes').then((m) => m.COLLECTIONS_ROUTES),
      },
      {
        path: 'uploads',
        loadChildren: () =>
          import('./features/uploads/uploads.routes').then((m) => m.UPLOADS_ROUTES),
      },
      {
        path: 'shares',
        loadChildren: () =>
          import('./features/shares/shares.routes').then((m) => m.SHARES_ROUTES),
      },
      {
        path: 'my-parts',
        loadChildren: () =>
          import('./features/my-parts/my-parts.routes').then((m) => m.MY_PARTS_ROUTES),
      },
      {
        path: 'musicians',
        loadChildren: () =>
          import('./features/musicians/musicians.routes').then((m) => m.MUSICIANS_ROUTES),
      },
      {
        path: 'admin/ensembles',
        loadChildren: () =>
          import('./features/ensembles/ensembles.routes').then((m) => m.ENSEMBLES_ROUTES),
      },
      {
        path: 'admin/instruments',
        loadChildren: () =>
          import('./features/instruments/instruments.routes').then((m) => m.INSTRUMENTS_ROUTES),
      },
      {
        path: 'admin/configuration',
        loadChildren: () =>
          import('./features/configuration/configuration.routes').then(
            (m) => m.CONFIGURATION_ROUTES,
          ),
      },
      {
        path: 'admin/event-logs',
        loadChildren: () =>
          import('./features/event-logs/event-logs.routes').then((m) => m.EVENT_LOGS_ROUTES),
      },
      {
        path: 'user/preferences',
        loadChildren: () =>
          import('./features/user-preferences/user-preferences.routes').then(
            (m) => m.USER_PREFERENCES_ROUTES,
          ),
      },
      {
        path: '',
        pathMatch: 'full',
        loadChildren: () => import('./features/home/home.routes').then((m) => m.HOME_ROUTES),
      },
    ],
  },
];
