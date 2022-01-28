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

package org.apache.hc.core5.http.impl.io;

import java.io.IOException;

import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpVersion;
import org.apache.hc.core5.http.ProtocolVersion;
import org.apache.hc.core5.http.message.LineFormatter;
import org.apache.hc.core5.http.message.RequestLine;
import org.apache.hc.core5.util.CharArrayBuffer;

/**
 * HTTP request writer that serializes its output to an instance of
 * {@link org.apache.hc.core5.http.io.SessionOutputBuffer}.
 *
 * @since 4.3
 */
public class DefaultHttpRequestWriter extends AbstractMessageWriter<ClassicHttpRequest> {

    /**
     * Creates an instance of DefaultHttpRequestWriter.
     *
     * @param formatter the line formatter If {@code null}
     *   {@link org.apache.hc.core5.http.message.BasicLineFormatter#INSTANCE}
     *   will be used.
     */
    public DefaultHttpRequestWriter(final LineFormatter formatter) {
        super(formatter);
    }

    public DefaultHttpRequestWriter() {
        this(null);
    }

    @Override
    protected void writeHeadLine(
            final ClassicHttpRequest message, final CharArrayBuffer lineBuf) throws IOException {
        final ProtocolVersion transportVersion = message.getVersion();
        getLineFormatter().formatRequestLine(lineBuf, new RequestLine(
                message.getMethod(),
                message.getRequestUri(),
                transportVersion != null ? transportVersion : HttpVersion.HTTP_1_1));
    }

}
