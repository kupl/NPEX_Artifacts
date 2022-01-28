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

package org.apache.fop.render.afp;

import org.apache.fop.render.RendererContextConstants;

/**
 * Defines a number of standard constants (keys) for use by the RendererContext class.
 */
public interface AFPRendererContextConstants extends RendererContextConstants {

    /**
     * Key for a Boolean value that enables grayscale processing instead of color
     * processing.
     */
    String AFP_GRAYSCALE = "afpGrayscale";

    /** The font information for the AFP renderer. */
    String AFP_FONT_INFO = "afpFontInfo";

    /** The afp resource manager */
    String AFP_RESOURCE_MANAGER = "afpResourceManager";

    /** The afp painting state */
    String AFP_PAINTING_STATE = "afpPaintingState";
}
