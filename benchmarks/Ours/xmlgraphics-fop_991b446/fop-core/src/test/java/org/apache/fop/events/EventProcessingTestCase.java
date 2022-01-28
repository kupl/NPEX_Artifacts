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

package org.apache.fop.events;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;

import org.apache.commons.io.output.NullOutputStream;

import org.apache.xmlgraphics.util.MimeConstants;

import org.apache.fop.ResourceEventProducer;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.area.AreaEventProducer;
import org.apache.fop.fo.FOValidationEventProducer;
import org.apache.fop.fo.flow.table.TableEventProducer;
import org.apache.fop.layoutmgr.BlockLevelEventProducer;
import org.apache.fop.layoutmgr.inline.InlineLevelEventProducer;

/**
 * Tests that the event notification system runs smoothly.
 */
public class EventProcessingTestCase {

    private final TransformerFactory tFactory = TransformerFactory.newInstance();
    private static final URI BASE_DIR;
    public static final URI CONFIG_BASE_DIR;
    static {
        URI base = (new File(".")).toURI();
        BASE_DIR = base.resolve("test/events/");

        /** The base directory of configuration files */
        CONFIG_BASE_DIR = base.resolve("test/config/");

    }

    public void doTest(InputStream inStream, URI fopConf, String expectedEventID, String mimeType,
            Map<String, Object> expectedParams) throws Exception {
        EventChecker eventChecker = new EventChecker(expectedEventID, expectedParams);
        FopFactory fopFactory;
        if (fopConf != null) {
            fopFactory = FopFactory.newInstance(new File(fopConf));
        } else {
            fopFactory = FopFactory.newInstance(BASE_DIR);
        }

        FOUserAgent userAgent = fopFactory.newFOUserAgent();

        userAgent.getEventBroadcaster().addEventListener(eventChecker);
        Fop fop = fopFactory.newFop(mimeType, userAgent, new NullOutputStream());
        Transformer transformer = tFactory.newTransformer();
        Source src = new StreamSource(inStream);
        Result res = new SAXResult(fop.getDefaultHandler());
        transformer.transform(src, res);
        eventChecker.end();

    }

    public void doTest(InputStream inStream, URI fopConf, String expectedEventID, String mimeType)
            throws Exception {
        Map<String, Object> noParams = Collections.emptyMap();
        doTest(inStream, fopConf, expectedEventID, mimeType, noParams);
    }

    public void doTest(String filename, String expectedEventID, Map<String, Object> expectedParams)
            throws Exception {
        doTest(BASE_DIR.resolve(filename).toURL().openStream(), null, expectedEventID,
                MimeConstants.MIME_PDF, expectedParams);
    }

    public void doTest(String filename, String expectedEventID) throws Exception {
        doTest(BASE_DIR.resolve(filename).toURL().openStream(), null, expectedEventID,
                MimeConstants.MIME_PDF);
    }

    @Test
    public void testArea() throws Exception {
        doTest("area.fo",
                AreaEventProducer.class.getName() + ".unresolvedIDReferenceOnPage");
    }

    @Test
    public void testResource() throws Exception {
        doTest("resource.fo",
                ResourceEventProducer.class.getName() + ".imageNotFound");
    }

    @Test
    public void testValidation() throws Exception {
        doTest("validation.fo",
                FOValidationEventProducer.class.getName() + ".invalidPropertyValue");
    }

    @Test
    public void testTable() throws Exception {
        doTest("table.fo",
                TableEventProducer.class.getName() + ".noTablePaddingWithCollapsingBorderModel");
    }

    @Test
    public void testBlockLevel() throws Exception {
        doTest("block-level.fo",
                BlockLevelEventProducer.class.getName() + ".overconstrainedAdjustEndIndent");
    }

    @Test
    public void testInlineLevel() throws Exception {
        doTest("inline-level.fo",
                InlineLevelEventProducer.class.getName() + ".lineOverflows");
    }

    @Test
    public void testViewportIPDOverflow() throws Exception {
        doTest("viewport-overflow.fo", BlockLevelEventProducer.class.getName() + ".viewportIPDOverflow");
    }

    @Test
    public void testViewportBPDOverflow() throws Exception {
        doTest("viewport-overflow.fo", BlockLevelEventProducer.class.getName() + ".viewportBPDOverflow");
    }

    @Test
    public void testPageOverflow() throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("page", "1");
        doTest("region-body_overflow.fo", BlockLevelEventProducer.class.getName() + ".regionOverflow",
                params);
    }

    @Test
    public void testHyphenationNotFound() throws Exception {
        Map<String, Object> noParams = Collections.emptyMap();
        doTest(BASE_DIR.resolve("hyphenation.fo").toURL().openStream(),
                new File("test/events/hyphenationfop.xconf").toURI(),
                ResourceEventProducer.class.getName() + ".hyphenationNotFound", MimeConstants.MIME_PDF, noParams);
    }
}
