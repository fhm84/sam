import { Component, inject, Input, OnChanges, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { Select } from 'primeng/select';
import { Checkbox } from 'primeng/checkbox';
import { Tooltip } from 'primeng/tooltip';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { FloatLabel } from 'primeng/floatlabel';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { EnsemblesApiService, InstrumentsApiService, MusiciansApiService } from '../../core/api';
import { CreateEnsembleMembership, EnsembleMembership, EnsembleVoice, Instrument, Musician } from '../../model/datamodels';
import { FETCH_ALL_SIZE } from '../../shared/constants';
import { instrumentLabel } from '../../shared/utils/format.utils';

@Component({
  selector: 'app-ensemble-memberships',
  imports: [
    ReactiveFormsModule,
    TableModule,
    Dialog,
    ConfirmDialog,
    Button,
    Tag,
    Select,
    Checkbox,
    Tooltip,
    IconField,
    InputIcon,
    InputText,
    FloatLabel,
    TranslatePipe,
  ],
  providers: [ConfirmationService],
  templateUrl: './ensemble-memberships.html',
})
export class EnsembleMemberships implements OnChanges, OnInit {
  protected readonly t = inject(TranslationService);
  private readonly api = inject(EnsemblesApiService);
  private readonly musiciansApi = inject(MusiciansApiService);
  private readonly instrumentsApi = inject(InstrumentsApiService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  @Input({ required: true }) ensembleId!: string;

  // ── Table data ────────────────────────────────────────
  protected readonly members = signal<EnsembleMembership[]>([]);
  protected readonly loading = signal(true);

  // ── Reference data ────────────────────────────────────
  protected readonly voices = signal<EnsembleVoice[]>([]);
  private readonly allMusicians = signal<Musician[]>([]);
  protected readonly filteredMusicians = signal<Musician[]>([]);
  protected readonly instrumentOptions = signal<{ label: string; value: string }[]>([]);

  // ── Voice options for Select ──────────────────────────
  protected voiceOptions: { label: string; value: string | null }[] = [];

  // ── Add dialog ────────────────────────────────────────
  protected addDialogVisible = false;
  protected selectedMusician: Musician | null = null;
  protected saving = false;

  protected readonly addForm = new FormGroup({
    voiceId: new FormControl<string | null>(null),
    instrumentId: new FormControl<string | null>(null),
    conductor: new FormControl(false, { nonNullable: true }),
  });

  // ── Edit dialog ───────────────────────────────────────
  protected editDialogVisible = false;
  protected editingMember: EnsembleMembership | null = null;

  protected readonly editForm = new FormGroup({
    voiceId: new FormControl<string | null>(null),
    instrumentId: new FormControl<string | null>(null),
    conductor: new FormControl(false, { nonNullable: true }),
  });

  ngOnInit(): void {
    this.musiciansApi.find({ size: FETCH_ALL_SIZE }).subscribe((res) => {
      const musicians = res.data ?? [];
      this.allMusicians.set(musicians);
      this.filteredMusicians.set(musicians);
    });
    this.instrumentsApi.find({ size: FETCH_ALL_SIZE }).subscribe((res) => {
      this.instrumentOptions.set(
        (res.data ?? []).map((i: Instrument) => ({ label: instrumentLabel(i), value: i.id! })),
      );
    });
  }

  ngOnChanges(): void {
    if (this.ensembleId) {
      this.loadMembers();
      this.loadVoices();
    }
  }

  // ── Musician filter ───────────────────────────────────
  protected readonly musicianFilterQuery = signal('');
  protected creatingMusician = false;

  protected onMusicianFilterInput(event: Event): void {
    const raw = (event.target as HTMLInputElement).value;
    this.musicianFilterQuery.set(raw);
    const query = raw.toLowerCase();
    this.filteredMusicians.set(
      this.allMusicians().filter((m) => m.name.toLowerCase().includes(query)),
    );
  }

  protected createMusician(): void {
    const name = this.musicianFilterQuery().trim();
    if (!name) return;
    this.creatingMusician = true;
    this.musiciansApi.create({ name } as Musician).subscribe({
      next: (created) => {
        this.creatingMusician = false;
        this.allMusicians.update((list) => [...list, created]);
        this.filteredMusicians.update((list) => [...list, created]);
        this.selectedMusician = created;
        this.messageService.add({
          severity: 'success',
          summary: this.t
            .t('ensembles.members.musician.createWithName')
            .replace('{name}', created.name),
        });
      },
      error: () => {
        this.creatingMusician = false;
      },
    });
  }

  protected selectMusician(musician: Musician): void {
    this.selectedMusician = musician;
  }

  protected isMusicianSelected(musician: Musician): boolean {
    return this.selectedMusician?.id === musician.id;
  }

  protected get canAdd(): boolean {
    return this.selectedMusician !== null;
  }

  // ── Add ───────────────────────────────────────────────
  protected openAdd(): void {
    this.selectedMusician = null;
    this.filteredMusicians.set(this.allMusicians());
    this.addForm.reset({ voiceId: null, instrumentId: null, conductor: false });
    this.addDialogVisible = true;
  }

  protected onAdd(): void {
    if (!this.canAdd) return;
    const raw = this.addForm.getRawValue();
    const payload: CreateEnsembleMembership = {
      musicianId: this.selectedMusician!.id!,
      voiceId: raw.voiceId ?? undefined,
      instrumentId: raw.instrumentId ?? undefined,
      conductor: raw.conductor,
    };
    this.saving = true;
    this.api.addMember(this.ensembleId, payload).subscribe({
      next: () => {
        this.saving = false;
        this.addDialogVisible = false;
        this.messageService.add({
          severity: 'success',
          summary: this.t.t('ensembles.members.messages.created'),
        });
        this.loadMembers();
      },
      error: () => {
        this.saving = false;
      },
    });
  }

  // ── Edit ──────────────────────────────────────────────
  protected openEdit(member: EnsembleMembership): void {
    this.editingMember = { ...member };
    this.editForm.reset({
      voiceId: member.voiceId ?? null,
      instrumentId: member.instrumentId ?? null,
      conductor: member.conductor,
    });
    this.editDialogVisible = true;
  }

  protected onUpdate(): void {
    if (!this.editingMember) return;
    const raw = this.editForm.getRawValue();
    const payload: EnsembleMembership = {
      musician: this.editingMember.musician,
      voiceId: raw.voiceId ?? undefined,
      instrumentId: raw.instrumentId ?? undefined,
      conductor: raw.conductor,
    };
    this.saving = true;
    this.api.updateMember(this.ensembleId, this.editingMember.id!, payload).subscribe({
      next: () => {
        this.saving = false;
        this.editDialogVisible = false;
        this.messageService.add({
          severity: 'success',
          summary: this.t.t('ensembles.members.messages.updated'),
        });
        this.loadMembers();
      },
      error: () => {
        this.saving = false;
      },
    });
  }

  // ── Delete ────────────────────────────────────────────
  protected confirmDelete(member: EnsembleMembership): void {
    this.confirmationService.confirm({
      message: this.t
        .t('ensembles.members.delete.confirm')
        .replace('{name}', member.musician.name),
      header: this.t.t('ensembles.members.delete.header'),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.api.deleteMember(this.ensembleId, member.id!).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: this.t.t('ensembles.members.messages.deleted'),
            });
            this.loadMembers();
          },
          error: () => {},
        });
      },
    });
  }

  // ── Helpers ───────────────────────────────────────────
  protected instrumentName(id: string | undefined): string {
    if (!id) return '—';
    return this.instrumentOptions().find((o) => o.value === id)?.label ?? id;
  }

  private loadMembers(): void {
    this.loading.set(true);
    this.api.listMembers(this.ensembleId).subscribe({
      next: (list) => {
        this.members.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  private loadVoices(): void {
    this.api.listVoices(this.ensembleId).subscribe({
      next: (list) => {
        this.voices.set(list);
        this.voiceOptions = [
          { label: '—', value: null },
          ...list.map((v) => ({ label: v.label, value: v.id! })),
        ];
      },
      error: () => {},
    });
  }
}
