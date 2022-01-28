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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.fop.apps.AbstractRendererConfigParserTester;
import org.apache.fop.apps.Java2DRendererConfBuilder;
import org.apache.fop.render.java2d.Java2DRendererConfig.Java2DRendererConfigParser;

public class Java2DRendererConfigParserTestcase
        extends AbstractRendererConfigParserTester<Java2DRendererConfBuilder, Java2DRendererConfig> {

    public Java2DRendererConfigParserTestcase() {
        super(new Java2DRendererConfigParser("Java2D"), Java2DRendererConfBuilder.class);
    }

    @Test
    public void testPageBackgroundTransparency() throws Exception {
        parseConfig(createRenderer().setPageBackgroundTransparency(true));
        assertTrue(conf.isPageBackgroundTransparent());

        parseConfig(createRenderer().setPageBackgroundTransparency(false));
        assertFalse(conf.isPageBackgroundTransparent());
    }

    @Test
    public void testNullPageBackgroundTransparency() throws Exception {
        parseConfig(createRenderer());
        assertNull(conf.isPageBackgroundTransparent());
    }
}
