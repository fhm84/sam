import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { UploadService } from '../../services/upload.service';
import { UploadMetadata } from '../../model/upload-metadata.model';
import { AttachmentType } from '../../model/attachement-type.enum';

@Component({
  selector: 'app-upload',
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule
  ],
  templateUrl: './upload.component.html',
  styleUrl: './upload.component.scss'
})
export class UploadComponent {

  attachmentTypes = Object.entries(AttachmentType);
  selectedType: keyof typeof AttachmentType = 'FULL_SCORE';
  selectedFile?: File;
  uploadProgress: number | string = '';
  uploadDone = false;
  uploadSub: Subscription | null = null;

  constructor(private uploadService: UploadService) { }

  onDrop(event: DragEvent) {
    this.uploadDone = false;
    event.preventDefault();
    this.handleFile(event.dataTransfer?.files?.[0]);
  }

  onFileSelected(event: Event) {
    this.uploadDone = false;
    const file = (event.target as HTMLInputElement).files?.[0];
    this.handleFile(file);
  }

  handleFile(file?: File) {
    if (!file) return;
    this.selectedFile = file;
    this.uploadProgress = '';
  }

  upload() {
    if (!this.selectedFile) return;

    const metadata: UploadMetadata = {
      file: this.selectedFile,
      partLabel: this.selectedType // optionally store this in a dedicated field
    };

    this.uploadSub = this.uploadService.upload(metadata)
      .subscribe({
        next: progress => this.uploadProgress = progress,
        error: err => this.uploadProgress = `Error: ${err.message}`,
        complete: () => {
          this.uploadDone = true;
          this.reset();
        }
      });
  }

  allowDrop(event: DragEvent) {
    event.preventDefault();
  }

  cancelUpload() {
    this.uploadSub?.unsubscribe();
    this.reset();
  }

  reset() {
    this.uploadProgress = '';
    this.uploadSub = null;
    this.selectedFile = undefined;
  }

}
