/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.fo.properties;

import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.fop.fo.Constants;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fonts.FontMetrics;
import org.apache.fop.fonts.Typeface;

/**
 * Store all common hyphenation properties.
 * See Sec. 7.9 of the XSL-FO Standard.
 * Public "structure" allows direct member access.
 */
public final class CommonHyphenation {

    /** Logger */
    private static final Log LOG = LogFactory.getLog(CommonHyphenation.class);

    private static final PropertyCache<CommonHyphenation> CACHE =
            new PropertyCache<CommonHyphenation>();

    private int hash;

    /** The "language" property */
    public final StringProperty language;

    /** The "country" property */
    public final StringProperty country;

    /** The "script" property */
    public final StringProperty script;

    /** The "hyphenate" property */
    public final EnumProperty hyphenate;

    /** The "hyphenation-character" property */
    public final OptionalCharacterProperty hyphenationCharacter;

    /** The "hyphenation-push-character-count" property */
    public final NumberProperty hyphenationPushCharacterCount;

    /** The "hyphenation-remain-character-count" property*/
    public final NumberProperty hyphenationRemainCharacterCount;

    /**
     * Construct a CommonHyphenation object holding the given properties
     *
     */
    private CommonHyphenation(StringProperty language,
                              StringProperty country,
                              StringProperty script,
                              EnumProperty hyphenate,
                              OptionalCharacterProperty hyphenationCharacter,
                              NumberProperty hyphenationPushCharacterCount,
                              NumberProperty hyphenationRemainCharacterCount) {
        this.language = language;
        this.country = country;
        this.script = script;
        this.hyphenate = hyphenate;
        this.hyphenationCharacter = hyphenationCharacter;
        this.hyphenationPushCharacterCount = hyphenationPushCharacterCount;
        this.hyphenationRemainCharacterCount = hyphenationRemainCharacterCount;
    }

    /**
     * Gets the canonical <code>CommonHyphenation</code> instance corresponding
     * to the values of the related properties present on the given
     * <code>PropertyList</code>
     * @param propertyList  the <code>PropertyList</code>
     * @return a common hyphenation instance
     * @throws PropertyException if a a property exception occurs
     */
    public static CommonHyphenation getInstance(PropertyList propertyList)
            throws PropertyException {
        StringProperty language
            = (StringProperty) propertyList.get(Constants.PR_LANGUAGE);
        StringProperty country
            = (StringProperty) propertyList.get(Constants.PR_COUNTRY);
        StringProperty script
            = (StringProperty) propertyList.get(Constants.PR_SCRIPT);
        EnumProperty hyphenate
            = (EnumProperty) propertyList.get(Constants.PR_HYPHENATE);
        OptionalCharacterProperty hyphenationCharacter
            = (OptionalCharacterProperty) propertyList.get(Constants.PR_HYPHENATION_CHARACTER);
        NumberProperty hyphenationPushCharacterCount
            = (NumberProperty) propertyList.get(Constants.PR_HYPHENATION_PUSH_CHARACTER_COUNT);
        NumberProperty hyphenationRemainCharacterCount
            = (NumberProperty) propertyList.get(Constants.PR_HYPHENATION_REMAIN_CHARACTER_COUNT);

        CommonHyphenation instance = new CommonHyphenation(
                                language,
                                country,
                                script,
                                hyphenate,
                                hyphenationCharacter,
                                hyphenationPushCharacterCount,
                                hyphenationRemainCharacterCount);

        return CACHE.fetch(instance);
    }

    private static final char HYPHEN_MINUS = '-';
    private static final char MINUS_SIGN = '\u2212';

    /**
     * Returns the effective hyphenation character for a font. The hyphenation character specified
     * in XSL-FO may be substituted if it's not available in the font.
     * @param font the font
     * @return the effective hyphenation character.
     */
    public Character getHyphChar(org.apache.fop.fonts.Font font) {
        if (hyphenationCharacter.getObject() == null) {
            return null;
        }
        char hyphChar = hyphenationCharacter.getCharacter();
        if (font.hasChar(hyphChar)) {
            return hyphChar; //short-cut
        }
        char effHyphChar = hyphChar;
        boolean warn = false;
        if (font.hasChar(HYPHEN_MINUS)) {
            effHyphChar = HYPHEN_MINUS;
            warn = true;
        } else if (font.hasChar(MINUS_SIGN)) {
            effHyphChar = MINUS_SIGN;
            FontMetrics metrics = font.getFontMetrics();
            if (metrics instanceof Typeface) {
                Typeface typeface = (Typeface)metrics;
                if ("SymbolEncoding".equals(typeface.getEncodingName())) {
                    //SymbolEncoding doesn't have HYPHEN_MINUS, so replace by MINUS_SIGN
                } else {
                    //only warn if the encoding is not SymbolEncoding
                    warn = true;
                }
            }
        } else {
            effHyphChar = ' ';
            FontMetrics metrics = font.getFontMetrics();
            if (metrics instanceof Typeface) {
                Typeface typeface = (Typeface)metrics;
                if ("ZapfDingbatsEncoding".equals(typeface.getEncodingName())) {
                    //ZapfDingbatsEncoding doesn't have HYPHEN_MINUS, so replace by ' '
                } else {
                    //only warn if the encoding is not ZapfDingbatsEncoding
                    warn = true;
                }
            }
        }
        if (warn) {
            LOG.warn("Substituted specified hyphenation character (0x"
                    + Integer.toHexString(hyphChar)
                    + ") with 0x" + Integer.toHexString(effHyphChar)
                    + " because the font doesn't have the specified hyphenation character: "
                    + font.getFontTriplet());
        }
        return effHyphChar;
    }

    /**
     * Returns the IPD for the hyphenation character for a font.
     * @param font the font
     * @return the IPD in millipoints for the hyphenation character.
     */
    public int getHyphIPD(org.apache.fop.fonts.Font font) {
        Character hyphChar = getHyphChar(font);
        return (hyphChar == null) ? 0 : font.getCharWidth(hyphChar);
    }

    /**
     * Creates and returns a {@link Locale} representation of the language and country.
     *
     * @return the language (and the country if set) represented as a locale, {@code null}
     * if the language has not been set (i.e., has been left to its initial value of
     * "none")
     */
    public Locale getLocale() {
        return toLocale(language.getString(), country.getString());
    }

    /**
     * Creates and returns a {@link Locale} representation of the given language, and the
     * given country if set. The country is considered to be set if not {@code null} and
     * not set to "none".
     *
     * @return the language and country represented as a locale, {@code null} if the
     * language is null or "none" (case insensitive)
     */
    public static Locale toLocale(String language, String country) {
        Locale locale = null;
        if (isDefined(language)) {
            if (isDefined(country)) {
                locale = new Locale(language, country);
            } else {
                locale = new Locale(language);
            }
        }
        return locale;
    }

    private static boolean isDefined(String property) {
        return !(property == null || property.equalsIgnoreCase("none"));
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof CommonHyphenation) {
            CommonHyphenation ch = (CommonHyphenation) obj;
            return (ch.language == this.language
                    && ch.country == this.country
                    && ch.script == this.script
                    && ch.hyphenate == this.hyphenate
                    && ch.hyphenationCharacter == this.hyphenationCharacter
                    && ch.hyphenationPushCharacterCount == this.hyphenationPushCharacterCount
                    && ch.hyphenationRemainCharacterCount == this.hyphenationRemainCharacterCount);
        }
        return false;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        if (this.hash == 0) {
            int hash = 17;
            hash = 37 * hash + (language == null ? 0 : language.hashCode());
            hash = 37 * hash + (script == null ? 0 : script.hashCode());
            hash = 37 * hash + (country == null ? 0 : country.hashCode());
            hash = 37 * hash + (hyphenate == null ? 0 : hyphenate.hashCode());
            hash = 37 * hash
                + (hyphenationCharacter == null
                   ? 0 : hyphenationCharacter.hashCode());
            hash = 37 * hash
                + (hyphenationPushCharacterCount == null
                 ? 0 : hyphenationPushCharacterCount.hashCode());
            hash = 37 * hash
                + (hyphenationRemainCharacterCount == null
                 ? 0 : hyphenationRemainCharacterCount.hashCode());
            this.hash = hash;
        }
        return this.hash;
    }

}
