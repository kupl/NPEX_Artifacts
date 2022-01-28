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

import java.util.LinkedList;
import java.util.List;

import org.apache.fop.layoutmgr.inline.InlineLevelLayoutManager;
import org.apache.fop.layoutmgr.inline.KnuthInlineBox;


/**
 * Represents a list of inline Knuth elements.
 * If closed, it represents all elements of a Knuth paragraph.
 */
public class InlineKnuthSequence extends KnuthSequence  {

    private static final long serialVersionUID = 1354774188859946549L;

    private boolean isClosed;

    /**
     * Creates a new and empty list.
     */
    public InlineKnuthSequence() {
        super();
    }

    /**
     * Creates a new list from an existing list.
     * @param list The list from which to create the new list.
     */
    public InlineKnuthSequence(List list) {
        super(list);
    }

    /**
     * Is this an inline or a block sequence?
     * @return false
     */
    public boolean isInlineSequence() {
        return true;
    }

    /** {@inheritDoc} */
    public boolean canAppendSequence(KnuthSequence sequence) {
        return sequence.isInlineSequence() && !isClosed;
    }

    /** {@inheritDoc} */
    public boolean appendSequence(KnuthSequence sequence) {
        if (!canAppendSequence(sequence)) {
            return false;
        }
        // does the first element of the first paragraph add to an existing word?
        ListElement lastOldElement;
        ListElement firstNewElement;
        lastOldElement = getLast();
        firstNewElement = sequence.getElement(0);
        if (firstNewElement.isBox() && !((KnuthElement) firstNewElement).isAuxiliary()
                && lastOldElement.isBox() && ((KnuthElement) lastOldElement).getWidth() != 0) {
            addALetterSpace();
        }
        addAll(sequence);
        return true;
    }

    /** {@inheritDoc} */
    public boolean appendSequence(KnuthSequence sequence, boolean keepTogether,
                                  BreakElement breakElement) {
        return appendSequence(sequence);
    }


    /** {@inheritDoc} */
    public KnuthSequence endSequence() {
        if (!isClosed) {
            add(new KnuthPenalty(0, -KnuthElement.INFINITE, false, null, false));
            isClosed = true;
        }
        return this;
    }

    /**
     * Add letter space.
     */
    public void addALetterSpace() {
        KnuthBox prevBox = (KnuthBox) getLast();
        if (prevBox.isAuxiliary()
            && (size() < 4
                || !getElement(size() - 2).isGlue()
                || !getElement(size() - 3).isPenalty()
                || !getElement(size() - 4).isBox()
               )
           ) {
            // Not the sequence we are expecting
            return;
        }
        removeLast();
        LinkedList oldList = new LinkedList();
        // if there are two consecutive KnuthBoxes the
        // first one does not represent a whole word,
        // so it must be given one more letter space
        if (!prevBox.isAuxiliary()) {
            // if letter spacing is constant,
            // only prevBox needs to be replaced;
            oldList.add(prevBox);
        } else {
            // prevBox is the last element
            // in the sub-sequence
            //   <box> <aux penalty> <aux glue> <aux box>
            // the letter space is added to <aux glue>,
            // while the other elements are not changed
            oldList.add(prevBox);
            oldList.addFirst((KnuthGlue) removeLast());
            oldList.addFirst((KnuthPenalty) removeLast());
            oldList.addFirst((KnuthBox) removeLast());
        }
        // adding a letter space could involve, according to the text
        // represented by oldList, replacing a glue element or adding
        // new elements
        addAll(((InlineLevelLayoutManager)
                     prevBox.getLayoutManager())
                    .addALetterSpaceTo(oldList));
        // prevBox may not be a KnuthInlineBox;
        // this may happen if it is a padding box; see bug 39571.
        if (prevBox instanceof KnuthInlineBox && ((KnuthInlineBox) prevBox).isAnchor()) {
            // prevBox represents a footnote citation: copy footnote info
            // from prevBox to the new box
            KnuthInlineBox newBox = (KnuthInlineBox) getLast();
            newBox.setFootnoteBodyLM(((KnuthInlineBox) prevBox).getFootnoteBodyLM());
        }
    }

}
