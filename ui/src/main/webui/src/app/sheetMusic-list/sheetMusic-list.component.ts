import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { SheetMusic } from '../model/sheetMusic';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { SheetMusicService } from '../services/sheetMusic.service';
import { SheetMusicAddEditComponent } from '../sheetMusic-add-edit/sheetMusic-add-edit.component';

@Component({
  selector: 'sheetMusic-list',
  imports: [
    CommonModule,
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
    FormsModule
  ],
  templateUrl: './sheetMusic-list.component.html',
  styleUrls: ['./sheetMusic-list.component.css']
})
export class SheetMusicListComponent implements OnInit {

  // the columns that will be displayed in the sheets table
  displayedColumns: string[] = [
    //'id',
    'title',
    'publisher',
    'composer',
    'genre',
    'type',
    'action'
  ];

  sheets: SheetMusic[] = [];

  // sheetMusic list will be assigned to this and it is passed as the data source to the mat-table in the HTML template
  dataSource = new MatTableDataSource<any>();
  totalItems = 0;
  pageSize = 10;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private dialog: MatDialog,
    private sheetMusicService: SheetMusicService) { }

  ngOnInit(): void {
    this.loadData(0, this.pageSize);
  }

  loadData(pageIndex: number, pageSize: number) {
    console.log("pageIndex: ${pageIndex}, pageSize: ${pageSize}");
    this.sheetMusicService.getSheetList(pageIndex, pageSize).subscribe({
      next: (res) => {
        this.dataSource.data = res.data;
        this.totalItems = res.totalCount;
        console.log(res);
      },
      error: (err) => {
        console.log(err);
      },
    });
  }

  onPageChange(event: any) {
    this.loadData(event.pageIndex, event.pageSize);
  }

  // for searching sheets with title, publisher, genre etc
  applyFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    // TODO: filter in backend!?
    this.dataSource.filter = filterValue.trim().toLowerCase();
    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  deleteSheet(id: string) {
    let confirm = window.confirm("Are you sure you want to delete this sheetMusic?");
    if (confirm) {
      this.sheetMusicService.deleteSheet(id).subscribe({
        next: (res) => {
          alert('Sheet deleted!');
          this.loadData(0, this.pageSize);
        },
        error: (err) => {
          console.log(err);
        },
      });
    }
  }

  openEditForm(data: any) {
    const dialogRef = this.dialog.open(SheetMusicAddEditComponent, {
      data,
    });

    dialogRef.afterClosed().subscribe({
      next: (val) => {
        if (val) {
          this.loadData(0, this.pageSize);
        }
      }
    });
  }

}
