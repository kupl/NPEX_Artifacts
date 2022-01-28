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

package org.apache.fop.render.ps;

import java.io.IOException;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.xmlgraphics.ps.PSGenerator;

import org.apache.fop.render.ps.extensions.PSPageTrailerCodeBefore;

public class PSRenderingUtilTestCase {

    private final String content = "<< /MyEntry 0 >> command";
    private final PSPageTrailerCodeBefore ptcb = new PSPageTrailerCodeBefore(content);
    private final PSGenerator gen = mock(PSGenerator.class);

    @Test
    public void testWriteEnclosedExtensionAttachment() throws IOException {
        PSRenderingUtil.writeEnclosedExtensionAttachment(gen, ptcb);
        verify(gen).writeln(content);
    }

}
