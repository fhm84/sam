import { describe, it, expect } from 'vitest';
import { instrumentLabel, formatSize } from './format.utils';

describe('instrumentLabel', () => {
  it('returns name when no displayName or transposition', () => {
    expect(instrumentLabel({ name: 'Flute' })).toBe('Flute');
  });

  it('prefers displayName over name', () => {
    expect(instrumentLabel({ name: 'Flute', displayName: 'Concert Flute' })).toBe('Concert Flute');
  });

  it('appends transposition in parens', () => {
    expect(instrumentLabel({ name: 'Trumpet', transposition: 'Bb' })).toBe('Trumpet (Bb)');
  });

  it('uses displayName with transposition', () => {
    expect(instrumentLabel({ name: 'Trumpet', displayName: 'Piccolo Trumpet', transposition: 'A' })).toBe(
      'Piccolo Trumpet (A)',
    );
  });

  it('returns name without parens when transposition is empty string', () => {
    expect(instrumentLabel({ name: 'Violin', transposition: '' })).toBe('Violin');
  });
});

describe('formatSize', () => {
  it('returns "0 B" for undefined', () => {
    expect(formatSize(undefined)).toBe('0 B');
  });

  it('returns "0 B" for 0', () => {
    expect(formatSize(0)).toBe('0 B');
  });

  it('formats bytes without decimal', () => {
    expect(formatSize(512)).toBe('512 B');
  });

  it('formats kilobytes with one decimal', () => {
    expect(formatSize(1024)).toBe('1.0 KB');
    expect(formatSize(1536)).toBe('1.5 KB');
  });

  it('formats megabytes with one decimal', () => {
    expect(formatSize(1024 * 1024)).toBe('1.0 MB');
    expect(formatSize(1024 * 1024 * 2.5)).toBe('2.5 MB');
  });

  it('formats gigabytes with one decimal', () => {
    expect(formatSize(1024 * 1024 * 1024)).toBe('1.0 GB');
  });
});
