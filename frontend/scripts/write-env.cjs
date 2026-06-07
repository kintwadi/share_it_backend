const fs = require('fs');
const path = require('path');

function normalizeApiUrl(raw) {
  const v = String(raw || '').trim().replace(/\/+$/, '');
  if (!v) return '';
  if (/\/api\/v1$/i.test(v)) return v;
  if (/\/api$/i.test(v)) return `${v}/v1`;
  if (/^https?:\/\//i.test(v)) return `${v}/api/v1`;
  return v;
}

function normalizeOptional(raw) {
  const v = String(raw || '').trim();
  return v || '';
}

const apiUrl =
  normalizeApiUrl(process.env.API_URL) ||
  normalizeApiUrl(process.env.NG_APP_API_URL) ||
  normalizeApiUrl(process.env.BACKEND_URL) ||
  '/api/v1';

const tenantHeaderName =
  normalizeOptional(process.env.TENANT_HEADER_NAME) ||
  'X-Tenant-ID';

const tenantId =
  normalizeOptional(process.env.TENANT_ID) ||
  normalizeOptional(process.env.NG_APP_TENANT_ID) ||
  normalizeOptional(process.env.X_TENANT_ID);

const content =
  'window.__env = window.__env || {};\n' +
  `window.__env.API_URL = ${JSON.stringify(apiUrl)};\n` +
  `window.__env.TENANT_HEADER_NAME = ${JSON.stringify(tenantHeaderName)};\n` +
  `window.__env.TENANT_ID = ${JSON.stringify(tenantId)};\n`;

const outPath = path.join(__dirname, '..', 'public', 'env.js');
fs.mkdirSync(path.dirname(outPath), { recursive: true });
fs.writeFileSync(outPath, content, 'utf8');
