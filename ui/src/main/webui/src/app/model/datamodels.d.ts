/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.2.1263 on 2026-03-10 22:18:15.

export interface Attachment {
    checksum?: string;
    displayName?: string;
    fileSize?: number;
    id?: string;
    mimeType?: string;
    type?: AttachmentType;
    uploadedAt?: Date;
}

export interface BatchDownloadRequest {
    baseName?: string;
    format?: DownloadFormat;
    ids?: string[];
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

export interface ClassificationApplyRequest {
    arrangerId?: string;
    arrangerName?: string;
    attachmentType?: AttachmentType;
    clef?: Clef;
    composerId?: string;
    composerName?: string;
    edition?: string;
    genre?: Genre;
    instrumentId?: string;
    instrumentName?: string;
    iswc?: string;
    notationType?: NotationType;
    partLabel?: string;
    publisher?: string;
    sheetId?: string;
    subtitle?: string;
    title?: string;
    yearOfComposition?: number;
}

export interface ClassificationApplyResult {
}

export interface CollectionSheet {
    duration?: long;
    genre?: Genre;
    id?: string;
    identifier: string;
    sheetId?: string;
    style?: Style;
    subtitle?: string;
    title?: string;
}

export interface CoverageResult {
    coverageScore?: number;
    details?: VoiceCoverageDetail[];
    ensembleId?: string;
    evaluatedAt?: Date;
    missingRequired?: boolean;
    missingRequiredCount?: number;
    playable?: boolean;
    sheetMusicId?: string;
    status?: CoverageStatus;
}

export interface CoverageSnapshotSummary {
    computedAt?: Date;
    coverageScore?: number;
    details?: VoiceCoverageDetail[];
    missingRequired?: boolean;
    status?: CoverageStatus;
}

export interface CreateCollectionSheet {
    identifier: string;
    sheetId: string;
}

export interface CreateEnsemble {
    description?: string;
    name: string;
}

export interface CreateEnsembleVoice {
    label: string;
    maxCount?: number;
    minCount?: number;
    required?: boolean;
    targetCount?: number;
    weight?: number;
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
    difficultyLevel?: short;
    duration?: long;
    edition?: string;
    favorite?: boolean;
    gemaWorkNumber?: string;
    genre?: Genre;
    iswc?: string;
    originalBy?: string;
    publisher?: string;
    publisherIpi?: string;
    rating?: number;
    style?: Style;
    subtitle?: string;
    tags?: string[];
    title: string;
    yearOfComposition?: number;
}

export interface CreateVoiceOption {
    factor?: number;
    instrumentId: string;
    type?: VoiceOptionType;
}

export interface DifficultyJsonbAdapter extends JsonbAdapter<short, number> {
}

export interface DocumentDownload {
  id?: string;
  filename?: string;
  size?: number;
  mimeType?: string;
  checksumSha256?: string;
}

export interface DocumentFilterRequest extends PaginationRequest {
}

export interface DocumentLinkRequest {
    attachmentType?: AttachmentType;
    instrumentationId?: string;
    sheetId?: string;
}

export interface DocumentUpload {
  document?: DocumentDownload;
  attachment?: Attachment;
}

export interface DurationJsonbAdapter extends JsonbAdapter<long, string> {
}

export interface Ensemble {
    description?: string;
    id?: string;
    name: string;
    voices?: EnsembleVoice[];
}

export interface EnsembleCoverageStatus {
    computedAt?: Date;
    sheetCount?: number;
}

export interface EnsembleFilterRequest extends PaginationRequest {
    name?: string;
}

export interface EnsembleVoice {
    id?: string;
    label: string;
    maxCount?: number;
    minCount?: number;
    options?: VoiceOption[];
    required?: boolean;
    targetCount?: number;
    weight?: number;
}

export interface FileUploadRequest {
    file?: FileUpload;
    type?: AttachmentType;
}

export interface Instrument {
    displayName?: string;
    id?: string;
    name: string;
    transposition?: InstrumentTransposing;
}

export interface InstrumentFilterRequest extends PaginationRequest {
    name?: string;
    transposition?: InstrumentTransposing;
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

export interface SheetClassification {
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
    type?: CollectionType;
}

export interface SheetFilterRequest extends PaginationRequest {
    composer?: string;
    ensemble?: string;
    favorite?: boolean;
    genre?: string;
    query?: string;
    title?: string;
    titleStartsWith?: string;
}

export interface SheetMusic {
    additionalNotes?: string;
    arranger?: Musician;
    attachments?: Attachment[];
    composer?: Musician;
    copyright?: string;
    difficultyLevel?: short;
    duration?: long;
    edition?: string;
    favorite?: boolean;
    gemaWorkNumber?: string;
    genre?: Genre;
    id?: string;
    instrumentations?: Instrumentation[];
    iswc?: string;
    originalBy?: string;
    publisher?: string;
    publisherIpi?: string;
    rating?: number;
    style?: Style;
    subtitle?: string;
    tags?: string[];
    title: string;
    yearOfComposition?: number;
}

export interface SheetMusicSearchResult extends SheetMusic {
    coverage?: CoverageSnapshotSummary;
    metrics?: SearchResultMetrics;
}

export interface VoiceCoverageDetail {
    effectiveCount?: number;
    explanation?: string;
    minCount?: number;
    missingRequired?: boolean;
    present?: boolean;
    required?: boolean;
    score?: number;
    targetCount?: number;
    voiceId?: string;
    voiceLabel?: string;
    weight?: number;
}

export interface VoiceOption {
    factor?: number;
    id?: string;
    instrumentId: string;
    type?: VoiceOptionType;
}

export interface FileUpload extends FilePart {
    headers?: any;
}

export interface JsonbAdapter<Original, Adapted> {
}

export interface FilePart {
}

export type AttachmentType = "FULL_SCORE" | "PART" | "COVER" | "LYRICS" | "MIDI" | "AUDIO" | "ANNOTATIONS" | "IMAGE" | "ANALYSIS" | "TRANSCRIPTION" | "EXTERNAL_LINK" | "MUSIC_XML" | "OTHER" | "UNSPECIFIED";

export type ClassificationStatus = "PENDING" | "CLASSIFIED" | "REJECTED";

export type Clef = "TREBLE" | "ALTO" | "TENOR" | "BASS";

export type CollectionType = "FOLDER" | "SETLIST";

export type CoverageStatus = "COMPLETE" | "PLAYABLE" | "INCOMPLETE";

export type DownloadFormat = "ZIP" | "MERGED_PDF";

export type Genre = "MARCH" | "MARCHING_SHOW" | "CONCERT_WORK" | "OVERTURE" | "SUITE" | "SYMPHONY" | "FANTASY" | "VARIATIONS" | "DANCE" | "WALTZ" | "POLKA" | "FOLK_SONG" | "HYMN_CHORALE" | "FILM_MUSIC" | "SHOW_MUSIC" | "POP_ROCK" | "JAZZ" | "LATIN" | "CHRISTMAS" | "SACRED" | "SOLO_WITH_BAND";

export type InstrumentTransposing = "C" | "D" | "Eb" | "F" | "G" | "A" | "Ab" | "Bb";

export type NotationType = "STANDARD" | "TABLATURE" | "PERCUSSION" | "LEAD_SHEET" | "GRAPHIC";

export type SortOrder = "ASC" | "DESC";

export type Style = "CLASSICAL" | "ROMANTIC" | "MODERN" | "CONTEMPORARY" | "POP" | "ROCK" | "FUNK" | "SWING" | "LATIN" | "TRADITIONAL" | "FOLKLORISTIC" | "EXPERIMENTAL";

export type VoiceOptionType = "PRIMARY" | "ALTERNATE" | "FALLBACK";
