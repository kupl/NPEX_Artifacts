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

package org.apache.fop.area.inline;

import org.apache.fop.area.Area;

/**
 * Image area for external-graphic.
 * This area holds information for rendering an image.
 * The url of the image is used as a key to reference the image cache.
 */
public class Image extends Area {

    private static final long serialVersionUID = 4800834714349695386L;

    private String url;

    /**
     * Create a new image with the given url.
     *
     * @param url the url of the image
     */
    public Image(String url) {
        this.url = url;
    }

    /**
     * Get the url of this image.
     * This url is used as a key to locate the actual image data.
     *
     * @return the url of this image
     */
    public String getURL() {
        return this.url;
    }
}

