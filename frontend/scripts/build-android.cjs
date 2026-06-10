const { spawnSync } = require('child_process');

const path = require('path');
const fs = require('fs');
const defaultLocalAndroidApiUrl = 'http://192.168.178.114:8081';
const defaultRemoteAndroidApiUrl = 'https://vicinity24api.com';
const shouldUseRemoteApi = process.argv.includes('--remote');
const androidApiUrl = (
  process.env.ANDROID_API_URL ||
  (shouldUseRemoteApi ? defaultRemoteAndroidApiUrl : defaultLocalAndroidApiUrl)
).trim();

const androidStudioJbr = 'C:\\Program Files\\Android\\Android Studio\\jbr';
const javaHome = fs.existsSync(androidStudioJbr)
  ? androidStudioJbr
  : process.env.JAVA_HOME;
const env = {
  ...process.env,
  API_URL: androidApiUrl,
  ...(javaHome ? { JAVA_HOME: javaHome } : {}),
};

if (javaHome) {
  const javaBin = path.join(javaHome, 'bin');
  env.Path = `${javaBin}${path.delimiter}${process.env.Path || ''}`;
}

const npmCommand = process.platform === 'win32' ? 'npm.cmd' : 'npm';
const npxCommand = process.platform === 'win32' ? 'npx.cmd' : 'npx';
const shouldOpenAndroid = process.argv.includes('--open');
const shouldRunAndroid = process.argv.includes('--run');

function normalizeApiUrl(raw) {
  const value = String(raw || '').trim().replace(/\/+$/, '');
  if (!value) return '';
  if (/\/api\/v1$/i.test(value)) return value;
  if (/\/api$/i.test(value)) return `${value}/v1`;
  if (/^https?:\/\//i.test(value)) return `${value}/api/v1`;
  return value;
}

function normalizeOptional(raw, fallback = '') {
  const value = String(raw || '').trim();
  return value || fallback;
}

function run(command, args) {
  const result = spawnSync(command, args, {
    stdio: 'inherit',
    env,
  });

  if (result.status !== 0) {
    process.exit(result.status || 1);
  }
}

// Android builds use a LAN-reachable backend by default so a real device can reach it.
// Use --remote to target the hosted API, or override with ANDROID_API_URL when needed.
// Local web development remains unchanged because npm start still writes /api/v1.
const runtimeEnvContent =
  'window.__env = window.__env || {};\n' +
  `window.__env.API_URL = ${JSON.stringify(normalizeApiUrl(androidApiUrl))};\n` +
  `window.__env.TENANT_HEADER_NAME = ${JSON.stringify(normalizeOptional(env.TENANT_HEADER_NAME, 'X-Tenant-ID'))};\n` +
  `window.__env.TENANT_ID = ${JSON.stringify(normalizeOptional(env.TENANT_ID || env.NG_APP_TENANT_ID || env.X_TENANT_ID))};\n`;

const publicEnvPath = path.join(__dirname, '..', 'public', 'env.js');
const distEnvPath = path.join(__dirname, '..', 'dist', 'share-it-client', 'browser', 'env.js');
const originalPublicEnv = fs.existsSync(publicEnvPath)
  ? fs.readFileSync(publicEnvPath, 'utf8')
  : null;

try {
  fs.writeFileSync(publicEnvPath, runtimeEnvContent, 'utf8');
  run(npxCommand, ['ng', 'build']);
  fs.copyFileSync(publicEnvPath, distEnvPath);
  run(npxCommand, ['cap', 'sync', 'android']);
} finally {
  if (originalPublicEnv == null) {
    if (fs.existsSync(publicEnvPath)) {
      fs.unlinkSync(publicEnvPath);
    }
  } else {
    fs.writeFileSync(publicEnvPath, originalPublicEnv, 'utf8');
  }
}

if (shouldOpenAndroid) {
  run(npxCommand, ['cap', 'open', 'android']);
}

if (shouldRunAndroid) {
  run(npxCommand, ['cap', 'run', 'android', '--no-sync']);
}
