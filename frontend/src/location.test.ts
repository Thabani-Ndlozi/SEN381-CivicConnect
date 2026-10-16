import { describe, expect, it } from 'vitest';
import { isValidPoint } from './LocationPicker';

describe('map point validation', () => {
  it('requires both coordinates', () => {
    expect(isValidPoint(null, null)).toBe(false);
    expect(isValidPoint(-25.7479, null)).toBe(false);
    expect(isValidPoint(null, 28.2293)).toBe(false);
  });
  it('accepts legitimate coordinate bounds', () => {
    expect(isValidPoint(-25.7479, 28.2293)).toBe(true);
    expect(isValidPoint(0, 0)).toBe(true);
    expect(isValidPoint(-90, 180)).toBe(true);
  });
  it('rejects invalid or non-finite values', () => {
    expect(isValidPoint(90.01, 20)).toBe(false);
    expect(isValidPoint(20, -180.01)).toBe(false);
    expect(isValidPoint(Number.NaN, 30)).toBe(false);
  });
});
