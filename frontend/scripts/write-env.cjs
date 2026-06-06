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

const apiUrl =
  normalizeApiUrl(process.env.API_URL) ||
  normalizeApiUrl(process.env.NG_APP_API_URL) ||
  normalizeApiUrl(process.env.BACKEND_URL) ||
  '/api/v1';

const content =
  'window.__env = window.__env || {};\n' +
  `window.__env.API_URL = ${JSON.stringify(apiUrl)};\n`;

const outPath = path.join(__dirname, '..', 'public', 'env.js');
fs.mkdirSync(path.dirname(outPath), { recursive: true });
fs.writeFileSync(outPath, content, 'utf8');
