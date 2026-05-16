/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 4.0.0 on 2026-05-09 16:24:46.

export interface SheetEnrichment {
    suggestedAdditionalNotes?: string;
    suggestedDifficultyLevel?: number;
    suggestedStyle?: Style;
    suggestedTags?: string[];
    suggestedYearOfComposition?: number;
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
    attachmentId?: string;
    instrumentationId?: string;
    sheetId?: string;
}

export interface SheetClassification {
    arranger?: string;
    clef?: Clef;
    composer?: string;
    documentId?: string;
    edition?: string;
    genre?: string;
    instrumentCandidates?: InstrumentMatch[];
    instrumentName?: string;
    iswc?: string;
    matchedArrangerId?: string;
    matchedComposerId?: string;
    matchedSheetId?: string;
    matchedSheetTitle?: string;
    notationType?: NotationType;
    partLabel?: string;
    publisher?: string;
    status?: ClassificationStatus;
    subtitle?: string;
    suggested?: ClassificationApplyRequest;
    title?: string;
    transposition?: string;
    yearOfComposition?: number;
}

export interface CollectionItem {
    attachment?: Attachment;
    duration?: long;
    genre?: Genre;
    id?: string;
    identifier: string;
    sheetId?: string;
    style?: Style;
    subtitle?: string;
    textContent?: string;
    title?: string;
    type: CollectionItemType;
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

export interface CreateCollectionItem {
    identifier?: string;
    sheetId?: string;
    textContent?: string;
    type: CollectionItemType;
}

export interface CreateCollectionSheet {
    identifier: string;
    sheetId: string;
}

export interface SheetCollection {
    date?: Date;
    description?: string;
    id?: string;
    items?: CollectionItem[];
    name: string;
    type?: CollectionType;
}

export interface SheetCollectionFilterRequest extends PaginationRequest {
    name?: string;
    query?: string;
    type?: CollectionType;
}

export interface Attachment {
    checksum?: string;
    displayName?: string;
    fileSize?: number;
    id?: string;
    mimeType?: string;
    type?: AttachmentType;
    uploadedAt?: Date;
}

export interface DocumentDownload {
    checksumSha256?: string;
    filename?: string;
    id?: string;
    mimeType?: string;
    size?: number;
}

export interface DocumentFilterRequest extends PaginationRequest {
}

export interface DocumentLinkRequest {
    attachmentType?: AttachmentType;
    instrumentationId?: string;
    sheetId?: string;
}

export interface DocumentUpload {
    attachment?: Attachment;
    document?: DocumentDownload;
}

export interface FileUploadRequest {
    file?: FileUpload;
    type?: AttachmentType;
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

export interface CreateEnsemble {
    description?: string;
    name: string;
}

export interface CreateEnsembleMembership {
    conductor?: boolean;
    instrumentId?: string;
    musicianId: string;
    voiceId?: string;
}

export interface CreateEnsembleVoice {
    label: string;
    maxCount?: number;
    minCount?: number;
    required?: boolean;
    targetCount?: number;
    weight?: number;
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

export interface EnsembleMembership {
    conductor?: boolean;
    id?: string;
    instrumentId?: string;
    instrumentName?: string;
    musician: Musician;
    voiceId?: string;
    voiceLabel?: string;
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

export interface EventLogEntry {
    entityId?: string;
    entityType?: string;
    eventType?: EventType;
    id?: string;
    metadata?: { [index: string]: any };
    occurredAt?: Date;
    shareTokenId?: string;
    userId?: string;
    username?: string;
}

export interface EventLogFilterRequest extends PaginationRequest {
    entityType?: string;
    eventTypes?: EventType[];
    shareTokenId?: string;
    userId?: string;
}

export interface CreateInstrument {
    displayName?: string;
    id: string;
    name: string;
    transposition?: InstrumentTransposing;
}

export interface CreateVoiceOption {
    factor?: number;
    instrumentId: string;
    type?: VoiceOptionType;
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

export interface InstrumentMatch {
    displayName?: string;
    id?: string;
    name?: string;
    score?: number;
    transposition?: string;
}

export interface VoiceOption {
    factor?: number;
    id?: string;
    instrumentId: string;
    type?: VoiceOptionType;
}

export interface Musician {
    birthYear?: number;
    deathYear?: number;
    id?: string;
    ipi?: string;
    name: string;
    userId?: string;
}

export interface UserInfo {
    id: string;
    username: string;
    email?: string;
    firstName?: string;
    lastName?: string;
}

export interface MusicianFilterRequest extends PaginationRequest {
    name?: string;
}

export interface MusicianMatch {
    id?: string;
    name?: string;
    score?: number;
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

export interface CreateShareRequest {
    expiresAt?: Date;
    resourceId: string;
    resourceType: ShareType;
}

export interface PublicShareCollectionItem {
    composerName?: string;
    instrumentations?: PublicShareInstrumentationItem[];
    sheetId?: string;
    subtitle?: string;
    title?: string;
}

export interface PublicShareInfo {
    attachments?: Attachment[];
    collectionName?: string;
    collectionType?: CollectionType;
    composerName?: string;
    expired?: boolean;
    expiresAt?: Date;
    instrumentName?: string;
    instrumentations?: PublicShareInstrumentationItem[];
    partLabel?: string;
    sheetTitle?: string;
    sheets?: PublicShareCollectionItem[];
    tokenId?: string;
    type?: ShareType;
}

export interface PublicShareInstrumentationItem {
    hasAttachments?: boolean;
    id?: string;
    instrumentName?: string;
    partLabel?: string;
}

export interface ShareResponse {
    createdAt?: Date;
    expiresAt?: Date;
    id?: string;
    resourceId?: string;
    resourceLabel?: string;
    resourceType?: ShareType;
    revoked?: boolean;
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

export interface CreateInstrumentation {
    clef?: Clef;
    instrumentId: string;
    notationType?: NotationType;
    notes?: string;
    partLabel?: string;
    physicalCondition?: PhysicalCondition;
    physicalLocation?: string;
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

export interface DifficultyJsonbAdapter extends JsonbAdapter<short, number> {
}

export interface DurationJsonbAdapter extends JsonbAdapter<long, string> {
}

export interface Instrumentation {
    attachments?: Attachment[];
    clef?: Clef;
    id?: string;
    instrument: Instrument;
    notationType?: NotationType;
    notes?: string;
    partLabel?: string;
    physicalCondition?: PhysicalCondition;
    physicalLocation?: string;
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

export interface SheetWithMyParts extends SheetMusicSearchResult {
    myInstrumentations?: Instrumentation[];
}

export interface FileUpload extends FilePart {
    headers?: any;
}

export interface FilePart {
}

export interface JsonbAdapter<Original, Adapted> {
}

export type ClassificationStatus = "PENDING" | "CLASSIFIED" | "REJECTED";

export type CollectionItemType = "SHEET" | "TEXT";

export type CollectionType = "FOLDER" | "SETLIST";

export type AttachmentType = "FULL_SCORE" | "PART" | "COVER" | "LYRICS" | "MIDI" | "AUDIO" | "ANNOTATIONS" | "IMAGE" | "ANALYSIS" | "TRANSCRIPTION" | "EXTERNAL_LINK" | "MUSIC_XML" | "PROGRAM_NOTE" | "OTHER" | "UNSPECIFIED";

export type CoverageStatus = "COMPLETE" | "PLAYABLE" | "INCOMPLETE";

export type EventType = "DOCUMENT_DOWNLOAD" | "DOCUMENT_BATCH_DOWNLOAD" | "SHEET_EXPORT" | "COLLECTION_EXPORT" | "COLLECTION_TOC_GENERATED" | "GEMA_SETLIST_GENERATED" | "DOCUMENT_CLASSIFIED" | "DOCUMENT_CLASSIFICATION_APPLIED" | "SHARE_CREATED" | "SHARE_ACCESSED" | "SHARE_REVOKED";

export type Clef = "TREBLE" | "ALTO" | "TENOR" | "BASS";

export type InstrumentTransposing = "C" | "D" | "Eb" | "F" | "G" | "A" | "Ab" | "Bb";

export type VoiceOptionType = "PRIMARY" | "ALTERNATE" | "FALLBACK";

export type SortOrder = "ASC" | "DESC";

export type ShareType = "INSTRUMENTATION" | "COLLECTION" | "SHEET";

export type DownloadFormat = "ZIP" | "MERGED_PDF";

export type ExportFormat = "ZIP" | "JSON" | "CSV";

export type Genre = "MARCH" | "MARCHING_SHOW" | "CONCERT_WORK" | "OVERTURE" | "SUITE" | "SYMPHONY" | "FANTASY" | "VARIATIONS" | "DANCE" | "WALTZ" | "POLKA" | "FOLK_SONG" | "HYMN_CHORALE" | "FILM_MUSIC" | "SHOW_MUSIC" | "POP_ROCK" | "JAZZ" | "LATIN" | "CHRISTMAS" | "SACRED" | "SOLO_WITH_BAND";

export type NotationType = "STANDARD" | "TABLATURE" | "PERCUSSION" | "LEAD_SHEET" | "GRAPHIC";

export type PhysicalCondition = "GOOD" | "WORN" | "DAMAGED" | "LOST";

export type Style = "CLASSICAL" | "ROMANTIC" | "MODERN" | "CONTEMPORARY" | "POP" | "ROCK" | "FUNK" | "SWING" | "LATIN" | "TRADITIONAL" | "FOLKLORISTIC" | "EXPERIMENTAL";
