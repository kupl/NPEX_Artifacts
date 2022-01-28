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

import org.apache.fop.fo.Constants;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;

/**
 * Special CorrespondingPropertyMaker that sets the conditionality subproperty
 * correctly for space-* properties.
 */
public class SpacePropertyMaker extends CorrespondingPropertyMaker {

    /**
     * @param baseMaker base property maker
     */
    public SpacePropertyMaker(PropertyMaker baseMaker) {
        super(baseMaker);
    }

    /**
     * {@inheritDoc}
     */
    public Property compute(PropertyList propertyList) throws PropertyException {
        Property prop = super.compute(propertyList);
        if (prop != null && prop instanceof SpaceProperty) {
            ((SpaceProperty)prop).setConditionality(
                    EnumProperty.getInstance(Constants.EN_RETAIN, "RETAIN"), false);
        }
        return prop;
    }
}
