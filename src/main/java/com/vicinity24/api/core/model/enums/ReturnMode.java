package com.vicinity24.api.core.model.enums;

public enum ReturnMode {
    ANY,
    QR_CODE,
    MANUAL,
    DISPUTE;

    public static ReturnMode from(String raw) {
        if (raw == null) return ANY;
        String s = raw.trim();
        if (s.isEmpty()) return ANY;
        s = s.replace("-", "_").replace(" ", "_").toUpperCase();
        if (s.equals("ALL")) return ANY;
        if (s.equals("QRCODE")) return QR_CODE;
        if (s.equals("QR")) return QR_CODE;
        if (s.equals("Q_R_CODE")) return QR_CODE;
        if (s.equals("QR_CODE")) return QR_CODE;
        if (s.equals("MANUAL")) return MANUAL;
        if (s.equals("DISPUTE")) return DISPUTE;
        if (s.equals("ANY")) return ANY;
        return ANY;
    }
}
