import { Component, computed, DestroyRef, inject, Input, OnChanges, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { FloatLabel } from 'primeng/floatlabel';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Select } from 'primeng/select';
import { AutoComplete, AutoCompleteCompleteEvent } from 'primeng/autocomplete';
import { Textarea } from 'primeng/textarea';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { MessageService } from 'primeng/api';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { AdminUsersApiService, InstrumentsApiService, MusiciansApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { Instrument, Musician, MusicianContact, MusicianInstrument, MusicianMembership, MusicianRole, MusicianStatus, UserInfo } from '../../model/datamodels';
import { map, Observable } from 'rxjs';
import { BaseForm } from '../../shared/base/base-form';
import { AuthService } from '../../core/auth/auth.service';

const STATUSES: MusicianStatus[] = ['ACTIVE', 'INACTIVE', 'INVITED', 'PENDING'];
const ROLES: MusicianRole[] = ['MEMBER', 'GUEST', 'SUBSTITUTE', 'CONDUCTOR'];

@Component({
  selector: 'app-musician-form',
  imports: [ReactiveFormsModule, FormsModule, FloatLabel, InputText, InputNumber, Select, AutoComplete, Textarea, Button, Tooltip, TranslatePipe],
  providers: [MessageService],
  templateUrl: './musician-form.html',
})
export class MusicianForm extends BaseForm<Musician, Musician> implements OnChanges {
  private readonly api = inject(MusiciansApiService);
  private readonly adminUsersApi = inject(AdminUsersApiService);
  private readonly instrumentsApi = inject(InstrumentsApiService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly auth = inject(AuthService);

  @Input() musician: Musician | null = null;

  protected readonly statusOptions = computed(() =>
    STATUSES.map((s) => ({ label: this.t.t(`musicians.status.${s}`), value: s })),
  );

  protected readonly roleOptions = computed(() =>
    ROLES.map((r) => ({ label: this.t.t(`musicians.role.${r}`), value: r })),
  );

  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    ipi: new FormControl('', { nonNullable: true }),
    birthYear: new FormControl<number | null>(null),
    deathYear: new FormControl<number | null>(null),
    email: new FormControl('', { nonNullable: true }),
    mobile: new FormControl('', { nonNullable: true }),
    notes: new FormControl('', { nonNullable: true }),
    status: new FormControl<MusicianStatus | null>(null),
    role: new FormControl<MusicianRole | null>(null),
    instruments: new FormControl<MusicianInstrument[]>([], { nonNullable: true }),
  });

  readonly linkedUser = signal<UserInfo | null>(null);
  readonly userSuggestions = signal<UserInfo[]>([]);
  userSearchModel: UserInfo | null = null;
  readonly linking = signal(false);

  readonly instrumentSuggestions = signal<Instrument[]>([]);

  getEntity = () => this.musician;

  patchFormValues(m: Musician): void {
    this.form.patchValue({
      name: m.name,
      ipi: m.ipi ?? '',
      birthYear: m.birthYear ?? null,
      deathYear: m.deathYear ?? null,
      email: m.contact?.email ?? '',
      mobile: m.contact?.mobile ?? '',
      notes: m.contact?.notes ?? '',
      status: m.membership?.status ?? null,
      role: m.membership?.role ?? null,
      instruments: m.instruments ?? [],
    });
    this.linkedUser.set(null);
    this.userSearchModel = null;
    if (m.userId && this.auth.isAdmin()) {
      this.adminUsersApi.getById(m.userId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (u) => {
            this.linkedUser.set(u);
            this.userSearchModel = u;
          },
          error: () => this.linkedUser.set(null),
        });
    }
  }

  buildSaveRequest(): Observable<Musician> {
    const raw = convertEmptyStringsToNull(this.form.getRawValue()) as Record<string, unknown>;

    const contact: MusicianContact = { email: raw['email'] as string, mobile: raw['mobile'] as string, notes: raw['notes'] as string };
    const membership: MusicianMembership = { status: raw['status'] as MusicianStatus, role: raw['role'] as MusicianRole };

    const payload: Musician = {
      name: raw['name'] as string,
      ipi: raw['ipi'] as string | undefined,
      birthYear: raw['birthYear'] as number | undefined,
      deathYear: raw['deathYear'] as number | undefined,
      instruments: raw['instruments'] as MusicianInstrument[],
      contact: Object.values(contact).some((v) => v != null) ? contact : undefined,
      membership: Object.values(membership).some((v) => v != null) ? membership : undefined,
    };

    return this.isEdit
      ? this.api.update(this.musician!.id!, payload).pipe(map(() => ({ ...payload, id: this.musician!.id, userId: this.musician!.userId }) as Musician))
      : this.api.create(payload);
  }

  userDisplayLabel(u: UserInfo): string {
    const parts: string[] = [];
    if (u.firstName) parts.push(u.firstName);
    if (u.lastName) parts.push(u.lastName);
    const name = parts.join(' ').trim();
    if (name && u.email) return `${name} (${u.email})`;
    if (name) return name;
    return u.email ?? u.username;
  }

  onUserSearch(event: AutoCompleteCompleteEvent): void {
    if (!event.query.trim()) {
      this.userSuggestions.set([]);
      return;
    }
    this.adminUsersApi.search(event.query)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((results) => this.userSuggestions.set(results));
  }

  onUserSelect(user: UserInfo): void {
    this.linking.set(true);
    this.api.linkUser(this.musician!.id!, user.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.linkedUser.set(user);
          this.linking.set(false);
          this.messageService.add({ severity: 'success', summary: this.t.t('musicians.form.linkSuccess') });
        },
        error: () => {
          this.userSearchModel = this.linkedUser();
          this.linking.set(false);
          this.messageService.add({ severity: 'error', summary: this.t.t('musicians.form.linkError') });
        },
      });
  }

  onUnlink(): void {
    this.linking.set(true);
    this.api.unlinkUser(this.musician!.id!)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.linkedUser.set(null);
          this.userSearchModel = null;
          this.linking.set(false);
          this.messageService.add({ severity: 'success', summary: this.t.t('musicians.form.unlinkSuccess') });
        },
        error: () => {
          this.linking.set(false);
          this.messageService.add({ severity: 'error', summary: this.t.t('musicians.form.unlinkError') });
        },
      });
  }

  onInstrumentSearch(event: AutoCompleteCompleteEvent): void {
    if (!event.query.trim()) {
      this.instrumentSuggestions.set([]);
      return;
    }
    this.instrumentsApi.find({ name: event.query, size: 20 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => this.instrumentSuggestions.set(result.data ?? []));
  }

  onInstrumentSelect(instrument: Instrument): void {
    const current = this.form.controls.instruments.value;
    if (current.some((mi) => mi.instrumentId === instrument.id)) {
      return;
    }
    const mi: MusicianInstrument = {
      instrumentId: instrument.id,
      instrumentName: instrument.name ?? instrument.displayName,
      primary: current.length === 0,
    };
    this.form.controls.instruments.setValue([...current, mi]);
  }

  onInstrumentRemove(instrumentId: string): void {
    const updated = this.form.controls.instruments.value.filter((mi) => mi.instrumentId !== instrumentId);
    this.form.controls.instruments.setValue(updated);
  }

  instrumentLabel(mi: MusicianInstrument): string {
    return mi.instrumentName ?? mi.instrumentId ?? '';
  }
}
