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
package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Discarding router extends the DefaultAsyncEventRouter by first verifying if the queue is fuller than the specified
 * threshold ratio; if this is the case, log events {@linkplain Level#isMoreSpecificThan(Level) more specific} than
 * the specified threshold level are dropped. If this is not the case, the {@linkplain DefaultAsyncEventRouter
 * default routing rules hold.
 */
public class DiscardingAsyncEventRouter extends DefaultAsyncEventRouter {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final int thresholdQueueRemainingCapacity;
    private final Level thresholdLevel;
    private final AtomicLong discardCount = new AtomicLong();

    /**
     * Constructs a router that will discard events {@linkplain Level#isLessSpecificThan(Level) equal or less specific}
     * than the specified threshold level when the queue is fuller than the specified threshold ratio.
     *
     * @param queueSize size of the queue
     * @param thresholdQueueFilledRatio threshold ratio: if queue is fuller than this, start discarding events
     * @param thresholdLevel level of events to discard
     */
    public DiscardingAsyncEventRouter(final int queueSize, final float thresholdQueueFilledRatio,
            final Level thresholdLevel) {
        thresholdQueueRemainingCapacity = calcThresholdQueueRemainingCapacity(queueSize, thresholdQueueFilledRatio);
        this.thresholdLevel = Objects.requireNonNull(thresholdLevel, "thresholdLevel");
    }

    private static int calcThresholdQueueRemainingCapacity(final int queueSize,
            final float thresholdQueueFilledRatio) {
        if (thresholdQueueFilledRatio >= 1F) {
            return 0;
        }
        if (thresholdQueueFilledRatio <= 0F) {
            return queueSize;
        }
        return (int) ((1 - thresholdQueueFilledRatio) * queueSize);
    }

    @Override
    public EventRoute getRoute(final long backgroundThreadId, final Level level, final int queueSize,
            final int queueRemainingCapacity) {
        if (queueRemainingCapacity <= thresholdQueueRemainingCapacity && level.isLessSpecificThan(thresholdLevel)) {
            if (discardCount.getAndIncrement() == 0) {
                LOGGER.warn("Async remaining queue capacity is {}, discarding event with level {}. " +
                        "This message will only appear once; future events from {} " +
                        "are silently discarded until queue capacity becomes available.",
                        queueRemainingCapacity, level, thresholdLevel);
            }
            return EventRoute.DISCARD;
        }
        return super.getRoute(backgroundThreadId, level, queueSize, queueRemainingCapacity);
    }

    public static long getDiscardCount(final AsyncEventRouter router) {
        if (router instanceof DiscardingAsyncEventRouter) {
            return ((DiscardingAsyncEventRouter) router).discardCount.get();
        }
        return 0;
    }

    public int getThresholdQueueRemainingCapacity() {
        return thresholdQueueRemainingCapacity;
    }

    public Level getThresholdLevel() {
        return thresholdLevel;
    }
}
