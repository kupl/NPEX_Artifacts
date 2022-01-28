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

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.ComponentColorModel;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.xmlgraphics.image.loader.ImageSize;
import org.apache.xmlgraphics.image.loader.impl.ImageRawPNG;
import org.apache.xmlgraphics.java2d.color.profile.ColorProfileUtil;

import org.apache.fop.pdf.FlateFilter;
import org.apache.fop.pdf.PDFAMode;
import org.apache.fop.pdf.PDFDictionary;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFICCBasedColorSpace;
import org.apache.fop.pdf.PDFICCStream;
import org.apache.fop.pdf.PDFName;
import org.apache.fop.pdf.PDFProfile;
import org.apache.fop.pdf.PDFResources;
import org.apache.fop.render.RawPNGTestUtil;

public class ImageRawPNGAdapterTestCase {

    @Test
    public void testSetupWithIndexColorModel() {
        IndexColorModel cm = mock(IndexColorModel.class);
        ImageRawPNG irpng = mock(ImageRawPNG.class);
        PDFDocument doc = mock(PDFDocument.class);
        PDFProfile profile = mock(PDFProfile.class);
        ImageRawPNGAdapter irpnga = new ImageRawPNGAdapter(irpng, "mock");
        ImageSize is = RawPNGTestUtil.getImageSize();

        when(irpng.getColorModel()).thenReturn(cm);
        when(irpng.getRenderingIntent()).thenReturn(-1);
        // when(cm.hasAlpha()).thenReturn(false);
        when(doc.getProfile()).thenReturn(profile);
        when(profile.getPDFAMode()).thenReturn(PDFAMode.PDFA_1A);
        when(irpng.getSize()).thenReturn(is);
        irpnga.setup(doc);
        FlateFilter filter = (FlateFilter) irpnga.getPDFFilter();
        assertEquals(1, filter.getColors());
    }

    @Test
    public void testSetupWithComponentColorModel() throws IOException {
        ComponentColorModel cm = mock(ComponentColorModel.class);
        ImageRawPNG irpng = mock(ImageRawPNG.class);
        PDFDocument doc = mock(PDFDocument.class);
        PDFProfile profile = mock(PDFProfile.class);
        ImageRawPNGAdapter irpnga = new ImageRawPNGAdapter(irpng, "mock");
        ImageSize is = RawPNGTestUtil.getImageSize();

        when(irpng.getColorModel()).thenReturn(cm);
        when(irpng.getRenderingIntent()).thenReturn(-1);
        when(cm.getNumComponents()).thenReturn(3);
        // when(cm.hasAlpha()).thenReturn(false);
        when(doc.getProfile()).thenReturn(profile);
        when(profile.getPDFAMode()).thenReturn(PDFAMode.PDFA_1A);
        when(irpng.getSize()).thenReturn(is);
        irpnga.setup(doc);
        FlateFilter filter = (FlateFilter) irpnga.getPDFFilter();
        assertEquals(3, filter.getColors());
    }

    @Test
    public void testOutputContentsWithRGBPNG() throws IOException {
        testOutputContentsWithGRGBAPNG(-1, 128, 128, 128, -1);
    }

    @Test
    public void testOutputContentsWithRGBAPNG() throws IOException {
        testOutputContentsWithGRGBAPNG(-1, 128, 128, 128, 128);
    }

    @Test
    public void testOutputContentsWithGPNG() throws IOException {
        testOutputContentsWithGRGBAPNG(128, -1, -1, -1, -1);
    }

    @Test
    public void testOutputContentsWithGAPNG() throws IOException {
        testOutputContentsWithGRGBAPNG(128, -1, -1, -1, 128);
    }

    private void testOutputContentsWithGRGBAPNG(int gray, int red, int green, int blue, int alpha)
            throws IOException {
        int numColorComponents = gray > -1 ? 1 : 3;
        int numComponents = numColorComponents + (alpha > -1 ? 1 : 0);
        ComponentColorModel cm = mock(ComponentColorModel.class);
        ImageRawPNG irpng = mock(ImageRawPNG.class);
        PDFDocument doc = mock(PDFDocument.class);
        PDFProfile profile = mock(PDFProfile.class);
        ImageRawPNGAdapter irpnga = new ImageRawPNGAdapter(irpng, "mock");
        ImageSize is = RawPNGTestUtil.getImageSize();

        when(irpng.getColorModel()).thenReturn(cm);
        when(irpng.getRenderingIntent()).thenReturn(-1);
        when(cm.getNumComponents()).thenReturn(numComponents);
        // when(cm.hasAlpha()).thenReturn(false);
        when(doc.getProfile()).thenReturn(profile);
        when(profile.getPDFAMode()).thenReturn(PDFAMode.PDFA_1A);
        when(irpng.getSize()).thenReturn(is);
        irpnga.setup(doc);
        FlateFilter filter = (FlateFilter) irpnga.getPDFFilter();
        assertEquals(numColorComponents, filter.getColors());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] data = RawPNGTestUtil.buildGRGBAData(gray, red, green, blue, alpha);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        when(irpng.createInputStream()).thenReturn(bais);
        irpnga.outputContents(baos);
        if (alpha > -1) {
            byte[] expected = RawPNGTestUtil.buildGRGBAData(gray, red, green, blue, -1);
            assertArrayEquals(expected, baos.toByteArray());
        } else {
            assertArrayEquals(data, baos.toByteArray());
        }
    }

    @Test
    public void testPopulateXObjectDictionaryWithComponentColorModelAndsRGB() {
        ComponentColorModel cm = mock(ComponentColorModel.class);
        ImageRawPNG irpng = mock(ImageRawPNG.class);
        PDFDictionary pdfDic = mock(PDFDictionary.class);
        ImageRawPNGAdapter irpnga = new ImageRawPNGAdapter(irpng, "mock");

        when(irpng.getColorModel()).thenReturn(cm);
        when(irpng.getRenderingIntent()).thenReturn(0);
        irpnga.populateXObjectDictionary(pdfDic);
        verify(pdfDic).put("Intent", new PDFName("Perceptual"));
        when(irpng.getRenderingIntent()).thenReturn(1);
        irpnga.populateXObjectDictionary(pdfDic);
        verify(pdfDic).put("Intent", new PDFName("RelativeColorimetric"));
        when(irpng.getRenderingIntent()).thenReturn(2);
        irpnga.populateXObjectDictionary(pdfDic);
        verify(pdfDic).put("Intent", new PDFName("Saturation"));
        when(irpng.getRenderingIntent()).thenReturn(3);
        irpnga.populateXObjectDictionary(pdfDic);
        verify(pdfDic).put("Intent", new PDFName("AbsoluteColorimetric"));
    }

    @Test
    public void testRenderingIntentImpliessRGBColorProfile() {
        ComponentColorModel cm = mock(ComponentColorModel.class);
        ImageRawPNG irpng = mock(ImageRawPNG.class);
        PDFDocument doc = mock(PDFDocument.class);
        PDFProfile profile = mock(PDFProfile.class);
        PDFResources resources = mock(PDFResources.class);
        PDFICCBasedColorSpace cs = mock(PDFICCBasedColorSpace.class);
        PDFICCStream stream = mock(PDFICCStream.class);
        ICC_Profile iccprof = ICC_Profile.getInstance(ColorSpace.CS_sRGB);
        ImageRawPNGAdapter irpnga = new ImageRawPNGAdapter(irpng, "mock");
        ImageSize is = RawPNGTestUtil.getImageSize();

        when(irpng.getColorModel()).thenReturn(cm);
        when(irpng.getRenderingIntent()).thenReturn(0);
        when(cm.getNumComponents()).thenReturn(3);
        // when(cm.hasAlpha()).thenReturn(false);
        when(doc.getProfile()).thenReturn(profile);
        when(doc.getResources()).thenReturn(resources);
        when(resources.getICCColorSpaceByProfileName("sRGB")).thenReturn(cs);
        when(profile.getPDFAMode()).thenReturn(PDFAMode.PDFA_1A);
        when(irpng.getSize()).thenReturn(is);
        when(cs.getICCStream()).thenReturn(stream);
        when(stream.getICCProfile()).thenReturn(iccprof);

        irpnga.setup(doc);
        PDFICCStream iccStream = irpnga.getICCStream();
        assertTrue(ColorProfileUtil.isDefaultsRGB(iccStream.getICCProfile()));
    }
}
