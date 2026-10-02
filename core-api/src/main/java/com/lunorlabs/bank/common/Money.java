package com.lunorlabs.bank.common;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

public final class Money {
    private static final BigDecimal MAX = new BigDecimal("1000000000000");

    private Money() {}

    /** "LKR 30,000.00" */
    public static String fmt(BigDecimal v) {
        DecimalFormat df = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return "LKR " + df.format(v);
    }

    /** Accepts "5000", "5,000.50" or a JSON number. Empty if missing, non-positive, > 2 decimals or absurdly large. */
    public static Optional<BigDecimal> tryParse(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) return Optional.empty();
        String t = n.isNumber() ? n.decimalValue().toPlainString() : n.asText("").trim().replace(",", "");
        if (t.isEmpty()) return Optional.empty();
        try {
            BigDecimal v = new BigDecimal(t);
            if (v.signum() <= 0 || v.compareTo(MAX) > 0) return Optional.empty();
            if (v.stripTrailingZeros().scale() > 2) return Optional.empty();
            return Optional.of(v.setScale(2, RoundingMode.UNNECESSARY));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
