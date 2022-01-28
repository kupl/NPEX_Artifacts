/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.layout;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.layout.GelfLayout.CompressionType;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.junit.ThreadContextRule;
import org.apache.logging.log4j.test.appender.EncodingListAppender;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.Assert.assertEquals;

public class GelfLayoutTest {
    
    static ConfigurationFactory configFactory = new BasicConfigurationFactory();
    
    private static final String HOSTNAME = "TheHost";
    private static final String KEY1 = "Key1";
    private static final String KEY2 = "Key2";
    private static final String LINE1 = "empty mdc";
    private static final String LINE2 = "filled mdc";
    private static final String LINE3 = "error message";
    private static final String MDCKEY1 = "MdcKey1";
    private static final String MDCKEY2 = "MdcKey2";
    private static final String MDCVALUE1 = "MdcValue1";
    private static final String MDCVALUE2 = "MdcValue2";
    private static final String VALUE1 = "Value1";
    private static final String VALUE2 = "Value2";

    @Rule
    public final ThreadContextRule threadContextRule = new ThreadContextRule(); 

    @AfterClass
    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(configFactory);
    }

    @BeforeClass
    public static void setupClass() {
        ConfigurationFactory.setConfigurationFactory(configFactory);
        final LoggerContext ctx = LoggerContext.getContext();
        ctx.reconfigure();
    }

    LoggerContext ctx = LoggerContext.getContext();

    Logger root = ctx.getRootLogger();

    private void testCompressedLayout(final CompressionType compressionType, final boolean includeStacktrace) throws IOException {
        for (final Appender appender : root.getAppenders().values()) {
            root.removeAppender(appender);
        }
        // set up appenders
        final GelfLayout layout = GelfLayout.createLayout(HOSTNAME, new KeyValuePair[] {
                new KeyValuePair(KEY1, VALUE1),
                new KeyValuePair(KEY2, VALUE2), }, compressionType, 1024, includeStacktrace);
        final ListAppender eventAppender = new ListAppender("Events", null, null, true, false);
        final ListAppender rawAppender = new ListAppender("Raw", null, layout, true, true);
        final ListAppender formattedAppender = new ListAppender("Formatted", null, layout, true, false);
        final EncodingListAppender encodedAppender = new EncodingListAppender("Encoded", null, layout, false, true);
        eventAppender.start();
        rawAppender.start();
        formattedAppender.start();
        encodedAppender.start();

        // set appenders on root and set level to debug
        root.addAppender(eventAppender);
        root.addAppender(rawAppender);
        root.addAppender(formattedAppender);
        root.addAppender(encodedAppender);
        root.setLevel(Level.DEBUG);

        root.debug(LINE1);

        ThreadContext.put(MDCKEY1, MDCVALUE1);
        ThreadContext.put(MDCKEY2, MDCVALUE2);

        root.info(LINE2);

        final Exception exception = new RuntimeException("some error");
        root.error(LINE3, exception);

        formattedAppender.stop();

        final List<LogEvent> events = eventAppender.getEvents();
        final List<byte[]> raw = rawAppender.getData();
        final List<String> messages = formattedAppender.getMessages();
        final List<byte[]> raw2 = encodedAppender.getData();
        final String threadName = Thread.currentThread().getName();

        //@formatter:off
        assertJsonEquals("{" +
                        "\"version\": \"1.1\"," +
                        "\"host\": \"" + HOSTNAME + "\"," +
                        "\"timestamp\": " + GelfLayout.formatTimestamp(events.get(0).getTimeMillis()) + "," +
                        "\"level\": 7," +
                        "\"_thread\": \"" + threadName + "\"," +
                        "\"_logger\": \"\"," +
                        "\"short_message\": \"" + LINE1 + "\"," +
                        "\"_" + KEY1 + "\": \"" + VALUE1 + "\"," +
                        "\"_" + KEY2 + "\": \"" + VALUE2 + "\"" +
                        "}",
                messages.get(0));

        assertJsonEquals("{" +
                        "\"version\": \"1.1\"," +
                        "\"host\": \"" + HOSTNAME + "\"," +
                        "\"timestamp\": " + GelfLayout.formatTimestamp(events.get(1).getTimeMillis()) + "," +
                        "\"level\": 6," +
                        "\"_thread\": \"" + threadName + "\"," +
                        "\"_logger\": \"\"," +
                        "\"short_message\": \"" + LINE2 + "\"," +
                        "\"_" + KEY1 + "\": \"" + VALUE1 + "\"," +
                        "\"_" + KEY2 + "\": \"" + VALUE2 + "\"," +
                        "\"_" + MDCKEY1 + "\": \"" + MDCVALUE1 + "\"," +
                        "\"_" + MDCKEY2 + "\": \"" + MDCVALUE2 + "\"" +
                        "}",
                messages.get(1));
        //@formatter:on
        final byte[] compressed = raw.get(2);
        final byte[] compressed2 = raw2.get(2);
        final ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
        final ByteArrayInputStream bais2 = new ByteArrayInputStream(compressed2);
        InputStream inflaterStream;
        InputStream inflaterStream2;
        switch (compressionType) {
        case GZIP:
            inflaterStream = new GZIPInputStream(bais);
            inflaterStream2 = new GZIPInputStream(bais2);
            break;
        case ZLIB:
            inflaterStream = new InflaterInputStream(bais);
            inflaterStream2 = new InflaterInputStream(bais2);
            break;
        case OFF:
            inflaterStream = bais;
            inflaterStream2 = bais2;
            break;
        default:
            throw new IllegalStateException("Missing test case clause");
        }
        final byte[] uncompressed = IOUtils.toByteArray(inflaterStream);
        final byte[] uncompressed2 = IOUtils.toByteArray(inflaterStream2);
        inflaterStream.close();
        inflaterStream2.close();
        final String uncompressedString = new String(uncompressed, layout.getCharset());
        final String uncompressedString2 = new String(uncompressed2, layout.getCharset());
        //@formatter:off
        final String expected = "{" +
                "\"version\": \"1.1\"," +
                "\"host\": \"" + HOSTNAME + "\"," +
                "\"timestamp\": " + GelfLayout.formatTimestamp(events.get(2).getTimeMillis()) + "," +
                "\"level\": 3," +
                "\"_thread\": \"" + threadName + "\"," +
                "\"_logger\": \"\"," +
                "\"short_message\": \"" + LINE3 + "\"," +
                "\"full_message\": \"" + String.valueOf(JsonStringEncoder.getInstance().quoteAsString(
                includeStacktrace ? GelfLayout.formatThrowable(exception).toString() : exception.toString())) + "\"," +
                "\"_" + KEY1 + "\": \"" + VALUE1 + "\"," +
                "\"_" + KEY2 + "\": \"" + VALUE2 + "\"," +
                "\"_" + MDCKEY1 + "\": \"" + MDCVALUE1 + "\"," +
                "\"_" + MDCKEY2 + "\": \"" + MDCVALUE2 + "\"" +
                "}";
        //@formatter:on
        assertJsonEquals(expected, uncompressedString);
        assertJsonEquals(expected, uncompressedString2);
    }

    @Test
    public void testLayoutGzipCompression() throws Exception {
        testCompressedLayout(CompressionType.GZIP, true);
    }

    @Test
    public void testLayoutNoCompression() throws Exception {
        testCompressedLayout(CompressionType.OFF, true);
    }

    @Test
    public void testLayoutZlibCompression() throws Exception {
        testCompressedLayout(CompressionType.ZLIB, true);
    }

    @Test
    public void testLayoutNoStacktrace() throws Exception {
        testCompressedLayout(CompressionType.OFF, false);
    }

    @Test
    public void testFormatTimestamp() {
        assertEquals("0", GelfLayout.formatTimestamp(0L).toString());
        assertEquals("1.000", GelfLayout.formatTimestamp(1000L).toString());
        assertEquals("1.001", GelfLayout.formatTimestamp(1001L).toString());
        assertEquals("1.010", GelfLayout.formatTimestamp(1010L).toString());
        assertEquals("1.100", GelfLayout.formatTimestamp(1100L).toString());
        assertEquals("1458741206.653", GelfLayout.formatTimestamp(1458741206653L).toString());
        assertEquals("9223372036854775.807", GelfLayout.formatTimestamp(Long.MAX_VALUE).toString());
    }
}
