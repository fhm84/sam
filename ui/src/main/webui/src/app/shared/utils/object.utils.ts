/**
 * Recursively converts empty strings to null in an object or array.
 * Useful for cleaning form data before sending to the API.
 */
export function convertEmptyStringsToNull<T>(obj: T): T {
  if (obj === '') return null as T;
  if (Array.isArray(obj)) {
    return obj.map((value) => convertEmptyStringsToNull(value)) as T;
  }
  if (obj !== null && typeof obj === 'object') {
    return Object.fromEntries(
      Object.entries(obj).map(([key, value]) => [key, convertEmptyStringsToNull(value)]),
    ) as T;
  }
  return obj;
}
