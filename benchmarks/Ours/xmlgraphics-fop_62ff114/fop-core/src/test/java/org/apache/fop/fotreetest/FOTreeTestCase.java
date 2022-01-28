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

package org.apache.fop.fotreetest;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

import org.apache.fop.DebugHelper;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.apache.fop.apps.FopFactoryConfig;
import org.apache.fop.apps.MutableConfig;
import org.apache.fop.fotreetest.ext.TestElementMapping;
import org.apache.fop.layoutengine.LayoutEngineTestUtils;
import org.apache.fop.layoutengine.TestFilesConfiguration;
import org.apache.fop.util.ConsoleEventListenerForTests;

/**
 * Test driver class for FO tree tests.
 */
@RunWith(Parameterized.class)
public class FOTreeTestCase {

    private static final String BASE_DIR = "test/fotree/";
    private static final String TEST_CASES = "testcases";

    @BeforeClass
    public static void registerElementListObservers() {
        DebugHelper.registerStandardElementListObservers();
    }

    /**
     * Gets the parameters to run the FO tree test cases.
     * @return a collection of file arrays containing the test files
     */
    @Parameters
    public static Collection<File[]> getParameters() {
        TestFilesConfiguration.Builder builder = new TestFilesConfiguration.Builder();
        builder.testDir(BASE_DIR)
               .singleProperty("fop.fotree.single")
               .startsWithProperty("fop.fotree.starts-with")
               .suffix(".fo")
               .testSet("testcases")
               .disabledProperty("fop.layoutengine.disabled", "test/fotree/disabled-testcases.xml")
               .privateTestsProperty("fop.fotree.private");

        TestFilesConfiguration testConfig = builder.build();
        return LayoutEngineTestUtils.getTestFiles(testConfig);
    }



    private final File testFile;

    /**
     * Main constructor
     *
     * @param testFile the FO file to test
     */
    public FOTreeTestCase(File testFile) {
        this.testFile = testFile;
    }

    /**
     * Runs a test.
     * @throws Exception if a test or FOP itself fails
     */
    @Test
    public void runTest() throws Exception {
        try {
            ResultCollector collector = ResultCollector.getInstance();
            collector.reset();

            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setValidating(false);
            SAXParser parser = spf.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            FopFactoryBuilder builder = new FopFactoryBuilder(new File(".").toURI());
            // Resetting values modified by processing instructions
            builder.setBreakIndentInheritanceOnReferenceAreaBoundary(
                   FopFactoryConfig.DEFAULT_BREAK_INDENT_INHERITANCE);
            builder.setSourceResolution(FopFactoryConfig.DEFAULT_SOURCE_RESOLUTION);

            MutableConfig mutableConfig = new MutableConfig(builder);

            FopFactory fopFactory = FopFactory.newInstance(mutableConfig);
            fopFactory.addElementMapping(new TestElementMapping());
            FOUserAgent ua = fopFactory.newFOUserAgent();
            ua.setFOEventHandlerOverride(new DummyFOEventHandler(ua));
            ua.getEventBroadcaster().addEventListener(
                    new ConsoleEventListenerForTests(testFile.getName()));

            // Used to set values in the user agent through processing instructions
            reader = new PIListener(reader, mutableConfig);
            Fop fop = fopFactory.newFop(ua);

            reader.setContentHandler(fop.getDefaultHandler());
            reader.setDTDHandler(fop.getDefaultHandler());
            reader.setErrorHandler(fop.getDefaultHandler());
            reader.setEntityResolver(fop.getDefaultHandler());
            try {
                reader.parse(testFile.toURI().toURL().toExternalForm());
            } catch (Exception e) {
                collector.notifyError(e.getLocalizedMessage());
                throw e;
            }

            List<String> results = collector.getResults();
            if (results.size() > 0) {
                for (String result : results) {
                    System.out.println(result);
                }
                throw new IllegalStateException(results.get(0));
            }
        } catch (Exception e) {
            org.apache.commons.logging.LogFactory.getLog(this.getClass()).info(
                    "Error on " + testFile.getName());
            throw e;
        }
    }

    private static class PIListener extends XMLFilterImpl {

        private final MutableConfig fopConfig;

        public PIListener(XMLReader parent, MutableConfig fopConfig) {
            super(parent);
            this.fopConfig = fopConfig;
        }

        /** @see org.xml.sax.helpers.XMLFilterImpl */
        public void processingInstruction(String target, String data) throws SAXException {
            if ("fop-useragent-break-indent-inheritance".equals(target)) {
                fopConfig.setBreakIndentInheritanceOnReferenceAreaBoundary(
                        Boolean.valueOf(data));
            } else if ("fop-source-resolution".equals(target)) {
                fopConfig.setSourceResolution(Float.parseFloat(data));
            }
            super.processingInstruction(target, data);
        }
    }
}
