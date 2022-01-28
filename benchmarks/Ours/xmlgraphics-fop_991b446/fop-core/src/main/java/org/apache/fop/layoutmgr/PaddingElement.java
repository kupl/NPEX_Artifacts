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

package org.apache.fop.layoutmgr;

import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.fo.properties.CondLengthProperty;
import org.apache.fop.traits.MinOptMax;

/**
 * This represents an unresolved padding element.
 */
public class PaddingElement extends BorderOrPaddingElement {

    /**
     * Main constructor
     * @param position the Position instance needed by the addAreas stage of the LMs.
     * @param side the side to which this space element applies.
     * @param condLength the length-conditional property for a border or padding specification
     * @param isFirst true if this is a padding- or border-before of the first area generated.
     * @param isLast true if this is a padding- or border-after of the last area generated.
     * @param context the property evaluation context
     */
    public PaddingElement(Position position, CondLengthProperty condLength,
            RelSide side,
            boolean isFirst, boolean isLast, PercentBaseContext context) {
        super(position, condLength, side, isFirst, isLast, context);
    }

    /** {@inheritDoc} */
    public void notifyLayoutManager(MinOptMax effectiveLength) {
        LayoutManager lm = getOriginatingLayoutManager();
        if (lm instanceof ConditionalElementListener) {
            ((ConditionalElementListener)lm).notifyPadding(
                    getSide(), effectiveLength);
        } else {
            log.warn("Cannot notify LM. It does not implement ConditionalElementListene: "
                    + lm.getClass().getName());
        }
    }

    /** {@inheritDoc} */
    public String toString() {
        StringBuffer sb = new StringBuffer("Padding[");
        sb.append(super.toString());
        sb.append("]");
        return sb.toString();
    }

}
