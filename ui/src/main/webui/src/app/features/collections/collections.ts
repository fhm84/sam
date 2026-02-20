import { LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime } from 'rxjs';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { SelectButton } from 'primeng/selectbutton';
import { Paginator } from 'primeng/paginator';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { CollectionsApiService } from '../../core/api';
import { SheetCollection } from '../../model/datamodels';
import { CollectionForm } from './collection-form';

@Component({
  selector: 'app-collections',
  imports: [
    TableModule,
    Dialog,
    ConfirmDialog,
    Toast,
    Button,
    InputText,
    IconField,
    InputIcon,
    SelectButton,
    Paginator,
    LowerCasePipe,
    FormsModule,
    TranslatePipe,
    CollectionForm,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './collections.html',
  styleUrl: './collections.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Collections implements OnInit {
  protected readonly t = inject(TranslationService);
  private readonly api = inject(CollectionsApiService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly collections = signal<SheetCollection[]>([]);
  protected readonly totalRecords = signal(0);
  protected readonly loading = signal(true);
  protected readonly viewMode = signal<'list' | 'cards'>('list');
  protected rows = 10;

  protected dialogVisible = false;
  protected editingCollection: SheetCollection | null = null;

  protected readonly viewOptions = [
    { icon: 'pi pi-list', value: 'list' },
    { icon: 'pi pi-th-large', value: 'cards' },
  ];

  private currentPage = 0;
  private nameFilter = '';
  private readonly filterSubject = new Subject<string>();

  ngOnInit(): void {
    this.filterSubject
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => {
        this.nameFilter = value;
        this.currentPage = 0;
        this.loadData();
      });
  }

  protected onLazyLoad(event: TableLazyLoadEvent): void {
    this.currentPage = (event.first ?? 0) / (event.rows ?? this.rows);
    this.rows = event.rows ?? this.rows;
    this.loadData();
  }

  protected onPageChange(event: { first?: number; rows?: number }): void {
    this.currentPage = (event.first ?? 0) / (event.rows ?? this.rows);
    this.rows = event.rows ?? this.rows;
    this.loadData();
  }

  protected onFilter(event: Event): void {
    this.filterSubject.next((event.target as HTMLInputElement).value);
  }

  protected onViewModeChange(): void {
    this.currentPage = 0;
    this.loadData();
  }

  protected openNew(): void {
    this.editingCollection = null;
    this.dialogVisible = true;
  }

  protected openEdit(collection: SheetCollection): void {
    this.editingCollection = { ...collection };
    this.dialogVisible = true;
  }

  protected confirmDelete(collection: SheetCollection): void {
    this.confirmationService.confirm({
      message: this.t.t('collections.delete.confirm').replace('{name}', collection.name),
      header: this.t.t('collections.delete.header'),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.api.delete(collection.id!).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: this.t.t('collections.messages.deleted'),
            });
            this.loadData();
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: this.t.t('collections.messages.error'),
            });
          },
        });
      },
    });
  }

  protected onSaved(): void {
    this.dialogVisible = false;
    const key = this.editingCollection
      ? 'collections.messages.updated'
      : 'collections.messages.created';
    this.messageService.add({ severity: 'success', summary: this.t.t(key) });
    this.loadData();
  }

  protected formatDate(date?: Date): string {
    if (!date) return '';
    const d = new Date(date);
    return d.toLocaleDateString();
  }

  private loadData(): void {
    this.loading.set(true);
    this.api
      .find({
        page: this.currentPage,
        size: this.rows,
        name: this.nameFilter || undefined,
      })
      .subscribe({
        next: (res) => {
          this.collections.set(res.data ?? []);
          this.totalRecords.set(res.totalCount ?? 0);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        },
      });
  }
}
