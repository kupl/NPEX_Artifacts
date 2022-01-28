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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.hc.core5.http.HeaderElement;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.MessageSupport;
import org.apache.hc.core5.util.Args;

/**
 * HTTP OPTIONS method.
 *
 * @since 4.0
 */
public class HttpOptions extends HttpUriRequestBase {

    private static final long serialVersionUID = 1L;

    public final static String METHOD_NAME = "OPTIONS";

    /**
     * Creates a new instance initialized with the given URI.
     *
     * @param uri a non-null request URI.
     * @throws IllegalArgumentException if the uri is null.
     */
    public HttpOptions(final URI uri) {
        super(METHOD_NAME, uri);
    }

    /**
     * Creates a new instance initialized with the given URI.
     *
     * @param uri a non-null request URI.
     * @throws IllegalArgumentException if the uri is invalid.
     */
    public HttpOptions(final String uri) {
        this(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }

    public Set<String> getAllowedMethods(final HttpResponse response) {
        Args.notNull(response, "HTTP response");

        final Iterator<HeaderElement> it = MessageSupport.iterate(response, "Allow");
        final Set<String> methods = new HashSet<>();
        while (it.hasNext()) {
            final HeaderElement element = it.next();
            methods.add(element.getName());
        }
        return methods;
    }

}
