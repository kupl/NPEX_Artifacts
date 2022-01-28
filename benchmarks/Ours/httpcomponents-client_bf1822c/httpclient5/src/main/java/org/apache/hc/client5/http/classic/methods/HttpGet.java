/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.hc.client5.http.classic.methods;

import java.net.URI;

/**
 * HTTP GET method.
 *
 * @since 4.0
 */
public class HttpGet extends HttpUriRequestBase {

    private static final long serialVersionUID = 1L;

    public final static String METHOD_NAME = "GET";

    /**
     * Creates a new instance initialized with the given URI.
     *
     * @param uri a non-null request URI.
     * @throws IllegalArgumentException if the uri is null.
     */
    public HttpGet(final URI uri) {
        super(METHOD_NAME, uri);
    }

    /**
     * Creates a new instance initialized with the given URI.
     *
     * @param uri a non-null request URI.
     * @throws IllegalArgumentException if the uri is invalid.
     */
    public HttpGet(final String uri) {
        this(URI.create(uri));
    }

}
