import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'sheets',
    loadComponent: () => import('./features/sheets/sheets').then((m) => m.Sheets),
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
    path: 'admin/instruments',
    loadComponent: () =>
      import('./features/instruments/instruments').then((m) => m.Instruments),
  },
  {
    path: 'admin/configuration',
    loadComponent: () =>
      import('./features/configuration/configuration').then((m) => m.Configuration),
  },
  { path: '', redirectTo: 'sheets', pathMatch: 'full' },
];
