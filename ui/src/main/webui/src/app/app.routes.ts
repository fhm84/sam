import { Routes } from '@angular/router';
import { SheetMusicAddEditComponent } from './sheetMusic-add-edit/sheetMusic-add-edit.component';

export const routes: Routes = [
  { path: '', redirectTo: 'sheets', pathMatch: 'full' },
  { path: 'sheets/add', component: SheetMusicAddEditComponent },
  { path: 'sheets/:id/edit', component: SheetMusicAddEditComponent }
  //{ path: 'add', component: AddSheetComponent }
];
