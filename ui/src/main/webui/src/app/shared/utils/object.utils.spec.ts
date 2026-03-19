import { describe, it, expect } from 'vitest';
import { convertEmptyStringsToNull } from './object.utils';

describe('convertEmptyStringsToNull', () => {
  it('converts empty string to null', () => {
    expect(convertEmptyStringsToNull('')).toBeNull();
  });

  it('leaves non-empty strings unchanged', () => {
    expect(convertEmptyStringsToNull('hello')).toBe('hello');
  });

  it('leaves numbers unchanged', () => {
    expect(convertEmptyStringsToNull(42)).toBe(42);
    expect(convertEmptyStringsToNull(0)).toBe(0);
  });

  it('leaves booleans unchanged', () => {
    expect(convertEmptyStringsToNull(true)).toBe(true);
    expect(convertEmptyStringsToNull(false)).toBe(false);
  });

  it('leaves null unchanged', () => {
    expect(convertEmptyStringsToNull(null)).toBeNull();
  });

  it('leaves Date instances unchanged', () => {
    const d = new Date('2024-01-01');
    expect(convertEmptyStringsToNull(d)).toBe(d);
  });

  it('converts empty strings in a flat object', () => {
    const result = convertEmptyStringsToNull({ name: 'Alice', notes: '' });
    expect(result).toEqual({ name: 'Alice', notes: null });
  });

  it('leaves non-empty strings in an object unchanged', () => {
    const result = convertEmptyStringsToNull({ a: 'x', b: 'y' });
    expect(result).toEqual({ a: 'x', b: 'y' });
  });

  it('converts empty strings in nested objects recursively', () => {
    const result = convertEmptyStringsToNull({ outer: { inner: '' } });
    expect(result).toEqual({ outer: { inner: null } });
  });

  it('converts empty strings in arrays', () => {
    const result = convertEmptyStringsToNull(['a', '', 'b']);
    expect(result).toEqual(['a', null, 'b']);
  });

  it('handles arrays of objects', () => {
    const result = convertEmptyStringsToNull([{ x: '' }, { x: 'val' }]);
    expect(result).toEqual([{ x: null }, { x: 'val' }]);
  });

  it('handles mixed nested structure', () => {
    const input = {
      title: 'Sheet',
      composer: '',
      instrumentation: {
        name: '',
        parts: ['Flute', ''],
      },
    };
    expect(convertEmptyStringsToNull(input)).toEqual({
      title: 'Sheet',
      composer: null,
      instrumentation: {
        name: null,
        parts: ['Flute', null],
      },
    });
  });
});
