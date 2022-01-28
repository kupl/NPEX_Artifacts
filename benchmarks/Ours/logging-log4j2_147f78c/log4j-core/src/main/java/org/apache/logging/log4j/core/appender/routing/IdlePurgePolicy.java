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
package org.apache.logging.log4j.core.appender.routing;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationScheduler;
import org.apache.logging.log4j.core.config.Scheduled;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Policy is purging appenders that were not in use specified time in minutes
 */
@Plugin(name = "IdlePurgePolicy", category = "Core", printObject = true)
@Scheduled
public class IdlePurgePolicy extends AbstractLifeCycle implements PurgePolicy, Runnable {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private final long timeToLive;
    private final ConcurrentMap<String, Long> appendersUsage = new ConcurrentHashMap<>();
    private RoutingAppender routingAppender;
    private final ConfigurationScheduler scheduler;
    private volatile ScheduledFuture<?> future = null;

    public IdlePurgePolicy(final long timeToLive, final ConfigurationScheduler scheduler) {
        this.timeToLive = timeToLive;
        this.scheduler = scheduler;
    }

    @Override
    public void initialize(final RoutingAppender routingAppender) {
        this.routingAppender = routingAppender;
    }

    @Override
    public void stop() {
        super.stop();
        future.cancel(true);
    }

    /**
     * Purging appenders that were not in use specified time
     */
    @Override
    public void purge() {
        final long createTime = System.currentTimeMillis() - timeToLive;
        for (final Entry<String, Long> entry : appendersUsage.entrySet()) {
            if (entry.getValue() < createTime) {
                LOGGER.debug("Removing appender " + entry.getKey());
                appendersUsage.remove(entry.getKey());
                routingAppender.deleteAppender(entry.getKey());
            }
        }
    }

    @Override
    public void update(final String key, final LogEvent event) {
        final long now = System.currentTimeMillis();
        appendersUsage.put(key, now);
        if (future == null) {
            synchronized (this) {
                if (future == null) {
                    scheduleNext();
                }
            }
        }

    }

    @Override
    public void run() {
        purge();
        scheduleNext();
    }

    private void scheduleNext() {
        long createTime = Long.MAX_VALUE;
        for (final Entry<String, Long> entry : appendersUsage.entrySet()) {
            if (entry.getValue() < createTime) {
                createTime = entry.getValue();
            }
        }
        if (createTime < Long.MAX_VALUE) {
            final long interval = timeToLive - (System.currentTimeMillis() - createTime);
            future = scheduler.schedule(this, interval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Create the PurgePolicy
     *
     * @param timeToLive the number of increments of timeUnit before the Appender should be purged.
     * @param timeUnit   the unit of time the timeToLive is expressed in.
     * @return The Routes container.
     */
    @PluginFactory
    public static PurgePolicy createPurgePolicy(
        @PluginAttribute("timeToLive") final String timeToLive,
        @PluginAttribute("timeUnit") final String timeUnit,
        @PluginConfiguration final Configuration configuration) {

        if (timeToLive == null) {
            LOGGER.error("A timeToLive  value is required");
            return null;
        }
        TimeUnit units;
        if (timeUnit == null) {
            units = TimeUnit.MINUTES;
        } else {
            try {
                units = TimeUnit.valueOf(timeUnit.toUpperCase());
            } catch (final Exception ex) {
                LOGGER.error("Invalid time unit {}", timeUnit);
                units = TimeUnit.MINUTES;
            }
        }

        final long ttl = units.toMillis(Long.parseLong(timeToLive));


        return new IdlePurgePolicy(ttl, configuration.getScheduler());
    }

    @Override
    public String toString() {
        return "timeToLive=" + timeToLive;
    }

}
