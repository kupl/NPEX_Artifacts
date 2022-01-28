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

package org.apache.fop.render.intermediate;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

import org.apache.fop.util.DelegatingContentHandler;

/**
 * This class is a {@link DelegatingContentHandler} subclass which swallows the
 * {@link #startDocument()} and {@link #endDocument()} methods. This is useful for handling
 * XML fragments.
 */
public class DelegatingFragmentContentHandler extends DelegatingContentHandler {

    /**
     * Main constructor
     * @param delegate the content handler to delegate the SAX events to
     */
    public DelegatingFragmentContentHandler(ContentHandler delegate) {
        setDelegateContentHandler(delegate);
        if (delegate instanceof LexicalHandler) {
            setDelegateLexicalHandler((LexicalHandler)delegate);
        }
        if (delegate instanceof DTDHandler) {
            setDelegateDTDHandler((DTDHandler)delegate);
        }
        if (delegate instanceof EntityResolver) {
            setDelegateEntityResolver((EntityResolver)delegate);
        }
        if (delegate instanceof ErrorHandler) {
            setDelegateErrorHandler((ErrorHandler)delegate);
        }
    }

    /** {@inheritDoc} */
    public void startDocument() throws SAXException {
        //nop/ignore
    }

    /** {@inheritDoc} */
    public void endDocument() throws SAXException {
        //nop/ignore
    }

}
