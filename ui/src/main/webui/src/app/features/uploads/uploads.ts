import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { TableModule } from 'primeng/table';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { ProgressBar } from 'primeng/progressbar';
import { FileUpload, FileSelectEvent } from 'primeng/fileupload';
import { Toolbar } from 'primeng/toolbar';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { DocumentsApiService, UploadProgress } from '../../core/api/documents-api.service';
import { DocumentDownload } from '../../model/datamodels';
import { formatSize } from '../../shared/utils/format.utils';

interface ActiveUpload {
  filename: string;
  progress: number;
}

@Component({
  selector: 'app-uploads',
  imports: [
    TableModule,
    ConfirmDialog,
    Button,
    ProgressBar,
    FileUpload,
    TranslatePipe,
    Toolbar,
  ],
  providers: [ConfirmationService],
  templateUrl: './uploads.html',
  styleUrl: './uploads.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Uploads implements OnInit {
  protected readonly t = inject(TranslationService);
  private readonly documentsApi = inject(DocumentsApiService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  protected readonly documents = signal<DocumentDownload[]>([]);
  protected readonly totalRecords = signal(0);
  protected readonly loading = signal(true);
  protected readonly uploads = signal<ActiveUpload[]>([]);

  private readonly basePath = this.documentsApi.forTopLevel();

  ngOnInit(): void {
    this.loadDocuments();
  }

  protected onSelect(event: FileSelectEvent): void {
    for (const file of event.files) {
      this.uploadFile(file);
    }
  }

  protected onDownload(doc: DocumentDownload): void {
    this.documentsApi.download(this.basePath, doc.id!).subscribe({
      next: (response) => {
        const blob = response.body!;
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = doc.filename ?? 'download';
        a.click();
        URL.revokeObjectURL(url);
      },
    });
  }

  protected confirmDelete(doc: DocumentDownload): void {
    this.confirmationService.confirm({
      message: this.t.t('uploads.delete.confirm'),
      header: this.t.t('uploads.delete.header'),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.documentsApi.delete(this.basePath, doc.id!).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: this.t.t('uploads.messages.deleted'),
            });
            this.loadDocuments();
          },
        });
      },
    });
  }

  protected formatSize = formatSize;

  protected truncateChecksum(checksum?: string): string {
    if (!checksum) return '';
    return checksum.substring(0, 12) + '...';
  }

  private uploadFile(file: File): void {
    const upload: ActiveUpload = { filename: file.name, progress: 0 };
    this.uploads.update((list) => [...list, upload]);

    this.documentsApi.upload(this.basePath, file).subscribe({
      next: (event: UploadProgress) => {
        if (event.type === 'progress') {
          upload.progress = event.progress ?? 0;
          this.uploads.update((list) => [...list]);
        }
        if (event.type === 'complete') {
          this.uploads.update((list) => list.filter((u) => u !== upload));
          this.messageService.add({
            severity: 'success',
            summary: this.t.t('uploads.messages.uploaded'),
          });
          this.loadDocuments();
        }
      },
      error: () => {
        this.uploads.update((list) => list.filter((u) => u !== upload));
      },
    });
  }

  private loadDocuments(): void {
    this.loading.set(true);
    this.documentsApi.listUnlinked().subscribe({
      next: (res) => {
        this.documents.set(res.data ?? []);
        this.totalRecords.set(res.totalCount ?? 0);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }
}
