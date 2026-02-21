import { Directive, inject, signal } from '@angular/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { TranslationService } from '../../core/translation.service';
import { DocumentsApiService } from '../../core/api/documents-api.service';
import { DocumentDownload } from '../../model/datamodels';
import { formatSize } from '../utils/format.utils';

@Directive()
export abstract class DocumentHandler {
  protected readonly t = inject(TranslationService);
  protected readonly documentsApi = inject(DocumentsApiService);
  protected readonly confirmationService = inject(ConfirmationService);
  protected readonly messageService = inject(MessageService);

  protected readonly documents = signal<DocumentDownload[]>([]);
  protected readonly documentsLoading = signal(false);
  protected readonly dragging = signal(false);

  protected formatSize = formatSize;

  protected abstract getDocumentBasePath(): string;

  protected downloadDocument(doc: DocumentDownload): void {
    this.documentsApi.download(this.getDocumentBasePath(), doc.id!).subscribe({
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

  protected confirmDeleteDocument(doc: DocumentDownload): void {
    this.confirmationService.confirm({
      message: this.t.t('sheets.detail.deleteDocument.confirm'),
      header: this.t.t('sheets.detail.deleteDocument.header'),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.documentsApi.delete(this.getDocumentBasePath(), doc.id!).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: this.t.t('sheets.detail.documentDeleted'),
            });
            this.loadDocuments();
          },
          error: () => {},
        });
      },
    });
  }

  protected uploadFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.doUpload(file);
    input.value = '';
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragging.set(true);
  }

  protected onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragging.set(false);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragging.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.doUpload(file);
    }
  }

  protected loadDocuments(): void {
    const basePath = this.getDocumentBasePath();
    if (!basePath) return;
    this.documentsLoading.set(true);
    this.documentsApi.list(basePath).subscribe({
      next: (res) => {
        this.documents.set(res.data ?? []);
        this.documentsLoading.set(false);
      },
      error: () => {
        this.documentsLoading.set(false);
      },
    });
  }

  private doUpload(file: File): void {
    this.documentsApi.upload(this.getDocumentBasePath(), file).subscribe({
      next: (ev) => {
        if (ev.type === 'complete') {
          this.messageService.add({
            severity: 'success',
            summary: this.t.t('sheets.detail.uploadComplete'),
          });
          this.loadDocuments();
        }
      },
    });
  }
}
