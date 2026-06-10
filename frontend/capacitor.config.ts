import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.vicinity24',
  appName: 'vicinity24',
  webDir: 'dist/share-it-client/browser',
  bundledWebRuntime: false,
  android: {
    allowMixedContent: true,
  },
};

export default config;
