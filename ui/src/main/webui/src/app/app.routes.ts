import { Routes } from '@angular/router';
import { AppLayout } from './layout/component/app-layout';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
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
