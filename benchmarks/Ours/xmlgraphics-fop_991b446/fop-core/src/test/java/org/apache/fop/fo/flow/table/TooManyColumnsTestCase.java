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

package org.apache.fop.fo.flow.table;

import org.junit.Test;

public class TooManyColumnsTestCase extends ErrorCheckTest {

    @Test
    public void testBody1() throws Exception {
        launchTest("table/too-many-columns_body_1.fo");
    }

    @Test
    public void testBody2() throws Exception {
        launchTest("table/too-many-columns_body_2.fo");
    }

    @Test
    public void testBody3() throws Exception {
        launchTest("table/too-many-columns_body_3.fo");
    }

    @Test
    public void testBody4() throws Exception {
        launchTest("table/too-many-columns_body_4.fo");
    }

    @Test
    public void testHeader() throws Exception {
        launchTest("table/too-many-columns_header.fo");
    }

    @Test
    public void testFooter() throws Exception {
        launchTest("table/too-many-columns_footer.fo");
    }
}
