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

package org.apache.fop.afp.fonts;

import org.apache.fop.afp.AFPEventProducer;
import org.apache.fop.fonts.Base14Font;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontCollection;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.base14.Courier;
import org.apache.fop.fonts.base14.CourierBold;
import org.apache.fop.fonts.base14.CourierBoldOblique;
import org.apache.fop.fonts.base14.CourierOblique;
import org.apache.fop.fonts.base14.Helvetica;
import org.apache.fop.fonts.base14.HelveticaBold;
import org.apache.fop.fonts.base14.HelveticaOblique;
import org.apache.fop.fonts.base14.TimesBold;
import org.apache.fop.fonts.base14.TimesBoldItalic;
import org.apache.fop.fonts.base14.TimesItalic;
import org.apache.fop.fonts.base14.TimesRoman;

/**
 * Sets up a typical Base 12 font configuration for AFP
 */
public class AFPBase12FontCollection implements FontCollection {

    private final AFPEventProducer eventProducer;

    /**
     * @param eventProducer the AFP-specific event producer
     */
    public AFPBase12FontCollection(AFPEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    /** standard raster font sizes */
    private static final int[] RASTER_SIZES = {6, 7, 8, 9, 10, 11, 12, 14, 16, 18, 20, 24, 30, 36};

    /** standard raster font charset references */
    private static final String[] CHARSET_REF = {
        "60", "70", "80", "90", "00", "A0", "B0", "D0", "F0", "H0", "J0", "N0", "T0", "Z0"};

    private void addCharacterSet(RasterFont font, String charsetName, Base14Font base14) {
        for (int i = 0; i < RASTER_SIZES.length; i++) {
            int size = RASTER_SIZES[i] * 1000;
            FopCharacterSet characterSet = new FopCharacterSet(
                    CharacterSet.DEFAULT_CODEPAGE, CharacterSet.DEFAULT_ENCODING,
                    charsetName + CHARSET_REF[i], base14, eventProducer);
            font.addCharacterSet(size, characterSet);
        }
    }

    private int addFontProperties(FontInfo fontInfo, AFPFont font,
            String[] names, String style, int weight, int num) {
        String internalFontKey = "F" + num;
        fontInfo.addMetrics(internalFontKey, font);
        fontInfo.addFontProperties(internalFontKey, names, style, weight);
        num++;
        return num;
    }

    /** {@inheritDoc} */
    public int setup(int start, FontInfo fontInfo) {

        /**
         * Add the base 12 fonts (Helvetica, Times and Courier)
         *
         * Note: this default font configuration may not be available
         * on your AFP environment.
         */
        int num = start;
        RasterFont font = null;

        /** standard font family reference names for Helvetica font */
        final String[] helveticaNames = {"Helvetica", "Arial", "sans-serif"};
        font = createReferencedRasterFont("Helvetica");
        addCharacterSet(font, "C0H200", new Helvetica());
        num = addFontProperties(fontInfo, font, helveticaNames,
                Font.STYLE_NORMAL, Font.WEIGHT_NORMAL, num);

        font = createReferencedRasterFont("Helvetica Italic");
        addCharacterSet(font, "C0H300", new HelveticaOblique());
        num = addFontProperties(fontInfo, font, helveticaNames,
                Font.STYLE_ITALIC, Font.WEIGHT_NORMAL, num);

        font = createReferencedRasterFont("Helvetica (Semi) Bold");
        addCharacterSet(font, "C0H400", new HelveticaBold());
        num = addFontProperties(fontInfo, font, helveticaNames,
                Font.STYLE_NORMAL, Font.WEIGHT_BOLD, num);

        font = createReferencedRasterFont("Helvetica Italic (Semi) Bold");
        addCharacterSet(font, "C0H500", new HelveticaOblique());
        num = addFontProperties(fontInfo, font, helveticaNames,
                Font.STYLE_ITALIC, Font.WEIGHT_BOLD, num);


        /** standard font family reference names for Times font */

        /** any is treated as serif */
        final String[] timesNames = {"Times", "TimesRoman", "Times Roman", "Times-Roman",
                "Times New Roman", "TimesNewRoman", "serif", "any"};

        font = createReferencedRasterFont("Times Roman");
        addCharacterSet(font, "C0N200", new TimesRoman());
        num = addFontProperties(fontInfo, font, timesNames,
                Font.STYLE_NORMAL, Font.WEIGHT_NORMAL, num);

        font = createReferencedRasterFont("Times Roman Italic");
        addCharacterSet(font, "C0N300", new TimesItalic());
        num = addFontProperties(fontInfo, font, timesNames,
                Font.STYLE_ITALIC, Font.WEIGHT_NORMAL, num);

        font = createReferencedRasterFont("Times Roman Bold");
        addCharacterSet(font, "C0N400", new TimesBold());
        num = addFontProperties(fontInfo, font, timesNames,
                Font.STYLE_NORMAL, Font.WEIGHT_BOLD, num);

        font = createReferencedRasterFont("Times Roman Italic Bold");
        addCharacterSet(font, "C0N500", new TimesBoldItalic());
        num = addFontProperties(fontInfo, font, timesNames,
                Font.STYLE_ITALIC, Font.WEIGHT_BOLD, num);


        /** standard font family reference names for Courier font */
        final String[] courierNames = {"Courier", "monospace"};

        font = createReferencedRasterFont("Courier");
        addCharacterSet(font, "C04200", new Courier());
        num = addFontProperties(fontInfo, font, courierNames,
                Font.STYLE_NORMAL, Font.WEIGHT_NORMAL, num);

        font = createReferencedRasterFont("Courier Italic");
        addCharacterSet(font, "C04300", new CourierOblique());
        num = addFontProperties(fontInfo, font, courierNames,
                Font.STYLE_ITALIC, Font.WEIGHT_NORMAL, num);

        font = createReferencedRasterFont("Courier Bold");
        addCharacterSet(font, "C04400", new CourierBold());
        num = addFontProperties(fontInfo, font, courierNames,
                Font.STYLE_NORMAL, Font.WEIGHT_BOLD, num);

        font = createReferencedRasterFont("Courier Italic Bold");
        addCharacterSet(font, "C04500", new CourierBoldOblique());
        num = addFontProperties(fontInfo, font, courierNames,
                Font.STYLE_ITALIC, Font.WEIGHT_BOLD, num);

        return num;
    }

    private RasterFont createReferencedRasterFont(String fontFamily) {
        boolean embeddable = false; //Font is assumed to be available on the target platform
        return new RasterFont(fontFamily, embeddable);
    }

}
