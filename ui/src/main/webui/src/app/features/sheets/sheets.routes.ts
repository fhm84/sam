import { Routes } from '@angular/router';
import { Sheets } from './sheets';
import { SheetFormPage } from './sheet-form-page';
import { SheetDetailPage } from './sheet-detail-page';

export const SHEETS_ROUTES: Routes = [
  { path: '', component: Sheets },
  { path: 'new', component: SheetFormPage },
  { path: ':id', component: SheetDetailPage },
  { path: ':id/edit', component: SheetFormPage },
];
