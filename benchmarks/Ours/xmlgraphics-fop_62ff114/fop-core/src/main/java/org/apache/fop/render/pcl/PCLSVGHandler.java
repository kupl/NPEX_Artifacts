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

package org.apache.fop.render.pcl;

// FOP
import org.apache.fop.render.AbstractGenericSVGHandler;
import org.apache.fop.render.Renderer;
import org.apache.fop.render.RendererContext;

/**
 * PCL XML handler for SVG. Uses Apache Batik for SVG processing.
 * This handler handles XML for foreign objects when rendering to HP GL/2.
 * It renders SVG to HP GL/2 using the PCLGraphics2D.
 */
public class PCLSVGHandler extends AbstractGenericSVGHandler {

    /** {@inheritDoc} */
    public boolean supportsRenderer(Renderer renderer) {
        return false;
    }

    /** {@inheritDoc} */
    protected void updateRendererContext(RendererContext context) {
        //Work around a problem in Batik: Gradients cannot be done in ColorSpace.CS_GRAY
        context.setProperty(PCLRendererContextConstants.PCL_COLOR_CANVAS,
                Boolean.TRUE);
    }

}

