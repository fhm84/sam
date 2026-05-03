import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TranslatePipe } from '../pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { SharesApiService } from '../../core/api/shares-api.service';
import { ShareResponse, ShareType } from '../../model/datamodels';

interface ExpiryOption {
  labelKey: string;
  days: number | null;
}

const EXPIRY_OPTIONS: ExpiryOption[] = [
  { labelKey: 'shares.dialog.expiry.never', days: null },
  { labelKey: 'shares.dialog.expiry.7days', days: 7 },
  { labelKey: 'shares.dialog.expiry.30days', days: 30 },
  { labelKey: 'shares.dialog.expiry.1year', days: 365 },
];

@Component({
  selector: 'app-share-dialog',
  imports: [Dialog, Button, InputText, Select, FormsModule, TranslatePipe],
  templateUrl: './share-dialog.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShareDialogComponent {
  protected readonly t = inject(TranslationService);
  private readonly sharesApi = inject(SharesApiService);

  readonly resourceType = input.required<ShareType>();
  readonly resourceId = input.required<string>();
  readonly resourceLabel = input.required<string>();
  readonly visible = input(false);
  readonly visibleChange = output<boolean>();

  protected readonly phase = signal<'setup' | 'creating' | 'ready' | 'error'>('setup');
  protected readonly share = signal<ShareResponse | null>(null);
  protected readonly copied = signal(false);

  protected readonly expiryOptions = EXPIRY_OPTIONS;
  protected selectedExpiry: ExpiryOption = EXPIRY_OPTIONS[0];

  protected get expiryOptionLabels(): Array<{ label: string; value: ExpiryOption }> {
    return this.expiryOptions.map((o) => ({ label: this.t.t(o.labelKey), value: o }));
  }

  protected onVisibleChange(visible: boolean): void {
    if (visible) {
      this.phase.set('setup');
      this.share.set(null);
      this.copied.set(false);
      this.selectedExpiry = EXPIRY_OPTIONS[0];
    }
    this.visibleChange.emit(visible);
  }

  protected createLink(): void {
    this.phase.set('creating');
    const expiresAt = this.selectedExpiry.days != null ? this.computeExpiresAt(this.selectedExpiry.days) : undefined;
    this.sharesApi
      .create({ resourceType: this.resourceType(), resourceId: this.resourceId(), expiresAt })
      .subscribe({
        next: (s) => {
          this.share.set(s);
          this.phase.set('ready');
        },
        error: () => this.phase.set('error'),
      });
  }

  private computeExpiresAt(days: number): Date {
    const d = new Date();
    d.setDate(d.getDate() + days);
    return d;
  }

  protected get shareUrl(): string {
    const s = this.share();
    return s?.id ? `${window.location.origin}/share/${s.id}` : '';
  }

  protected copyLink(): void {
    navigator.clipboard.writeText(this.shareUrl).then(() => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    });
  }

  protected close(): void {
    this.visibleChange.emit(false);
  }
}
