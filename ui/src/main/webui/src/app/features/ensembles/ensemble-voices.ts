import { Component, inject, Input, OnChanges, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';
import { FloatLabel } from 'primeng/floatlabel';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Checkbox } from 'primeng/checkbox';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { EnsemblesApiService } from '../../core/api';
import { CreateEnsembleVoice, EnsembleVoice } from '../../model/datamodels';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { VoiceOptions } from './voice-options';

@Component({
  selector: 'app-ensemble-voices',
  imports: [
    ReactiveFormsModule,
    TableModule,
    Dialog,
    ConfirmDialog,
    Button,
    Tag,
    Tooltip,
    FloatLabel,
    InputText,
    InputNumber,
    Checkbox,
    TranslatePipe,
    VoiceOptions,
  ],
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

  // ── Voices table ───────────────────────────────────────
  protected readonly voices = signal<EnsembleVoice[]>([]);
  protected readonly loading = signal(true);

  // ── Add dialog ─────────────────────────────────────────
  protected addDialogVisible = false;
  protected saving = false;

  protected readonly addForm = new FormGroup({
    label: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    weight: new FormControl<number | null>(null),
    required: new FormControl(false, { nonNullable: true }),
  });

  // ── Edit dialog ────────────────────────────────────────
  protected editDialogVisible = false;
  protected editingVoice: EnsembleVoice | null = null;

  protected readonly editForm = new FormGroup({
    weight: new FormControl<number | null>(null),
    required: new FormControl(false, { nonNullable: true }),
  });

  // ── Options dialog ─────────────────────────────────────
  protected optionsDialogVisible = false;
  protected optionsVoice: EnsembleVoice | null = null;

  ngOnChanges(): void {
    if (this.ensembleId) {
      this.loadVoices();
    }
  }

  // ── Add ───────────────────────────────────────────────
  protected openNewVoice(): void {
    this.addForm.reset({ label: '', weight: null, required: false });
    this.addDialogVisible = true;
  }

  protected onAdd(): void {
    if (this.addForm.invalid) return;
    const raw = convertEmptyStringsToNull(this.addForm.getRawValue()) as CreateEnsembleVoice;
    this.saving = true;
    this.api.createVoice(this.ensembleId, raw).subscribe({
      next: () => {
        this.saving = false;
        this.addDialogVisible = false;
        this.messageService.add({
          severity: 'success',
          summary: this.t.t('ensembles.voices.messages.created'),
        });
        this.loadVoices();
      },
      error: () => {
        this.saving = false;
      },
    });
  }

  // ── Edit ──────────────────────────────────────────────
  protected openEditVoice(voice: EnsembleVoice): void {
    this.editingVoice = { ...voice };
    this.editForm.reset({ weight: voice.weight ?? null, required: voice.required ?? false });
    this.editDialogVisible = true;
  }

  protected onUpdate(): void {
    if (!this.editingVoice) return;
    const raw = convertEmptyStringsToNull(this.editForm.getRawValue()) as {
      weight: number | null;
      required: boolean;
    };
    const payload: EnsembleVoice = {
      label: this.editingVoice.label,
      weight: raw.weight ?? undefined,
      required: raw.required,
    };
    this.saving = true;
    this.api.updateVoice(this.ensembleId, this.editingVoice.id!, payload).subscribe({
      next: () => {
        this.saving = false;
        this.editDialogVisible = false;
        this.messageService.add({
          severity: 'success',
          summary: this.t.t('ensembles.voices.messages.updated'),
        });
        this.loadVoices();
      },
      error: () => {
        this.saving = false;
      },
    });
  }

  // ── Options ───────────────────────────────────────────
  protected openOptions(voice: EnsembleVoice): void {
    this.optionsVoice = voice;
    this.optionsDialogVisible = true;
  }

  // ── Delete ────────────────────────────────────────────
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
