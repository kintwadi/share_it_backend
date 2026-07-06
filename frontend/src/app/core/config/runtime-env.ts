function readRuntimeEnv(): Record<string, any> {
  return ((globalThis as any)?.__env ?? {}) as Record<string, any>;
}

function normalize(value: unknown): string | null {
  const trimmed = String(value ?? '').trim();
  return trimmed ? trimmed : null;
}

export function getRuntimeApiUrl(): string | null {
  return normalize(readRuntimeEnv()['API_URL']);
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
