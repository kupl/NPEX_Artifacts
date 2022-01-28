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

package org.apache.fop.apps;

import static org.apache.fop.render.bitmap.TIFFRendererConfig.TIFFRendererOption.COMPRESSION;
import static org.apache.fop.render.bitmap.TIFFRendererConfig.TIFFRendererOption.ENDIANNESS;
import static org.apache.fop.render.bitmap.TIFFRendererConfig.TIFFRendererOption.SINGLE_STRIP;
public class TIFFRendererConfBuilder extends BitmapRendererConfBuilder {

    public TIFFRendererConfBuilder() {
        super(MimeConstants.MIME_TIFF);
    }

    public TIFFRendererConfBuilder setCompressionMode(String mode) {
        createTextElement(COMPRESSION, mode);
        return this;
    }

    public TIFFRendererConfBuilder setSingleStrip(boolean single) {
        createTextElement(SINGLE_STRIP, String.valueOf(single));
        return this;
    }

    public TIFFRendererConfBuilder setEndianness(String endianness) {
        createTextElement(ENDIANNESS, endianness);
        return this;
    }
}
