import { Component, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';

import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { SheetMusicService } from './services/sheetMusic.service';
import { SheetMusicAddEditComponent } from './sheetMusic-add-edit/sheetMusic-add-edit.component';

import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';

import { MatDialog } from '@angular/material/dialog';
import { SheetMusicListComponent } from './sheetMusic-list/sheetMusic-list.component';

@Component({
  selector: 'app-root',
  imports: [
    CommonModule,
    RouterOutlet,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatRadioModule,
    MatSelectModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    FormsModule,
    SheetMusicListComponent
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.less'
})
export class AppComponent {
  title = 'sam';

  @ViewChild(SheetMusicListComponent) sheetList! : SheetMusicListComponent;

  constructor(
    private dialog: MatDialog,
    private sheetService: SheetMusicService
  ) { }

  openAddEditSheetDialog() {
    const dialogRef = this.dialog.open(SheetMusicAddEditComponent);
    dialogRef.afterClosed().subscribe({
      next: (val) => {
        console.log("dialog after close " + val);
        if (val) {
          // update sheetMusic list
          this.sheetList.loadData(0, this.sheetList.pageSize);
        }
      },
    });
  }

}
