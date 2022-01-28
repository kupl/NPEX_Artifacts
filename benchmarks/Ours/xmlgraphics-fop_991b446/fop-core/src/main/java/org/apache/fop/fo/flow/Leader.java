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

package org.apache.fop.fo.flow;

import java.util.Stack;

import org.xml.sax.Locator;

import org.apache.fop.apps.FOPException;
import org.apache.fop.complexscripts.bidi.DelimitedTextRange;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.FONode;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.ValidationException;
import org.apache.fop.fo.properties.LengthRangeProperty;
import org.apache.fop.util.CharUtilities;

/**
 * Class modelling the <a href="http://www.w3.org/TR/xsl/#fo_leader">
 * <code>fo:leader</code></a> object.
 * The main property of <code>fo:leader</code> is leader-pattern.
 * The following patterns are treated: rule, space, dots and use-content.
 */
public class Leader extends InlineLevel {
    // The value of properties relevant for fo:leader.
    // See also superclass InlineLevel
    private Length alignmentAdjust;
    private int alignmentBaseline;
    private Length baselineShift;
    private int dominantBaseline;
    private int leaderAlignment;
    private LengthRangeProperty leaderLength;
    private int leaderPattern;
    private Length leaderPatternWidth;
    private int ruleStyle;
    private Length ruleThickness;
    // private ToBeImplementedProperty letterSpacing;
    // private ToBeImplementedProperty textShadow;
    // Unused but valid items, commented out for performance:
    //     private CommonRelativePosition commonRelativePosition;
    //     private Length textDepth;
    //     private Length textAltitude;
    // End of property values

    /**
     * Base constructor
     *
     * @param parent {@link FONode} that is the parent of this object
     */
    public Leader(FONode parent) {
        super(parent);
    }

    /** {@inheritDoc} */
    public void bind(PropertyList pList) throws FOPException {
        super.bind(pList);
        alignmentAdjust = pList.get(PR_ALIGNMENT_ADJUST).getLength();
        alignmentBaseline = pList.get(PR_ALIGNMENT_BASELINE).getEnum();
        baselineShift = pList.get(PR_BASELINE_SHIFT).getLength();
        dominantBaseline = pList.get(PR_DOMINANT_BASELINE).getEnum();
        leaderAlignment = pList.get(PR_LEADER_ALIGNMENT).getEnum();
        leaderLength = pList.get(PR_LEADER_LENGTH).getLengthRange();
        leaderPattern = pList.get(PR_LEADER_PATTERN).getEnum();
        leaderPatternWidth = pList.get(PR_LEADER_PATTERN_WIDTH).getLength();
        // use default rule thickness as a default
        ruleThickness = getPropertyMakerFor(PR_RULE_THICKNESS).make(pList).getLength();
        switch(leaderPattern) {
        case EN_SPACE:
            // use Space
            break;
        case EN_RULE:
            // the following properties only apply
            // for leader-pattern = "rule"
            ruleStyle = pList.get(PR_RULE_STYLE).getEnum();
            // use specified rule thickness to override default (established above)
            ruleThickness = pList.get(PR_RULE_THICKNESS).getLength();
            break;
        case EN_DOTS:
            break;
        case EN_USECONTENT:
            // use inline layout manager to create inline areas
            // add the inline parent multiple times until leader full
            break;
        default:
            throw new RuntimeException("Invalid leader pattern: " + leaderPattern);
        }
        // letterSpacing = pList.get(PR_LETTER_SPACING);
        // textShadow = pList.get(PR_TEXT_SHADOW);
    }

    /**
     * {@inheritDoc}
     * <br>XSL Content Model: (#PCDATA|%inline;)*
     * <br><i>Additionally: "The content must not contain an
     * fo:leader, fo:inline-container, fo:block-container, fo:float,
     * fo:footnote, or fo:marker either as a direct child or as a
     * descendant."</i>
     */
    protected void validateChildNode(Locator loc, String nsURI, String localName)
        throws ValidationException {
        if (FO_URI.equals(nsURI)) {
            if (localName.equals("leader")
                 || localName.equals("inline-container")
                 || localName.equals("block-container")
                 || localName.equals("float")
                 || localName.equals("marker")
                 || !isInlineItem(nsURI, localName)) {
                invalidChildError(loc, nsURI, localName);
            }
        }
    }

    /** @return the "rule-style" property */
    public int getRuleStyle() {
        return ruleStyle;
    }

    /** @return the "rule-thickness" property */
    public Length getRuleThickness() {
        return ruleThickness;
    }

    /** @return the "leader-alignment" property */
    public int getLeaderAlignment() {
        return leaderAlignment;
    }

    /** @return the "leader-length" property */
    public LengthRangeProperty getLeaderLength() {
        return leaderLength;
    }

    /** @return the "leader-pattern" property */
    public int getLeaderPattern() {
        return leaderPattern;
    }

    /** @return the "leader-pattern-width" property */
    public Length getLeaderPatternWidth() {
        return leaderPatternWidth;
    }

    /** @return the "alignment-adjust" property */
    public Length getAlignmentAdjust() {
        return alignmentAdjust;
    }

    /** @return the "alignment-baseline" property */
    public int getAlignmentBaseline() {
        return alignmentBaseline;
    }

    /** @return the "baseline-shift" property */
    public Length getBaselineShift() {
        return baselineShift;
    }

    /** @return the "dominant-baseline" property */
    public int getDominantBaseline() {
        return dominantBaseline;
    }

    /** {@inheritDoc} */
    public String getLocalName() {
        return "leader";
    }

    /**
     * {@inheritDoc}
     * @return {@link org.apache.fop.fo.Constants#FO_LEADER}
     */
    public int getNameId() {
        return FO_LEADER;
    }

    @Override
    public void startOfNode() throws FOPException {
        super.startOfNode();
        getFOEventHandler().startLeader(this);
    }

    @Override
    public void endOfNode() throws FOPException {
        super.endOfNode();
        getFOEventHandler().endLeader(this);
    }

    @Override
    protected Stack<DelimitedTextRange> collectDelimitedTextRanges(Stack<DelimitedTextRange> ranges,
        DelimitedTextRange currentRange) {
        if (currentRange != null) {
            if (leaderPattern == EN_USECONTENT) {
                ranges = super.collectDelimitedTextRanges(ranges, currentRange);
            } else {
                currentRange.append(CharUtilities.OBJECT_REPLACEMENT_CHARACTER, this);
            }
        }
        return ranges;
    }

}
