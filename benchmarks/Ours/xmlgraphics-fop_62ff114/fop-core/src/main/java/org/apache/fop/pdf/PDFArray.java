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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.output.CountingOutputStream;

/**
 * Class representing an array object.
 */
public class PDFArray extends PDFObject {
    /**
     * List holding the values of this array
     */
    protected List<Object> values = new java.util.ArrayList<Object>();

    /**
     * Create a new, empty array object
     * @param parent the array's parent if any
     */
    public PDFArray(PDFObject parent) {
        /* generic creation of PDF object */
        super(parent);
    }

    /**
     * Create a new, empty array object with no parent.
     */
    public PDFArray() {
        this((PDFObject) null);
    }

    /**
     * Create an array object.
     * @param parent the array's parent if any
     * @param values the actual array wrapped by this object
     */
    public PDFArray(PDFObject parent, int[] values) {
        /* generic creation of PDF object */
        super(parent);

        for (int value : values) {
            this.values.add(value);
        }
    }

    /**
     * Create an array object.
     * @param parent the array's parent if any
     * @param values the actual array wrapped by this object
     */
    public PDFArray(PDFObject parent, double[] values) {
        /* generic creation of PDF object */
        super(parent);

        for (double value : values) {
            this.values.add(value);
        }
    }

    /**
     * Create an array object.
     * @param parent the array's parent if any
     * @param values the actual values wrapped by this object
     */
    public PDFArray(PDFObject parent, List<?> values) {
        /* generic creation of PDF object */
        super(parent);

        this.values.addAll(values);
    }

    /**
     * Creates an array object made of the given elements.
     *
     * @param elements the array content
     */
    public PDFArray(Object... elements) {
        this(null, elements);
    }

    /**
     * Creates an array object made of the given elements.
     *
     * @param elements the array content
     */
    public PDFArray(List<?> elements) {
        this(null, elements);
    }

    /**
     * Create the array object
     * @param parent the array's parent if any
     * @param values the actual array wrapped by this object
     */
    public PDFArray(PDFObject parent, Object[] values) {
        /* generic creation of PDF object */
        super(parent);

        Collections.addAll(this.values, values);
    }

    /**
     * Indicates whether the given object exists in the array.
     * @param obj the object to look for
     * @return true if obj is contained
     */
    public boolean contains(Object obj) {
        return this.values.contains(obj);
    }

    /**
     * Returns the length of the array
     * @return the length of the array
     */
    public int length() {
        return this.values.size();
    }

    /**
     * Sets an entry at a given location.
     * @param index the index of the value to set
     * @param obj the new value
     */
    public void set(int index, Object obj) {
        this.values.set(index, obj);
    }

    /**
     * Sets an entry at a given location.
     * @param index the index of the value to set
     * @param value the new value
     */
    public void set(int index, double value) {
        this.values.set(index, value);
    }

    /**
     * Gets an entry at a given location.
     * @param index the index of the value to set
     * @return the requested value
     */
    public Object get(int index) {
        return this.values.get(index);
    }

    /**
     * Adds a new value to the array.
     * @param obj the value
     */
    public void add(Object obj) {
        if (obj instanceof PDFObject) {
            PDFObject pdfObj = (PDFObject)obj;
            if (!pdfObj.hasObjectNumber()) {
                pdfObj.setParent(this);
            }
        }
        this.values.add(obj);
    }

    /**
     * Adds a new value to the array.
     * @param value the value
     */
    public void add(double value) {
        this.values.add(value);
    }

    /**
     * Clears the PDF array.
     */
    public void clear() {
        this.values.clear();
    }

    /** {@inheritDoc} */
    @Override
    public int output(OutputStream stream) throws IOException {
        CountingOutputStream cout = new CountingOutputStream(stream);
        StringBuilder textBuffer = new StringBuilder(64);
        textBuffer.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                textBuffer.append(' ');
            }
            Object obj = this.values.get(i);
            formatObject(obj, cout, textBuffer);
        }
        textBuffer.append(']');
        PDFDocument.flushTextBuffer(textBuffer, cout);
        return cout.getCount();
    }

    @Override
    public void getChildren(Set<PDFObject> children) {
        List<Object> contents = new ArrayList<Object>();
        for (Object c : values) {
            if (!(c instanceof PDFReference)) {
                contents.add(c);
            }
        }
        PDFDictionary.getChildren(contents, children);
    }
}
