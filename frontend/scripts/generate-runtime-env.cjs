const fs = require('fs');
const path = require('path');

const envFile = path.join(__dirname, '..', 'public', 'env.js');
const existing = fs.existsSync(envFile) ? fs.readFileSync(envFile, 'utf8') : '';

function readExistingValue(key, fallback) {
  const escapedKey = key.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = existing.match(new RegExp(`window\\.__env\\.${escapedKey}\\s*=\\s*["']([^"']*)["'];?`));
  return match ? match[1] : fallback;
}

function pickValue(key, fallback) {
  const envValue = process.env[key];
  if (typeof envValue === 'string' && envValue.trim()) {
    return envValue.trim();
  }
  return readExistingValue(key, fallback);
}

function normalizeUiLayout(value) {
  const normalized = String(value || '').trim().toUpperCase();
  return normalized === 'STANDARD' ? 'STANDARD' : 'MODERN';
}

const runtimeConfig = {
  API_URL: pickValue('API_URL', '/api/v1'),
  UI_LAYOUT: normalizeUiLayout(pickValue('UI_LAYOUT', 'MODERN')),
  TENANT_HEADER_NAME: pickValue('TENANT_HEADER_NAME', 'X-Tenant-ID'),
  TENANT_ID: pickValue('TENANT_ID', '')
};

const output = [
  'window.__env = window.__env || {};',
  `window.__env.API_URL = ${JSON.stringify(runtimeConfig.API_URL)};`,
  `window.__env.UI_LAYOUT = ${JSON.stringify(runtimeConfig.UI_LAYOUT)};`,
  `window.__env.TENANT_HEADER_NAME = ${JSON.stringify(runtimeConfig.TENANT_HEADER_NAME)};`,
  `window.__env.TENANT_ID = ${JSON.stringify(runtimeConfig.TENANT_ID)};`,
  ''
].join('\n');

fs.writeFileSync(envFile, output, 'utf8');
