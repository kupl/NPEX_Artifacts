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

package org.apache.fop.pdf;

// Java
import java.io.IOException;
import java.io.OutputStream;

import org.apache.fop.fonts.type1.PFBData;

/**
 * Special PDFStream for embedding Type 1 fonts.
 */
public class PDFT1Stream extends AbstractPDFFontStream {

    private PFBData pfb;

    /** {@inheritDoc} */
    protected int getSizeHint() throws IOException {
        if (this.pfb != null) {
            return pfb.getLength();
        } else {
            return 0; //no hint available
        }
    }

    /**
     * Overload the base object method so we don't have to copy
     * byte arrays around so much
     * {@inheritDoc}
     */
    public int output(java.io.OutputStream stream)
            throws java.io.IOException {
        if (pfb == null) {
            throw new IllegalStateException("pfb must not be null at this point");
        }
        if (log.isDebugEnabled()) {
            log.debug("Writing " + pfb.getLength() + " bytes of Type 1 font data");
        }

        int length = super.output(stream);
        log.debug("Embedded Type1 font");
        return length;
    }

    /** {@inheritDoc} */
    protected void populateStreamDict(Object lengthEntry) {
        super.populateStreamDict(lengthEntry);
        put("Length1", pfb.getLength1());
        put("Length2", pfb.getLength2());
        put("Length3", pfb.getLength3());
    }

    /**
     * {@inheritDoc}
     */
    protected void outputRawStreamData(OutputStream out) throws IOException {
        this.pfb.outputAllParts(out);
    }

    /**
     * Used to set the PFBData object that represents the embeddable Type 1
     * font.
     * @param pfb The PFB file
     * @throws IOException in case of an I/O problem
     */
    public void setData(PFBData pfb) throws IOException {
        this.pfb = pfb;
    }

}
