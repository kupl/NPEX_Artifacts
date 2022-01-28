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

package org.apache.fop.afp;

/**
 * Constants used by the AFP renderer.
 *
 */
public interface AFPConstants {

    /**
     * The encoding to use to convert to EBCIDIC
     */
    String EBCIDIC_ENCODING = "Cp1146";

    /**
     * The encoding to use to convert to ASCII
     */
    String ASCII_ENCODING = "Cp1252";

    /**
     * The encoding to use to convert to US ASCII (7 bit)
     */
    String US_ASCII_ENCODING = "US-ASCII";

    /**
     * The scaling of the default transform is set to
     * approximately 72 user space coordinates per square inch
     */
    int DPI_72 = 72;

    /**
     * 72dpi in millipoints
     */
    int DPI_72_MPTS = DPI_72 * 1000;

    /**
     * The line width is set as a multiplier of a default line with; the width of the default
     * line width is implementation defined, which probably means different devices use different
     * actual widths; this means that the source line width (as specified in, say, a SVG line
     * element) needs to be corrected by a fudge factor that depends on the output device so that
     * the final output (print to paper, screen viewer) looks as intended.
     */
    float LINE_WIDTH_CORRECTION = 2.5f;

}

