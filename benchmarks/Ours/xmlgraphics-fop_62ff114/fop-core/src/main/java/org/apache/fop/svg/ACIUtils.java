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

package org.apache.fop.svg;

import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.CharacterIterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.batik.bridge.SVGFontFamily;
import org.apache.batik.gvt.font.GVTFont;
import org.apache.batik.gvt.font.GVTFontFamily;
import org.apache.batik.gvt.text.GVTAttributedCharacterIterator;

import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.svg.font.FOPGVTFont;
import org.apache.fop.svg.font.FOPGVTFontFamily;

/**
 * Utilities for java.text.AttributedCharacterIterator.
 */
public final class ACIUtils {

    /** the logger for this class */
    private static final Log LOG = LogFactory.getLog(ACIUtils.class);

    private ACIUtils() {
        //This class shouldn't be instantiated.
    }

    /**
     * Tries to find matching fonts in FOP's {@link FontInfo} instance for fonts used by
     * Apache Batik. The method inspects the various GVT attributes found in the ACI.
     * @param aci the ACI to find matching fonts for
     * @param fontInfo the font info instance with FOP's fonts
     * @return an array of matching fonts
     */
    public static Font[] findFontsForBatikACI(AttributedCharacterIterator aci, FontInfo fontInfo) {
        List<Font> fonts = new java.util.ArrayList<Font>();
        @SuppressWarnings("unchecked")
        List<GVTFontFamily> gvtFonts = (List<GVTFontFamily>) aci.getAttribute(
                GVTAttributedCharacterIterator.TextAttribute.GVT_FONT_FAMILIES);
        String style = toStyle((Float) aci.getAttribute(TextAttribute.POSTURE));
        int weight = toCSSWeight((Float) aci.getAttribute(TextAttribute.WEIGHT));
        float fontSize = (Float) aci.getAttribute(TextAttribute.SIZE);

        String firstFontFamily = null;
        //GVT_FONT can sometimes be different from the fonts in GVT_FONT_FAMILIES
        //or GVT_FONT_FAMILIES can even be empty and only GVT_FONT is set
        GVTFont gvtFont = (GVTFont) aci.getAttribute(GVTAttributedCharacterIterator.TextAttribute.GVT_FONT);
        if (gvtFont != null) {
            String gvtFontFamily = gvtFont.getFamilyName();
            if (gvtFont instanceof FOPGVTFont) {
                Font font = ((FOPGVTFont) gvtFont).getFont();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Found a font that matches the GVT font: "
                              + gvtFontFamily + ", " + weight + ", " + style
                              + " -> " + font);
                }
                fonts.add(font);
            }
            firstFontFamily = gvtFontFamily;
        }

        if (gvtFonts != null) {
            boolean haveInstanceOfSVGFontFamily = false;
            for (GVTFontFamily fontFamily : gvtFonts) {
                if (fontFamily instanceof SVGFontFamily) {
                    haveInstanceOfSVGFontFamily = true;
                } else if (fontFamily instanceof FOPGVTFontFamily) {
                    Font font = ((FOPGVTFontFamily) fontFamily).deriveFont(fontSize, aci).getFont();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Found a font that matches the GVT font family: "
                                + fontFamily.getFamilyName() + ", " + weight + ", " + style + " -> " + font);
                    }
                    fonts.add(font);
                }
                if (firstFontFamily == null) {
                    firstFontFamily = fontFamily.getFamilyName();
                }
            }
            // SVG fonts are embedded fonts in the SVG document and are rarely used; however if they
            // are used but the fonts also exists in the system and are known to FOP then FOP should
            // use them; then the decision whether Batik should stroke the text should be made after
            // no matching fonts are found
            if (fonts.isEmpty() && haveInstanceOfSVGFontFamily) {
                fontInfo.notifyStrokingSVGTextAsShapes(firstFontFamily);
                return null; // Let Batik paint this text!
            }
        }
        return fonts.isEmpty() ? null : fonts.toArray(new Font[fonts.size()]);
    }

    public static int toCSSWeight(Float weight) {
        if (weight == null) {
            return 400;
        } else if (weight <= TextAttribute.WEIGHT_EXTRA_LIGHT) {
            return 100;
        } else if (weight <= TextAttribute.WEIGHT_LIGHT) {
            return 200;
        } else if (weight <= TextAttribute.WEIGHT_DEMILIGHT) {
            return 300;
        } else if (weight <= TextAttribute.WEIGHT_REGULAR) {
            return 400;
        } else if (weight <= TextAttribute.WEIGHT_SEMIBOLD) {
            return 500;
        } else if (weight < TextAttribute.WEIGHT_BOLD) {
            return 600;
        } else if (weight == TextAttribute.WEIGHT_BOLD.floatValue()) {
            return 700;
        } else if (weight <= TextAttribute.WEIGHT_HEAVY) {
            return 800;
        } else if (weight <= TextAttribute.WEIGHT_EXTRABOLD) {
            return 900;
        } else {
            return 900;
        }
    }

    public static String toStyle(Float posture) {
        return ((posture != null) && (posture > 0.0))
                       ? Font.STYLE_ITALIC
                       : Font.STYLE_NORMAL;
    }

    /**
     * Dumps the contents of an ACI to System.out. Used for debugging only.
     * @param aci the ACI to dump
     */
    public static void dumpAttrs(AttributedCharacterIterator aci) {
        aci.first();
        Set<Entry<Attribute, Object>> entries = aci.getAttributes().entrySet();
        for (Map.Entry<Attribute, Object> entry : entries) {
            if (entry.getValue() != null) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        }
        int start = aci.getBeginIndex();
        System.out.print("AttrRuns: ");
        while (aci.current() != CharacterIterator.DONE) {
            int end = aci.getRunLimit();
            System.out.print("" + (end - start) + ", ");
            aci.setIndex(end);
            if (start == end) {
                break;
            }
            start = end;
        }
        System.out.println("");
    }

}
