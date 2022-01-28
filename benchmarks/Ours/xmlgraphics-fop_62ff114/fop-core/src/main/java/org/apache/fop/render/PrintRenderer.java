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

package org.apache.fop.render;

// FOP
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.area.Area;
import org.apache.fop.area.Trait;
import org.apache.fop.fonts.CustomFontCollection;
import org.apache.fop.fonts.EmbedFontInfo;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontCollection;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontManager;
import org.apache.fop.fonts.FontTriplet;
import org.apache.fop.fonts.base14.Base14FontCollection;

/** Abstract base class of "Print" type renderers.  */
public abstract class PrintRenderer extends AbstractRenderer {

    /**
     * @param userAgent the user agent that contains configuration details. This cannot be null.
     */
    public PrintRenderer(FOUserAgent userAgent) {
        super(userAgent);
    }

    /** Font configuration */
    protected FontInfo fontInfo;

    /** list of fonts */
    protected List<EmbedFontInfo> embedFontInfoList;

    /**
     * Adds a font list to current list of fonts
     * @param fontList a font info list
     */
    public void addFontList(List<EmbedFontInfo> fontList) {
        if (embedFontInfoList == null) {
            setFontList(fontList);
        } else {
            embedFontInfoList.addAll(fontList);
        }
    }

    /**
     * @param embedFontInfoList list of available fonts
     */
    public void setFontList(List<EmbedFontInfo> embedFontInfoList) {
        this.embedFontInfoList = embedFontInfoList;
    }

    /**
     * @return list of available embedded fonts
     */
    public List<EmbedFontInfo> getFontList() {
        return this.embedFontInfoList;
    }

    /** {@inheritDoc} */
    public void setupFontInfo(FontInfo inFontInfo) throws FOPException {
        this.fontInfo = inFontInfo;
        FontManager fontManager = userAgent.getFontManager();
        FontCollection[] fontCollections = new FontCollection[] {
                new Base14FontCollection(fontManager.isBase14KerningEnabled()),
                new CustomFontCollection(fontManager.getResourceResolver(), getFontList(),
                        userAgent.isComplexScriptFeaturesEnabled())
        };
        fontManager.setup(getFontInfo(), fontCollections);
    }

    /**
     * Returns the internal font key for a font triplet coming from the area tree
     * @param area the area from which to retrieve the font triplet information
     * @return the internal font key (F1, F2 etc.) or null if not found
     */
    protected String getInternalFontNameForArea(Area area) {
        FontTriplet triplet = (FontTriplet)area.getTrait(Trait.FONT);
        String key = fontInfo.getInternalFontKey(triplet);
        if (key == null) {
            //Find a default fallback font as last resort
            triplet = FontTriplet.DEFAULT_FONT_TRIPLET;
            key = fontInfo.getInternalFontKey(triplet);
        }
        return key;
    }

    /**
     * Returns a Font object constructed based on the font traits in an area
     * @param area the area from which to retrieve the font triplet information
     * @return the requested Font instance or null if not found
     */
    protected Font getFontFromArea(Area area) {
        FontTriplet triplet = (FontTriplet)area.getTrait(Trait.FONT);
        int size = (Integer) area.getTrait(Trait.FONT_SIZE);
        return fontInfo.getFontInstance(triplet, size);
    }

    /**
     * Instantiates a RendererContext for an image
     * @return a newly created RendererContext.
     */
    protected RendererContext instantiateRendererContext() {
        return new RendererContext(this, getMimeType());
    }

    /**
     * Creates a RendererContext for an image.
     * @param x the x coordinate (in millipoints)
     * @param y the y coordinate (in millipoints)
     * @param width the width of the image (in millipoints)
     * @param height the height of the image (in millipoints)
     * @param foreignAttributes a Map or foreign attributes, may be null
     * @return the RendererContext
     */
    protected RendererContext createRendererContext(int x, int y, int width, int height,
            Map foreignAttributes) {
        RendererContext context = instantiateRendererContext();
        context.setUserAgent(userAgent);

        context.setProperty(RendererContextConstants.WIDTH,
                width);
        context.setProperty(RendererContextConstants.HEIGHT,
                height);
        context.setProperty(RendererContextConstants.XPOS,
                x);
        context.setProperty(RendererContextConstants.YPOS,
                y);
        context.setProperty(RendererContextConstants.PAGE_VIEWPORT,
                            getCurrentPageViewport());
        if (foreignAttributes != null) {
            context.setProperty(RendererContextConstants.FOREIGN_ATTRIBUTES, foreignAttributes);
        }
        return context;
    }

    /**
     * Renders an XML document (SVG for example).
     * @param doc the DOM Document containing the XML document to be rendered
     * @param ns the namespace URI for the XML document
     * @param pos the position for the generated graphic/image
     * @param foreignAttributes the foreign attributes containing rendering hints, or null
     */
    public void renderDocument(Document doc, String ns, Rectangle2D pos, Map foreignAttributes) {
        int x = currentIPPosition + (int) pos.getX();
        int y = currentBPPosition + (int) pos.getY();
        int width = (int)pos.getWidth();
        int height = (int)pos.getHeight();
        RendererContext context = createRendererContext(x, y, width, height, foreignAttributes);

        renderXML(context, doc, ns);
    }

    /**
     * @return the font info
     */
    public FontInfo getFontInfo() {
        return this.fontInfo;
    }
}
