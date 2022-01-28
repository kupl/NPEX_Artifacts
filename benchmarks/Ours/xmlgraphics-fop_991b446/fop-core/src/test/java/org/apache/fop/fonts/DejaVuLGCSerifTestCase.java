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

package org.apache.fop.fonts;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import org.apache.fop.apps.io.InternalResourceResolver;
import org.apache.fop.apps.io.ResourceResolverFactory;

/**
 *
 */
public class DejaVuLGCSerifTestCase {

    private InternalResourceResolver resolver =
            ResourceResolverFactory.createDefaultInternalResourceResolver(new File(".").toURI());
    private CustomFont font;

    /**
     * sets up the testcase by loading the DejaVu Font.
     *
     * @throws Exception
     *             if the test fails.
     */
    @Before
    public void setUp() throws Exception {
        File file = new File("test/resources/fonts/ttf/DejaVuLGCSerif.ttf");
        FontUris fontUris = new FontUris(file.toURI(), null);
        font = FontLoader.loadFont(fontUris, "", true, EmbeddingMode.AUTO, EncodingMode.AUTO,
                false, false, resolver, false, false);
    }

    /**
     * Simple test to see if font name was detected correctly.
     */
    @Test
    public void testFontName() {
        assertEquals("DejaVuLGCSerif", font.getFontName());
    }

    @Test
    public void testUnderline() {
        assertEquals(-840, font.getUnderlinePosition(10));
        assertEquals(430, font.getUnderlineThickness(10));
    }

    @Test
    public void testStrikeout() {
        assertEquals(2340, font.getStrikeoutPosition(10));
        assertEquals(490, font.getStrikeoutThickness(10));
    }

}
