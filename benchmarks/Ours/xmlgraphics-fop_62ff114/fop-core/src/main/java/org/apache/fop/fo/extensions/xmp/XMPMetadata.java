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

package org.apache.fop.fo.extensions.xmp;

import java.io.Serializable;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.apache.xmlgraphics.util.XMLizable;
import org.apache.xmlgraphics.xmp.Metadata;
import org.apache.xmlgraphics.xmp.XMPConstants;

import org.apache.fop.fo.extensions.ExtensionAttachment;

/**
 * This is the pass-through value object for the XMP metadata extension.
 */
public class XMPMetadata implements ExtensionAttachment, Serializable, XMLizable {

    private static final long serialVersionUID = 591347206217931578L;

    /** The category URI for this extension attachment. */
    public static final String CATEGORY = XMPConstants.XMP_NAMESPACE;

    private Metadata meta;
    private boolean readOnly = true;

    /**
     * No-argument contructor.
     */
    public XMPMetadata() {
        //nop
    }

    /**
     * Default constructor.
     * @param metadata the XMP metadata
     */
    public XMPMetadata(Metadata metadata) {
        this.meta = metadata;
    }

    /** @return the XMP metadata */
    public Metadata getMetadata() {
        return this.meta;
    }

    /**
     * Sets the XMP metadata.
     * @param metadata the XMP metadata
     */
    public void setMetadata(Metadata metadata) {
        this.meta = metadata;
    }

    /** @return true if the XMP metadata is marked read-only. */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Sets the flag that decides whether a metadata packet may be modified.
     * @param readOnly true if the XMP metadata packet should be marked read-only.
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /** {@inheritDoc} */
    public String getCategory() {
        return CATEGORY;
    }

    /** {@inheritDoc} */
    public void toSAX(ContentHandler handler) throws SAXException {
        getMetadata().toSAX(handler);
    }

}
