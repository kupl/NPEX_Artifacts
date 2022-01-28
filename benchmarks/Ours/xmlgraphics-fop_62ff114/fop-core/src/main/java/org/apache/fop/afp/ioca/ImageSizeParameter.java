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

package org.apache.fop.afp.ioca;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.fop.afp.modca.AbstractAFPObject;
import org.apache.fop.afp.util.BinaryUtils;

/**
 * Describes the measurement characteristics of the image when it is created.
 */
public class ImageSizeParameter extends AbstractAFPObject {

    private int hSize;
    private int vSize;
    private int hRes;
    private int vRes;

    /**
     * Constructor for a ImageSizeParameter for the specified
     * resolution, hsize and vsize.
     *
     * @param hsize The horizontal size of the image.
     * @param vsize The vertical size of the image.
     * @param hresol The horizontal resolution of the image.
     * @param vresol The vertical resolution of the image.
     */
    public ImageSizeParameter(int hsize, int vsize, int hresol, int vresol) {
        this.hSize = hsize;
        this.vSize = vsize;
        this.hRes = hresol;
        this.vRes = vresol;
    }

    /** {@inheritDoc} */
    public void writeToStream(OutputStream os) throws IOException {
        byte[] data = new byte[] {
            (byte)0x94, // ID = Image Size Parameter
            0x09, // Length
            0x00, // Unit base - 10 Inches
            0x00, // HRESOL
            0x00, //
            0x00, // VRESOL
            0x00, //
            0x00, // HSIZE
            0x00, //
            0x00, // VSIZE
            0x00, //
        };

        byte[] x = BinaryUtils.convert(hRes, 2);
        data[3] = x[0];
        data[4] = x[1];

        byte[] y = BinaryUtils.convert(vRes, 2);
        data[5] = y[0];
        data[6] = y[1];

        byte[] w = BinaryUtils.convert(hSize, 2);
        data[7] = w[0];
        data[8] = w[1];

        byte[] h = BinaryUtils.convert(vSize, 2);
        data[9] = h[0];
        data[10] = h[1];

        os.write(data);
    }
}
