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
import { Tooltip } from 'primeng/tooltip';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { EnsemblesApiService } from '../../core/api';
import { Ensemble } from '../../model/datamodels';
import { EnsembleForm } from './ensemble-form';
import { EnsembleVoices } from './ensemble-voices';

@Component({
  selector: 'app-ensembles',
  imports: [
    TableModule,
    Dialog,
    ConfirmDialog,
    Toast,
    Button,
    InputText,
    IconField,
    InputIcon,
    Tooltip,
    TranslatePipe,
    EnsembleForm,
    EnsembleVoices,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './ensembles.html',
  styleUrl: './ensembles.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Ensembles implements OnInit {
  protected readonly t = inject(TranslationService);
  private readonly api = inject(EnsemblesApiService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly ensembles = signal<Ensemble[]>([]);
  protected readonly totalRecords = signal(0);
  protected readonly loading = signal(true);
  protected rows = 10;

  protected dialogVisible = false;
  protected editingEnsemble: Ensemble | null = null;

  protected voicesDialogVisible = false;
  protected voicesEnsemble: Ensemble | null = null;

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

  protected onFilter(event: Event): void {
    this.filterSubject.next((event.target as HTMLInputElement).value);
  }

  protected openNew(): void {
    this.editingEnsemble = null;
    this.dialogVisible = true;
  }

  protected openEdit(ensemble: Ensemble): void {
    this.editingEnsemble = { ...ensemble };
    this.dialogVisible = true;
  }

  protected openVoices(ensemble: Ensemble): void {
    this.voicesEnsemble = ensemble;
    this.voicesDialogVisible = true;
  }

  protected confirmDelete(ensemble: Ensemble): void {
    this.confirmationService.confirm({
      message: this.t.t('ensembles.delete.confirm').replace('{name}', ensemble.name),
      header: this.t.t('ensembles.delete.header'),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.api.delete(ensemble.id!).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: this.t.t('ensembles.messages.deleted'),
            });
            this.loadData();
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: this.t.t('ensembles.messages.error'),
            });
          },
        });
      },
    });
  }

  protected onSaved(): void {
    this.dialogVisible = false;
    const key = this.editingEnsemble
      ? 'ensembles.messages.updated'
      : 'ensembles.messages.created';
    this.messageService.add({ severity: 'success', summary: this.t.t(key) });
    this.loadData();
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
          this.ensembles.set(res.data ?? []);
          this.totalRecords.set(res.totalCount ?? 0);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        },
      });
  }
}
