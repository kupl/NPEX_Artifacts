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

/**
 * This class encapsulates the font attributes that need to be included
 * in the AFP data stream. This class does not assist in converting the
 * font attributes to AFP code pages and character set values.
 */
public class AFPFontAttributes {

    /** the font reference */
    private int fontReference;

    /** the font key */
    private final String fontKey;

    /** the font */
    private final AFPFont font;

    /** the point size */
    private final int pointSize;

    /**
     * Constructor for the AFPFontAttributes
     *
     * @param fontKey the font key
     * @param font the font
     * @param pointSize the point size
     */
    public AFPFontAttributes(String fontKey, AFPFont font, int pointSize) {
        this.fontKey = fontKey;
        this.font = font;
        this.pointSize = pointSize;
    }

    /**
     * Return the font
     *
     * @return the font
     */
    public AFPFont getFont() {
        return font;
    }

    /**
     * Return the FontKey attribute
     *
     * @return the FontKey attribute
     */
    public String getFontKey() {
        return fontKey + pointSize;
    }

    /**
     * Return the point size attribute
     *
     * @return the point size attribute
     */
    public int getPointSize() {
        return pointSize;
    }

    /**
     * Return the FontReference attribute
     *
     * @return the FontReference attribute
     */
    public int getFontReference() {
        return fontReference;
    }

    /**
     * Sets the FontReference attribute
     *
     * @param fontReference the FontReference to set
     */
    public void setFontReference(int fontReference) {
        this.fontReference = fontReference;
    }

    /** {@inheritDoc} */
    public String toString() {
        return "fontReference=" + fontReference
            + ", fontKey=" + fontKey
            + ", font=" + font
            + ", pointSize=" + pointSize;
    }
}
