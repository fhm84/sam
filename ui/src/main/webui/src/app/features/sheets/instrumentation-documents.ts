import { Component, Input, OnChanges, Output, EventEmitter, SimpleChanges } from '@angular/core';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { Checkbox } from 'primeng/checkbox';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { DocumentHandler } from '../../shared/base/document-handler';
import { Attachment, Instrumentation } from '../../model/datamodels';

export interface DocToggleEvent {
  instrId: string;
  docId: string;
  selected: boolean;
}

export interface DocsLoadedEvent {
  instrId: string;
  docs: Attachment[];
}

@Component({
  selector: 'app-instrumentation-documents',
  imports: [Button, Tooltip, Checkbox, FormsModule, TranslatePipe],
  templateUrl: './instrumentation-documents.html',
  styleUrl: './instrumentation-documents.scss',
})
export class InstrumentationDocuments extends DocumentHandler implements OnChanges {
  @Input({ required: true }) sheetId!: string;
  @Input({ required: true }) instrumentation!: Instrumentation;
  @Input() selectedIds: Set<string> = new Set();

  @Output() docsLoaded = new EventEmitter<DocsLoadedEvent>();
  @Output() docToggle = new EventEmitter<DocToggleEvent>();
  @Output() relinkRequested = new EventEmitter<Attachment>();
  @Output() alsoLinkRequested = new EventEmitter<Attachment>();

  protected override getDocumentBasePath(): string {
    return this.documentsApi.forInstrumentations(this.sheetId, this.instrumentation.id!);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['sheetId'] || changes['instrumentation']) && this.sheetId && this.instrumentation?.id) {
      // Attachments are already included in the instrumentation list response — no extra HTTP call needed.
      const docs = this.instrumentation.attachments ?? [];
      this.documents.set(docs);
      this.docsLoaded.emit({ instrId: this.instrumentation.id!, docs });
    }
  }

  protected override loadDocuments(): void {
    const basePath = this.getDocumentBasePath();
    if (!basePath) return;
    this.documentsLoading.set(true);
    this.documentsApi.list(basePath).subscribe({
      next: (res) => {
        const docs = res.data ?? [];
        this.documents.set(docs);
        this.documentsLoading.set(false);
        this.docsLoaded.emit({ instrId: this.instrumentation.id!, docs });
      },
      error: () => {
        this.documentsLoading.set(false);
      },
    });
  }

  protected isSelected(doc: Attachment): boolean {
    return this.selectedIds.has(doc.id!);
  }

  protected toggleDoc(doc: Attachment): void {
    this.docToggle.emit({
      instrId: this.instrumentation.id!,
      docId: doc.id!,
      selected: !this.isSelected(doc),
    });
  }

  protected downloadAttachment(doc: Attachment): void {
    this.documentsApi.download(this.getDocumentBasePath(), doc.id!).subscribe({
      next: (response) => {
        const blob = response.body!;
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = doc.displayName ?? 'download';
        a.click();
        URL.revokeObjectURL(url);
      },
    });
  }
}
