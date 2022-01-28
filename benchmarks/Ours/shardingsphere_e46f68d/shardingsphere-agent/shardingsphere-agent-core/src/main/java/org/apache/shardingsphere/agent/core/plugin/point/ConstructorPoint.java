/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.shardingsphere.agent.core.plugin.point;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * Configuration of constructor intercepting point.
 */
public class ConstructorPoint {
    private final ElementMatcher<? super MethodDescription> matcher;

    private final String advice;

    public ConstructorPoint(final ElementMatcher<? super MethodDescription> matcher, final String advice) {
        this.matcher = matcher;
        this.advice = advice;
    }

    /**
     * To detect target constructor constraints.
     *
     * @return constraints
     */
    public ElementMatcher<? super MethodDescription> getConstructorMatcher() {
        return matcher;
    }

    /**
     * To get the class name of advice.
     *
     * @return the class name of advice.
     */
    public String getAdvice() {
        return advice;
    }

}
