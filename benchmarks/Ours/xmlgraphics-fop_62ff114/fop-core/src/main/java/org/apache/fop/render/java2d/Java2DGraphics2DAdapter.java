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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import org.apache.xmlgraphics.java2d.Graphics2DImagePainter;

import org.apache.fop.render.AbstractGraphics2DAdapter;
import org.apache.fop.render.RendererContext;

/**
 * Graphics2DAdapter implementation for Java2D.
 */
public class Java2DGraphics2DAdapter extends AbstractGraphics2DAdapter {

    /** {@inheritDoc} */
    public void paintImage(Graphics2DImagePainter painter,
            RendererContext context,
            int x, int y, int width, int height) throws IOException {

        float fwidth = width / 1000f;
        float fheight = height / 1000f;
        float fx = x / 1000f;
        float fy = y / 1000f;

        // get the 'width' and 'height' attributes of the SVG document
        Dimension dim = painter.getImageSize();
        float imw = (float)dim.getWidth() / 1000f;
        float imh = (float)dim.getHeight() / 1000f;

        float sx = fwidth / imw;
        float sy = fheight / imh;

        Java2DRenderer renderer = (Java2DRenderer)context.getRenderer();
        Java2DGraphicsState state = renderer.state;

        //Create copy and paint on that
        Graphics2D g2d = (Graphics2D)state.getGraph().create();
        g2d.setColor(Color.black);
        g2d.setBackground(Color.black);

        //TODO Clip to the image area.

        // transform so that the coordinates (0,0) is from the top left
        // and positive is down and to the right. (0,0) is where the
        // viewBox puts it.
        g2d.translate(fx, fy);
        AffineTransform at = AffineTransform.getScaleInstance(sx, sy);
        if (!at.isIdentity()) {
            g2d.transform(at);
        }

        Rectangle2D area = new Rectangle2D.Double(0.0, 0.0, imw, imh);
        painter.paint(g2d, area);

        g2d.dispose();
    }

}
