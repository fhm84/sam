import { DestroyRef, Directive, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime } from 'rxjs';
import { TableLazyLoadEvent } from 'primeng/table';
import { ConfirmationService, MessageService } from 'primeng/api';
import { TranslationService } from '../../core/translation.service';
import { PaginationRequest } from '../../model/datamodels';
import { CrudApi } from './crud-api.interface';

@Directive()
export abstract class BaseCrudList<T, F extends PaginationRequest> implements OnInit {
  protected readonly t = inject(TranslationService);
  protected readonly confirmationService = inject(ConfirmationService);
  protected readonly messageService = inject(MessageService);
  protected readonly destroyRef = inject(DestroyRef);

  protected readonly items = signal<T[]>([]);
  protected readonly totalRecords = signal(0);
  protected readonly loading = signal(true);
  protected rows = 10;

  protected dialogVisible = false;
  protected editingItem: T | null = null;

  protected currentPage = 0;
  protected nameFilter = '';
  protected readonly filterSubject = new Subject<string>();

  abstract api: CrudApi<T, F>;
  abstract translationPrefix: string;
  abstract getItemId(item: T): string;
  abstract getItemName(item: T): string;
  abstract buildFilter(): F;

  ngOnInit(): void {
    this.filterSubject
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => {
        this.nameFilter = value;
        this.currentPage = 0;
        this.loadData();
      });
    this.init();
  }

  protected init(): void {}

  protected onLazyLoad(event: TableLazyLoadEvent): void {
    this.currentPage = (event.first ?? 0) / (event.rows ?? this.rows);
    this.rows = event.rows ?? this.rows;
    this.loadData();
  }

  protected onFilter(event: Event): void {
    this.filterSubject.next((event.target as HTMLInputElement).value);
  }

  protected openNew(): void {
    this.editingItem = null;
    this.dialogVisible = true;
  }

  protected openEdit(item: T): void {
    this.editingItem = { ...item };
    this.dialogVisible = true;
  }

  protected confirmDelete(item: T): void {
    this.confirmationService.confirm({
      message: this.t.t(`${this.translationPrefix}.delete.confirm`).replace('{name}', this.getItemName(item)),
      header: this.t.t(`${this.translationPrefix}.delete.header`),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.api.delete(this.getItemId(item)).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: this.t.t(`${this.translationPrefix}.messages.deleted`),
            });
            this.loadData();
          },
          error: () => {},
        });
      },
    });
  }

  protected onSaved(): void {
    this.dialogVisible = false;
    const key = this.editingItem
      ? `${this.translationPrefix}.messages.updated`
      : `${this.translationPrefix}.messages.created`;
    this.messageService.add({ severity: 'success', summary: this.t.t(key) });
    this.loadData();
  }

  protected loadData(): void {
    this.loading.set(true);
    this.api.find(this.buildFilter()).subscribe({
      next: (res) => {
        this.items.set(res.data ?? []);
        this.totalRecords.set(res.totalCount ?? 0);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }
}
