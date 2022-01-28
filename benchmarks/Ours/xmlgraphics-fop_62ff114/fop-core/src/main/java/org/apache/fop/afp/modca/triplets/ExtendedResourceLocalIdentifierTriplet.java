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

package org.apache.fop.afp.modca.triplets;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.fop.afp.util.BinaryUtils;

/**
 * The Extended Resource Local Identifier triplet specifies a resource type and a
 * four byte local identifier or LID. The LID usually is associated with a specific
 * resource name by a map structured field, such as a Map Data Resource structured
 * field, or a Map Media Type structured field.
 */
public class ExtendedResourceLocalIdentifierTriplet extends AbstractTriplet {

    /** the image resource type */
    public static final byte TYPE_IMAGE_RESOURCE = 0x10;

    /** the retired value type */
    public static final byte TYPE_RETIRED_VALUE = 0x30;

    /** the retired value type */
    public static final byte TYPE_MEDIA_RESOURCE = 0x40;

    /** the resource type */
    private final byte type;

    /** the resource local id */
    private final int localId;

    /**
     * Main constructor
     *
     * @param type the resource type
     * @param localId the resource local id
     */
    public ExtendedResourceLocalIdentifierTriplet(byte type, int localId) {
        super(AbstractTriplet.EXTENDED_RESOURCE_LOCAL_IDENTIFIER);
        this.type = type;
        this.localId = localId;
    }

    /** {@inheritDoc} */
    public void writeToStream(OutputStream os) throws IOException {
        byte[] data = getData();
        data[2] = type;
        byte[] resLID = BinaryUtils.convert(localId, 4); // 4 bytes
        System.arraycopy(resLID, 0, data, 3, resLID.length);
        os.write(data);
    }

    /** {@inheritDoc} */
    public int getDataLength() {
        return 7;
    }
}
