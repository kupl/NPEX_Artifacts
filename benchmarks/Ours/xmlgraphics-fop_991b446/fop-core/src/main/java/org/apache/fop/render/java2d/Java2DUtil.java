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

package org.apache.fop.render.java2d;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fonts.FontCollection;
import org.apache.fop.fonts.FontEventAdapter;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontManager;

/**
 * Rendering-related utilities for Java2D.
 */
public final class Java2DUtil {

    private Java2DUtil() {
    }

    /**
     * Builds a default {@link FontInfo} object for use with output formats using the Java2D
     * font setup.
     * @param fontInfo the font info object to populate
     * @param userAgent the user agent
     * @return the populated font information object
     */
    public static FontInfo buildDefaultJava2DBasedFontInfo(
            FontInfo fontInfo, FOUserAgent userAgent) {
        Java2DFontMetrics java2DFontMetrics = new Java2DFontMetrics();

        FontManager fontManager = userAgent.getFontManager();
        FontCollection[] fontCollections = new FontCollection[] {
                new org.apache.fop.render.java2d.Base14FontCollection(java2DFontMetrics),
                new InstalledFontCollection(java2DFontMetrics)
        };

        FontInfo fi = (fontInfo != null ? fontInfo : new FontInfo());
        fi.setEventListener(new FontEventAdapter(userAgent.getEventBroadcaster()));
        fontManager.setup(fi, fontCollections);
        return fi;
    }


}
