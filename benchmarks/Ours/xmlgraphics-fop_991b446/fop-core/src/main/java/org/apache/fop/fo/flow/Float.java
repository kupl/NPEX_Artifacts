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

package org.apache.fop.fo.flow;

// XML
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_float">
 * <code>fo:float</code></a> object.
 */
public class Float extends FObj {
    // The value of properties relevant for fo:float (commented out for performance.
    private int foFloat;
    private int clear;
    // End of property values
    private boolean inWhiteSpace;
    private boolean disabled;

    /**
     * Base constructor
     *
     * @param parent    the parent {@link FONode}
     */
    public Float(FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    public void bind(PropertyList pList) throws FOPException {
        super.bind(pList);
        foFloat = pList.get(PR_FLOAT).getEnum();
        clear = pList.get(PR_CLEAR).getEnum();
    }

    /**
     * {@inheritDoc}
     * <br>XSL Content Model: (%block;)+
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName)
                throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (!isBlockItem(nsURI, localName)) {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /** {@inheritDoc} */
    public void endOfNode() throws FOPException {
        if (firstChild == null) {
            missingChildElementError("(%block;)+");
        }
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return "float";
    }

    /**
     * {@inheritDoc}
     * @return {@link org.apache.fop.fo.Constants#FO_FLOAT}
     */
    public int getNameId() {
        return FO_FLOAT;
    }

    public int getFloat() {
        return foFloat;
    }

    public void setInWhiteSpace(boolean iws) {
        inWhiteSpace = iws;
    }

    public boolean getInWhiteSpace() {
        return inWhiteSpace;
    }

    public void processNode(String elementName, Locator locator, Attributes attlist, PropertyList pList)
            throws FOPException {
        if (findAncestor(FO_TABLE) > 0) {
            disabled = true;
            getFOValidationEventProducer().unimplementedFeature(this, "fo:table", getName(), getLocator());
        } else {
            super.processNode(elementName, locator, attlist, pList);
        }
    }

    public boolean isDisabled() {
        return disabled;
    }
}
