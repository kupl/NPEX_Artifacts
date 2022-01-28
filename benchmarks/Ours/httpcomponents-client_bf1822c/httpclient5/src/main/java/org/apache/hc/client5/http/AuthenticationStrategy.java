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

package org.apache.hc.client5.http;

import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.auth.AuthChallenge;
import org.apache.hc.client5.http.auth.AuthScheme;
import org.apache.hc.client5.http.auth.ChallengeType;
import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.apache.hc.core5.http.protocol.HttpContext;

/**
 * Strategy to select auth schemes in order of preference based on auth challenges
 * presented by the opposite endpoint (target server or a proxy).
 * <p>
 * Implementations of this interface must be thread-safe. Access to shared data must be
 * synchronized as methods of this interface may be executed from multiple threads.
 *
 * @since 4.2
 */
@Contract(threading = ThreadingBehavior.STATELESS)
public interface AuthenticationStrategy {

    /**
     * Returns an list of {@link AuthScheme}s to handle the given {@link AuthChallenge}s
     * in their order of preference.
     *
     * @param challengeType challenge type.
     * @param challenges map of challenges keyed by lowercase auth scheme names.
     * @param context HTTP context.
     * @return authentication auth schemes that can be used for authentication. Can be empty.
     *
     *  @since 5.0
     */
    List<AuthScheme> select(
            ChallengeType challengeType,
            Map<String, AuthChallenge> challenges,
            HttpContext context);

}
