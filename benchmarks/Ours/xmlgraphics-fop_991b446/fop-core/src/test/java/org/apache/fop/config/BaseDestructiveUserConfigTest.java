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

package org.apache.fop.config;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.xml.sax.SAXException;

import org.apache.fop.apps.FOPException;

/**
 * Super class for several user configuration failure cases.
 */
public abstract class BaseDestructiveUserConfigTest extends BaseUserConfigTest {

    public BaseDestructiveUserConfigTest(InputStream confStream) throws SAXException, IOException {
        super(confStream);
    }

    /**
     * Test the user configuration failure.
     */
    @Test(expected = FOPException.class)
    public void testUserConfig() throws Exception {
        convertFO();
    }
}
