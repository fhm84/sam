import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpEventType } from '@angular/common/http';
import { Observable, catchError, map, throwError } from 'rxjs';
import { UploadMetadata } from '../model/upload-metadata.model';

@Injectable({
    providedIn: 'root'
})
export class UploadService {

    private readonly uploadUrl = '/api/documents';

    constructor(private http: HttpClient) { }

    upload(metadata: UploadMetadata): Observable<number | string> {
        const formData = new FormData();
        formData.append('file', metadata.file);

        if (metadata.title) formData.append('title', metadata.title);
        if (metadata.composer) formData.append('composer', metadata.composer);
        if (metadata.instrument) formData.append('instrument', metadata.instrument);
        if (metadata.transposition) formData.append('transposition', metadata.transposition);
        if (metadata.partLabel) formData.append('partLabel', metadata.partLabel);

        return this.http.post<{ fileName: string }>(
            this.uploadUrl,
            formData,
            { observe: 'events', reportProgress: true }
        ).pipe(
            map(event => {
                switch (event.type) {
                    case HttpEventType.UploadProgress:
                        return Math.round((event.loaded / (event.total || 1)) * 100);
                    case HttpEventType.Response:
                        return event.body?.fileName ?? 'Uploaded';
                    default:
                        return 0;
                }
            }),
            catchError(this.handleError)
        );
    }

    private handleError(error: HttpErrorResponse) {
        const msg = error.error?.error || 'An unexpected error occurred during upload.';
        return throwError(() => new Error(msg));
    }
}
