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

package org.apache.fop.render.pdf;

import org.apache.xmlgraphics.image.loader.Image;
import org.apache.xmlgraphics.image.loader.ImageFlavor;
import org.apache.xmlgraphics.image.loader.impl.ImageRendered;

import org.apache.fop.pdf.PDFImage;
import org.apache.fop.render.RenderingContext;

/**
 * Image handler implementation which handles RenderedImage instances for PDF output.
 */
public class PDFImageHandlerRenderedImage extends AbstractPDFImageHandler {

    private static final ImageFlavor[] FLAVORS = new ImageFlavor[] {
        ImageFlavor.BUFFERED_IMAGE,
        ImageFlavor.RENDERED_IMAGE
    };

    @Override
    PDFImage createPDFImage(Image image, String xobjectKey) {
        return new ImageRenderedAdapter((ImageRendered) image, xobjectKey);
    }

    /** {@inheritDoc} */
    public int getPriority() {
        return 300;
    }

    /** {@inheritDoc} */
    public Class getSupportedImageClass() {
        return ImageRendered.class;
    }

    /** {@inheritDoc} */
    public ImageFlavor[] getSupportedImageFlavors() {
        return FLAVORS;
    }

    /** {@inheritDoc} */
    public boolean isCompatible(RenderingContext targetContext, Image image) {
        return (image == null || image instanceof ImageRendered)
                && targetContext instanceof PDFRenderingContext;
    }

}
