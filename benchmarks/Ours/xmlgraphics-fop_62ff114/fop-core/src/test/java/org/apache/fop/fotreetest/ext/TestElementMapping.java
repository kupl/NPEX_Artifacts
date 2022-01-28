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

package org.apache.fop.fotreetest.ext;

import org.apache.fop.fo.ElementMapping;
import org.apache.fop.fo.FONode;

/**
 * This class provides the element mapping for FOP.
 */
public class TestElementMapping extends ElementMapping {

    /** MathML Namespace */
    public static final String NAMESPACE = "http://xmlgraphics.apache.org/fop/test";

    /**
     * Main constructor
     */
    public TestElementMapping() {
        this.namespaceURI = NAMESPACE;
    }

    /** @see org.apache.fop.fo.ElementMapping#initialize() */
    protected void initialize() {
        if (foObjs == null) {
            foObjs = new java.util.HashMap<String, Maker>();
            foObjs.put("assert", new AssertMaker());
        }
    }

    static class AssertMaker extends ElementMapping.Maker {
        public FONode make(FONode parent) {
            return new AssertElement(parent);
        }
    }

}
