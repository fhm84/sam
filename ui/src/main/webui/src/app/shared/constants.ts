import { AttachmentType, Clef, Genre, NotationType, Style } from '../model/datamodels';

/** DifficultyLevel enum values in grade order (grade = index + 1). */
export const DIFFICULTY_LEVELS = [
  'VERY_EASY', 'EASY', 'MEDIUM', 'ADVANCED', 'DIFFICULT', 'VERY_DIFFICULT',
] as const;
export type DifficultyLevelKey = (typeof DIFFICULTY_LEVELS)[number];

/** Page size used when fetching all items for dropdown/select lists. */
export const FETCH_ALL_SIZE = 1000;

/** All possible Genre enum values, in display order. */
export const GENRES = [
  'MARCH', 'MARCHING_SHOW', 'CONCERT_WORK', 'OVERTURE', 'SUITE', 'SYMPHONY',
  'FANTASY', 'VARIATIONS', 'DANCE', 'WALTZ', 'POLKA', 'FOLK_SONG',
  'HYMN_CHORALE', 'FILM_MUSIC', 'SHOW_MUSIC', 'POP_ROCK', 'JAZZ',
  'LATIN', 'CHRISTMAS', 'SACRED', 'SOLO_WITH_BAND',
] as const satisfies readonly Genre[];

/** All possible Style enum values, in display order. */
export const STYLES = [
  'CLASSICAL', 'ROMANTIC', 'MODERN', 'CONTEMPORARY',
  'POP', 'ROCK', 'FUNK', 'SWING', 'LATIN',
  'TRADITIONAL', 'FOLKLORISTIC', 'EXPERIMENTAL',
] as const satisfies readonly Style[];

/** All possible Clef enum values. */
export const CLEFS = ['TREBLE', 'ALTO', 'TENOR', 'BASS'] as const satisfies readonly Clef[];

/** All possible NotationType enum values. */
export const NOTATION_TYPES = [
  'STANDARD', 'TABLATURE', 'PERCUSSION', 'LEAD_SHEET', 'GRAPHIC',
] as const satisfies readonly NotationType[];

/**
 * Selectable attachment types for file uploads.
 * EXTERNAL_LINK is omitted (URL-based, not a file upload).
 * UNSPECIFIED is last so it reads as a fallback choice.
 */
export const ATTACHMENT_TYPES = [
  'FULL_SCORE', 'PART', 'COVER', 'LYRICS',
  'MIDI', 'AUDIO', 'ANNOTATIONS', 'IMAGE',
  'ANALYSIS', 'TRANSCRIPTION', 'MUSIC_XML', 'OTHER', 'UNSPECIFIED',
] as const satisfies readonly AttachmentType[];
