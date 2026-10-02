package com.lunorlabs.bank.common;

import java.util.Locale;

public enum Lang {
    EN, SI, TA;

    /** Parses an Accept-Language value such as "si", "ta-LK" or "en-US,en;q=0.9". Defaults to EN. */
    public static Lang fromHeader(String header) {
        if (header == null || header.isBlank()) return EN;
        String t = header.trim().toLowerCase(Locale.ROOT);
        if (t.startsWith("si")) return SI;
        if (t.startsWith("ta")) return TA;
        return EN;
    }

    public static Lang fromCode(String code) {
        if (code == null) return EN;
        try {
            return valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return EN;
        }
    }
}
