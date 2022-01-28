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

import java.util.List;
import java.util.ListIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.fop.traits.MinOptMax;

/**
 * This class resolves spaces and conditional borders and paddings by replacing the
 * UnresolvedListElements descendants by the right combination of KnuthElements on an element
 * list.
 */
public final class SpaceResolver {

    /** Logger instance */
    private static final Log LOG = LogFactory.getLog(SpaceResolver.class);

    private UnresolvedListElementWithLength[] firstPart;
    private BreakElement breakPoss;
    private UnresolvedListElementWithLength[] secondPart;
    private UnresolvedListElementWithLength[] noBreak;

    private MinOptMax[] firstPartLengths;
    private MinOptMax[] secondPartLengths;
    private MinOptMax[] noBreakLengths;

    private boolean isFirst;
    private boolean isLast;

    /**
     * Main constructor.
     * @param first Element list before a break (optional)
     * @param breakPoss Break possibility (optional)
     * @param second Element list after a break (or if no break possibility in vicinity)
     * @param isFirst Resolution at the beginning of a (full) element list
     * @param isLast Resolution at the end of a (full) element list
     */
    private SpaceResolver(List first, BreakElement breakPoss, List second,
            boolean isFirst, boolean isLast) {
        this.isFirst = isFirst;
        this.isLast = isLast;
        //Create combined no-break list
        int c = 0;
        if (first != null) {
            c += first.size();
        }
        if (second != null) {
            c += second.size();
        }
        noBreak = new UnresolvedListElementWithLength[c];
        noBreakLengths = new MinOptMax[c];
        int i = 0;
        ListIterator iter;
        if (first != null) {
            iter = first.listIterator();
            while (iter.hasNext()) {
                noBreak[i] = (UnresolvedListElementWithLength)iter.next();
                noBreakLengths[i] = noBreak[i].getLength();
                i++;
            }
        }
        if (second != null) {
            iter = second.listIterator();
            while (iter.hasNext()) {
                noBreak[i] = (UnresolvedListElementWithLength)iter.next();
                noBreakLengths[i] = noBreak[i].getLength();
                i++;
            }
        }

        //Add pending elements from higher level FOs
        if (breakPoss != null) {
            if (breakPoss.getPendingAfterMarks() != null) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("    adding pending before break: "
                            + breakPoss.getPendingAfterMarks());
                }
                first.addAll(0, breakPoss.getPendingAfterMarks());
            }
            if (breakPoss.getPendingBeforeMarks() != null) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("    adding pending after break: "
                            + breakPoss.getPendingBeforeMarks());
                }
                second.addAll(0, breakPoss.getPendingBeforeMarks());
            }
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("before: " + first);
            LOG.trace("  break: " + breakPoss);
            LOG.trace("after: " + second);
            LOG.trace("NO-BREAK: " + toString(noBreak, noBreakLengths));
        }

        if (first != null) {
            firstPart = new UnresolvedListElementWithLength[first.size()];
            firstPartLengths = new MinOptMax[firstPart.length];
            first.toArray(firstPart);
            for (i = 0; i < firstPart.length; i++) {
                firstPartLengths[i] = firstPart[i].getLength();
            }
        }
        this.breakPoss = breakPoss;
        if (second != null) {
            secondPart = new UnresolvedListElementWithLength[second.size()];
            secondPartLengths = new MinOptMax[secondPart.length];
            second.toArray(secondPart);
            for (i = 0; i < secondPart.length; i++) {
                secondPartLengths[i] = secondPart[i].getLength();
            }
        }
        resolve();
    }

    private String toString(Object[] arr1, Object[] arr2) {
        if (arr1.length != arr2.length) {
            throw new IllegalArgumentException("The length of both arrays must be equal");
        }
        StringBuffer sb = new StringBuffer("[");
        for (int i = 0; i < arr1.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(String.valueOf(arr1[i]));
            sb.append("/");
            sb.append(String.valueOf(arr2[i]));
        }
        sb.append("]");
        return sb.toString();
    }

    private void removeConditionalBorderAndPadding(
            UnresolvedListElement[] elems, MinOptMax[] lengths, boolean reverse) {
        for (int i = 0; i < elems.length; i++) {
            int effIndex;
            if (reverse) {
                effIndex = elems.length - 1 - i;
            } else {
                effIndex = i;
            }
            if (elems[effIndex] instanceof BorderOrPaddingElement) {
                BorderOrPaddingElement bop = (BorderOrPaddingElement)elems[effIndex];
                if (bop.isConditional() && !(bop.isFirst() || bop.isLast())) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Nulling conditional element: " + bop);
                    }
                    lengths[effIndex] = null;
                }
            }
        }
        if (LOG.isTraceEnabled() && elems.length > 0) {
            LOG.trace("-->Resulting list: " + toString(elems, lengths));
        }
    }

    private void performSpaceResolutionRule1(UnresolvedListElement[] elems, MinOptMax[] lengths,
            boolean reverse) {
        for (int i = 0; i < elems.length; i++) {
            int effIndex;
            if (reverse) {
                effIndex = elems.length - 1 - i;
            } else {
                effIndex = i;
            }
            if (lengths[effIndex] == null) {
                //Zeroed border or padding doesn't create a fence
                continue;
            } else if (elems[effIndex] instanceof BorderOrPaddingElement) {
                //Border or padding form fences!
                break;
            } else if (!elems[effIndex].isConditional()) {
                break;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Nulling conditional element using 4.3.1, rule 1: " + elems[effIndex]);
            }
            lengths[effIndex] = null;
        }
        if (LOG.isTraceEnabled() && elems.length > 0) {
            LOG.trace("-->Resulting list: " + toString(elems, lengths));
        }
    }

    private void performSpaceResolutionRules2to3(UnresolvedListElement[] elems,
            MinOptMax[] lengths, int start, int end) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("rule 2-3: " + start + "-" + end);
        }
        SpaceElement space;
        int remaining;

        //Rule 2 (4.3.1, XSL 1.0)
        boolean hasForcing = false;
        remaining = 0;
        for (int i = start; i <= end; i++) {
            if (lengths[i] == null) {
                continue;
            }
            remaining++;
            space = (SpaceElement)elems[i];
            if (space.isForcing()) {
                hasForcing = true;
                break;
            }
        }
        if (remaining == 0) {
            return; //shortcut
        }
        if (hasForcing) {
            for (int i = start; i <= end; i++) {
                if (lengths[i] == null) {
                    continue;
                }
                space = (SpaceElement)elems[i];
                if (!space.isForcing()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Nulling non-forcing space-specifier using 4.3.1, rule 2: "
                                + elems[i]);
                    }
                    lengths[i] = null;
                }
            }
            return; //If rule is triggered skip rule 3
        }

        //Rule 3 (4.3.1, XSL 1.0)
        //Determine highes precedence
        int highestPrecedence = Integer.MIN_VALUE;
        for (int i = start; i <= end; i++) {
            if (lengths[i] == null) {
                continue;
            }
            space = (SpaceElement)elems[i];
            highestPrecedence = Math.max(highestPrecedence, space.getPrecedence());
        }
        if (highestPrecedence != 0 && LOG.isDebugEnabled()) {
            LOG.debug("Highest precedence is " + highestPrecedence);
        }
        //Suppress space-specifiers with lower precedence
        remaining = 0;
        int greatestOptimum = Integer.MIN_VALUE;
        for (int i = start; i <= end; i++) {
            if (lengths[i] == null) {
                continue;
            }
            space = (SpaceElement)elems[i];
            if (space.getPrecedence() != highestPrecedence) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Nulling space-specifier with precedence "
                            + space.getPrecedence() + " using 4.3.1, rule 3: "
                            + elems[i]);
                }
                lengths[i] = null;
            } else {
                greatestOptimum = Math.max(greatestOptimum, space.getLength().getOpt());
                remaining++;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Greatest optimum: " + greatestOptimum);
        }
        if (remaining <= 1) {
            return;
        }
        //Suppress space-specifiers with smaller optimum length
        remaining = 0;
        for (int i = start; i <= end; i++) {
            if (lengths[i] == null) {
                continue;
            }
            space = (SpaceElement)elems[i];
            if (space.getLength().getOpt() < greatestOptimum) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Nulling space-specifier with smaller optimum length "
                            + "using 4.3.1, rule 3: "
                            + elems[i]);
                }
                lengths[i] = null;
            } else {
                remaining++;
            }
        }
        if (remaining <= 1) {
            return;
        }
        //Construct resolved space-specifier from the remaining spaces
        int min = Integer.MIN_VALUE;
        int max = Integer.MAX_VALUE;
        for (int i = start; i <= end; i++) {
            if (lengths[i] == null) {
                continue;
            }
            space = (SpaceElement)elems[i];
            min = Math.max(min, space.getLength().getMin());
            max = Math.min(max, space.getLength().getMax());
            if (remaining > 1) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Nulling non-last space-specifier using 4.3.1, rule 3, second part: "
                            + elems[i]);
                }
                lengths[i] = null;
                remaining--;
            } else {
                lengths[i] = MinOptMax.getInstance(min, lengths[i].getOpt(), max);
            }
        }

        if (LOG.isTraceEnabled() && elems.length > 0) {
            LOG.trace("Remaining spaces: " + remaining);
            LOG.trace("-->Resulting list: " + toString(elems, lengths));
        }
    }

    private void performSpaceResolutionRules2to3(UnresolvedListElement[] elems,
            MinOptMax[] lengths) {
        int start = 0;
        int i = start;
        while (i < elems.length) {
            if (elems[i] instanceof SpaceElement) {
                while (i < elems.length) {
                    if (elems[i] == null || elems[i] instanceof SpaceElement) {
                        i++;
                    } else {
                        break;
                    }
                }
                performSpaceResolutionRules2to3(elems, lengths, start, i - 1);
            }
            i++;
            start = i;
        }
    }

    private boolean hasFirstPart() {
        return firstPart != null && firstPart.length > 0;
    }

    private boolean hasSecondPart() {
        return secondPart != null && secondPart.length > 0;
    }

    private void resolve() {
        if (breakPoss != null) {
            if (hasFirstPart()) {
                removeConditionalBorderAndPadding(firstPart, firstPartLengths, true);
                performSpaceResolutionRule1(firstPart, firstPartLengths, true);
                performSpaceResolutionRules2to3(firstPart, firstPartLengths);
            }
            if (hasSecondPart()) {
                removeConditionalBorderAndPadding(secondPart, secondPartLengths, false);
                performSpaceResolutionRule1(secondPart, secondPartLengths, false);
                performSpaceResolutionRules2to3(secondPart, secondPartLengths);
            }
            if (noBreak != null) {
                performSpaceResolutionRules2to3(noBreak, noBreakLengths);
            }
        } else {
            if (isFirst) {
                removeConditionalBorderAndPadding(secondPart, secondPartLengths, false);
                performSpaceResolutionRule1(secondPart, secondPartLengths, false);
            }
            if (isLast) {
                removeConditionalBorderAndPadding(firstPart, firstPartLengths, true);
                performSpaceResolutionRule1(firstPart, firstPartLengths, true);
            }

            if (hasFirstPart()) {
                //Now that we've handled isFirst/isLast conditions, we need to look at the
                //active part in its normal order so swap it back.
                LOG.trace("Swapping first and second parts.");
                UnresolvedListElementWithLength[] tempList;
                MinOptMax[] tempLengths;
                tempList = secondPart;
                tempLengths = secondPartLengths;
                secondPart = firstPart;
                secondPartLengths = firstPartLengths;
                firstPart = tempList;
                firstPartLengths = tempLengths;
                if (hasFirstPart()) {
                    throw new IllegalStateException("Didn't expect more than one parts in a"
                            + "no-break condition.");
                }
            }
            performSpaceResolutionRules2to3(secondPart, secondPartLengths);
        }
    }

    private MinOptMax sum(MinOptMax[] lengths) {
        MinOptMax sum = MinOptMax.ZERO;
        for (MinOptMax length : lengths) {
            if (length != null) {
                sum = sum.plus(length);
            }
        }
        return sum;
    }

    private void generate(ListIterator iter) {
        MinOptMax spaceBeforeBreak = sum(firstPartLengths);
        MinOptMax spaceAfterBreak = sum(secondPartLengths);

        boolean hasPrecedingNonBlock = false;
        if (breakPoss != null) {
            if (spaceBeforeBreak.isNonZero()) {
                iter.add(new KnuthPenalty(0, KnuthPenalty.INFINITE, false, null, true));
                iter.add(new KnuthGlue(spaceBeforeBreak, null, true));
                if (breakPoss.isForcedBreak()) {
                    //Otherwise, the preceding penalty and glue will be cut off
                    iter.add(new KnuthBox(0, null, true));
                }
            }
            iter.add(new KnuthPenalty(breakPoss.getPenaltyWidth(), breakPoss.getPenaltyValue(),
                    false, breakPoss.getBreakClass(),
                    new SpaceHandlingBreakPosition(this, breakPoss), false));
            if (breakPoss.getPenaltyValue() <= -KnuthPenalty.INFINITE) {
                return; //return early. Not necessary (even wrong) to add additional elements
            }

            // No break
            // TODO: We can't use a MinOptMax for glue2,
            // because min <= opt <= max is not always true - why?
            MinOptMax noBreakLength = sum(noBreakLengths);
            MinOptMax spaceSum = spaceBeforeBreak.plus(spaceAfterBreak);
            int glue2width = noBreakLength.getOpt() - spaceSum.getOpt();
            int glue2stretch = noBreakLength.getStretch() - spaceSum.getStretch();
            int glue2shrink = noBreakLength.getShrink() - spaceSum.getShrink();

            if (glue2width != 0 || glue2stretch != 0 || glue2shrink != 0) {
                iter.add(new KnuthGlue(glue2width, glue2stretch, glue2shrink, null, true));
            }
        } else {
            if (spaceBeforeBreak.isNonZero()) {
                throw new IllegalStateException("spaceBeforeBreak should be 0 in this case");
            }
        }
        Position pos = null;
        if (breakPoss == null) {
            pos = new SpaceHandlingPosition(this);
        }
        if (spaceAfterBreak.isNonZero() || pos != null) {
            iter.add(new KnuthBox(0, pos, true));
        }
        if (spaceAfterBreak.isNonZero()) {
            iter.add(new KnuthPenalty(0, KnuthPenalty.INFINITE, false, null, true));
            iter.add(new KnuthGlue(spaceAfterBreak, null, true));
            hasPrecedingNonBlock = true;
        }
        if (isLast && hasPrecedingNonBlock) {
            //Otherwise, the preceding penalty and glue will be cut off
            iter.add(new KnuthBox(0, null, true));
        }
    }

    /**
     * Position class for break possibilities. It is used to notify layout manager about the
     * effective spaces and conditional lengths.
     */
    public static class SpaceHandlingBreakPosition extends Position {

        private SpaceResolver resolver;
        private Position originalPosition;

        /**
         * Main constructor.
         * @param resolver the space resolver that provides the info about the actual situation
         * @param breakPoss the original break possibility that creates this Position
         */
        public SpaceHandlingBreakPosition(SpaceResolver resolver, BreakElement breakPoss) {
            super(null);
            this.resolver = resolver;
            this.originalPosition = breakPoss.getPosition();
            //Unpack since the SpaceHandlingBreakPosition is a non-wrapped Position, too
            while (this.originalPosition instanceof NonLeafPosition) {
                this.originalPosition = this.originalPosition.getPosition();
            }
        }

        /** @return the space resolver */
        public SpaceResolver getSpaceResolver() {
            return this.resolver;
        }

        /**
         * Notifies all affected layout managers about the current situation in the part to be
         * handled for area generation.
         * @param isBreakSituation true if this is a break situation.
         * @param side defines to notify about the situation whether before or after the break.
         *             May be null if isBreakSituation is null.
         */
        public void notifyBreakSituation(boolean isBreakSituation, RelSide side) {
            if (isBreakSituation) {
                if (RelSide.BEFORE == side) {
                    for (int i = 0; i < resolver.secondPart.length; i++) {
                        resolver.secondPart[i].notifyLayoutManager(resolver.secondPartLengths[i]);
                    }
                } else {
                    for (int i = 0; i < resolver.firstPart.length; i++) {
                        resolver.firstPart[i].notifyLayoutManager(resolver.firstPartLengths[i]);
                    }
                }
            } else {
                for (int i = 0; i < resolver.noBreak.length; i++) {
                    resolver.noBreak[i].notifyLayoutManager(resolver.noBreakLengths[i]);
                }
            }
        }

        /** {@inheritDoc} */
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("SpaceHandlingBreakPosition(");
            sb.append(this.originalPosition);
            sb.append(")");
            return sb.toString();
        }

        /**
         * @return the original Position instance set at the BreakElement that this Position was
         *         created for.
         */
        public Position getOriginalBreakPosition() {
            return this.originalPosition;
        }

        /** {@inheritDoc} */
        public Position getPosition() {
            return originalPosition;
        }

    }

    /**
     * Position class for no-break situations. It is used to notify layout manager about the
     * effective spaces and conditional lengths.
     */
    public static class SpaceHandlingPosition extends Position {

        private SpaceResolver resolver;

        /**
         * Main constructor.
         * @param resolver the space resolver that provides the info about the actual situation
         */
        public SpaceHandlingPosition(SpaceResolver resolver) {
            super(null);
            this.resolver = resolver;
        }

        /** @return the space resolver */
        public SpaceResolver getSpaceResolver() {
            return this.resolver;
        }

        /**
         * Notifies all affected layout managers about the current situation in the part to be
         * handled for area generation.
         */
        public void notifySpaceSituation() {
            if (resolver.breakPoss != null) {
                throw new IllegalStateException("Only applicable to no-break situations");
            }
            for (int i = 0; i < resolver.secondPart.length; i++) {
                resolver.secondPart[i].notifyLayoutManager(resolver.secondPartLengths[i]);
            }
        }

        /** {@inheritDoc} */
        public String toString() {
            return "SpaceHandlingPosition";
        }
    }

    /**
     * Resolves unresolved elements applying the space resolution rules defined in 4.3.1.
     * @param elems the element list
     */
    public static void resolveElementList(List elems) {
        if (LOG.isTraceEnabled()) {
            LOG.trace(elems);
        }
        boolean first = true;
        boolean last = false;
        boolean skipNextElement = false;
        List unresolvedFirst = new java.util.ArrayList();
        List unresolvedSecond = new java.util.ArrayList();
        List currentGroup;
        ListIterator iter = elems.listIterator();
        while (iter.hasNext()) {
            ListElement el = (ListElement)iter.next();
            if (el.isUnresolvedElement()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("unresolved found: " + el + " " + first + "/" + last);
                }
                BreakElement breakPoss = null;
                //Clear temp lists
                unresolvedFirst.clear();
                unresolvedSecond.clear();
                //Collect groups
                if (el instanceof BreakElement) {
                    breakPoss = (BreakElement)el;
                    currentGroup = unresolvedSecond;
                } else {
                    currentGroup = unresolvedFirst;
                    currentGroup.add(el);
                }
                iter.remove();
                last = true;
                skipNextElement = true;
                while (iter.hasNext()) {
                    el = (ListElement)iter.next();
                    if (el instanceof BreakElement && breakPoss != null) {
                        skipNextElement = false;
                        last = false;
                        break;
                    } else if (currentGroup == unresolvedFirst && (el instanceof BreakElement)) {
                        breakPoss = (BreakElement)el;
                        iter.remove();
                        currentGroup = unresolvedSecond;
                    } else if (el.isUnresolvedElement()) {
                        currentGroup.add(el);
                        iter.remove();
                    } else {
                        last = false;
                        break;
                    }
                }
                //last = !iter.hasNext();
                if (breakPoss == null && unresolvedSecond.isEmpty() && !last) {
                    LOG.trace("Swap first and second parts in no-break condition,"
                            + " second part is empty.");
                    //The first list is reversed, so swap if this shouldn't happen
                    List swapList = unresolvedSecond;
                    unresolvedSecond = unresolvedFirst;
                    unresolvedFirst = swapList;
                }

                LOG.debug("----start space resolution (first=" + first + ", last=" + last + ")...");
                SpaceResolver resolver = new SpaceResolver(
                        unresolvedFirst, breakPoss, unresolvedSecond, first, last);
                if (!last) {
                    iter.previous();
                }
                resolver.generate(iter);
                if (!last && skipNextElement) {
                    iter.next();
                }
                LOG.debug("----end space resolution.");
            }
            first = false;
        }
    }

    /**
     * Inspects an effective element list and notifies all layout managers about the state of
     * the spaces and conditional lengths.
     * @param effectiveList the effective element list
     * @param startElementIndex index of the first element in the part to be processed
     * @param endElementIndex index of the last element in the part to be processed
     * @param prevBreak index of the the break possibility just before this part (used to
     *                  identify a break condition, lastBreak &lt;= 0 represents a no-break condition)
     */
    public static void performConditionalsNotification(List effectiveList,
            int startElementIndex, int endElementIndex, int prevBreak) {
        KnuthElement el = null;
        if (prevBreak > 0) {
            el = (KnuthElement)effectiveList.get(prevBreak);
        }
        SpaceResolver.SpaceHandlingBreakPosition beforeBreak = null;
        SpaceResolver.SpaceHandlingBreakPosition afterBreak = null;
        if (el != null && el.isPenalty()) {
            Position pos = el.getPosition();
            if (pos instanceof SpaceResolver.SpaceHandlingBreakPosition) {
                beforeBreak = (SpaceResolver.SpaceHandlingBreakPosition)pos;
                beforeBreak.notifyBreakSituation(true, RelSide.BEFORE);
            }
        }
        el = endElementIndex > -1 ? (KnuthElement) effectiveList.get(endElementIndex) : null;
        if (el != null && el.isPenalty()) {
            Position pos = el.getPosition();
            if (pos instanceof SpaceResolver.SpaceHandlingBreakPosition) {
                afterBreak = (SpaceResolver.SpaceHandlingBreakPosition)pos;
                afterBreak.notifyBreakSituation(true, RelSide.AFTER);
            }
        }
        for (int i = startElementIndex; i <= endElementIndex; i++) {
            Position pos = ((KnuthElement)effectiveList.get(i)).getPosition();
            if (pos instanceof SpaceResolver.SpaceHandlingPosition) {
                ((SpaceResolver.SpaceHandlingPosition)pos).notifySpaceSituation();
            } else if (pos instanceof SpaceResolver.SpaceHandlingBreakPosition) {
                SpaceResolver.SpaceHandlingBreakPosition noBreak;
                noBreak = (SpaceResolver.SpaceHandlingBreakPosition)pos;
                if (noBreak != beforeBreak && noBreak != afterBreak) {
                    noBreak.notifyBreakSituation(false, null);
                }
            }
        }
    }


}
