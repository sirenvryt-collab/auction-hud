package com.donutauction.util;

import java.util.Locale;

/**
 * Handles parsing and formatting of DonutSMP-style money amounts.
 * <p>
 * DonutSMP chat messages can express amounts either as a plain integer
 * ("15000") or with a shorthand suffix ("47M", "1.5B", "250K"). This class
 * converts between that shorthand and a plain {@code double} value used
 * internally, and back again for display in the HUD / browser.
 */
public final class MoneyFormat {

    private MoneyFormat() {
    }

    /**
     * Parses a raw amount string (e.g. "15000", "47M", "1.5B", "2,500") into
     * a plain double value. Returns 0 if the string cannot be parsed.
     */
    public static double parseAmount(String raw) {
        if (raw == null) {
            return 0;
        }

        String cleaned = raw.trim().replace(",", "");
        if (cleaned.isEmpty()) {
            return 0;
        }

        char suffix = Character.toUpperCase(cleaned.charAt(cleaned.length() - 1));
        double multiplier = 1.0;
        String numberPart = cleaned;

        switch (suffix) {
            case 'K':
                multiplier = 1_000.0;
                numberPart = cleaned.substring(0, cleaned.length() - 1);
                break;
            case 'M':
                multiplier = 1_000_000.0;
                numberPart = cleaned.substring(0, cleaned.length() - 1);
                break;
            case 'B':
                multiplier = 1_000_000_000.0;
                numberPart = cleaned.substring(0, cleaned.length() - 1);
                break;
            case 'T':
                multiplier = 1_000_000_000_000.0;
                numberPart = cleaned.substring(0, cleaned.length() - 1);
                break;
            default:
                // No suffix, plain number.
                break;
        }

        try {
            return Double.parseDouble(numberPart) * multiplier;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Formats a plain double amount back into DonutSMP-style shorthand for
     * display, e.g. 47000000 -> "47M", 1500 -> "1.5K", 900 -> "900".
     */
    public static String format(double amount) {
        double abs = Math.abs(amount);
        if (abs >= 1_000_000_000_000.0) {
            return trimTrailingZero(amount / 1_000_000_000_000.0) + "T";
        }
        if (abs >= 1_000_000_000.0) {
            return trimTrailingZero(amount / 1_000_000_000.0) + "B";
        }
        if (abs >= 1_000_000.0) {
            return trimTrailingZero(amount / 1_000_000.0) + "M";
        }
        if (abs >= 1_000.0) {
            return trimTrailingZero(amount / 1_000.0) + "K";
        }
        return trimTrailingZero(amount);
    }

    private static String trimTrailingZero(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.format(Locale.ROOT, "%,d", (long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
