import { Routes } from '@angular/router';
import { SheetMusicAddEditComponent } from './components/sheetMusic-add-edit/sheetMusic-add-edit.component';
import { SheetMusicListComponent } from './components/sheetMusic-list/sheetMusic-list.component';
import { SheetMusicDetailsComponent } from './components/sheetMusic-details/sheetMusic-details.component';
import { UploadComponent } from './components/upload/upload.component';

export const routes: Routes = [
  { path: 'sheets', component: SheetMusicListComponent },
  { path: 'sheets/add', component: SheetMusicAddEditComponent },
  { path: 'sheets/:id', component: SheetMusicDetailsComponent },
  { path: 'sheets/:id/edit', component: SheetMusicAddEditComponent },
  { path: 'uploads', component: UploadComponent },
  { path: '', redirectTo: 'sheets', pathMatch: 'full' }
];
