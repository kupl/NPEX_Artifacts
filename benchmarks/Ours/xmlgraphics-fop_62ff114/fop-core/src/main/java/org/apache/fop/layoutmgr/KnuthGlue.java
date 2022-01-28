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

import org.apache.fop.traits.MinOptMax;

/**
 * An instance of this class represents a piece of content with adjustable
 * width: for example a space between words of justified text.
 *
 * A KnuthGlue is a feasible breaking point only if it immediately follows
 * a KnuthBox.
 *
 * The represented piece of content is suppressed if either the KnuthGlue
 * is a chosen breaking point or there isn't any KnuthBox between the
 * previous breaking point and the KnuthGlue itself.
 *
 * So, an unsuppressible piece of content with adjustable width, for example
 * a leader or a word with adjustable letter space, cannot be represented
 * by a single KnuthGlue; it can be represented using the sequence:
 *   KnuthBox(width = 0)
 *   KnuthPenalty(width = 0, penalty = infinity)
 *   KnuthGlue(...)
 *   KnuthBox(width = 0)
 * where the infinity penalty avoids choosing the KnuthGlue as a breaking point
 * and the 0-width KnuthBoxes prevent suppression.
 *
 * Besides the inherited methods and attributes, this class has two attributes
 * used to store the stretchability (difference between max and opt width) and
 * the shrinkability (difference between opt and min width), and the methods
 * to get these values.
 */
public class KnuthGlue extends KnuthElement {

    private final int stretch;
    private final int shrink;
    private final Adjustment adjustmentClass;

    /**
     * Creates a new <code>KnuthGlue</code>.
     *
     * @param minOptMax a <code>MinOptMax</code> where the {@link MinOptMax#getOpt() opt-value} is
     *                  mapped to the width, the {@link MinOptMax#getStretch()
     *                  stretchability} is mapped to the stretchability and the the {@link
     *                  MinOptMax#getShrink() shrinkability} is mapped to the shrinkability
     * @param pos       the Position stored in this glue
     * @param auxiliary is this glue auxiliary?
     */
    public KnuthGlue(MinOptMax minOptMax, Position pos, boolean auxiliary) {
        super(minOptMax.getOpt(), pos, auxiliary);
        this.stretch = minOptMax.getStretch();
        this.shrink = minOptMax.getShrink();
        this.adjustmentClass = Adjustment.NO_ADJUSTMENT;
    }

    /**
     * Creates a new <code>KnuthGlue</code>.
     *
     * @param width     the width of this glue
     * @param stretch   the stretchability of this glue
     * @param shrink    the shrinkability of this glue
     * @param pos       the Position stored in this glue
     * @param auxiliary is this glue auxiliary?
     */
    public KnuthGlue(int width, int stretch, int shrink, Position pos,
                     boolean auxiliary) {
        super(width, pos, auxiliary);
        this.stretch = stretch;
        this.shrink = shrink;
        this.adjustmentClass = Adjustment.NO_ADJUSTMENT;
    }

    /**
     * Creates a new <code>KnuthGlue</code>.
     *
     * @param width     the width of this glue
     * @param stretch   the stretchability of this glue
     * @param shrink    the shrinkability of this glue
     * @param adjustmentClass the adjsutment class
     * @param pos       the Position stored in this glue
     * @param auxiliary is this glue auxiliary?
     */
    public KnuthGlue(int width, int stretch, int shrink, Adjustment adjustmentClass,
                     Position pos, boolean auxiliary) {
        super(width, pos, auxiliary);
        this.stretch = stretch;
        this.shrink = shrink;
        this.adjustmentClass = adjustmentClass;
    }

    /** {@inheritDoc} */
    public boolean isGlue() {
        return true;
    }

    /** @return the stretchability of this glue. */
    public int getStretch() {
        return stretch;
    }

    /** @return the shrinkability of this glue. */
    public int getShrink() {
        return shrink;
    }

    /** @return the adjustment class (or role) of this glue. */
    public Adjustment getAdjustmentClass() {
        return adjustmentClass;
    }

    /** {@inheritDoc} */
    public String toString() {
        StringBuffer buffer = new StringBuffer(64);
        if (isAuxiliary()) {
            buffer.append("aux. ");
        }
        buffer.append("glue");
        buffer.append(" w=").append(getWidth());
        buffer.append(" stretch=").append(getStretch());
        buffer.append(" shrink=").append(getShrink());
        if (!getAdjustmentClass().equals(Adjustment.NO_ADJUSTMENT)) {
            buffer.append(" adj-class=").append(getAdjustmentClass());
        }
        return buffer.toString();
    }

}
