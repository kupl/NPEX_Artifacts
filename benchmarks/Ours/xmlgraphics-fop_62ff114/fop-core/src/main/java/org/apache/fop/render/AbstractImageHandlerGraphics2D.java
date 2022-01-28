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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import org.apache.xmlgraphics.util.UnitConv;

/**
 * Abstract base class for ImageHandler implementations that process Java2D images through
 * the Graphics2DImagePainter interface.
 */
public abstract class AbstractImageHandlerGraphics2D implements ImageHandler {

    /**
     * Paints the image to a BufferedImage and returns that.
     * @param painter the painter which will paint the actual image
     * @param targetDimension the target dimensions of the image to be converted to a bitmap
     * @param resolution the requested bitmap resolution
     * @param gray true if the generated image should be in grayscales
     * @param withAlpha true if an alpha channel should be created
     * @return the generated BufferedImage
     */
    protected BufferedImage paintToBufferedImage(
            org.apache.xmlgraphics.java2d.Graphics2DImagePainter painter,
            Dimension targetDimension,
            int resolution, boolean gray, boolean withAlpha) {
        int bmw = (int)Math.ceil(UnitConv.mpt2px(targetDimension.getWidth(), resolution));
        int bmh = (int)Math.ceil(UnitConv.mpt2px(targetDimension.getHeight(), resolution));
        BufferedImage bi;
        if (gray) {
            if (withAlpha) {
                bi = createGrayBufferedImageWithAlpha(bmw, bmh);
            } else {
                bi = new BufferedImage(bmw, bmh, BufferedImage.TYPE_BYTE_GRAY);
            }
        } else {
            if (withAlpha) {
                bi = new BufferedImage(bmw, bmh, BufferedImage.TYPE_INT_ARGB);
            } else {
                bi = new BufferedImage(bmw, bmh, BufferedImage.TYPE_INT_RGB);
            }
        }
        Graphics2D g2d = bi.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            setRenderingHintsForBufferedImage(g2d);

            g2d.setBackground(Color.white);
            g2d.setColor(Color.black);
            if (!withAlpha) {
                g2d.clearRect(0, 0, bmw, bmh);
            }
            /* debug code
            int off = 2;
            g2d.drawLine(off, 0, off, bmh);
            g2d.drawLine(bmw - off, 0, bmw - off, bmh);
            g2d.drawLine(0, off, bmw, off);
            g2d.drawLine(0, bmh - off, bmw, bmh - off);
            */
            double sx = (double)bmw / targetDimension.getWidth();
            double sy = (double)bmh / targetDimension.getHeight();
            g2d.scale(sx, sy);

            //Paint the image on the BufferedImage
            Rectangle2D area = new Rectangle2D.Double(
                    0.0, 0.0, targetDimension.getWidth(), targetDimension.getHeight());
            painter.paint(g2d, area);
        } finally {
            g2d.dispose();
        }
        return bi;
    }

    private static BufferedImage createGrayBufferedImageWithAlpha(int width, int height) {
        BufferedImage bi;
        boolean alphaPremultiplied = true;
        int bands = 2;
        int[] bits = new int[bands];
        for (int i = 0; i < bands; i++) {
            bits[i] = 8;
        }
        ColorModel cm = new ComponentColorModel(
                ColorSpace.getInstance(ColorSpace.CS_GRAY),
                bits,
                true, alphaPremultiplied,
                Transparency.TRANSLUCENT,
                DataBuffer.TYPE_BYTE);
        WritableRaster wr = Raster.createInterleavedRaster(
                DataBuffer.TYPE_BYTE,
                width, height, bands,
                new Point(0, 0));
        bi = new BufferedImage(cm, wr, alphaPremultiplied, null);
        return bi;
    }

    /**
     * Sets rendering hints on the Graphics2D created for painting to a BufferedImage. Subclasses
     * can modify the settings to customize the behavior.
     * @param g2d the Graphics2D instance
     */
    protected void setRenderingHintsForBufferedImage(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

}
