const fs = require('fs');
const path = require('path');

function normalizeApiUrl(raw) {
  const v = String(raw || '').trim().replace(/\/+$/, '');
  if (!v) return '';
  if (/\/shareit\/api$/i.test(v)) return v;
  if (/\/shareit$/i.test(v)) return `${v}/api`;
  if (/^https?:\/\//i.test(v)) return `${v}/shareit/api`;
  return v;
}

const apiUrl =
  normalizeApiUrl(process.env.API_URL) ||
  normalizeApiUrl(process.env.NG_APP_API_URL) ||
  normalizeApiUrl(process.env.BACKEND_URL) ||
  '/shareit/api';

const content =
  'window.__env = window.__env || {};\n' +
  `window.__env.API_URL = ${JSON.stringify(apiUrl)};\n`;

const outPath = path.join(__dirname, '..', 'public', 'env.js');
fs.mkdirSync(path.dirname(outPath), { recursive: true });
fs.writeFileSync(outPath, content, 'utf8');
