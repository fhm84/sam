import { Routes } from '@angular/router';
import { Collections } from './collections';
import { CollectionDetailPage } from './collection-detail-page';

export const COLLECTIONS_ROUTES: Routes = [
  { path: '', component: Collections },
  { path: ':id', component: CollectionDetailPage },
];
