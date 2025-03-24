import { Routes } from '@angular/router';
import { SheetMusicAddEditComponent } from './components/sheetMusic-add-edit/sheetMusic-add-edit.component';
import { SheetMusicListComponent } from './components/sheetMusic-list/sheetMusic-list.component';

export const routes: Routes = [
  { path: 'sheets', component: SheetMusicListComponent },
  { path: 'sheets/add', component: SheetMusicAddEditComponent },
  { path: 'sheets/:id/edit', component: SheetMusicAddEditComponent },
  { path: '', redirectTo: 'sheets', pathMatch: 'full' }
];
