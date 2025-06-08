/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.2.1263 on 2025-06-07 17:46:48.

export interface Attachment {
    checksum?: number;
    displayName?: string;
    docIdentifier?: string;
    fileSize?: number;
    id?: string;
    mimeType?: string;
    type?: AttachmentType;
    uploadedAt?: Date;
}

export interface CollectionSheet {
    id?: string;
    identifier: string;
    sheetMusic: SheetMusic;
}

export interface Genre {
    name: string;
}

export interface Instrumentation {
    attachments?: Attachment[];
    clef?: Clef;
    id?: string;
    instrumentName: string;
    notationType?: NotationType;
    notes?: string;
    partLabel?: string;
    transposition?: InstrumentTransposing;
}

export interface Musician {
    birthYear?: number;
    deathYear?: number;
    id?: string;
    ipi?: string;
    name: string;
}

export interface MusicianFilterRequest extends PaginationRequest {
    name?: string;
}

export interface PaginatedResponse<T> {
    data?: T[];
    size?: number;
    totalCount?: number;
}

export interface PaginationRequest {
    page?: number;
    size?: number;
    sortBy?: string[];
    sortOrder?: SortOrder;
}

export interface SheetCollection {
    date?: Date;
    description?: string;
    id?: string;
    name: string;
    sheets?: CollectionSheet[];
}

export interface SheetFilterRequest extends PaginationRequest {
    title?: string;
}

export interface SheetMusic {
    additionalNotes?: string;
    arranger?: Musician;
    composer?: Musician;
    copyright?: string;
    difficultyLevel?: string;
    edition?: string;
    gemaWorkNumber?: string;
    genre?: string;
    id?: string;
    instrumentations?: Instrumentation[];
    iswc?: string;
    originalBy?: string;
    publisher?: string;
    publisherIpi?: string;
    rating?: number;
    subtitle?: string;
    title: string;
    yearOfComposition?: number;
}

export type AttachmentType = "FULL_SCORE" | "PART" | "COVER" | "LYRICS" | "MIDI" | "AUDIO" | "ANNOTATIONS" | "IMAGE" | "ANALYSIS" | "TRANSCRIPTION" | "EXTERNAL_LINK" | "MUSIC_XML" | "OTHER" | "UNSPECIFIED";

export type ClassificationStatus = "PENDING" | "CLASSIFIED" | "REJECTED";

export type Clef = "TREBLE" | "ALTO" | "TENOR" | "BASS";

export type InstrumentTransposing = "C" | "D" | "Eb" | "F" | "G" | "A" | "Ab" | "Bb";

export type NotationType = "STANDARD" | "TABLATURE" | "PERCUSSION" | "LEAD_SHEET" | "GRAPHIC";

export type SortOrder = "ASC" | "DESC";
