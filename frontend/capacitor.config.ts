import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.vicinity24',
  appName: 'v24pool',
  webDir: 'dist/share-it-client/browser',
  bundledWebRuntime: false,
  android: {
    allowMixedContent: true,
  },
};

export default config;
