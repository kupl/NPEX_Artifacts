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


import org.xml.sax.Attributes;
import org.xml.sax.Locator;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FOPropertyMapping;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.properties.KeepProperty;
import org.apache.fop.fo.properties.LengthPairProperty;
import org.apache.fop.fo.properties.LengthRangeProperty;
import org.apache.fop.fo.properties.PercentLength;
import org.apache.fop.fo.properties.Property;
import org.apache.fop.fo.properties.SpaceProperty;
import org.apache.fop.fotreetest.ResultCollector;

/**
 * Defines the assert element for the FOP Test extension.
 */
public class AssertElement extends TestObj {

    /**
     * Creates a new AssertElement instance that is a child
     * of the given {@link FONode}
     *
     * @param   parent  the parent {@link FONode}
     */
    public AssertElement(FONode parent) {
        super(parent);
    }

    /**
     * @see org.apache.fop.fo.FONode#processNode
     */
    public void processNode(String elementName,
                            Locator locator,
                            Attributes attlist,
                            PropertyList propertyList) throws FOPException {
        //super.processNode(elementName, locator, attlist, propertyList);

        ResultCollector collector = ResultCollector.getInstance();
        String propName = attlist.getValue("property");
        String expected = attlist.getValue("expected");
        String component = null;
        int dotIndex = propName.indexOf('.');
        if (dotIndex >= 0) {
            component = propName.substring(dotIndex + 1);
            propName = propName.substring(0, dotIndex);
        }
        int propID = FOPropertyMapping.getPropertyId(propName);
        if (propID < 0) {
            collector.notifyAssertionFailure("Property not found: " + propName);
        } else {
            Property prop;
            prop = propertyList.getParentPropertyList().get(propID);
            if (component != null) {
                //Access subcomponent
                Property mainProp = prop;
                prop = null;
                LengthPairProperty lpp = mainProp.getLengthPair();
                if (lpp != null) {
                    prop = lpp.getComponent(FOPropertyMapping.getSubPropertyId(component));
                }
                LengthRangeProperty lrp = mainProp.getLengthRange();
                if (lrp != null) {
                    prop = lrp.getComponent(FOPropertyMapping.getSubPropertyId(component));
                }
                KeepProperty kp = mainProp.getKeep();
                if (kp != null) {
                    prop = kp.getComponent(FOPropertyMapping.getSubPropertyId(component));
                }
                SpaceProperty sp = mainProp.getSpace();
                if (sp != null) {
                    prop = sp.getComponent(FOPropertyMapping.getSubPropertyId(component));
                }
            }
            String s;
            if (prop instanceof PercentLength) {
                s = prop.getString();
            } else {
                s = String.valueOf(prop);
            }
            if (!expected.equals(s)) {
                collector.notifyAssertionFailure(
                    locator.getSystemId()
                        + "\nProperty '" + propName
                        + "' expected to evaluate to '" + expected
                        + "' but got '" + s
                        + "'\n(test:assert in "
                        + propertyList.getParentFObj().getName()
                        + " at line #" + locator.getLineNumber()
                        + ", column #" + locator.getColumnNumber() + ")\n");
            }
        }

    }

    /** @see org.apache.fop.fo.FONode#getLocalName() */
    public String getLocalName() {
        return "assert";
    }

}
