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

import org.apache.fop.fonts.CustomFont;
import org.apache.fop.fonts.EmbeddingMode;
import org.apache.fop.fonts.FontType;

/**
 * PDFStream for embeddable OpenType CFF fonts.
 */
public class PDFCFFStreamType0C extends AbstractPDFFontStream {

    private byte[] cffData;
    private String type;

    /**
     * Main constructor
     */
    public PDFCFFStreamType0C(CustomFont font) {
        super();
        if (font.getEmbeddingMode() == EmbeddingMode.FULL) {
            type = "OpenType";
        } else if (font.getFontType() == FontType.TYPE0) {
            type = "CIDFontType0C";
        } else {
            type = font.getFontType().getName();
        }
    }

    protected int getSizeHint() throws IOException {
        if (this.cffData != null) {
            return cffData.length;
        } else {
            return 0; //no hint available
        }
    }

    /** {@inheritDoc} */
    protected void outputRawStreamData(OutputStream out) throws IOException {
        out.write(this.cffData);
    }

    /** {@inheritDoc} */
    protected void populateStreamDict(Object lengthEntry) {
        put("Subtype", new PDFName(type));
        super.populateStreamDict(lengthEntry);
    }

    /**
     * Sets the CFF font data.
     * @param data the font payload
     * @param size size of the payload
     * @throws IOException in case of an I/O problem
     */
    public void setData(byte[] data, int size) throws IOException {
        this.cffData = new byte[size];
        System.arraycopy(data, 0, this.cffData, 0, size);
    }

}

