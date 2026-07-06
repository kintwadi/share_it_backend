export function formatBikeEnumLabel(value: string | null | undefined): string {
  if (!value) {
    return '-';
  }

  return value
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (character) => character.toUpperCase());
}
