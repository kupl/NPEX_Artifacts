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

package org.apache.fop.util;

import java.util.Locale;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link LanguageTags}.
 */
public class LanguageTagsTestCase {

    @Test(expected = NullPointerException.class)
    public void toLanguageTagRejectsNull() {
        LanguageTags.toLanguageTag(null);
    }

    @Test
    public void testToLanguageTag() throws Exception {
        assertEquals("", LanguageTags.toLanguageTag(new Locale("")));
        assertEquals("en", LanguageTags.toLanguageTag(new Locale("en")));
        assertEquals("en-US", LanguageTags.toLanguageTag(new Locale("en", "US")));
        assertEquals("en-US", LanguageTags.toLanguageTag(new Locale("EN", "us")));
    }

    @Test(expected = NullPointerException.class)
    public void toLocaleRejectsNull() {
        LanguageTags.toLocale(null);
    }

    @Test
    public void testRFC3066ToLocale() throws Exception {
        assertEquals(new Locale(""), LanguageTags.toLocale(""));
        assertEquals(new Locale("en"), LanguageTags.toLocale("en"));
        assertEquals(new Locale("en", "US"), LanguageTags.toLocale("en-US"));
        assertEquals(new Locale("en", "US"), LanguageTags.toLocale("EN-us"));
    }
}
