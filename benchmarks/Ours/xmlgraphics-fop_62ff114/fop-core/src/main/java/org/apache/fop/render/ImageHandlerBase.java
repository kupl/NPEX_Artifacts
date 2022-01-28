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

package org.apache.fop.render;

import org.apache.xmlgraphics.image.loader.ImageFlavor;

/**
 * This interface is a service provider base interface for image handlers. It only contains
 * methods necessary for registration and is extended by sub-interfaces with the actual
 * image handling contract.
 */
public interface ImageHandlerBase {

    /**
     * Returns the priority for this image handler. A lower value means higher priority. This
     * information is used to build the ordered/prioritized list of supported ImageFlavors.
     * The built-in handlers use priorities between 100 and 999.
     * @return a positive integer (&gt;0) indicating the priority
     */
    int getPriority();

    /**
     * Returns the {@link ImageFlavor}s supported by this instance
     * @return the supported image flavors
     */
    ImageFlavor[] getSupportedImageFlavors();

    /**
     * Returns the {@link Class} subclass supported by this instance.
     * @return the image Class type
     */
    Class getSupportedImageClass();
}
