//log4j-core/src/main/java/org/apache/logging/log4j/core/appender/rolling/CronTriggeringPolicy.java
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

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationScheduler;
import org.apache.logging.log4j.core.config.CronScheduledFuture;
import org.apache.logging.log4j.core.config.Scheduled;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.CronExpression;

/**
 * Rolls a file over based on a cron schedule.
 */
@Plugin(name = "CronTriggeringPolicy", category = Core.CATEGORY_NAME, printObject = true)
@Scheduled
public final class CronTriggeringPolicy extends AbstractTriggeringPolicy {

    private static final String defaultSchedule = "0 0 0 * * ?";
    private RollingFileManager manager;
    private final CronExpression cronExpression;
    private final Configuration configuration;
    private final boolean checkOnStartup;
    private volatile Date nextRollDate;
    private CronScheduledFuture<?> future;

    private CronTriggeringPolicy(final CronExpression schedule, final boolean checkOnStartup,
            final Configuration configuration) {
        this.cronExpression = Objects.requireNonNull(schedule, "schedule");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.checkOnStartup = checkOnStartup;
    }

    /**
     * Initializes the policy.
     *
     * @param aManager
     *            The RollingFileManager.
     */
    @Override
    public void initialize(final RollingFileManager aManager) {
        this.manager = aManager;
        final Date nextDate = new Date(this.manager.getFileTime());
        nextRollDate = cronExpression.getNextValidTimeAfter(nextDate);
        if (checkOnStartup) {
            if (nextRollDate.getTime() < System.currentTimeMillis()) {
                rollover();
            }
        }
        final ConfigurationScheduler scheduler = configuration.getScheduler();
        /*genesis generated change*/
        if ((this.future) == null) {
            return ;
        }
        if (!(scheduler.isStarted())) {
            scheduler.incrementScheduledItems();
            scheduler.start();
        }
        future = scheduler.scheduleWithCron(cronExpression, new CronTrigger());
    }

    /**
     * Determines whether a rollover should occur.
     *
     * @param event
     *            A reference to the currently event.
     * @return true if a rollover should occur.
     */
    @Override
    public boolean isTriggeringEvent(final LogEvent event) {
        return false;
    }

    public CronExpression getCronExpression() {
        return cronExpression;
    }

    /**
     * Creates a ScheduledTriggeringPolicy.
     *
     * @param configuration
     *            the Configuration.
     * @param evaluateOnStartup
     *            check if the file should be rolled over immediately.
     * @param schedule
     *            the cron expression.
     * @return a ScheduledTriggeringPolicy.
     */
    @PluginFactory
    public static CronTriggeringPolicy createPolicy(@PluginConfiguration final Configuration configuration,
            @PluginAttribute("evaluateOnStartup") final String evaluateOnStartup,
            @PluginAttribute("schedule") final String schedule) {
        CronExpression cronExpression;
        final boolean checkOnStartup = Boolean.parseBoolean(evaluateOnStartup);
        if (schedule == null) {
            LOGGER.info("No schedule specified, defaulting to Daily");
            cronExpression = getSchedule(defaultSchedule);
        } else {
            cronExpression = getSchedule(schedule);
            if (cronExpression == null) {
                LOGGER.error("Invalid expression specified. Defaulting to Daily");
                cronExpression = getSchedule(defaultSchedule);
            }
        }
        return new CronTriggeringPolicy(cronExpression, checkOnStartup, configuration);
    }

    private static CronExpression getSchedule(final String expression) {
        try {
            return new CronExpression(expression);
        } catch (final ParseException pe) {
            LOGGER.error("Invalid cron expression - " + expression, pe);
            return null;
        }
    }

    private void rollover() {
        manager.getPatternProcessor().setPrevFileTime(nextRollDate.getTime());
        manager.rollover();
        final Date fireDate = future != null ? future.getFireTime() : new Date();
        final Calendar cal = Calendar.getInstance();
        cal.setTime(fireDate);
        cal.add(Calendar.SECOND, -1);
        nextRollDate = cal.getTime();
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        final boolean stopped = stop(future);
        setStopped();
        return stopped;
    }

    @Override
    public String toString() {
        return "CronTriggeringPolicy(schedule=" + cronExpression.getCronExpression() + ")";
    }

    private class CronTrigger implements Runnable {

        @Override
        public void run() {
            rollover();
        }
    }
}
