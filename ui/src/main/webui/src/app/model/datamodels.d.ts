/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.2.1263 on 2026-01-22 23:02:42.

export interface Attachment {
    checksum?: string;
    displayName?: string;
    fileSize?: number;
    id?: string;
    mimeType?: string;
    type?: AttachmentType;
    uploadedAt?: Date;
}

export interface Booklet {
    description?: string;
    id?: string;
    name: string;
    sheets?: CollectionSheet[];
}

export interface CollectionSheet {
    id?: string;
    identifier: string;
    sheetMusic: SheetMusic;
}

export interface DocumentDownload {
}

export interface DocumentFilterRequest extends PaginationRequest {
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

export interface SearchResultMetrics {
    composerSimilarity?: number;
    finalRank?: number;
    ftsRank?: number;
    phoneticMatch?: boolean;
    titleSimilarity?: number;
}

export interface SheetCollection {
    date?: Date;
    description?: string;
    id?: string;
    name: string;
    sheets?: CollectionSheet[];
}

export interface SheetFilterRequest extends PaginationRequest {
    composer?: string;
    query?: string;
    title?: string;
}

export interface SheetMusic {
    additionalNotes?: string;
    arranger?: Musician;
    attachments?: Attachment[];
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

export interface SheetMusicSearchResult extends SheetMusic {
    metrics?: SearchResultMetrics;
}

export type AttachmentType = "FULL_SCORE" | "PART" | "COVER" | "LYRICS" | "MIDI" | "AUDIO" | "ANNOTATIONS" | "IMAGE" | "ANALYSIS" | "TRANSCRIPTION" | "EXTERNAL_LINK" | "MUSIC_XML" | "OTHER" | "UNSPECIFIED";

export type ClassificationStatus = "PENDING" | "CLASSIFIED" | "REJECTED";

export type Clef = "TREBLE" | "ALTO" | "TENOR" | "BASS";

export type InstrumentTransposing = "C" | "D" | "Eb" | "F" | "G" | "A" | "Ab" | "Bb";

export type NotationType = "STANDARD" | "TABLATURE" | "PERCUSSION" | "LEAD_SHEET" | "GRAPHIC";

export type SortOrder = "ASC" | "DESC";
