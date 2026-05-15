const fs = require('fs');
const path = require('path');

const apiUrl = process.env.API_URL || process.env.NG_APP_API_URL || '/shareit/api';

const content =
  'window.__env = window.__env || {};\n' +
  `window.__env.API_URL = ${JSON.stringify(apiUrl)};\n`;

const outPath = path.join(__dirname, '..', 'public', 'env.js');
fs.mkdirSync(path.dirname(outPath), { recursive: true });
fs.writeFileSync(outPath, content, 'utf8');
