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
package org.apache.hc.client5.http.impl;

import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.net.URIAuthority;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

/**
 * Simple tests for {@link ProtocolSupport}.
 */
public class TestProtocolSupport {

    @Test
    public void testGetRequestUri() {
        final HttpRequest request = new BasicHttpRequest(Method.GET, "");
        Assert.assertThat(ProtocolSupport.getRequestUri(request), CoreMatchers.equalTo("/"));
        request.setAuthority(new URIAuthority("testUser", "localhost", 8080));
        Assert.assertThat(ProtocolSupport.getRequestUri(request), CoreMatchers.equalTo("http://testUser@localhost:8080/"));
        request.setScheme("https");
        Assert.assertThat(ProtocolSupport.getRequestUri(request), CoreMatchers.equalTo("https://testUser@localhost:8080/"));
        request.setPath("blah");
        Assert.assertThat(ProtocolSupport.getRequestUri(request), CoreMatchers.equalTo("https://testUser@localhost:8080/blah"));
        request.setPath("/blah/blah");
        Assert.assertThat(ProtocolSupport.getRequestUri(request), CoreMatchers.equalTo("https://testUser@localhost:8080/blah/blah"));
    }
}
