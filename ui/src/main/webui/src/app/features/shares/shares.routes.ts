import { Routes } from '@angular/router';

export const SHARES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./shares-page').then((m) => m.SharesPage),
  },
];
