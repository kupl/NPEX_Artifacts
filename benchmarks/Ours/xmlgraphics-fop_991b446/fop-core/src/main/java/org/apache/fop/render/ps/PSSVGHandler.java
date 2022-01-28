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

// Java
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.Map;

import org.w3c.dom.Document;

import org.apache.avalon.framework.configuration.Configuration;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.gvt.GraphicsNode;

import org.apache.xmlgraphics.java2d.ps.PSGraphics2D;
import org.apache.xmlgraphics.ps.PSGenerator;

import org.apache.fop.fonts.FontInfo;
import org.apache.fop.image.loader.batik.BatikUtil;
import org.apache.fop.render.AbstractGenericSVGHandler;
import org.apache.fop.render.ImageHandlerUtil;
import org.apache.fop.render.Renderer;
import org.apache.fop.render.RendererContext;
import org.apache.fop.render.RendererContextConstants;
import org.apache.fop.svg.SVGEventProducer;
import org.apache.fop.svg.SVGUserAgent;

/**
 * PostScript XML handler for SVG. Uses Apache Batik for SVG processing.
 * This handler handles XML for foreign objects when rendering to PostScript.
 * It renders SVG to the PostScript document using the PSGraphics2D.
 * The properties from the PostScript renderer are subject to change.
 *
 * @version $Id$
 */
public class PSSVGHandler extends AbstractGenericSVGHandler
            implements PSRendererContextConstants {

    /**
     * Create a new PostScript XML handler for use by the PostScript renderer.
     */
    public PSSVGHandler() {
    }

    /**
     * Get the pdf information from the render context.
     *
     * @param context the renderer context
     * @return the pdf information retrieved from the context
     */
    public static PSInfo getPSInfo(RendererContext context) {
        PSInfo psi = new PSInfo();
        psi.psGenerator = (PSGenerator)context.getProperty(PS_GENERATOR);
        psi.fontInfo = (org.apache.fop.fonts.FontInfo) context.getProperty(PS_FONT_INFO);
        psi.width = (Integer) context.getProperty(WIDTH);
        psi.height = (Integer) context.getProperty(HEIGHT);
        psi.currentXPosition = (Integer) context.getProperty(XPOS);
        psi.currentYPosition = (Integer) context.getProperty(YPOS);
        psi.cfg = (Configuration)context.getProperty(HANDLER_CONFIGURATION);
        return psi;
    }

    /**
     * PostScript information structure for drawing the XML document.
     */
    public static class PSInfo {

        /** see PS_GENERATOR */
        private PSGenerator psGenerator;
        /** see PS_FONT_INFO */
        private org.apache.fop.fonts.FontInfo fontInfo;
        /** see WIDTH */
        private int width;
        /** see HEIGHT */
        private int height;
        /** see XPOS */
        private int currentXPosition;
        /** see YPOS */
        private int currentYPosition;
        /** see HANDLER_CONFIGURATION */
        private Configuration cfg;

        /**
         * Returns the PSGenerator.
         * @return PSGenerator
         */
        public PSGenerator getPSGenerator() {
            return psGenerator;
        }

        /**
         * Sets the PSGenerator.
         * @param psGenerator The PSGenerator to set
         */
        public void setPsGenerator(PSGenerator psGenerator) {
            this.psGenerator = psGenerator;
        }

        /**
         * Returns the fontInfo.
         * @return FontInfo
         */
        public FontInfo getFontInfo() {
            return fontInfo;
        }

        /**
         * Sets the fontInfo.
         * @param fontInfo The fontInfo to set
         */
        public void setFontInfo(FontInfo fontInfo) {
            this.fontInfo = fontInfo;
        }

        /**
         * Returns the currentXPosition.
         * @return int
         */
        public int getCurrentXPosition() {
            return currentXPosition;
        }

        /**
         * Sets the currentXPosition.
         * @param currentXPosition The currentXPosition to set
         */
        public void setCurrentXPosition(int currentXPosition) {
            this.currentXPosition = currentXPosition;
        }

        /**
         * Returns the currentYPosition.
         * @return int
         */
        public int getCurrentYPosition() {
            return currentYPosition;
        }

        /**
         * Sets the currentYPosition.
         * @param currentYPosition The currentYPosition to set
         */
        public void setCurrentYPosition(int currentYPosition) {
            this.currentYPosition = currentYPosition;
        }

        /**
         * Returns the width.
         * @return int
         */
        public int getWidth() {
            return width;
        }

        /**
         * Sets the width.
         * @param width The pageWidth to set
         */
        public void setWidth(int width) {
            this.width = width;
        }

        /**
         * Returns the height.
         * @return int
         */
        public int getHeight() {
            return height;
        }

        /**
         * Sets the height.
         * @param height The height to set
         */
        public void setHeight(int height) {
            this.height = height;
        }

        /**
         * Returns the height.
         * @return int
         */
        public Configuration getHandlerConfiguration() {
            return this.cfg;
        }

        /**
         * Sets the handler configuration.
         * @param cfg the configuration object
         */
        public void setHandlerConfiguration(Configuration cfg) {
            this.cfg = cfg;
        }

    }

    /**
     * Render the svg document.
     * @param context the renderer context
     * @param doc the svg document
     */
    protected void renderSVGDocument(RendererContext context, Document doc) {
        assert context != null;
        PSInfo psInfo = getPSInfo(context);
        int xOffset = psInfo.currentXPosition;
        int yOffset = psInfo.currentYPosition;
        PSGenerator gen = psInfo.psGenerator;

        boolean paintAsBitmap = false;
        Map foreign = (Map)context.getProperty(RendererContextConstants.FOREIGN_ATTRIBUTES);
        paintAsBitmap = ImageHandlerUtil.isConversionModeBitmap(foreign);
        if (paintAsBitmap) {
            try {
                super.renderSVGDocument(context, doc);
            } catch (IOException ioe) {
                SVGEventProducer eventProducer = SVGEventProducer.Provider.get(
                        context.getUserAgent().getEventBroadcaster());
                eventProducer.svgRenderingError(this, ioe, getDocumentURI(doc));
            }
            return;
        }

        //Controls whether text painted by Batik is generated using text or path operations
        boolean strokeText = false;
        Configuration cfg = psInfo.getHandlerConfiguration();
        if (cfg != null) {
            strokeText = cfg.getChild("stroke-text", true).getValueAsBoolean(strokeText);
        }

        SVGUserAgent ua = new SVGUserAgent(context.getUserAgent(), null /* TODO */, new AffineTransform());

        PSGraphics2D graphics = new PSGraphics2D(strokeText, gen);
        graphics.setGraphicContext(new org.apache.xmlgraphics.java2d.GraphicContext());

        BridgeContext ctx = new PSBridgeContext(ua,
                (strokeText ? null : psInfo.fontInfo),
                context.getUserAgent().getImageManager(),
                context.getUserAgent().getImageSessionContext());

        //Cloning SVG DOM as Batik attaches non-thread-safe facilities (like the CSS engine)
        //to it.
        Document clonedDoc = BatikUtil.cloneSVGDocument(doc);

        GraphicsNode root;
        try {
            GVTBuilder builder = new GVTBuilder();
            root = builder.build(ctx, clonedDoc);
        } catch (Exception e) {
            SVGEventProducer eventProducer = SVGEventProducer.Provider.get(
                    context.getUserAgent().getEventBroadcaster());
            eventProducer.svgNotBuilt(this, e, getDocumentURI(doc));
            return;
        }
        // get the 'width' and 'height' attributes of the SVG document
        float w = (float)ctx.getDocumentSize().getWidth() * 1000f;
        float h = (float)ctx.getDocumentSize().getHeight() * 1000f;

        float sx = psInfo.getWidth() / w;
        float sy = psInfo.getHeight() / h;

        try {
            gen.commentln("%FOPBeginSVG");
            gen.saveGraphicsState();
            /*
             * Clip to the svg area.
             * Note: To have the svg overlay (under) a text area then use
             * an fo:block-container
             */
            gen.writeln("newpath");
            gen.defineRect(xOffset / 1000f, yOffset / 1000f,
                    psInfo.getWidth() / 1000f, psInfo.getHeight() / 1000f);
            gen.writeln("clip");

            // transform so that the coordinates (0,0) is from the top left
            // and positive is down and to the right. (0,0) is where the
            // viewBox puts it.
            gen.concatMatrix(sx, 0, 0, sy, xOffset / 1000f, yOffset / 1000f);

            /*
            SVGSVGElement svg = ((SVGDocument)doc).getRootElement();
            AffineTransform at = ViewBox.getPreserveAspectRatioTransform(svg,
                    psInfo.getWidth() / 1000f, psInfo.getHeight() / 1000f, ctx);
            if (!at.isIdentity()) {
                double[] vals = new double[6];
                at.getMatrix(vals);
                gen.concatMatrix(vals);
            }*/

            AffineTransform transform = new AffineTransform();
            // scale to viewbox
            transform.translate(xOffset, yOffset);
            gen.getCurrentState().concatMatrix(transform);
            try {
                root.paint(graphics);
            } catch (Exception e) {
                SVGEventProducer eventProducer = SVGEventProducer.Provider.get(
                        context.getUserAgent().getEventBroadcaster());
                eventProducer.svgRenderingError(this, e, getDocumentURI(doc));
            }

            gen.restoreGraphicsState();
            gen.commentln("%FOPEndSVG");
        } catch (IOException ioe) {
            SVGEventProducer eventProducer = SVGEventProducer.Provider.get(
                    context.getUserAgent().getEventBroadcaster());
            eventProducer.svgRenderingError(this, ioe, getDocumentURI(doc));
        }
    }

    /** {@inheritDoc} */
    public boolean supportsRenderer(Renderer renderer) {
        return false;
    }

}

