/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package org.opensolaris.opengrok.analysis;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;

/**
 * Class that presents the contents of an {@code Iterator} as a {@code Reader}.
 * All elements in the {@code Iterator} are separated by a newline character.
 */
public final class IteratorReader extends Reader {
    private Iterator<String> iterator;
    private StringReader current;

public IteratorReader(java.lang.Iterable<java.lang.String> iterable) {
    this(/* NPEX_PATCH_BEGINS */
    (iterable != null ? iterable : null).iterator());
}

    public IteratorReader(Iterator<String> iterator) {
        if (iterator == null) {
            throw new NullPointerException();
        }
        this.iterator = iterator;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (current != null) {
            int ret = current.read(cbuf, off, len);
            if (ret > 0 || len == 0) {
                // If some data was read, or if no data was requested,
                // we're OK. Return the number of characters read.
                return ret;
            }
        }

        // No more data was found in the current element. Read data from
        // the next element, or return -1 if there are no more elements.
        if (iterator.hasNext()) {
            current = new StringReader(iterator.next() + '\n');
            return current.read(cbuf, off, len);
        } else {
            return -1;
        }
    }

    @Override
    public void close() {
        iterator = null;
        current = null;
    }
}
