/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 4.1.1 on 2026-08-17 17:34:28.

export interface SheetEnrichment {
    suggestedAdditionalNotes?: string;
    suggestedDifficultyLevel?: number;
    suggestedStyle?: Style;
    suggestedTags?: string[];
    suggestedYearOfComposition?: number;
}

export interface UserInfo {
    displayLabel?: string;
    email?: string;
    firstName?: string;
    id: string;
    lastName?: string;
    username: string;
}

export interface DraftTextResult {
    draftText?: string;
}

export interface SetlistSuggestions {
    items?: SuggestedSetlistItem[];
}

export interface SuggestSetlistItemsRequest {
    goal: string;
}

export interface SuggestedSetlistItem {
    composer?: string;
    rationale?: string;
    sheetId?: string;
    title?: string;
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
    coverColor?: string;
    coverImageId?: string;
    date?: Date;
    description?: string;
    ensembleId?: string;
    id?: string;
    items?: CollectionItem[];
    name: string;
    type?: CollectionType;
    visibility?: CollectionVisibility;
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
    aliases?: string[];
    catalogPosition?: number;
    catalogSection?: string;
    defaultClef?: Clef;
    displayName?: string;
    family?: InstrumentFamily;
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
    aliases?: string[];
    catalogPosition?: number;
    catalogSection?: string;
    defaultClef?: Clef;
    displayName?: string;
    family?: InstrumentFamily;
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
    contact?: MusicianContact;
    deathYear?: number;
    id?: string;
    instruments?: MusicianInstrument[];
    ipi?: string;
    membership?: MusicianMembership;
    name: string;
    userId?: string;
}

export interface MusicianContact {
    email?: string;
    mobile?: string;
    notes?: string;
}

export interface MusicianFilterRequest extends PaginationRequest {
    name?: string;
}

export interface MusicianInstrument {
    instrumentId?: string;
    instrumentName?: string;
    primary?: boolean;
}

export interface MusicianMatch {
    id?: string;
    name?: string;
    score?: number;
}

export interface MusicianMembership {
    lastInviteSentAt?: Date;
    role?: MusicianRole;
    status?: MusicianStatus;
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
    pages?: string;
    partLabel?: string;
    physicalCondition?: PhysicalCondition;
    physicalLocation?: string;
}

export interface CreateSheetMusic {
    additionalNotes?: string;
    arranger?: Musician;
    collectionId?: string;
    composer?: Musician;
    copyright?: string;
    difficultyLevel?: short;
    duration?: long;
    edition?: string;
    favorite?: boolean;
    gemaReportable?: GemaReportable;
    gemaWorkNumber?: string;
    genre?: Genre;
    iswc?: string;
    originalBy?: string;
    publisher?: string;
    publisherIpi?: string;
    rating?: number;
    rightsStatus?: RightsStatus;
    source?: string;
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

export interface ExploreShelves {
    bigFinishes?: SheetMusicSearchResult[];
    crowdPleasers?: SheetMusicSearchResult[];
    hiddenGems?: SheetMusicSearchResult[];
    needsAttention?: SheetMusicSearchResult[];
    quickFillers?: SheetMusicSearchResult[];
    recentlyAdded?: SheetMusicSearchResult[];
    tagCloud?: TagCount[];
}

export interface Instrumentation {
    attachments?: Attachment[];
    clef?: Clef;
    id?: string;
    instrument: Instrument;
    notationType?: NotationType;
    notes?: string;
    pages?: string;
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
    tag?: string;
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
    gemaReportable?: GemaReportable;
    gemaWorkNumber?: string;
    genre?: Genre;
    id?: string;
    instrumentations?: Instrumentation[];
    iswc?: string;
    originalBy?: string;
    publisher?: string;
    publisherIpi?: string;
    rating?: number;
    rightsStatus?: RightsStatus;
    source?: string;
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

export interface TagCount {
    count?: number;
    tag?: string;
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

export type CollectionVisibility = "WHOLE_ENSEMBLE" | "ADMINS_ONLY" | "PRIVATE";

export type AttachmentType = "FULL_SCORE" | "PART" | "COVER" | "LYRICS" | "MIDI" | "AUDIO" | "ANNOTATIONS" | "IMAGE" | "ANALYSIS" | "TRANSCRIPTION" | "EXTERNAL_LINK" | "MUSIC_XML" | "PROGRAM_NOTE" | "OTHER" | "UNSPECIFIED";

export type CoverageStatus = "COMPLETE" | "PLAYABLE" | "INCOMPLETE";

export type EventType = "DOCUMENT_DOWNLOAD" | "DOCUMENT_BATCH_DOWNLOAD" | "SHEET_EXPORT" | "COLLECTION_EXPORT" | "COLLECTION_TOC_GENERATED" | "GEMA_SETLIST_GENERATED" | "DOCUMENT_CLASSIFIED" | "DOCUMENT_CLASSIFICATION_APPLIED" | "SHARE_CREATED" | "SHARE_ACCESSED" | "SHARE_REVOKED" | "SETLIST_AI_SUGGESTION_GENERATED" | "SETLIST_AI_TEXT_DRAFTED";

export type Clef = "TREBLE" | "ALTO" | "TENOR" | "BASS";

export type InstrumentFamily = "BRASS" | "WOODWIND" | "STRING" | "PERCUSSION" | "KEYBOARD" | "VOICE" | "OTHER";

export type InstrumentTransposing = "C" | "D" | "Eb" | "F" | "G" | "A" | "Ab" | "Bb";

export type VoiceOptionType = "PRIMARY" | "ALTERNATE" | "FALLBACK";

export type MusicianRole = "MEMBER" | "GUEST" | "SUBSTITUTE" | "CONDUCTOR";

export type MusicianStatus = "ACTIVE" | "INACTIVE" | "INVITED" | "PENDING";

export type SortOrder = "ASC" | "DESC";

export type ShareType = "INSTRUMENTATION" | "COLLECTION" | "SHEET";

export type DownloadFormat = "ZIP" | "MERGED_PDF";

export type ExportFormat = "ZIP" | "JSON" | "CSV";

export type GemaReportable = "UNKNOWN" | "YES" | "NO";

export type Genre = "MARCH" | "MARCHING_SHOW" | "CONCERT_WORK" | "OVERTURE" | "SUITE" | "SYMPHONY" | "FANTASY" | "VARIATIONS" | "DANCE" | "WALTZ" | "POLKA" | "FOLK_SONG" | "HYMN_CHORALE" | "FILM_MUSIC" | "SHOW_MUSIC" | "POP_ROCK" | "JAZZ" | "LATIN" | "CHRISTMAS" | "SACRED" | "SOLO_WITH_BAND";

export type NotationType = "STANDARD" | "TABLATURE" | "PERCUSSION" | "LEAD_SHEET" | "GRAPHIC";

export type PhysicalCondition = "GOOD" | "WORN" | "DAMAGED" | "LOST";

export type RightsStatus = "UNKNOWN" | "PUBLIC_DOMAIN" | "LICENSED" | "PERMITTED_ARCHIVE" | "RESTRICTED" | "NO_DIGITALIZATION";

export type Style = "CLASSICAL" | "ROMANTIC" | "MODERN" | "CONTEMPORARY" | "POP" | "ROCK" | "FUNK" | "SWING" | "LATIN" | "TRADITIONAL" | "FOLKLORISTIC" | "EXPERIMENTAL";
