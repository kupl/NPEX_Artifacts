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
package org.apache.hc.client5.http.impl.win;

import java.util.Locale;

import org.apache.hc.client5.http.auth.AuthSchemeFactory;
import org.apache.hc.client5.http.auth.StandardAuthScheme;
import org.apache.hc.client5.http.impl.auth.BasicSchemeFactory;
import org.apache.hc.client5.http.impl.auth.DigestSchemeFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;

import com.sun.jna.platform.win32.Sspi;

/**
 * Factory methods for {@link org.apache.hc.client5.http.impl.classic.CloseableHttpClient} instances configured to use integrated
 * Windows authentication by default.
 *
 * @since 4.4
 */
public class WinHttpClients {

    private WinHttpClients() {
        super();
    }

    public static boolean isWinAuthAvailable() {
        String os = System.getProperty("os.name");
        os = os != null ? os.toLowerCase(Locale.ROOT) : null;
        if (os != null && os.contains("windows")) {
            try {
                return Sspi.MAX_TOKEN_SIZE > 0;
            } catch (final Exception ignore) { // Likely ClassNotFound
                return false;
            }
        }
        return false;
    }

    private static HttpClientBuilder createBuilder() {
        if (isWinAuthAvailable()) {
            final Registry<AuthSchemeFactory> authSchemeRegistry = RegistryBuilder.<AuthSchemeFactory>create()
                    .register(StandardAuthScheme.BASIC, BasicSchemeFactory.INSTANCE)
                    .register(StandardAuthScheme.DIGEST, DigestSchemeFactory.INSTANCE)
                    .register(StandardAuthScheme.NTLM, WindowsNTLMSchemeFactory.DEFAULT)
                    .register(StandardAuthScheme.SPNEGO, WindowsNegotiateSchemeFactory.DEFAULT)
                    .build();
            return HttpClientBuilder.create()
                    .setDefaultAuthSchemeRegistry(authSchemeRegistry);
        }
        return HttpClientBuilder.create();
    }

    /**
     * Creates builder object for construction of custom
     * {@link CloseableHttpClient} instances.
     */
    public static HttpClientBuilder custom() {
        return createBuilder();
    }

    /**
     * Creates {@link CloseableHttpClient} instance with default
     * configuration.
     */
    public static CloseableHttpClient createDefault() {
        return createBuilder().build();
    }

    /**
     * Creates {@link CloseableHttpClient} instance with default
     * configuration based on system properties.
     */
    public static CloseableHttpClient createSystem() {
        return createBuilder().useSystemProperties().build();
    }


}
