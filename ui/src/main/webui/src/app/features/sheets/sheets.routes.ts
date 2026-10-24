import { Routes } from '@angular/router';
import { roleGuard } from '../../core/auth/role.guard';
import { WRITE_ROLES } from '../../core/auth/roles';
import { Sheets } from './sheets';
import { SheetFormPage } from './sheet-form-page';
import { SheetDetailPage } from './sheet-detail-page';

export const SHEETS_ROUTES: Routes = [
  { path: '', component: Sheets },
  { path: 'new', component: SheetFormPage, canActivate: [roleGuard(...WRITE_ROLES)] },
  { path: ':id', component: SheetDetailPage },
  { path: ':id/edit', component: SheetFormPage, canActivate: [roleGuard(...WRITE_ROLES)] },
];
