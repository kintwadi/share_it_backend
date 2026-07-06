declare global {
  interface Window {
    __env?: {
      API_URL?: string;
      TENANT_HEADER_NAME?: string;
      TENANT_ID?: string;
    };
  }
}

export interface RuntimeEnv {
  apiUrl: string;
  tenantHeaderName: string;
  tenantId: string;
}

const fallbackEnv: RuntimeEnv = {
  apiUrl: 'http://localhost:8081/api/v1',
  tenantHeaderName: 'X-Tenant-ID',
  tenantId: ''
};

export function getRuntimeEnv(): RuntimeEnv {
  const env = window.__env ?? {};
  return {
    apiUrl: (env.API_URL || fallbackEnv.apiUrl).replace(/\/+$/, ''),
    tenantHeaderName: env.TENANT_HEADER_NAME || fallbackEnv.tenantHeaderName,
    tenantId: env.TENANT_ID || fallbackEnv.tenantId
  };
}
