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

package org.apache.fop.render.ps;

import java.util.Map;

import org.apache.xmlgraphics.ps.PSResource;

import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.LazyFont;
import org.apache.fop.fonts.Typeface;

/**
 * A cache for font resource objects.
 */
class FontResourceCache {

    private final FontInfo fontInfo;

    /** This is a map of PSResource instances of all fonts defined (key: font key) */
    private Map fontResources = new java.util.HashMap();

    public FontResourceCache(FontInfo fontInfo) {
        this.fontInfo = fontInfo;
    }

    /**
     * Returns the PSFontResource for the given font key.
     * @param key the font key ("F*")
     * @return the matching PSFontResource instance
     */
    public PSFontResource getFontResourceForFontKey(String key) {
        PSFontResource res = null;
        if (this.fontResources != null) {
            res = (PSFontResource)this.fontResources.get(key);
        } else {
            this.fontResources = new java.util.HashMap();
        }
        if (res == null) {
            res = PSFontResource.createFontResource(
                    new PSResource(PSResource.TYPE_FONT, getPostScriptNameForFontKey(key)));
            this.fontResources.put(key, res);
        }
        return res;
    }

    private String getPostScriptNameForFontKey(String key) {
        int pos = key.indexOf('_');
        String postFix = null;
        if (pos > 0) {
            postFix = key.substring(pos);
            key = key.substring(0, pos);
        }
        Map<String, Typeface> fonts = fontInfo.getFonts();
        Typeface tf = fonts.get(key);
        if (tf instanceof LazyFont) {
            tf = ((LazyFont)tf).getRealFont();
        }
        if (tf == null) {
            throw new IllegalStateException("Font not available: " + key);
        }
        if (postFix == null) {
            return tf.getEmbedFontName();
        } else {
            return tf.getEmbedFontName() + postFix;
        }
    }

    /**
     * Adds a number of fonts to the cache.
     * @param fontMap the font map
     */
    public void addAll(Map fontMap) {
        this.fontResources.putAll(fontMap);
    }

}
