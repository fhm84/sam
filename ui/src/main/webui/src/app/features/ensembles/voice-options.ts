import { Component, inject, Input, OnChanges, OnInit, signal } from '@angular/core';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { EnsemblesApiService, InstrumentsApiService } from '../../core/api';
import { Instrument, VoiceOption } from '../../model/datamodels';
import { FETCH_ALL_SIZE } from '../../shared/constants';
import { VoiceOptionForm } from './voice-option-form';

@Component({
  selector: 'app-voice-options',
  imports: [TableModule, Dialog, ConfirmDialog, Button, Tag, TranslatePipe, VoiceOptionForm],
  providers: [ConfirmationService],
  templateUrl: './voice-options.html',
  styleUrl: './voice-options.scss',
})
export class VoiceOptions implements OnChanges, OnInit {
  protected readonly t = inject(TranslationService);
  private readonly api = inject(EnsemblesApiService);
  private readonly instrumentsApi = inject(InstrumentsApiService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  @Input({ required: true }) ensembleId!: string;
  @Input({ required: true }) voiceId!: string;

  protected readonly options = signal<VoiceOption[]>([]);
  protected readonly loading = signal(true);
  private readonly instrumentMap = signal<Map<string, string>>(new Map());

  protected optionDialogVisible = false;
  protected editingOption: VoiceOption | null = null;

  ngOnInit(): void {
    this.instrumentsApi.find({ size: FETCH_ALL_SIZE }).subscribe((res) => {
      const map = new Map<string, string>();
      for (const i of res.data ?? []) {
        map.set(i.id!, i.name);
      }
      this.instrumentMap.set(map);
    });
  }

  ngOnChanges(): void {
    if (this.ensembleId && this.voiceId) {
      this.loadOptions();
    }
  }

  protected instrumentName(id: string): string {
    return this.instrumentMap().get(id) ?? id;
  }

  protected openNewOption(): void {
    this.editingOption = null;
    this.optionDialogVisible = true;
  }

  protected openEditOption(option: VoiceOption): void {
    this.editingOption = { ...option };
    this.optionDialogVisible = true;
  }

  protected confirmDeleteOption(option: VoiceOption): void {
    this.confirmationService.confirm({
      message: this.t.t('ensembles.voices.options.delete.confirm'),
      header: this.t.t('ensembles.voices.options.delete.header'),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.api.deleteVoiceOption(this.ensembleId, this.voiceId, option.id!).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: this.t.t('ensembles.voices.options.messages.deleted'),
            });
            this.loadOptions();
          },
          error: () => {},
        });
      },
    });
  }

  protected onOptionSaved(): void {
    this.optionDialogVisible = false;
    const key = this.editingOption
      ? 'ensembles.voices.options.messages.updated'
      : 'ensembles.voices.options.messages.created';
    this.messageService.add({ severity: 'success', summary: this.t.t(key) });
    this.loadOptions();
  }

  protected typeSeverity(type?: string): 'success' | 'warn' | 'secondary' {
    switch (type) {
      case 'PRIMARY':
        return 'success';
      case 'ALTERNATE':
        return 'warn';
      default:
        return 'secondary';
    }
  }

  private loadOptions(): void {
    this.loading.set(true);
    this.api.listVoiceOptions(this.ensembleId, this.voiceId).subscribe({
      next: (opts) => {
        this.options.set(opts);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }
}
