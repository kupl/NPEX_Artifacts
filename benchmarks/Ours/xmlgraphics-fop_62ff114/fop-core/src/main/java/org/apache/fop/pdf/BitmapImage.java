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

package org.apache.fop.pdf;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Bitmap image.
 * This is used to create a bitmap image that will be inserted
 * into pdf.
 */
public class BitmapImage implements PDFImage {
    private int height;
    private int width;
    private int bitsPerComponent;
    private PDFDeviceColorSpace colorSpace;
    private byte[] bitmaps;
    private PDFReference maskRef;
    private PDFColor transparent;
    private String key;
    private PDFDocument pdfDoc;
    private PDFFilter pdfFilter;
    private boolean multipleFiltersAllowed = true;

    /**
     * Create a bitmap image.
     * Creates a new bitmap image with the given data.
     *
     * @param k the key to be used to lookup the image
     * @param width the width of the image
     * @param height the height of the image
     * @param data the bitmap data
     * @param mask the transparency mask reference if any
     */
    public BitmapImage(String k, int width, int height, byte[] data,
                  PDFReference mask) {
        this.key = k;
        this.height = height;
        this.width = width;
        this.bitsPerComponent = 8;
        this.colorSpace = new PDFDeviceColorSpace(PDFDeviceColorSpace.DEVICE_RGB);
        this.bitmaps = data;
        if (mask != null) {
            maskRef = mask;
        }
    }

    /**
     * Setup this image with the pdf document.
     *
     * @param doc the pdf document this will be inserted into
     */
    public void setup(PDFDocument doc) {
        this.pdfDoc = doc;
    }

    /**
     * Get the key for this image.
     * This key is used by the pdf document so that it will only
     * insert an image once. All other references to the same image
     * will use the same XObject reference.
     *
     * @return the unique key to identify this image
     */
    public String getKey() {
        return key;
    }

    /**
     * Get the width of this image.
     *
     * @return the width of the image
     */
    public int getWidth() {
        return width;
    }

    /**
     * Get the height of this image.
     *
     * @return the height of the image
     */
    public int getHeight() {
        return height;
    }

    /**
     * Set the color space for this image.
     *
     * @param cs the pdf color space
     */
    public void setColorSpace(PDFDeviceColorSpace cs) {
        colorSpace = cs;
    }

    /**
     * Get the color space for the image data.
     * Possible options are: DeviceGray, DeviceRGB, or DeviceCMYK
     *
     * @return the pdf doclor space
     */
    public PDFDeviceColorSpace getColorSpace() {
        return colorSpace;
    }

    /** {@inheritDoc} */
    public int getBitsPerComponent() {
        return bitsPerComponent;
    }

    /**
     * Set the transparent color for this iamge.
     *
     * @param t the transparent color
     */
    public void setTransparent(PDFColor t) {
        transparent = t;
    }

    /**
     * Check if this image has a transparent color.
     *
     * @return true if it has a transparent color
     */
    public boolean isTransparent() {
        return transparent != null;
    }

    /**
     * Get the transparent color for this image.
     *
     * @return the transparent color if any
     */
    public PDFColor getTransparentColor() {
        return transparent;
    }

    /**
     * Get the bitmap mask reference for this image.
     * Current not supported.
     *
     * @return the bitmap mask reference
     */
    public String getMask() {
        return null;
    }

    /** {@inheritDoc} */
    public PDFReference getSoftMaskReference() {
        return maskRef;
    }

    /** {@inheritDoc} */
    public boolean isInverted() {
        return false;
    }

    /** {@inheritDoc} */
    public void outputContents(OutputStream out) throws IOException {
        out.write(bitmaps);
    }

    /** {@inheritDoc} */
    public void populateXObjectDictionary(PDFDictionary dict) {
        //nop
    }

    /**
     * Get the ICC stream.
     * @return always returns null since this has no icc color space
     */
    public PDFICCStream getICCStream() {
        return null;
    }

    /**
     * Check if this is a postscript image.
     * @return always returns false
     */
    public boolean isPS() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String getFilterHint() {
        return PDFFilterList.IMAGE_FILTER;
    }

    /**
     * {@inheritDoc}
     */
    public PDFFilter getPDFFilter() {
        return pdfFilter;
    }

    public void setPDFFilter(PDFFilter pdfFilter) {
        this.pdfFilter = pdfFilter;
    }

    /** {@inheritDoc} */
    public boolean multipleFiltersAllowed() {
        return multipleFiltersAllowed;
    }

    /**
     * Disallows multiple filters.
     */
    public void disallowMultipleFilters() {
        multipleFiltersAllowed = false;
    }

}
