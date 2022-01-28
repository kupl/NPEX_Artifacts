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

package org.apache.fop.render.ps;

import org.apache.xmlgraphics.ps.PSResource;

/**
 * PostScript Resource class representing a FOP form. This is used by PSRenderer to keep track
 * of images.
 */
public class PSImageFormResource extends PSResource {

    private String uri;

    /**
     * Create a new Form Resource.
     * @param id An ID for the form
     * @param uri the URI to the image
     */
    public PSImageFormResource(int id, String uri) {
        this("FOPForm:" + Integer.toString(id), uri);
    }

    /**
    /**
     * Create a new Form Resource.
     * @param name the name of the resource
     * @param uri the URI to the image
     */
    public PSImageFormResource(String name, String uri) {
        super(PSResource.TYPE_FORM, name);
        this.uri = uri;
    }

    /**
     * Returns the image URI.
     * @return the image URI
     */
    public String getImageURI() {
        return this.uri;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

}
