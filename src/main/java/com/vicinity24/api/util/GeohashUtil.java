package com.vicinity24.api.util;

public final class GeohashUtil {
    private static final char[] BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz".toCharArray();

    private GeohashUtil() {
    }

    public static String encode(Double latitude, Double longitude, int precision) {
        if (latitude == null || longitude == null) return null;
        int p = Math.max(1, Math.min(12, precision));

        boolean even = true;
        int bit = 0;
        int ch = 0;
        StringBuilder geohash = new StringBuilder(p);

        double[] latRange = {-90.0, 90.0};
        double[] lonRange = {-180.0, 180.0};

        while (geohash.length() < p) {
            double mid;
            if (even) {
                mid = (lonRange[0] + lonRange[1]) / 2D;
                if (longitude >= mid) {
                    ch |= 1 << (4 - bit);
                    lonRange[0] = mid;
                } else {
                    lonRange[1] = mid;
                }
            } else {
                mid = (latRange[0] + latRange[1]) / 2D;
                if (latitude >= mid) {
                    ch |= 1 << (4 - bit);
                    latRange[0] = mid;
                } else {
                    latRange[1] = mid;
                }
            }

            even = !even;
            if (bit < 4) {
                bit++;
            } else {
                geohash.append(BASE32[ch]);
                bit = 0;
                ch = 0;
            }
        }

        return geohash.toString();
    }
}

