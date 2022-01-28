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
import org.xml.sax.Locator;

import org.apache.fop.apps.FOPException;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.CommonAccessibility;
import org.apache.fop.fo.properties.CommonAccessibilityHolder;
import org.apache.fop.fo.properties.SpaceProperty;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_initial-property-set">
 * <code>fo:initial-property-set</code></a> object.
 */
public class InitialPropertySet extends FObj implements CommonAccessibilityHolder {

    private CommonAccessibility commonAccessibility;

    // private ToBeImplementedProperty letterSpacing;
    private SpaceProperty lineHeight;
    // private ToBeImplementedProperty textShadow;
    // Unused but valid items, commented out for performance:
    //     private CommonAural commonAural;
    //     private CommonBorderPaddingBackground commonBorderPaddingBackground;
    //     private CommonFont commonFont;
    //     private CommonRelativePosition commonRelativePosition;
    //     private Color color;
    //     private int scoreSpaces;
    //     private int textDecoration;
    //     private int textTransform;
    //     private SpaceProperty wordSpacing;
    // End of property values

    /**
     * Base constructor
     *
     * @param parent {@link FONode} that is the parent of this object
     */
    public InitialPropertySet(FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    public void bind(PropertyList pList) throws FOPException {
        super.bind(pList);
        commonAccessibility = CommonAccessibility.getInstance(pList);
        // letterSpacing = pList.get(PR_LETTER_SPACING);
        lineHeight = pList.get(PR_LINE_HEIGHT).getSpace();
        // textShadow = pList.get(PR_TEXT_SHADOW);
    }

    /**
     * {@inheritDoc}
     * <br>XSL Content Model: empty
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName)
                throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            invalidChildError(loc, nsURI, localName);
        }
    }

    /** @return the "line-height" property */
    public SpaceProperty getLineHeight() {
        return lineHeight;
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return "initial-property-set";
    }

    /**
     * {@inheritDoc}
     * @return {@link org.apache.fop.fo.Constants#FO_INITIAL_PROPERTY_SET}
     */
    public int getNameId() {
        return FO_INITIAL_PROPERTY_SET;
    }

    /** {@inheritDoc} */
    public CommonAccessibility getCommonAccessibility() {
        return commonAccessibility;
    }

}
