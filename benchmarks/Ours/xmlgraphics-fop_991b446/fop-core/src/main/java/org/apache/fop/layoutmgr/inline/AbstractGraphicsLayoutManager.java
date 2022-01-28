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

package org.apache.fop.layoutmgr.inline;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.List;

import org.apache.fop.area.Area;
import org.apache.fop.area.inline.InlineArea;
import org.apache.fop.area.inline.InlineViewport;
import org.apache.fop.datatypes.LengthBase;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.flow.AbstractGraphics;
import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.TraitSetter;


/**
 * LayoutManager handling the common tasks for the fo:instream-foreign-object
 * and fo:external-graphics formatting objects
 */
public abstract class AbstractGraphicsLayoutManager extends LeafNodeLayoutManager {

    /**
     * Constructor.
     *
     * @param node
     *            the formatting object that creates this area
     */
    public AbstractGraphicsLayoutManager(AbstractGraphics node) {
        super(node);
    }

    /**
     * Get the inline area created by this element.
     *
     * @return the viewport inline area
     */
    private InlineViewport getInlineArea() {
        final AbstractGraphics fobj = (AbstractGraphics)this.fobj;
        Dimension intrinsicSize = new Dimension(
                fobj.getIntrinsicWidth(),
                fobj.getIntrinsicHeight());
        int bidiLevel = fobj.getBidiLevel();

        //TODO Investigate if the line-height property has to be taken into the calculation
        //somehow. There was some code here that hints in this direction but it was disabled.

        ImageLayout imageLayout = new ImageLayout(fobj, this, intrinsicSize);
        Rectangle placement = imageLayout.getPlacement();

        CommonBorderPaddingBackground borderProps = fobj.getCommonBorderPaddingBackground();
        setCommonBorderPaddingBackground(borderProps);

        //Determine extra BPD from borders and padding
        int beforeBPD = borderProps.getPadding(CommonBorderPaddingBackground.BEFORE, false, this);
        beforeBPD += borderProps.getBorderWidth(CommonBorderPaddingBackground.BEFORE, false);

        placement.y += beforeBPD;

        //Determine extra IPD from borders and padding
        if ((bidiLevel == -1) || ((bidiLevel & 1) == 0)) {
            int startIPD = borderProps.getPadding(CommonBorderPaddingBackground.START, false, this);
            startIPD += borderProps.getBorderWidth(CommonBorderPaddingBackground.START, false);
            placement.x += startIPD;
        } else {
            int endIPD = borderProps.getPadding(CommonBorderPaddingBackground.END, false, this);
            endIPD += borderProps.getBorderWidth(CommonBorderPaddingBackground.END, false);
            placement.x += endIPD;
        }

        Area viewportArea = getChildArea();
        TraitSetter.setProducerID(viewportArea, fobj.getId());
        transferForeignAttributes(viewportArea);

        InlineViewport vp = new InlineViewport(viewportArea, bidiLevel);
        TraitSetter.setProducerID(vp, fobj.getId());
        vp.setIPD(imageLayout.getViewportSize().width);
        vp.setBPD(imageLayout.getViewportSize().height);
        vp.setContentPosition(placement);
        vp.setClip(imageLayout.isClipped());
        vp.setBlockProgressionOffset(0);

        // Common Border, Padding, and Background Properties
        TraitSetter.addBorders(vp, borderProps
                                , false, false, false, false, this);
        TraitSetter.addPadding(vp, borderProps
                                , false, false, false, false, this);
        TraitSetter.addBackground(vp, borderProps, this);

        return vp;
    }

    /** {@inheritDoc} */
    public List getNextKnuthElements(LayoutContext context,
                                           int alignment) {
        InlineViewport areaCurrent = getInlineArea();
        setCurrentArea(areaCurrent);
        return super.getNextKnuthElements(context, alignment);
    }

    @Override
    protected InlineArea getEffectiveArea(LayoutContext layoutContext) {
        /*
         * If an image is in a repeated table heading, then it must be treated as real
         * content the first time and then as artifact. Therefore we cannot re-use the
         * area, as we have to account for the likely different values of treatAsArtifact.
         */
        InlineArea area = curArea != null ? curArea : getInlineArea();
        curArea = null;
        if (!layoutContext.treatAsArtifact()) {
            TraitSetter.addStructureTreeElement(area, ((AbstractGraphics) fobj).getStructureTreeElement());
        }
        return area;
    }

    /** {@inheritDoc} */
    protected AlignmentContext makeAlignmentContext(LayoutContext context) {
        final AbstractGraphics fobj = (AbstractGraphics)this.fobj;
        return new AlignmentContext(
                get(context).getAllocBPD()
                , fobj.getAlignmentAdjust()
                , fobj.getAlignmentBaseline()
                , fobj.getBaselineShift()
                , fobj.getDominantBaseline()
                , context.getAlignmentContext()
            );
    }

    /**
     * Returns the image of foreign object area to be put into
     * the viewport.
     * @return the appropriate area
     */
    protected abstract Area getChildArea();

    // --------- Property Resolution related functions --------- //

    /**
     * {@inheritDoc}
     */
    public int getBaseLength(int lengthBase, FObj fobj) {
        switch (lengthBase) {
        case LengthBase.IMAGE_INTRINSIC_WIDTH:
            return ((AbstractGraphics)fobj).getIntrinsicWidth();
        case LengthBase.IMAGE_INTRINSIC_HEIGHT:
            return ((AbstractGraphics)fobj).getIntrinsicHeight();
        case LengthBase.ALIGNMENT_ADJUST:
            return get(null).getBPD();
        default: // Delegate to super class
            return super.getBaseLength(lengthBase, fobj);
        }
    }
}

