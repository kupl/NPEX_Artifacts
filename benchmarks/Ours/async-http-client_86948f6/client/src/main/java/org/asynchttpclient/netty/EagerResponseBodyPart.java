/*
 * Copyright (c) 2014 AsyncHttpClient Project. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.asynchttpclient.netty;

import static org.asynchttpclient.util.ByteBufUtils.byteBuf2Bytes;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

import org.asynchttpclient.HttpResponseBodyPart;

/**
 * A callback class used when an HTTP response body is received.
 * Bytes are eagerly fetched from the ByteBuf
 */
public class EagerResponseBodyPart extends HttpResponseBodyPart {

    private final byte[] bytes;

    public EagerResponseBodyPart(ByteBuf buf, boolean last) {
        super(last);
        bytes = byteBuf2Bytes(buf);
    }

    /**
     * Return the response body's part bytes received.
     * 
     * @return the response body's part bytes received.
     */
    @Override
    public byte[] getBodyPartBytes() {
        return bytes;
    }

    @Override
    public int length() {
        return bytes.length;
    }

    @Override
    public ByteBuffer getBodyByteBuffer() {
        return ByteBuffer.wrap(bytes);
    }
}
