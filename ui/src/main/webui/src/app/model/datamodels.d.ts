/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.2.1263 on 2026-02-14 22:33:10.

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

export interface BookletFilterRequest extends PaginationRequest {
    name?: string;
    query?: string;
}

export interface CollectionSheet {
    id?: string;
    identifier: string;
    sheetMusic: SheetMusic;
}

export interface CreateInstrument {
    displayName?: string;
    id: string;
    name: string;
    transposition?: InstrumentTransposing;
}

export interface CreateInstrumentation {
    clef?: Clef;
    instrumentId: string;
    notationType?: NotationType;
    notes?: string;
    partLabel?: string;
}

export interface CreateSheetMusic {
    additionalNotes?: string;
    arranger?: Musician;
    composer?: Musician;
    copyright?: string;
    difficultyLevel?: string;
    edition?: string;
    gemaWorkNumber?: string;
    genre?: string;
    iswc?: string;
    originalBy?: string;
    publisher?: string;
    publisherIpi?: string;
    rating?: number;
    subtitle?: string;
    title: string;
    yearOfComposition?: number;
}

export interface DocumentDownload {
}

export interface DocumentFilterRequest extends PaginationRequest {
}

export interface FileUploadRequest {
    file?: FileUpload;
    type?: AttachmentType;
}

export interface Genre {
    name: string;
}

export interface Instrument {
    displayName?: string;
    id?: string;
    name: string;
    transposition?: InstrumentTransposing;
}

export interface InstrumentFilterRequest extends PaginationRequest {
    name?: string;
}

export interface Instrumentation {
    attachments?: Attachment[];
    clef?: Clef;
    id?: string;
    instrument: Instrument;
    notationType?: NotationType;
    notes?: string;
    partLabel?: string;
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
    page?: number;
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
    type?: CollectionType;
}

export interface SheetCollectionFilterRequest extends PaginationRequest {
    name?: string;
    query?: string;
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

export interface FileUpload extends FilePart {
    headers?: any;
}

export interface FilePart {
}

export type AttachmentType = "FULL_SCORE" | "PART" | "COVER" | "LYRICS" | "MIDI" | "AUDIO" | "ANNOTATIONS" | "IMAGE" | "ANALYSIS" | "TRANSCRIPTION" | "EXTERNAL_LINK" | "MUSIC_XML" | "OTHER" | "UNSPECIFIED";

export type ClassificationStatus = "PENDING" | "CLASSIFIED" | "REJECTED";

export type Clef = "TREBLE" | "ALTO" | "TENOR" | "BASS";

export type CollectionType = "FOLDER" | "SETLIST";

export type InstrumentTransposing = "C" | "D" | "Eb" | "F" | "G" | "A" | "Ab" | "Bb";

export type NotationType = "STANDARD" | "TABLATURE" | "PERCUSSION" | "LEAD_SHEET" | "GRAPHIC";

export type SortOrder = "ASC" | "DESC";
