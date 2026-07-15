function readRuntimeEnv(): Record<string, any> {
  return ((globalThis as any)?.__env ?? {}) as Record<string, any>;
}

function normalize(value: unknown): string | null {
  const trimmed = String(value ?? '').trim();
  return trimmed ? trimmed : null;
}

export function getRuntimeUiLayout(): 'modern' | 'standard' | null {
  const configured = normalize(readRuntimeEnv()['UI_LAYOUT']);
  if (!configured) return null;
  const normalized = configured.toLowerCase();
  if (normalized === 'standard') return 'standard';
  if (normalized === 'modern') return 'modern';
  return null;
}

function normalizeApiUrl(value: string): string {
  const trimmed = value.trim().replace(/\/+$/, '');
  if (!/^https?:\/\//i.test(trimmed)) {
    return trimmed;
  }
  try {
    const url = new URL(trimmed);
    const path = url.pathname.replace(/\/+$/, '');
    if (!path || path === '/') {
      url.pathname = '/api/v1';
      return url.toString().replace(/\/+$/, '');
    }
    return url.toString().replace(/\/+$/, '');
  } catch {
    return trimmed;
  }
}

export function getRuntimeApiUrl(): string | null {
  const configured = normalize(readRuntimeEnv()['API_URL']);
  const host = String(globalThis?.location?.host || '').trim().toLowerCase();
  if (configured) {
    // Production safety: when the SPA runs on v24pool.com, a relative `/api/v1`
    // runtime value incorrectly targets the frontend host. Force the dedicated API host.
    if ((host === 'v24pool.com' || host === 'www.v24pool.com') && configured === '/api/v1') {
      return 'https://vicinity24api.com/api/v1';
    }
    return normalizeApiUrl(configured);
  }

  // Production safety fallback: if the SPA is served from the public frontend
  // domain without a runtime API override, call the dedicated API host instead
  // of incorrectly using the frontend origin as `/api/v1`.
  if (host === 'v24pool.com' || host === 'www.v24pool.com') {
    return 'https://vicinity24api.com/api/v1';
  }

  return null;
}

export function getTenantHeaderName(): string {
  return normalize(readRuntimeEnv()['TENANT_HEADER_NAME']) ?? 'X-Tenant-ID';
}

export function getTenantId(): string | null {
  const runtimeEnv = readRuntimeEnv();
  return normalize(
    runtimeEnv['TENANT_ID'] ??
    runtimeEnv['NG_APP_TENANT_ID'] ??
    runtimeEnv['X_TENANT_ID']
  );
}

export function withTenantHeader(headers: Record<string, string> = {}): Record<string, string> {
  const tenantId = getTenantId();
  if (!tenantId) {
    return headers;
  }

  return {
    ...headers,
    [getTenantHeaderName()]: tenantId
  };
}
