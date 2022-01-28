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

import java.io.IOException;

import org.junit.Test;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.fop.config.BaseConstructiveUserConfigTest;
import org.apache.fop.render.RendererConfig.RendererConfigParser;
import org.apache.fop.render.pdf.PDFRendererConfig;

public class FopFactoryTestCase extends BaseConstructiveUserConfigTest {

    public FopFactoryTestCase() throws SAXException, IOException {
        super(new FopConfBuilder().setStrictValidation(true)
                .startRendererConfig(PDFRendererConfBuilder.class)
                .startFontsConfig()
                    .startFont(null, "test/resources/fonts/ttf/glb12.ttf.xml")
                        .addTriplet("Gladiator", "normal", "normal")
                    .endFont()
                .endFontConfig()
            .endRendererConfig().build());
    }

    @Test
    @Override
    public void testUserConfig() throws Exception {
        RendererConfigParser mock = mock(RendererConfigParser.class);
        when(mock.getMimeType()).thenReturn(MimeConstants.MIME_PDF);
        try {
            convertFO();
            PDFRendererConfig config = (PDFRendererConfig) fopFactory.getRendererConfig(null, null,
                    mock);
            convertFO();
            assertEquals(config, fopFactory.getRendererConfig(null, null, mock));
        } catch (Exception e) {
            // this should *not* happen!
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
