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

package org.apache.fop.layoutmgr;

/**
 * An instance of this class represents an unbreakable piece of content with
 * fixed width: for example an image, a syllable (but only if letter spacing
 * is constant), ...
 *
 * A KnuthBox is never a feasible breaking point.
 *
 * The represented piece of content is never suppressed.
 *
 * Besides the inherited methods and attributes, this class has some more
 * attributes to store information about the content height and its vertical
 * positioning, and the methods used to get them.
 */
public class KnuthBox extends KnuthElement {

    /**
     * Creates a new <code>KnuthBox</code>.
     *
     * @param width    the width of this box
     * @param pos  the Position stored in this box
     * @param auxiliary is this box auxiliary?
     */
    public KnuthBox(int width, Position pos, boolean auxiliary) {
        super(width, pos, auxiliary);
    }

    /** {@inheritDoc} */
    public boolean isBox() {
        return true;
    }

    /** {@inheritDoc} */
    public String toString() {
        StringBuffer buffer = new StringBuffer(64);
        if (isAuxiliary()) {
            buffer.append("aux. ");
        }
        buffer.append("box");
        buffer.append(" w=");
        buffer.append(getWidth());
        return buffer.toString();
    }

}
