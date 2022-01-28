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

package org.apache.hc.client5.http.impl.classic;

import java.io.IOException;
import java.io.InterruptedIOException;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.SchemePortResolver;
import org.apache.hc.client5.http.classic.ExecRuntime;
import org.apache.hc.client5.http.config.Configurable;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.ConnectionShutdownException;
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver;
import org.apache.hc.client5.http.impl.ExecSupport;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.protocol.RequestClientConnControl;
import org.apache.hc.client5.http.routing.RoutingSupport;
import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.apache.hc.core5.concurrent.CancellableDependency;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ConnectionReuseStrategy;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.impl.DefaultConnectionReuseStrategy;
import org.apache.hc.core5.http.impl.io.HttpRequestExecutor;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.DefaultHttpProcessor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.apache.hc.core5.http.protocol.HttpProcessor;
import org.apache.hc.core5.http.protocol.RequestContent;
import org.apache.hc.core5.http.protocol.RequestTargetHost;
import org.apache.hc.core5.http.protocol.RequestUserAgent;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.net.URIAuthority;
import org.apache.hc.core5.util.Args;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.VersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal implementation of {@link CloseableHttpClient}. This client is
 * optimized for HTTP/1.1 message transport and does not support advanced
 * HTTP protocol functionality such as request execution via a proxy, state
 * management, authentication and request redirects.
 * <p>
 * Concurrent message exchanges executed by this client will get assigned to
 * separate connections leased from the connection pool.
 * </p>
 *
 * @since 4.3
 */
@Contract(threading = ThreadingBehavior.SAFE_CONDITIONAL)
public class MinimalHttpClient extends CloseableHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(MinimalHttpClient.class);

    private final HttpClientConnectionManager connManager;
    private final ConnectionReuseStrategy reuseStrategy;
    private final SchemePortResolver schemePortResolver;
    private final HttpRequestExecutor requestExecutor;
    private final HttpProcessor httpProcessor;

    MinimalHttpClient(final HttpClientConnectionManager connManager) {
        super();
        this.connManager = Args.notNull(connManager, "HTTP connection manager");
        this.reuseStrategy = DefaultConnectionReuseStrategy.INSTANCE;
        this.schemePortResolver = DefaultSchemePortResolver.INSTANCE;
        this.requestExecutor = new HttpRequestExecutor(this.reuseStrategy);
        this.httpProcessor = new DefaultHttpProcessor(
                new RequestContent(),
                new RequestTargetHost(),
                new RequestClientConnControl(),
                new RequestUserAgent(VersionInfo.getSoftwareInfo(
                        "Apache-HttpClient", "org.apache.hc.client5", getClass())));
    }

    @Override
protected org.apache.hc.client5.http.impl.classic.CloseableHttpResponse doExecute(final org.apache.hc.core5.http.HttpHost target, final org.apache.hc.core5.http.ClassicHttpRequest request, final org.apache.hc.core5.http.protocol.HttpContext context) throws java.io.IOException {
    org.apache.hc.core5.util.Args.notNull(target, "Target host");
    org.apache.hc.core5.util.Args.notNull(request, "HTTP request");
    if (request.getScheme() == null) {
        request.setScheme(target.getSchemeName());
    }
    if (request.getAuthority() == null) {
        request.setAuthority(new org.apache.hc.core5.net.URIAuthority(target));
    }
    final org.apache.hc.client5.http.protocol.HttpClientContext clientContext = org.apache.hc.client5.http.protocol.HttpClientContext.adapt(context != null ? context : new org.apache.hc.core5.http.protocol.BasicHttpContext());
    org.apache.hc.client5.http.config.RequestConfig config = null;
    if (request instanceof org.apache.hc.client5.http.config.Configurable) {
        config = ((org.apache.hc.client5.http.config.Configurable) (request)).getConfig();
    }
    if (config != null) {
        clientContext.setRequestConfig(config);
    }
    final org.apache.hc.client5.http.HttpRoute route = new org.apache.hc.client5.http.HttpRoute(org.apache.hc.client5.http.routing.RoutingSupport.normalize(target, schemePortResolver));
    final java.lang.String exchangeId = org.apache.hc.client5.http.impl.ExecSupport.getNextExchangeId();
    final org.apache.hc.client5.http.classic.ExecRuntime execRuntime = new org.apache.hc.client5.http.impl.classic.InternalExecRuntime(org.apache.hc.client5.http.impl.classic.MinimalHttpClient.LOG, connManager, requestExecutor, request instanceof org.apache.hc.core5.concurrent.CancellableDependency ? ((org.apache.hc.core5.concurrent.CancellableDependency) (request)) : null);
    try {
        if (!execRuntime.isEndpointAcquired()) {
            execRuntime.acquireEndpoint(exchangeId, route, null, clientContext);
        }
        if (!execRuntime.isEndpointConnected()) {
            execRuntime.connectEndpoint(clientContext);
        }
        /* NPEX_PATCH_BEGINS */
        if (context != null) {
            context.setAttribute(org.apache.hc.core5.http.protocol.HttpCoreContext.HTTP_REQUEST, request);
        }
        context.setAttribute(org.apache.hc.client5.http.protocol.HttpClientContext.HTTP_ROUTE, route);
        httpProcessor.process(request, request.getEntity(), context);
        final org.apache.hc.core5.http.ClassicHttpResponse response = execRuntime.execute(exchangeId, request, clientContext);
        httpProcessor.process(response, response.getEntity(), context);
        if (reuseStrategy.keepAlive(request, response, context)) {
            execRuntime.markConnectionReusable(null, org.apache.hc.core5.util.TimeValue.NEG_ONE_MILLISECOND);
        } else {
            execRuntime.markConnectionNonReusable();
        }
        // check for entity, release connection if possible
        final org.apache.hc.core5.http.HttpEntity entity = response.getEntity();
        if ((entity == null) || (!entity.isStreaming())) {
            // connection not needed and (assumed to be) in re-usable state
            execRuntime.releaseEndpoint();
            return new org.apache.hc.client5.http.impl.classic.CloseableHttpResponse(response, null);
        }
        org.apache.hc.client5.http.impl.classic.ResponseEntityProxy.enhance(response, execRuntime);
        return new org.apache.hc.client5.http.impl.classic.CloseableHttpResponse(response, execRuntime);
    } catch (final org.apache.hc.client5.http.impl.ConnectionShutdownException ex) {
        final java.io.InterruptedIOException ioex = new java.io.InterruptedIOException("Connection has been shut down");
        ioex.initCause(ex);
        execRuntime.discardEndpoint();
        throw ioex;
    } catch (final org.apache.hc.core5.http.HttpException httpException) {
        execRuntime.discardEndpoint();
        throw new org.apache.hc.client5.http.ClientProtocolException(httpException);
    } catch (java.lang.RuntimeException | java.io.IOException ex) {
        execRuntime.discardEndpoint();
        throw ex;
    } catch (final java.lang.Error error) {
        connManager.close(org.apache.hc.core5.io.CloseMode.IMMEDIATE);
        throw error;
    }
}

    @Override
    public void close() throws IOException {
        this.connManager.close();
    }

    @Override
    public void close(final CloseMode closeMode) {
        this.connManager.close(closeMode);
    }

}
