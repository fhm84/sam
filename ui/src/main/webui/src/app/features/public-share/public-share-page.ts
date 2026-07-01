import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { SharesApiService } from '../../core/api/shares-api.service';
import { PublicShareInfo } from '../../model/datamodels';

@Component({
  selector: 'app-public-share-page',
  imports: [Button, Tag, TranslatePipe],
  templateUrl: './public-share-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicSharePage implements OnInit {
  protected readonly t = inject(TranslationService);
  private readonly route = inject(ActivatedRoute);
  private readonly sharesApi = inject(SharesApiService);

  protected readonly phase = signal<'loading' | 'ready' | 'error' | 'expired'>('loading');
  protected readonly info = signal<PublicShareInfo | null>(null);
  private token = '';

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token') ?? '';
    if (!this.token) {
      this.phase.set('error');
      return;
    }
    this.sharesApi.getPublicInfo(this.token).subscribe({
      next: (data) => {
        this.info.set(data);
        this.phase.set(data.expired ? 'expired' : 'ready');
      },
      error: () => this.phase.set('error'),
    });
  }

  protected expiredTitle(): string | null {
    const data = this.info();
    return data ? (data.sheetTitle ?? data.collectionName ?? null) : null;
  }

  protected downloadInstrumentation(): void {
    window.open(this.sharesApi.instrDownloadUrl(this.token));
  }

  protected downloadToc(): void {
    window.open(this.sharesApi.collectionTocUrl(this.token));
  }

  protected downloadCollectionAll(): void {
    window.open(this.sharesApi.collectionDownloadAllUrl(this.token));
  }

  protected downloadPart(instrumentationId: string): void {
    window.open(this.sharesApi.collectionPartUrl(this.token, instrumentationId));
  }

  protected downloadSheetAll(): void {
    window.open(this.sharesApi.sheetDownloadAllUrl(this.token));
  }

  protected downloadSheetPart(instrumentationId: string): void {
    window.open(this.sharesApi.sheetPartUrl(this.token, instrumentationId));
  }

  protected downloadSheetAttachment(attachmentId: string): void {
    window.open(this.sharesApi.sheetAttachmentUrl(this.token, attachmentId));
  }
}
