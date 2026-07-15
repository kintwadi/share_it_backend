# vicinity24 Mobile And Desktop Deployment Guide

This guide shows how to package the existing Angular app in `frontend` for Android, iOS, and Electron with Capacitor while keeping a single Angular codebase.

## Project Facts

- Angular app path: `frontend`
- Angular project name: `share-it-client`
- Angular production output: `frontend/dist/share-it-client/browser`
- App name: `vicinity24`
- App ID: `com.vicinity24`

## Core Principle

- Do not refactor the Angular app unless a native platform bridge truly requires it.
- Keep business logic, routing, API calls, and UI inside the existing Angular app.
- Let Capacitor wrap the compiled Angular app for Android, iOS, and Electron.

## Prerequisites

Install these before starting:

- Node.js and npm
- Angular CLI-compatible environment
- Java 17+ and Android Studio for Android
- macOS with Xcode for iOS
- Git

Recommended platform tooling:

- Android SDK and emulator
- Xcode command line tools

## Scenario A: Capacitor Core Setup

### 1. Install Dependencies

Run all commands from `frontend`:

```bash
cd frontend
npm install @capacitor/core @capacitor/android @capacitor/ios @capacitor/geolocation
npm install -D @capacitor/cli @capacitor-community/electron
```

### 2. Create `capacitor.config.ts`

Create `frontend/capacitor.config.ts`:

```ts
import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.vicinity24',
  appName: 'vicinity24',

  // Angular 21 application builder output for this repo.
  webDir: 'dist/share-it-client/browser',

  bundledWebRuntime: false,

  android: {
    allowMixedContent: false,
  },

  ios: {
    contentInset: 'automatic',
  },
};

export default config;
```

Notes:

- The `webDir` must point to the Angular build output folder.
- In this repository that folder is `dist/share-it-client/browser`.
- If the Angular project name changes in `frontend/angular.json`, update `webDir` accordingly.

### 3. Initialize Capacitor

If you already created `capacitor.config.ts`, this is optional, but still safe:

```bash
npx cap init vicinity24 com.vicinity24 --web-dir dist/share-it-client/browser
```

### 4. Add Platforms

```bash
npx cap add android
npx cap add ios
npx cap add @capacitor-community/electron
```

This creates:

- `frontend/android`
- `frontend/ios`
- `frontend/electron`

### 5. Build And Sync Pipeline

Capacitor does not build Angular for you. The correct flow is:

1. Build Angular
2. Sync compiled assets into native shells
3. Open native toolchain and run

Use these commands:

```bash
cd frontend
npm run build
npx cap sync
```

Platform-specific sync:

```bash
npx cap sync android
npx cap sync ios
npx cap sync @capacitor-community/electron
```

Daily workflow:

```bash
cd frontend
npm run build
npx cap sync android
npx cap sync ios
npx cap sync @capacitor-community/electron
```

## Runtime API Configuration

The web runtime configuration is generated into `frontend/public/env.js` before `npm start` and `npm run build` from environment variables such as `API_URL`, `UI_LAYOUT`, `TENANT_HEADER_NAME`, and `TENANT_ID`.

Current generated shape:

```js
window.__env = window.__env || {};
window.__env.API_URL = "/api/v1";
window.__env.UI_LAYOUT = "MODERN";
```

Important:

- Relative `/api/v1` works for Angular web development because `ng serve` uses the proxy in `frontend/proxy.conf.json`.
- Native Android, native iOS, and Electron do not use the Angular dev proxy.
- For Android, iOS, and Electron, `API_URL` must be an absolute backend URL that the device can reach.

Examples:

- Web local dev: `/api/v1`
- Android emulator local backend: `http://10.0.2.2:8081/api/v1`
- iOS simulator local backend: `http://127.0.0.1:8081/api/v1`
- Electron local backend: `http://localhost:8081/api/v1`
- Production: `https://<YOUR_DOMAIN>/api/v1`
- Standard layout deployment: `UI_LAYOUT=STANDARD`

Recommended approach:

- Keep the Angular app unchanged.
- Let web runtime config be generated into `frontend/public/env.js` from environment variables.
- Keep Android runtime config in `frontend/.env.android` and let the Android build script apply it temporarily during `npm run build:android`.

## Scenario B: Minimally Invasive Native Geolocation

The current app already centralizes LocationIQ requests in `frontend/src/app/core/services/location-api.service.ts`.

That means you do not need to rewrite your LocationIQ integration.

You only need a small service that decides how coordinates are obtained:

- Native shells: use Capacitor Geolocation
- Web browser: use the existing browser geolocation fallback

### 1. Add a Native/Web Geolocation Bridge

Create:

- `frontend/src/app/core/services/platform-geolocation.service.ts`

```ts
import { Injectable, inject } from '@angular/core';
import { Capacitor } from '@capacitor/core';
import { Geolocation, PositionOptions } from '@capacitor/geolocation';
import { LocationApiService, LocationResponse } from './location-api.service';

export interface CurrentCoordinates {
  lat: number;
  lng: number;
}

@Injectable({
  providedIn: 'root'
})
export class PlatformGeolocationService {
  private readonly locationApi = inject(LocationApiService);

  private readonly options: PositionOptions = {
    enableHighAccuracy: true,
    timeout: 10000,
    maximumAge: 60000,
  };

  async getCurrentCoordinates(): Promise<CurrentCoordinates> {
    // On native shells, use Capacitor's geolocation bridge.
    // On the web, keep the browser-based fallback to avoid invasive app changes.
    if (Capacitor.isNativePlatform()) {
      const permissions = await Geolocation.checkPermissions();

      if (
        permissions.location !== 'granted' &&
        permissions.coarseLocation !== 'granted'
      ) {
        await Geolocation.requestPermissions();
      }

      const position = await Geolocation.getCurrentPosition(this.options);

      return {
        lat: position.coords.latitude,
        lng: position.coords.longitude,
      };
    }

    return this.getBrowserCoordinates();
  }

  async reverseGeocodeCurrentPosition(): Promise<LocationResponse> {
    const { lat, lng } = await this.getCurrentCoordinates();
    return this.locationApi.reverseGeocode(lat, lng);
  }

  private getBrowserCoordinates(): Promise<CurrentCoordinates> {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error('Browser geolocation is unavailable.'));
        return;
      }

      navigator.geolocation.getCurrentPosition(
        (position) =>
          resolve({
            lat: position.coords.latitude,
            lng: position.coords.longitude,
          }),
        (error) => reject(error),
        this.options
      );
    });
  }
}
```

### 2. Use It With Minimal Code Changes

Wherever the app currently reads browser coordinates directly, replace only that part with:

```ts
import { inject } from '@angular/core';
import { PlatformGeolocationService } from '../core/services/platform-geolocation.service';

private readonly platformGeolocation = inject(PlatformGeolocationService);

async loadCurrentLocation() {
  const place = await this.platformGeolocation.reverseGeocodeCurrentPosition();
  // Keep the rest of the existing LocationIQ response handling unchanged.
}
```

This preserves:

- the current Angular service structure
- the current REST integration
- the existing web behavior

### 3. Android Location Permissions

Add these lines to `frontend/android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### 4. iOS Location Permission

Add this to `frontend/ios/App/App/Info.plist`:

```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>Vicinity24 uses your location to find nearby listings and improve local discovery.</string>
```

Do not add background location permissions unless the app truly needs them.

## Scenario C: Electron Wrapper

### 1. Add And Initialize Electron

Run from `frontend`:

```bash
npm install -D @capacitor-community/electron
npx cap add @capacitor-community/electron
npm run build
npx cap sync @capacitor-community/electron
```

Open the Electron project:

```bash
npx cap open @capacitor-community/electron
```

### 2. Keep Electron Isolated

Keep the main Angular app clean:

- Angular app remains in `frontend/src`
- shared runtime config remains in `frontend/public/env.js`
- Capacitor config remains in `frontend/capacitor.config.ts`
- Electron-specific code stays in `frontend/electron`

This prevents desktop-only code from muddying the Angular app.

### 3. How Capacitor Maps Into Electron

Capacitor uses `frontend/capacitor.config.ts` as the single source of truth for:

- `appId`
- `appName`
- `webDir`

When you run:

```bash
npx cap sync @capacitor-community/electron
```

Capacitor copies the Angular build output from:

```text
dist/share-it-client/browser
```

into the generated Electron wrapper.

Electron-specific concerns then stay in the Electron workspace:

- main process
- preload scripts
- desktop packaging
- window configuration

## Steps To Run Android

### First-Time Setup

```bash
cd frontend
npm install
npm run build
npx cap add android
npx cap sync android
npx cap open android
```

### Day-To-Day Run

```bash
cd frontend
npm run build
npx cap sync android
npx cap open android
```

Then:

- start an Android emulator or connect a device
- build and run from Android Studio

CLI alternative:

```bash
npx cap run android
```

## Steps To Run iOS

iOS requires macOS and Xcode.

### First-Time Setup

```bash
cd frontend
npm install
npm run build
npx cap add ios
npx cap sync ios
npx cap open ios
```

Then in Xcode:

- set signing team
- choose simulator or device
- run the app

### Day-To-Day Run

```bash
cd frontend
npm run build
npx cap sync ios
npx cap open ios
```

## Steps To Run Electron

### First-Time Setup

```bash
cd frontend
npm install
npx cap add @capacitor-community/electron
npm run build
npx cap sync @capacitor-community/electron
npx cap open @capacitor-community/electron
```

Depending on the generated plugin template, install Electron workspace dependencies if needed:

```bash
cd electron
npm install
```

### Day-To-Day Run

```bash
cd frontend
npm run build
npx cap sync @capacitor-community/electron
```

Then start Electron from the generated Electron workspace:

```bash
cd electron
npm run electron:start
```

Packaging commonly uses:

```bash
npm run electron:pack
```

If the generated Electron template uses slightly different script names, use the scripts shipped in the generated `frontend/electron/package.json`.

## Production Release Checklist

- Keep one Angular codebase in `frontend/src`
- Set `API_URL` to an absolute production backend URL for native and Electron builds
- Build Angular before every Capacitor sync
- Re-run `npx cap sync` after dependency or native plugin changes
- Re-open Android Studio or Xcode after sync if native files changed
- Use HTTPS in production
- Keep LocationIQ API usage unchanged except for the coordinate source bridge

## Minimal Change Summary

New files recommended:

- `frontend/capacitor.config.ts`
- `frontend/src/app/core/services/platform-geolocation.service.ts`

Minimal existing file changes:

- `frontend/public/env.js` or the existing env generation script
- whichever Angular component or service directly calls `navigator.geolocation`
- native permission files generated by Capacitor

## Fully Working Target Result

To get a fully working Android, iOS, and Electron app from this repository:

1. Add Capacitor core and platform dependencies
2. Add `frontend/capacitor.config.ts`
3. Add Android, iOS, and Electron platforms
4. Add the small geolocation bridge service
5. Keep LocationIQ REST integration unchanged
6. Set an absolute backend `API_URL` for native and desktop targets
7. Build Angular with `npm run build`
8. Sync with `npx cap sync`
9. Run Android from Android Studio, iOS from Xcode, and Electron from its generated workspace

## Command Cheat Sheet

```bash
cd frontend
npm install @capacitor/core @capacitor/android @capacitor/ios @capacitor/geolocation
npm install -D @capacitor/cli @capacitor-community/electron
npx cap add android
npx cap add ios
npx cap add @capacitor-community/electron
npm run build
npx cap sync
```

Platform opens:

```bash
npx cap open android
npx cap open ios
npx cap open @capacitor-community/electron
```

## Final Note

The existing Angular app is already in good shape for a single-codebase Capacitor migration. The only critical operational requirement is making sure native and Electron builds use a reachable absolute backend `API_URL`, because they cannot rely on the Angular web dev proxy.
