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

import org.apache.fop.afp.AFPConstants;

/**
 * An inline comment triplet.
 */
public class CommentTriplet extends AbstractTriplet {

    private final String commentString;

    /**
     * Construct a triplet.
     * @param id the triplet identification number
     * @param commentString a comment
     */
    public CommentTriplet(byte id, String commentString) {
        super(id);
        this.commentString = commentString;
    }

    /** {@inheritDoc} */
    public int getDataLength() {
        return 2 + commentString.length();
    }

    /** {@inheritDoc} */
    public void writeToStream(OutputStream os) throws IOException {
        byte[] data = getData();
        byte[] strData = commentString.getBytes(AFPConstants.EBCIDIC_ENCODING);
        System.arraycopy(strData, 0, data, 2, strData.length);
        os.write(data);
    }

}
