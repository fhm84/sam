/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.2.1263 on 2025-03-26 15:57:08.

export interface CollectionSheet {
    id?: string;
    identifier: string;
    sheetMusic: SheetMusic;
}

export interface Genre {
    name: string;
}

export interface Instrumentation {
    clef?: Clef;
    id?: string;
    instrumentName: string;
    key?: number;
    keySignature?: InstrumentTransposing;
    midiFile?: string;
    notationType?: NotationType;
    pdfFile?: string;
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
    difficultyLevel?: string;
    edition?: string;
    gemaWorkNumber?: string;
    genre?: string;
    id?: string;
    instrumentations?: Instrumentation[];
    iswc?: string;
    license?: string;
    publisher?: string;
    publisherIpi?: string;
    rating?: number;
    subtitle?: string;
    title: string;
    yearOfComposition?: number;
}

export type Clef = "TREBLE" | "ALTO" | "TENOR" | "BASS";

export type InstrumentTransposing = "C" | "D" | "Eb" | "F" | "G" | "A" | "Ab" | "Bb";

export type NotationType = "STANDARD" | "TABLATURE" | "PERCUSSION" | "LEAD_SHEET" | "GRAPHIC";

export type SortOrder = "ASC" | "DESC";
