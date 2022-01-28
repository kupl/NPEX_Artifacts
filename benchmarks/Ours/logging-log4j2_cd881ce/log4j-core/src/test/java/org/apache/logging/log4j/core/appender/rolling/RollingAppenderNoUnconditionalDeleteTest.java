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
package org.apache.logging.log4j.core.appender.rolling;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

/**
 *
 */
@RunWith(Parameterized.class)
public class RollingAppenderNoUnconditionalDeleteTest {

    private final File directory;
    private Logger logger;

    @Parameterized.Parameters(name = "{0} \u2192 {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { //
                // @formatter:off
                {"log4j-rolling-with-custom-delete-unconditional1.xml", "target/rolling-unconditional-delete1/test"}, //
                {"log4j-rolling-with-custom-delete-unconditional2.xml", "target/rolling-unconditional-delete2/test"}, //
                {"log4j-rolling-with-custom-delete-unconditional3.xml", "target/rolling-unconditional-delete3/test"}, //
                // @formatter:on
        });
    }

    @Rule
    public LoggerContextRule init;

    public RollingAppenderNoUnconditionalDeleteTest(final String configFile, final String dir) {
        this.directory = new File(dir);
        this.init = new LoggerContextRule(configFile);
    }

    @Before
    public void setUp() throws Exception {
        this.logger = this.init.getLogger();
    }

    @After
    public void tearDown() throws Exception {
        deleteDir();
    }

    @Test
    public void testAppender() throws Exception {
        final int LINECOUNT = 18; // config has max="100"
        for (int i = 0; i < LINECOUNT; ++i) {
            // 30 chars per message: each message triggers a rollover
            logger.debug("This is a test message number " + i); // 30 chars:
        }
        Thread.sleep(100); // Allow time for rollover to complete

        assertTrue("Dir " + directory + " should exist", directory.exists());
        assertTrue("Dir " + directory + " should contain files", directory.listFiles().length > 0);

        int total = 0;
        for (File file : directory.listFiles()) {
            List<String> lines = Files.readAllLines(file.toPath(), Charset.defaultCharset());
            total += lines.size();
        }
        assertEquals("rolled over lines", LINECOUNT - 1, total);
    }

    private void deleteDir() {
        if (directory.exists()) {
            final File[] files = directory.listFiles();
            for (final File file : files) {
                file.delete();
            }
            directory.delete();
        }
    }
}
