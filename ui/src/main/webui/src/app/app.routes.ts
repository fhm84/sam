import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'sheets',
    loadComponent: () => import('./features/sheets/sheets').then((m) => m.Sheets),
  },
  {
    path: 'sheets/new',
    loadComponent: () =>
      import('./features/sheets/sheet-form-page').then((m) => m.SheetFormPage),
  },
  {
    path: 'sheets/:id',
    loadComponent: () =>
      import('./features/sheets/sheet-detail-page').then((m) => m.SheetDetailPage),
  },
  {
    path: 'sheets/:id/edit',
    loadComponent: () =>
      import('./features/sheets/sheet-form-page').then((m) => m.SheetFormPage),
  },
  {
    path: 'collections',
    loadComponent: () => import('./features/collections/collections').then((m) => m.Collections),
  },
  {
    path: 'uploads',
    loadComponent: () => import('./features/uploads/uploads').then((m) => m.Uploads),
  },
  {
    path: 'musicians',
    loadComponent: () => import('./features/musicians/musicians').then((m) => m.Musicians),
  },
  {
    path: 'admin/ensembles',
    loadComponent: () => import('./features/ensembles/ensembles').then((m) => m.Ensembles),
  },
  {
    path: 'admin/ensembles/:id',
    loadComponent: () =>
      import('./features/ensembles/ensemble-detail-page').then((m) => m.EnsembleDetailPage),
  },
  {
    path: 'admin/instruments',
    loadComponent: () =>
      import('./features/instruments/instruments').then((m) => m.Instruments),
  },
  {
    path: 'admin/configuration',
    loadComponent: () =>
      import('./features/configuration/configuration').then((m) => m.Configuration),
  },
  {
    path: 'user/preferences',
    loadComponent: () =>
      import('./features/user-preferences/user-preferences').then((m) => m.UserPreferences),
  },
  {
    path: '',
    loadComponent: () => import('./features/home/home').then((m) => m.Home),
    pathMatch: 'full',
  },
];
