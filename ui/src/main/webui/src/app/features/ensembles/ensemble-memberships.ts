import {
  Component,
  computed,
  inject,
  Input,
  OnChanges,
  OnInit,
  signal,
  ChangeDetectionStrategy,
  DestroyRef,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TableModule } from '@openng/optimus-ui/table';
import { Dialog } from '@openng/optimus-ui/dialog';
import { ConfirmDialog } from '@openng/optimus-ui/confirmdialog';
import { ConfirmationService, MenuItem, MessageService } from '@openng/optimus-ui/api';
import { Button } from '@openng/optimus-ui/button';
import { Tag } from '@openng/optimus-ui/tag';
import { Select } from '@openng/optimus-ui/select';
import { Tooltip } from '@openng/optimus-ui/tooltip';
import { IconField } from '@openng/optimus-ui/iconfield';
import { InputIcon } from '@openng/optimus-ui/inputicon';
import { InputText } from '@openng/optimus-ui/inputtext';
import { FloatLabel } from '@openng/optimus-ui/floatlabel';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { EnsemblesApiService, InstrumentsApiService, MembersApiService, PersonsApiService } from '../../core/api';
import { CreateEnsembleMembership, EnsembleMembership, EnsembleRole, EnsembleVoice, Instrument, Member, Person } from '../../model/datamodels';

const ROLES: EnsembleRole[] = ['MEMBER', 'GUEST', 'SUBSTITUTE', 'CONDUCTOR'];
import { FETCH_ALL_SIZE } from '../../shared/constants';
import { instrumentLabel } from '../../shared/utils/format.utils';
import { RowActions } from '../../shared/components/row-actions/row-actions';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-ensemble-memberships',
  imports: [
    ReactiveFormsModule,
    TableModule,
    Dialog,
    ConfirmDialog,
    RowActions,
    Button,
    Tag,
    Select,
    Tooltip,
    IconField,
    InputIcon,
    InputText,
    FloatLabel,
    TranslatePipe,
  ],
  providers: [ConfirmationService],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './ensemble-memberships.html',
})
export class EnsembleMemberships implements OnChanges, OnInit {
  private readonly destroyRef = inject(DestroyRef);

  protected readonly t = inject(TranslationService);
  private readonly api = inject(EnsemblesApiService);
  private readonly membersApi = inject(MembersApiService);
  private readonly personsApi = inject(PersonsApiService);
  private readonly instrumentsApi = inject(InstrumentsApiService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  @Input({ required: true }) ensembleId!: string;

  // ── Table data ────────────────────────────────────────
  protected readonly members = signal<EnsembleMembership[]>([]);
  protected readonly loading = signal(true);

  // ── Reference data ────────────────────────────────────
  protected readonly voices = signal<EnsembleVoice[]>([]);
  private readonly allMembers = signal<Member[]>([]);
  protected readonly filteredMembers = signal<Member[]>([]);
  protected readonly instrumentOptions = signal<{ label: string; value: string }[]>([]);

  protected readonly roleOptions = computed(() =>
    ROLES.map((r) => ({ label: this.t.t(`ensembles.members.role.${r}`), value: r })),
  );

  // ── Voice options for Select ──────────────────────────
  protected voiceOptions: { label: string; value: string | null }[] = [];

  // ── Add dialog ────────────────────────────────────────
  protected addDialogVisible = false;
  protected selectedMember: Member | null = null;
  protected saving = false;

  protected readonly addForm = new FormGroup({
    voiceId: new FormControl<string | null>(null),
    instrumentId: new FormControl<string | null>(null),
    role: new FormControl<EnsembleRole>('MEMBER', { nonNullable: true }),
  });

  // ── Edit dialog ───────────────────────────────────────
  protected editDialogVisible = false;
  protected editingMember: EnsembleMembership | null = null;

  protected readonly editForm = new FormGroup({
    voiceId: new FormControl<string | null>(null),
    instrumentId: new FormControl<string | null>(null),
    role: new FormControl<EnsembleRole>('MEMBER', { nonNullable: true }),
  });

  ngOnInit(): void {
    this.membersApi.find({ size: FETCH_ALL_SIZE })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((res) => {
        const members = res.data ?? [];
        this.allMembers.set(members);
        this.filteredMembers.set(members);
      });
    this.instrumentsApi.find({ size: FETCH_ALL_SIZE })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((res) => {
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

  // ── Member filter ───────────────────────────────────
  protected readonly memberFilterQuery = signal('');
  protected creatingMember = false;

  /** Persons matching the filter who have no member profile yet, e.g. an arranger who joins the band. */
  protected readonly personCandidates = signal<Person[]>([]);

  protected onMemberFilterInput(event: Event): void {
    const raw = (event.target as HTMLInputElement).value;
    this.memberFilterQuery.set(raw);
    const query = raw.toLowerCase();
    this.filteredMembers.set(
      this.allMembers().filter((m) => m.name.toLowerCase().includes(query)),
    );
    this.searchPersonCandidates(raw.trim());
  }

  private searchPersonCandidates(query: string): void {
    if (query.length < 2) {
      this.personCandidates.set([]);
      return;
    }
    this.personsApi.find({ name: query, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((res) => {
        // A late answer for an older query must not replace the current candidates
        if (this.memberFilterQuery().trim() !== query) return;
        const memberIds = new Set(this.allMembers().map((m) => m.id));
        this.personCandidates.set((res.data ?? []).filter((p) => !memberIds.has(p.id)));
      });
  }

  protected createMember(): void {
    const name = this.memberFilterQuery().trim();
    if (!name) return;
    this.saveNewMember({ name } as Member);
  }

  /** Gives an existing person a member profile (same ID), so their credits and seats stay one record. */
  protected makeMember(person: Person): void {
    this.saveNewMember({ id: person.id, name: person.name } as Member);
  }

  private saveNewMember(member: Member): void {
    this.creatingMember = true;
    this.membersApi.create(member)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (created) => {
          this.creatingMember = false;
          this.allMembers.update((list) => [...list, created]);
          this.filteredMembers.update((list) => [...list, created]);
          this.personCandidates.update((list) => list.filter((p) => p.id !== created.id));
          this.selectMember(created);
          this.messageService.add({
            severity: 'success',
            summary: this.t.t('ensembles.members.messages.memberCreated').replace('{name}', created.name),
          });
        },
        error: () => {
          this.creatingMember = false;
        },
      });
  }

  protected selectMember(member: Member): void {
    this.selectedMember = member;
    // A member's voice seats share one role: start from the role they already have here
    const voiceSeat = this.members().find((m) => m.member.id === member.id && m.voiceId);
    this.addForm.controls.role.setValue(voiceSeat?.role ?? 'MEMBER');
  }

  protected isMemberSelected(member: Member): boolean {
    return this.selectedMember?.id === member.id;
  }

  protected get canAdd(): boolean {
    return this.selectedMember !== null;
  }

  // ── Add ───────────────────────────────────────────────
  protected openAdd(): void {
    this.selectedMember = null;
    this.filteredMembers.set(this.allMembers());
    this.memberFilterQuery.set('');
    this.personCandidates.set([]);
    this.addForm.reset({ voiceId: null, instrumentId: null, role: 'MEMBER' });
    this.addDialogVisible = true;
  }

  protected onAdd(): void {
    if (!this.canAdd) return;
    const raw = this.addForm.getRawValue();
    const payload: CreateEnsembleMembership = {
      memberId: this.selectedMember!.id!,
      voiceId: raw.voiceId ?? undefined,
      instrumentId: raw.instrumentId ?? undefined,
      role: raw.role,
    };
    this.saving = true;
    this.api.addMember(this.ensembleId, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
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
      role: member.role ?? 'MEMBER',
    });
    this.editDialogVisible = true;
  }

  protected onUpdate(): void {
    if (!this.editingMember) return;
    const raw = this.editForm.getRawValue();
    const payload: EnsembleMembership = {
      member: this.editingMember.member,
      voiceId: raw.voiceId ?? undefined,
      instrumentId: raw.instrumentId ?? undefined,
      role: raw.role,
    };
    this.saving = true;
    this.api.updateMember(this.ensembleId, this.editingMember.id!, payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
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
        .replace('{name}', member.member.name),
      header: this.t.t('ensembles.members.delete.header'),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.api.deleteMember(this.ensembleId, member.id!)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
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

  protected rowMenuItems(member: EnsembleMembership): MenuItem[] {
    return [
      { label: this.t.t('ensembles.members.edit'), icon: 'pi pi-pencil', command: () => this.openEdit(member) },
      { label: this.t.t('common.delete'), icon: 'pi pi-trash', styleClass: 'row-menu-danger', command: () => this.confirmDelete(member) },
    ];
  }

  // ── Helpers ───────────────────────────────────────────
  protected instrumentName(id: string | undefined): string {
    if (!id) return '—';
    return this.instrumentOptions().find((o) => o.value === id)?.label ?? id;
  }

  private loadMembers(): void {
    this.loading.set(true);
    this.api.listMembers(this.ensembleId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
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
    this.api.listVoices(this.ensembleId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
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
