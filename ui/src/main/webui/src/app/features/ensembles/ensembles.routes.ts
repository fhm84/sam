import { Routes } from '@angular/router';
import { Ensembles } from './ensembles';
import { EnsembleDetailPage } from './ensemble-detail-page';

export const ENSEMBLES_ROUTES: Routes = [
  { path: '', component: Ensembles },
  { path: ':id', component: EnsembleDetailPage },
];
