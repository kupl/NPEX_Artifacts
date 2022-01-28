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

package org.apache.fop.afp.modca;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.apache.fop.afp.Streamable;

/**
 * A Resource Group contains a set of overlays.
 */
public class ResourceGroup extends AbstractNamedAFPObject {

    /** Set of resource uri */
    private final Set<AbstractNamedAFPObject> resourceSet = new java.util.HashSet<AbstractNamedAFPObject>();

    /**
     * Constructor for the ResourceGroup, this takes a
     * name parameter which must be 8 characters long.
     *
     * @param name the resource group name
     */
    public ResourceGroup(String name) {
        super(name);
    }

    /**
     * Add this named object to this resource group
     *
     * @param namedObject a named object
     * @throws IOException thrown if an I/O exception of some sort has occurred.
     */
    public void addObject(AbstractNamedAFPObject namedObject) throws IOException {
        resourceSet.add(namedObject);
    }

    /**
     * Returns the number of resources contained in this resource group
     *
     * @return the number of resources contained in this resource group
     */
    public int getResourceCount() {
        return resourceSet.size();
    }

    /** {@inheritDoc} */
    public void writeStart(OutputStream os) throws IOException {
        byte[] data = new byte[17];
        copySF(data, Type.BEGIN, Category.RESOURCE_GROUP);
        os.write(data);
    }

    /** {@inheritDoc} */
    public void writeContent(OutputStream os) throws IOException {
        for (Object object : resourceSet) {
            if (object instanceof Streamable) {
                Streamable streamableObject = (Streamable) object;
                streamableObject.writeToStream(os);
            }
        }
    }

    /** {@inheritDoc} */
    public void writeEnd(OutputStream os) throws IOException {
        byte[] data = new byte[17];
        copySF(data, Type.END, Category.RESOURCE_GROUP);
        os.write(data);
    }

    /** {@inheritDoc} */
    public String toString() {
        return this.name + " " + resourceSet/*getResourceMap()*/;
    }
}
