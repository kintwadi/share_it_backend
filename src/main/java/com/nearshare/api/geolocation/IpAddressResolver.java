package com.nearshare.api.geolocation;

import jakarta.servlet.http.HttpServletRequest;

public class IpAddressResolver {
    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String xff = header(request, "X-Forwarded-For");
        String ip = firstForwardedFor(xff);
        if (isPublicIp(ip)) return ip;
        String xri = header(request, "X-Real-IP");
        if (isPublicIp(xri)) return xri;
        String remote = safe(request.getRemoteAddr());
        if (isPublicIp(remote)) return remote;
        return null;
    }

    private static String header(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        return safe(v);
    }

    private static String safe(String s) {
        String v = s == null ? null : s.trim();
        if (v == null || v.isEmpty()) return null;
        if ("unknown".equalsIgnoreCase(v)) return null;
        return v;
    }

    private static String firstForwardedFor(String xff) {
        String v = safe(xff);
        if (v == null) return null;
        String[] parts = v.split(",");
        for (String p : parts) {
            String ip = safe(p);
            if (ip != null) return ip;
        }
        return null;
    }

    private static boolean isPublicIp(String ip) {
        String v = safe(ip);
        if (v == null) return false;
        if (v.contains(":")) {
            String lower = v.toLowerCase();
            if (lower.equals("::1")) return false;
            if (lower.startsWith("fe80:")) return false;
            if (lower.startsWith("fc") || lower.startsWith("fd")) return false;
            return true;
        }
        if (v.startsWith("10.")) return false;
        if (v.startsWith("127.")) return false;
        if (v.startsWith("192.168.")) return false;
        if (v.startsWith("172.")) {
            String[] parts = v.split("\\.");
            if (parts.length >= 2) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    if (second >= 16 && second <= 31) return false;
                } catch (NumberFormatException ignored) { }
            }
        }
        return true;
    }
}

