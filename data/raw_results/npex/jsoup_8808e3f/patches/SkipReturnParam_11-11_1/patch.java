package org.jsoup.internal;

import java.util.Locale;

/**
 * Util methods for normalizing strings. Jsoup internal use only, please don't depend on this API.
 */
public final class Normalizer {

public static java.lang.String lowerCase(final java.lang.String input) {
    /* NPEX_PATCH_BEGINS */
    if (input == null) {
        return "";
    }
    return input.toLowerCase(java.util.Locale.ENGLISH);
}

    public static String normalize(final String input) {
        return lowerCase(input).trim();
    }
}
