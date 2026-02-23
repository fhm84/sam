import { Genre } from '../model/datamodels';

/** Page size used when fetching all items for dropdown/select lists. */
export const FETCH_ALL_SIZE = 1000;

/** All possible Genre enum values, in display order. */
export const GENRES = [
  'MARCH', 'MARCHING_SHOW', 'CONCERT_WORK', 'OVERTURE', 'SUITE', 'SYMPHONY',
  'FANTASY', 'VARIATIONS', 'DANCE', 'WALTZ', 'POLKA', 'FOLK_SONG',
  'HYMN_CHORALE', 'FILM_MUSIC', 'SHOW_MUSIC', 'POP_ROCK', 'JAZZ',
  'LATIN', 'CHRISTMAS', 'SACRED', 'SOLO_WITH_BAND',
] as const satisfies readonly Genre[];
