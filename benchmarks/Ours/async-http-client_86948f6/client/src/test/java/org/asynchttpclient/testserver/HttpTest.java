/*
 * Copyright (c) 2016 AsyncHttpClient Project. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at
 *     http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.asynchttpclient.testserver;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import static org.asynchttpclient.Dsl.*;

public abstract class HttpTest {

    protected static final String COMPLETED_EVENT = "Completed";
    protected static final String STATUS_RECEIVED_EVENT = "StatusReceived";
    protected static final String HEADERS_RECEIVED_EVENT = "HeadersReceived";
    protected static final String HEADERS_WRITTEN_EVENT = "HeadersWritten";
    protected static final String CONTENT_WRITTEN_EVENT = "ContentWritten";
    protected static final String CONNECTION_OPEN_EVENT = "ConnectionOpen";
    protected static final String HOSTNAME_RESOLUTION_EVENT = "HostnameResolution";
    protected static final String HOSTNAME_RESOLUTION_SUCCESS_EVENT = "HostnameResolutionSuccess";
    protected static final String HOSTNAME_RESOLUTION_FAILURE_EVENT = "HostnameResolutionFailure";
    protected static final String CONNECTION_SUCCESS_EVENT = "ConnectionSuccess";
    protected static final String CONNECTION_FAILURE_EVENT = "ConnectionFailure";
    protected static final String TLS_HANDSHAKE_EVENT = "TlsHandshake";
    protected static final String TLS_HANDSHAKE_SUCCESS_EVENT = "TlsHandshakeSuccess";
    protected static final String TLS_HANDSHAKE_FAILURE_EVENT = "TlsHandshakeFailure";
    protected static final String CONNECTION_POOL_EVENT = "ConnectionPool";
    protected static final String CONNECTION_POOLED_EVENT = "ConnectionPooled";
    protected static final String CONNECTION_OFFER_EVENT = "ConnectionOffer";
    protected static final String REQUEST_SEND_EVENT = "RequestSend";
    protected static final String RETRY_EVENT = "Retry";

    @FunctionalInterface
    protected interface ClientFunction {
        void apply(AsyncHttpClient client) throws Throwable;
    }

    @FunctionalInterface
    protected interface ServerFunction {
        void apply(HttpServer server) throws Throwable;
    }

    protected static class ClientTestBody {

        private final AsyncHttpClientConfig config;

        private ClientTestBody(AsyncHttpClientConfig config) {
            this.config = config;
        }

        public void run(ClientFunction f) throws Throwable {
            try (AsyncHttpClient client = asyncHttpClient(config)) {
                f.apply(client);
            }
        }
    }

    protected static class ServerTestBody {

        private final HttpServer server;

        private ServerTestBody(HttpServer server) {
            this.server = server;
        }

        public void run(ServerFunction f) throws Throwable {
            try {
                f.apply(server);
            } finally {
                server.reset();
            }
        }
    }

    protected ClientTestBody withClient() {
        return withClient(config().setMaxRedirects(0));
    }

    protected ClientTestBody withClient(DefaultAsyncHttpClientConfig.Builder builder) {
        return withClient(builder.build());
    }

    protected ClientTestBody withClient(AsyncHttpClientConfig config) {
        return new ClientTestBody(config);
    }

    protected ServerTestBody withServer(HttpServer server) {
        return new ServerTestBody(server);
    }
}
