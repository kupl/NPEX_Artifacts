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

package org.apache.fop.render.afp;

import java.io.ObjectStreamException;

/** Enumeration of the AFP shading modes. */
public enum AFPShadingMode {
    /** the color mode (the default) */
    COLOR("COLOR"),
    /** the dithered mode */
    DITHERED("DITHERED");

    private String name;

    /**
     * Constructor to add a new named item.
     * @param name Name of the item.
     */
    private AFPShadingMode(String name) {
        this.name = name;
    }

    /** @return the name of the enumeration */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the enumeration/singleton object based on its name.
     * @param name the name of the enumeration value
     * @return the enumeration object
     */
    public static AFPShadingMode getValueOf(String name) {
        if (name == null || "".equals(name) || COLOR.getName().equalsIgnoreCase(name)) {
            return COLOR;
        } else if (DITHERED.getName().equalsIgnoreCase(name)) {
            return DITHERED;
        } else {
            throw new IllegalArgumentException("Illegal value for enumeration: " + name);
        }
    }

    private Object readResolve() throws ObjectStreamException {
        return valueOf(getName());
    }

    /** {@inheritDoc} */
    public String toString() {
        return getClass().getName() + ":" + name;
    }
}
