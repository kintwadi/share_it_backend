import { Injectable } from '@angular/core';
import { Capacitor } from '@capacitor/core';
import { Geolocation, PositionOptions } from '@capacitor/geolocation';

@Injectable({
  providedIn: 'root'
})
export class PlatformGeolocationService {
  async getCurrentPosition(options: PositionOptions): Promise<GeolocationPosition> {
    if (Capacitor.isNativePlatform()) {
      const permissions = await Geolocation.checkPermissions();

      if (
        permissions.location !== 'granted' &&
        permissions.coarseLocation !== 'granted'
      ) {
        await Geolocation.requestPermissions();
      }

      const position = await Geolocation.getCurrentPosition(options);
      return {
        coords: {
          accuracy: position.coords.accuracy,
          altitude: position.coords.altitude,
          altitudeAccuracy: position.coords.altitudeAccuracy,
          heading: position.coords.heading,
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          speed: position.coords.speed,
          toJSON: () => ({
            accuracy: position.coords.accuracy,
            altitude: position.coords.altitude,
            altitudeAccuracy: position.coords.altitudeAccuracy,
            heading: position.coords.heading,
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            speed: position.coords.speed
          })
        } as GeolocationCoordinates,
        timestamp: position.timestamp,
        toJSON: () => ({
          coords: {
            accuracy: position.coords.accuracy,
            altitude: position.coords.altitude,
            altitudeAccuracy: position.coords.altitudeAccuracy,
            heading: position.coords.heading,
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            speed: position.coords.speed
          },
          timestamp: position.timestamp
        })
      } as GeolocationPosition;
    }

    return new Promise<GeolocationPosition>((resolve, reject) => {
      if (!('geolocation' in navigator)) {
        reject(new Error('Geolocation is not supported.'));
        return;
      }

      navigator.geolocation.getCurrentPosition(resolve, reject, options);
    });
  }
}
