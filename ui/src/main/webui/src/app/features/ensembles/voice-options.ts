import { Component, inject, Input, OnChanges, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { Select } from 'primeng/select';
import { InputNumber } from 'primeng/inputnumber';
import { FloatLabel } from 'primeng/floatlabel';
import { Tooltip } from 'primeng/tooltip';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { EnsemblesApiService, InstrumentsApiService } from '../../core/api';
import { CreateVoiceOption, Instrument, VoiceOption, VoiceOptionType } from '../../model/datamodels';
import { FETCH_ALL_SIZE } from '../../shared/constants';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';

@Component({
  selector: 'app-voice-options',
  imports: [
    ReactiveFormsModule,
    TableModule,
    Dialog,
    ConfirmDialog,
    Button,
    Tag,
    Select,
    InputNumber,
    FloatLabel,
    Tooltip,
    IconField,
    InputIcon,
    InputText,
    TranslatePipe,
  ],
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

  // ── Options table ──────────────────────────────────────
  protected readonly options = signal<VoiceOption[]>([]);
  protected readonly loading = signal(true);

  // ── Instruments ────────────────────────────────────────
  private readonly allInstruments = signal<Instrument[]>([]);
  protected readonly filteredInstruments = signal<Instrument[]>([]);

  // ── Add dialog ─────────────────────────────────────────
  protected addDialogVisible = false;
  protected selectedInstrument: Instrument | null = null;
  protected saving = false;
  protected readonly typeOptions: VoiceOptionType[] = ['PRIMARY', 'ALTERNATE', 'FALLBACK'];

  protected readonly addForm = new FormGroup({
    type: new FormControl<VoiceOptionType | null>(null),
    factor: new FormControl<number | null>(null),
  });

  // ── Edit dialog ────────────────────────────────────────
  protected editDialogVisible = false;
  protected editingOption: VoiceOption | null = null;

  protected readonly editForm = new FormGroup({
    type: new FormControl<VoiceOptionType | null>(null),
    factor: new FormControl<number | null>(null),
  });

  ngOnInit(): void {
    this.instrumentsApi.find({ size: FETCH_ALL_SIZE }).subscribe((res) => {
      const instruments = res.data ?? [];
      this.allInstruments.set(instruments);
      this.filteredInstruments.set(instruments);
    });
  }

  ngOnChanges(): void {
    if (this.ensembleId && this.voiceId) {
      this.loadOptions();
    }
  }

  protected instrumentName(id: string): string {
    return this.allInstruments().find((i) => i.id === id)?.name ?? id;
  }

  protected instrumentTransposition(id: string): string {
    return this.allInstruments().find((i) => i.id === id)?.transposition ?? '—';
  }

  // ── Add ───────────────────────────────────────────────
  protected openNewOption(): void {
    this.selectedInstrument = null;
    this.filteredInstruments.set(this.allInstruments());
    this.addForm.reset();
    this.addDialogVisible = true;
  }

  protected onFilterInput(event: Event): void {
    const query = (event.target as HTMLInputElement).value.toLowerCase();
    this.filteredInstruments.set(
      this.allInstruments().filter((i) => i.name.toLowerCase().includes(query)),
    );
  }

  protected selectInstrument(instrument: Instrument): void {
    this.selectedInstrument = instrument;
  }

  protected isInstrumentSelected(instrument: Instrument): boolean {
    return this.selectedInstrument?.id === instrument.id;
  }

  protected get canAdd(): boolean {
    return this.selectedInstrument !== null;
  }

  protected onAdd(): void {
    if (!this.canAdd) return;
    const raw = convertEmptyStringsToNull(this.addForm.getRawValue()) as {
      type: VoiceOptionType | null;
      factor: number | null;
    };
    const payload: CreateVoiceOption = {
      instrumentId: this.selectedInstrument!.id!,
      type: raw.type ?? undefined,
      factor: raw.factor ?? undefined,
    };
    this.saving = true;
    this.api.createVoiceOption(this.ensembleId, this.voiceId, payload).subscribe({
      next: () => {
        this.saving = false;
        this.addDialogVisible = false;
        this.messageService.add({
          severity: 'success',
          summary: this.t.t('ensembles.voices.options.messages.created'),
        });
        this.loadOptions();
      },
      error: () => {
        this.saving = false;
      },
    });
  }

  // ── Edit ──────────────────────────────────────────────
  protected openEditOption(option: VoiceOption): void {
    this.editingOption = { ...option };
    this.editForm.reset({ type: option.type ?? null, factor: option.factor ?? null });
    this.editDialogVisible = true;
  }

  protected onUpdate(): void {
    if (!this.editingOption) return;
    const raw = convertEmptyStringsToNull(this.editForm.getRawValue()) as {
      type: VoiceOptionType | null;
      factor: number | null;
    };
    const payload: VoiceOption = {
      instrumentId: this.editingOption.instrumentId,
      type: raw.type ?? undefined,
      factor: raw.factor ?? undefined,
    };
    this.saving = true;
    this.api.updateVoiceOption(this.ensembleId, this.voiceId, this.editingOption.id!, payload).subscribe({
      next: () => {
        this.saving = false;
        this.editDialogVisible = false;
        this.messageService.add({
          severity: 'success',
          summary: this.t.t('ensembles.voices.options.messages.updated'),
        });
        this.loadOptions();
      },
      error: () => {
        this.saving = false;
      },
    });
  }

  // ── Delete ────────────────────────────────────────────
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

  // ── Helpers ───────────────────────────────────────────
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
