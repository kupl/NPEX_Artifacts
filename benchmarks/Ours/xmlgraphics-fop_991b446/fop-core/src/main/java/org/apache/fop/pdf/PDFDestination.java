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

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.output.CountingOutputStream;

/**
 * class representing a named destination
 */
public class PDFDestination extends PDFObject {

    /**
     * ID Reference for this destination
     */
    private String idRef;

    /**
     * PDFReference (object reference) for this destination
     */
     private Object goToReference;

    /**
     * Create a named destination
     * @param idRef ID Reference for this destination (the name of the destination)
     * @param goToRef Object reference to the GoTo Action
     */
    public PDFDestination(String idRef, Object goToRef) {
        super();
        this.goToReference = goToRef;
        this.idRef = idRef;
    }

    @Override
    public int output(OutputStream stream) throws IOException {
        CountingOutputStream cout = new CountingOutputStream(stream);
        StringBuilder textBuffer = new StringBuilder(64);

        formatObject(getIDRef(), cout, textBuffer);
        textBuffer.append(' ');
        formatObject(goToReference, cout, textBuffer);

        PDFDocument.flushTextBuffer(textBuffer, cout);
        return cout.getCount();
    }

    /**
     * Sets the GoToReference in the associated DestinationData object.
     *
     * @param goToReference the reference to set in the associated DestinationData object.
     * @deprecated use setGoToReference(Object) instead
     */
    @Deprecated
    public void setGoToReference(String goToReference) {
        this.goToReference = goToReference;
    }

    /**
     * Sets the GoToReference in the associated DestinationData object.
     *
     * @param goToReference the reference to set in the associated DestinationData object.
     */
    public void setGoToReference(Object goToReference) {
        this.goToReference = goToReference;
    }

    /**
     * Returns the GoToReference from the associated DestinationData object.
     *
     * @return the GoToReference from the associated DestinationData object.
     */
    public Object getGoToReference() {
        return this.goToReference;
    }

    /**
     * Returns the RefID from the associated DestinationData object.
     *
     * @return the RefID from the associated DestinationData object.
     */
    public String getIDRef() {
        return this.idRef;
    }

    /**
     * Check if this equals another object.
     *
     * @param obj the object to compare
     * @return true if this equals other object
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof PDFDestination)) {
            return false;
        }

        PDFDestination dest = (PDFDestination)obj;
        if (dest.getIDRef().equals(this.getIDRef())) {
            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return getIDRef().hashCode();
    }

}

