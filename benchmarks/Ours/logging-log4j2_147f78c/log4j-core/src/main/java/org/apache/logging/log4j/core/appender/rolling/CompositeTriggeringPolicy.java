/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender.rolling;

import java.util.Arrays;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Triggering policy that wraps other policies.
 */
@Plugin(name = "Policies", category = "Core", printObject = true)
public final class CompositeTriggeringPolicy implements TriggeringPolicy {

    private final TriggeringPolicy[] triggeringPolicy;

    private CompositeTriggeringPolicy(final TriggeringPolicy... policies) {
        this.triggeringPolicy = policies;
    }

    public TriggeringPolicy[] getTriggeringPolicies() {
        return triggeringPolicy;
    }

    /**
     * Initializes the policy.
     * @param manager The RollingFileManager.
     */
    @Override
    public void initialize(final RollingFileManager manager) {
        for (final TriggeringPolicy policy : triggeringPolicy) {
            policy.initialize(manager);
        }
    }

    /**
     * Determines if a rollover should occur.
     * @param event A reference to the currently event.
     * @return true if a rollover should occur, false otherwise.
     */
    @Override
    public boolean isTriggeringEvent(final LogEvent event) {
        for (final TriggeringPolicy policy : triggeringPolicy) {
            if (policy.isTriggeringEvent(event)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a CompositeTriggeringPolicy.
     * @param policies The triggering policies.
     * @return A CompositeTriggeringPolicy.
     */
    @PluginFactory
    public static CompositeTriggeringPolicy createPolicy(
                                                @PluginElement("Policies") final TriggeringPolicy... policies) {
        return new CompositeTriggeringPolicy(policies);
    }

    @Override
    public String toString() {
        return "CompositeTriggeringPolicy(policies=" + Arrays.toString(triggeringPolicy) + ")";
    }

}
