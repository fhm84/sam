import { Component, inject, Input, OnChanges, signal } from '@angular/core';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { EnsemblesApiService } from '../../core/api';
import { EnsembleVoice } from '../../model/datamodels';
import { VoiceForm } from './voice-form';
import { VoiceOptions } from './voice-options';

@Component({
  selector: 'app-ensemble-voices',
  imports: [TableModule, Dialog, ConfirmDialog, Button, Tag, Tooltip, TranslatePipe, VoiceForm, VoiceOptions],
  providers: [ConfirmationService],
  templateUrl: './ensemble-voices.html',
  styleUrl: './ensemble-voices.scss',
})
export class EnsembleVoices implements OnChanges {
  protected readonly t = inject(TranslationService);
  private readonly api = inject(EnsemblesApiService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  @Input({ required: true }) ensembleId!: string;

  protected readonly voices = signal<EnsembleVoice[]>([]);
  protected readonly loading = signal(true);

  protected voiceDialogVisible = false;
  protected editingVoice: EnsembleVoice | null = null;

  protected optionsDialogVisible = false;
  protected optionsVoice: EnsembleVoice | null = null;

  ngOnChanges(): void {
    if (this.ensembleId) {
      this.loadVoices();
    }
  }

  protected openNewVoice(): void {
    this.editingVoice = null;
    this.voiceDialogVisible = true;
  }

  protected openEditVoice(voice: EnsembleVoice): void {
    this.editingVoice = { ...voice };
    this.voiceDialogVisible = true;
  }

  protected openOptions(voice: EnsembleVoice): void {
    this.optionsVoice = voice;
    this.optionsDialogVisible = true;
  }

  protected confirmDeleteVoice(voice: EnsembleVoice): void {
    this.confirmationService.confirm({
      message: this.t.t('ensembles.voices.delete.confirm').replace('{label}', voice.label),
      header: this.t.t('ensembles.voices.delete.header'),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.api.deleteVoice(this.ensembleId, voice.id!).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: this.t.t('ensembles.voices.messages.deleted'),
            });
            this.loadVoices();
          },
          error: () => {},
        });
      },
    });
  }

  protected onVoiceSaved(): void {
    this.voiceDialogVisible = false;
    const key = this.editingVoice
      ? 'ensembles.voices.messages.updated'
      : 'ensembles.voices.messages.created';
    this.messageService.add({ severity: 'success', summary: this.t.t(key) });
    this.loadVoices();
  }

  private loadVoices(): void {
    this.loading.set(true);
    this.api.listVoices(this.ensembleId).subscribe({
      next: (voices) => {
        this.voices.set(voices);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }
}
