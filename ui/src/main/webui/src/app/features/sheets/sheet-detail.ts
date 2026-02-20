import { Component, EventEmitter, inject, Input, OnChanges, Output, signal } from '@angular/core';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { SheetsApiService, InstrumentationsApiService } from '../../core/api';
import { Instrumentation, SheetMusic } from '../../model/datamodels';
import { InstrumentationForm } from './instrumentation-form';

@Component({
  selector: 'app-sheet-detail',
  imports: [TableModule, Dialog, ConfirmDialog, Button, TranslatePipe, InstrumentationForm],
  providers: [ConfirmationService],
  templateUrl: './sheet-detail.html',
  styleUrl: './sheet-detail.scss',
})
export class SheetDetail implements OnChanges {
  protected readonly t = inject(TranslationService);
  private readonly sheetsApi = inject(SheetsApiService);
  private readonly instrumentationsApi = inject(InstrumentationsApiService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  @Input({ required: true }) sheetId!: string;
  @Output() edit = new EventEmitter<SheetMusic>();
  @Output() deleted = new EventEmitter<void>();

  protected readonly sheet = signal<SheetMusic | null>(null);
  protected readonly instrumentations = signal<Instrumentation[]>([]);
  protected readonly loading = signal(true);
  protected readonly instrumentationsLoading = signal(true);

  protected instrumentationDialogVisible = false;
  protected editingInstrumentation: Instrumentation | null = null;

  ngOnChanges(): void {
    if (this.sheetId) {
      this.loadSheet();
      this.loadInstrumentations();
    }
  }

  protected onEdit(): void {
    const s = this.sheet();
    if (s) {
      this.edit.emit(s);
    }
  }

  protected openNewInstrumentation(): void {
    this.editingInstrumentation = null;
    this.instrumentationDialogVisible = true;
  }

  protected openEditInstrumentation(instr: Instrumentation): void {
    this.editingInstrumentation = { ...instr };
    this.instrumentationDialogVisible = true;
  }

  protected confirmDeleteInstrumentation(instr: Instrumentation): void {
    const instrumentName = instr.instrument?.name ?? '';
    this.confirmationService.confirm({
      message: this.t.t('sheets.instrumentations.delete.confirm').replace('{instrument}', instrumentName),
      header: this.t.t('sheets.instrumentations.delete.header'),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.instrumentationsApi.delete(this.sheetId, instr.id!).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: this.t.t('sheets.instrumentations.messages.deleted'),
            });
            this.loadInstrumentations();
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: this.t.t('sheets.instrumentations.messages.error'),
            });
          },
        });
      },
    });
  }

  protected onInstrumentationSaved(): void {
    this.instrumentationDialogVisible = false;
    const key = this.editingInstrumentation
      ? 'sheets.instrumentations.messages.updated'
      : 'sheets.instrumentations.messages.created';
    this.messageService.add({ severity: 'success', summary: this.t.t(key) });
    this.loadInstrumentations();
  }

  reloadSheet(): void {
    this.loadSheet();
  }

  private loadSheet(): void {
    this.loading.set(true);
    this.sheetsApi.load(this.sheetId).subscribe({
      next: (sheet) => {
        this.sheet.set(sheet);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  private loadInstrumentations(): void {
    this.instrumentationsLoading.set(true);
    this.instrumentationsApi.list(this.sheetId).subscribe({
      next: (items) => {
        this.instrumentations.set(items);
        this.instrumentationsLoading.set(false);
      },
      error: () => {
        this.instrumentationsLoading.set(false);
      },
    });
  }
}
