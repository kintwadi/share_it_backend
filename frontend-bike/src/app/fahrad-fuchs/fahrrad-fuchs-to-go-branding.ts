export interface FahrradFuchsToGoBrandTheme {
  accent: string;
  soft: string;
  deep: string;
  muted: string;
}

const defaultTheme: FahrradFuchsToGoBrandTheme = {
  accent: '#0f766e',
  soft: '#dff6f1',
  deep: '#115e59',
  muted: '#0f172a'
};

export function extractManufacturer(title?: string, fallback = 'Fahrrad Fuchs to go'): string {
  const value = title?.trim();
  if (!value) {
    return fallback;
  }
  return value.split(' ')[0] || fallback;
}

export function extractModelLine(title?: string): string {
  const value = title?.trim();
  if (!value) {
    return '';
  }
  const parts = value.split(' ');
  return parts.slice(1).join(' ') || value;
}

export function resolveBrandTheme(title?: string): FahrradFuchsToGoBrandTheme {
  const manufacturer = extractManufacturer(title).toLowerCase();
  switch (manufacturer) {
    case 'specialized':
      return {
        accent: '#dc2626',
        soft: '#fee2e2',
        deep: '#991b1b',
        muted: '#450a0a'
      };
    case 'kalkhoff':
      return {
        accent: '#0f766e',
        soft: '#d1fae5',
        deep: '#115e59',
        muted: '#042f2e'
      };
    case 'orbea':
      return {
        accent: '#7c3aed',
        soft: '#ede9fe',
        deep: '#5b21b6',
        muted: '#2e1065'
      };
    case 'cannondale':
      return {
        accent: '#ca8a04',
        soft: '#fef3c7',
        deep: '#a16207',
        muted: '#422006'
      };
    default:
      return defaultTheme;
  }
}

export function resolveReadinessKey(category?: string): string {
  const value = (category || '').toLowerCase();
  if (value.includes('e-bike')) {
    return 'readiness.batteryCharged';
  }
  if (value.includes('road')) {
    return 'readiness.raceSetupReady';
  }
  if (value.includes('gravel')) {
    return 'readiness.mixedTerrainReady';
  }
  return 'readiness.readyToRide';
}

export function resolveBookingStatusKey(status?: string): string {
  switch ((status || '').toUpperCase()) {
    case 'CONFIRMED':
      return 'bookingStatus.confirmed';
    case 'PENDING':
      return 'bookingStatus.pendingReview';
    case 'CANCELLED':
      return 'bookingStatus.cancelled';
    default:
      return 'bookingStatus.open';
  }
}
