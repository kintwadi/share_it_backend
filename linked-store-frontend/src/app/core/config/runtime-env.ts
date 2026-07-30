declare global {
  interface Window {
    __env?: {
      API_URL?: string;
      STORE_HEADER_NAME?: string;
      DEFAULT_STORE_ID?: string;
    };
  }
}

export interface RuntimeEnv {
  apiUrl: string;
  storeHeaderName: string;
  defaultStoreId: string;
}

function normalizeApiUrl(value: string): string {
  const trimmed = String(value || '').trim().replace(/\/+$/, '');
  if (!trimmed) {
    return '/api/v1';
  }
  if (!/^https?:\/\//i.test(trimmed)) {
    return trimmed;
  }

  try {
    const url = new URL(trimmed);
    const path = url.pathname.replace(/\/+$/, '');
    if (!path || path === '/') {
      url.pathname = '/api/v1';
    }
    return url.toString().replace(/\/+$/, '');
  } catch {
    return trimmed;
  }
}

const fallbackEnv: RuntimeEnv = {
  apiUrl: '/api/v1',
  storeHeaderName: 'X-Store-Id',
  defaultStoreId: ''
};

export function getRuntimeEnv(): RuntimeEnv {
  const env = window.__env ?? {};
  return {
    apiUrl: normalizeApiUrl(env.API_URL || fallbackEnv.apiUrl),
    storeHeaderName: env.STORE_HEADER_NAME || fallbackEnv.storeHeaderName,
    defaultStoreId: String(env.DEFAULT_STORE_ID || fallbackEnv.defaultStoreId)
  };
}
