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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link EncodingMode}.
 */
public class EncodingModeTestCase {

    @Test
    public void testGetName() {
        assertEquals("auto", EncodingMode.AUTO.getName());
        assertEquals("single-byte", EncodingMode.SINGLE_BYTE.getName());
        assertEquals("cid", EncodingMode.CID.getName());
    }

    @Test
    public void testGetValue() {
        assertEquals(EncodingMode.AUTO, EncodingMode.getValue("auto"));
        assertEquals(EncodingMode.SINGLE_BYTE, EncodingMode.getValue("single-byte"));
        assertEquals(EncodingMode.CID, EncodingMode.getValue("cid"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getValueMustCheckForIllegalArguments() {
        EncodingMode.getValue("fail");
    }
}
