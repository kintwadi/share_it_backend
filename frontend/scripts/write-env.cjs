const fs = require('fs');
const path = require('path');

const initialEnvKeys = new Set(Object.keys(process.env));

function loadEnvFile(filePath, options = {}) {
  const { override = false } = options;
  if (!fs.existsSync(filePath)) {
    return;
  }

  const content = fs.readFileSync(filePath, 'utf8');
  for (const rawLine of content.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#')) {
      continue;
    }

    const eqIndex = line.indexOf('=');
    if (eqIndex <= 0) {
      continue;
    }

    const key = line.slice(0, eqIndex).trim();
    if (!key) {
      continue;
    }

    const hasExisting = Object.prototype.hasOwnProperty.call(process.env, key);
    const existingValue = hasExisting ? String(process.env[key] ?? '').trim() : '';
    const hasMeaningfulExistingValue = existingValue !== '';
    if (hasExisting && hasMeaningfulExistingValue && (!override || initialEnvKeys.has(key))) {
      continue;
    }

    let value = line.slice(eqIndex + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }

    process.env[key] = value;
  }
}

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

const frontendRoot = path.join(__dirname, '..');
loadEnvFile(path.join(frontendRoot, '.env'));
loadEnvFile(path.join(frontendRoot, '.env.local'), { override: true });

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

const outPath = path.join(frontendRoot, 'public', 'env.js');
fs.mkdirSync(path.dirname(outPath), { recursive: true });
fs.writeFileSync(outPath, content, 'utf8');
