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

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;

import org.apache.xmlgraphics.java2d.color.ColorUtil;

import org.apache.fop.util.AbstractPaintingState;

/**
 * This keeps information about the current painting state when writing to pdf.
 * It allows for creating new graphics states with the q operator.
 * This class is only used to store the information about the state
 * the caller needs to handle the actual pdf operators.
 *
 * When setting the state for pdf there are three possible ways of
 * handling the situation.
 * The values can be set to override previous or default values.
 * A new state can be added and then the values set.
 * The current state can be popped and values will return to a
 * previous state then the necessary values can be overridden.
 * The current transform behaves differently to other values as the
 * matrix is combined with the current resolved value.
 */
public class PDFPaintingState extends org.apache.fop.util.AbstractPaintingState {

    private static final long serialVersionUID = 5384726143906371279L;

    /**
     * PDF State for storing graphics state.
     */
    public PDFPaintingState() {
    }

    /**
     * Set the current paint.
     * This checks if the paint will change and then sets the current paint.
     *
     * @param p the new paint
     * @return true if the new paint changes the current paint
     */
    public boolean setPaint(Paint p) {
        PDFData data = getPDFData();
        Paint currentPaint = data.paint;
        if (currentPaint == null) {
            if (p != null) {
                data.paint = p;
                return true;
            }
        } else if (p instanceof Color && currentPaint instanceof Color) {
            if (!ColorUtil.isSameColor((Color)p, (Color)currentPaint)) {
                data.paint = p;
                return true;
            }
        } else if (!currentPaint.equals(p)) {
            data.paint = p;
            return true;
        }
        return false;
    }

    /**
     * Check if the clip will change the current state.
     * A clip is assumed to be used in a situation where it will add
     * to any clip in the current or parent states.
     * A clip cannot be cleared, this can only be achieved by going to
     * a parent level with the correct clip.
     * If the clip is different then it may start a new state so that
     * it can return to the previous clip.
     *
     * @param cl the clip shape to check
     * @return true if the clip will change the current clip.
     */
    public boolean checkClip(Shape cl) {
        Shape clip = getPDFData().clip;
        if (clip == null) {
            if (cl != null) {
                return true;
            }
        } else if (!new Area(clip).equals(new Area(cl))) {
            return true;
        }
        //TODO check for clips that are larger than the current
        return false;
    }

    /**
     * Set the current clip.
     * This either sets a new clip or sets the clip to the intersect of
     * the old clip and the new clip.
     *
     * @param cl the new clip in the current state
     */
    public void setClip(Shape cl) {
        PDFData data = getPDFData();
        Shape clip = data.clip;
        if (clip != null) {
            Area newClip = new Area(clip);
            newClip.intersect(new Area(cl));
            data.clip = new GeneralPath(newClip);
        } else {
            data.clip = cl;
        }
    }

    /**
     * Sets the character spacing (Tc).
     * @param value the new value
     * @return true if the value was changed with respect to the previous value
     */
    public boolean setCharacterSpacing(float value) {
        PDFData data = getPDFData();
        if (value != data.characterSpacing) {
            data.characterSpacing = value;
            return true;
        }
        return false;
    }

    /**
     * Returns the current character spacing (Tc) value.
     * @return the Tc value
     */
    public float getCharacterSpacing() {
        return getPDFData().characterSpacing;
    }

    /**
     * Get the current stack level.
     *
     * @return the current stack level
     */
    public int getStackLevel() {
        return getStateStack().size();
    }

    /**
     * Get the graphics state.
     * This gets the combination of all graphic states for
     * the current context.
     * This is the graphic state set with the gs operator not
     * the other graphic state changes.
     *
     * @return the calculated ExtGState in the current context
     */
    public PDFGState getGState() {
        PDFGState defaultState = PDFGState.DEFAULT;

        PDFGState state;
        PDFGState newState = new PDFGState();
        newState.addValues(defaultState);
        for (AbstractData abstractData : getStateStack()) {
            PDFData data = (PDFData) abstractData;
            state = data.gstate;
            if (state != null) {
                newState.addValues(state);
            }
        }
        if (getPDFData().gstate != null) {
            newState.addValues(getPDFData().gstate);
        }
        return newState;
    }

    public void setLayer(String layer) {
        getPDFData().setLayer(layer);
    }

    public String getLayer() {
        return getPDFData().getLayer();
    }

    public boolean getLayerChanged() {
        String layerCurrent = getLayer();
        if (layerCurrent == null) {
            return false;
        } else if (getStateStack().isEmpty()) {
            return true;
        } else {
            for (int i = getStackLevel(); i > 0; --i) {
                String layerPrev = ((PDFData) getStateStack().get(i - 1)).getLayer();
                if (layerPrev == null) {
                    continue;
                } else {
                    // Both current and prior are set, so, if same, then we know layer
                    // didn't change (and can stop search), otherwise it did change.
                    return !layerCurrent.equals(layerPrev);
                }
            }
            // Current layer set, but no prior saved layer set, so must have changed.
            return true;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected AbstractData instantiateData() {
        return new PDFData();
    }

    /** {@inheritDoc} */
    @Override
    protected AbstractPaintingState instantiate() {
        return new PDFPaintingState();
    }

    /**
     * Push the current state onto the stack.
     * This call should be used when the q operator is used
     * so that the state is known when popped.
     */
    @Override
    public void save() {
        AbstractData data = getData();
        AbstractData copy = (AbstractData)data.clone();
        data.clearTransform();
        getStateStack().push(copy);
    }

    private PDFData getPDFData() {
        return (PDFData)getData();
    }

    // @SuppressFBWarnings("SE_INNER_CLASS")
    private class PDFData extends org.apache.fop.util.AbstractPaintingState.AbstractData {

        private static final long serialVersionUID = 3527950647293177764L;

        private Paint paint;
        private Paint backPaint;
        //private int lineCap = 0; //Disabled the ones that are not used, yet
        //private int lineJoin = 0;
        //private float miterLimit = 0;
        //private int dashOffset = 0;
        private Shape clip;
        private PDFGState gstate;

        //text state
        private float characterSpacing;

        /** {@inheritDoc} */
        @Override
        public Object clone() {
            PDFData obj = (PDFData)super.clone();
            obj.paint = this.paint;
            obj.backPaint = this.paint;
            //obj.lineCap = this.lineCap;
            //obj.lineJoin = this.lineJoin;
            //obj.miterLimit = this.miterLimit;
            //obj.dashOffset = this.dashOffset;
            obj.clip = this.clip;
            obj.gstate = this.gstate;
            obj.characterSpacing = this.characterSpacing;
            return obj;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return super.toString()
                + ", paint=" + paint
                + ", backPaint=" + backPaint
                //+ ", lineCap=" + lineCap
                //+ ", miterLimit=" + miterLimit
                //+ ", dashOffset=" + dashOffset
                + ", clip=" + clip
                + ", gstate=" + gstate;
        }

        /** {@inheritDoc} */
        @Override
        protected AbstractData instantiate() {
            return new PDFData();
        }
    }

}

