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

import java.util.Map;

import org.apache.fop.apps.FOUserAgent;

/**
 * Implementations of this interface provide context information needed by supporting classes
 * during specific tasks (like image rendering).
 */
public interface RenderingContext {

    /**
     * Returns the MIME type associated with the current output format.
     * @return the MIME type (ex. application/pdf)
     */
    String getMimeType();

    /**
     * Returns the user agent. The user agent is used to access configuration and other information
     * for the rendering process.
     * @return the user agent
     */
    FOUserAgent getUserAgent();

    /**
     * Adds additional hints to the existing hints, overriding existing hints.
     * @param additionalHints a map of additional hints
     */
    void putHints(Map additionalHints);

    /**
     * Sets an additional hint, overriding an existing hint.
     * @param key the key
     * @param value the value
     */
    void putHint(Object key, Object value);

    /**
     * Returns an unmodifiable representation of all hints.
     * @return the hints
     */
    Map getHints();

    /**
     * Returns a hint identified by a key.
     * @param key the key
     * @return the hint or null if no hint with the given key could be found
     */
    Object getHint(Object key);

}
