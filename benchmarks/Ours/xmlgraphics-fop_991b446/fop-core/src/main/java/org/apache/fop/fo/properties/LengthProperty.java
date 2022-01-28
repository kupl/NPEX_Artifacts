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

package org.apache.fop.fo.properties;

import org.apache.xmlgraphics.util.UnitConv;

import org.apache.fop.datatypes.Length;
import org.apache.fop.datatypes.Numeric;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;

/**
 * Superclass for properties wrapping a Length value.
 */
public abstract class LengthProperty extends Property
    implements Length, Numeric {

    /**
     * Inner class for making instances of LengthProperty
     */
    public static class Maker extends PropertyMaker {

        /**
         * Constructor
         *
         * @param propId the id of the property for which a Maker should be created
         */
        public Maker(int propId) {
            super(propId);
        }

        /** {@inheritDoc} */
        @Override
        public Property convertProperty(Property p,
                                        PropertyList propertyList,
                                        FObj fo) throws PropertyException {
            if (p instanceof EnumProperty) {
                return new EnumLength(p);
            }
            if (p instanceof LengthProperty) {
                return p;
            }
            if (p instanceof NumberProperty) {
                //Assume pixels (like in HTML) when there's no unit
                float resolution = propertyList.getFObj().getUserAgent().getSourceResolution();
                return FixedLength.getInstance(
                        p.getNumeric().getNumericValue(), "px",
                        UnitConv.IN2PT / resolution);
            }
            Length val = p.getLength();
            if (val != null) {
                return (Property) val;
            }
            /* always null ?? */
            return convertPropertyDatatype(p, propertyList, fo);
        }

    }

    /** @return the numeric dimension. Length always a dimension of 1 */
    public int getDimension() {
        return 1;
    }

    /** @return this.length cast as a Numeric */
    @Override
    public Numeric getNumeric() {
        return this;
    }

    /** @return this.length */
    @Override
    public Length getLength() {
        return this;
    }

    /** @return this.length cast as an Object */
    @Override
    public Object getObject() {
        return this;
    }

}

