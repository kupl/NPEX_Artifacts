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
package org.apache.logging.log4j.core.appender;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.async.AsyncEventRouter;
import org.apache.logging.log4j.core.async.AsyncEventRouterFactory;
import org.apache.logging.log4j.core.async.DiscardingAsyncEventRouter;
import org.apache.logging.log4j.core.async.EventRoute;
import org.apache.logging.log4j.core.async.RingBufferLogEvent;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.Constants;

/**
 * Appends to one or more Appenders asynchronously. You can configure an AsyncAppender with one or more Appenders and an
 * Appender to append to if the queue is full. The AsyncAppender does not allow a filter to be specified on the Appender
 * references.
 */
@Plugin(name = "Async", category = "Core", elementType = "appender", printObject = true)
public final class AsyncAppender extends AbstractAppender {

    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_QUEUE_SIZE = 128;
    private static final String SHUTDOWN = "Shutdown";

    private static final AtomicLong THREAD_SEQUENCE = new AtomicLong(1);

    private final BlockingQueue<Serializable> queue;
    private final int queueSize;
    private final boolean blocking;
    private final long shutdownTimeout;
    private final Configuration config;
    private final AppenderRef[] appenderRefs;
    private final String errorRef;
    private final boolean includeLocation;
    private AppenderControl errorAppender;
    private AsyncThread thread;
    private AsyncEventRouter asyncEventRouter;

    private AsyncAppender(final String name, final Filter filter, final AppenderRef[] appenderRefs,
            final String errorRef, final int queueSize, final boolean blocking, final boolean ignoreExceptions,
            final long shutdownTimeout, final Configuration config, final boolean includeLocation) {
        super(name, filter, null, ignoreExceptions);
        this.queue = new ArrayBlockingQueue<>(queueSize);
        this.queueSize = queueSize;
        this.blocking = blocking;
        this.shutdownTimeout = shutdownTimeout;
        this.config = config;
        this.appenderRefs = appenderRefs;
        this.errorRef = errorRef;
        this.includeLocation = includeLocation;
    }

    @Override
    public void start() {
        final Map<String, Appender> map = config.getAppenders();
        final List<AppenderControl> appenders = new ArrayList<>();
        for (final AppenderRef appenderRef : appenderRefs) {
            final Appender appender = map.get(appenderRef.getRef());
            if (appender != null) {
                appenders.add(new AppenderControl(appender, appenderRef.getLevel(), appenderRef.getFilter()));
            } else {
                LOGGER.error("No appender named {} was configured", appenderRef);
            }
        }
        if (errorRef != null) {
            final Appender appender = map.get(errorRef);
            if (appender != null) {
                errorAppender = new AppenderControl(appender, null, null);
            } else {
                LOGGER.error("Unable to set up error Appender. No appender named {} was configured", errorRef);
            }
        }
        if (appenders.size() > 0) {
            thread = new AsyncThread(appenders, queue);
            thread.setName("AsyncAppender-" + getName());
        } else if (errorRef == null) {
            throw new ConfigurationException("No appenders are available for AsyncAppender " + getName());
        }
        asyncEventRouter = AsyncEventRouterFactory.create(queueSize);

        thread.start();
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        LOGGER.trace("AsyncAppender stopping. Queue still has {} events.", queue.size());
        thread.shutdown();
        try {
            thread.join(shutdownTimeout);
        } catch (final InterruptedException ex) {
            LOGGER.warn("Interrupted while stopping AsyncAppender {}", getName());
        }
        LOGGER.trace("AsyncAppender stopped. Queue has {} events.", queue.size());

        if (DiscardingAsyncEventRouter.getDiscardCount(asyncEventRouter) > 0) {
            LOGGER.trace("AsyncAppender: {} discarded {} events.", asyncEventRouter,
                    DiscardingAsyncEventRouter.getDiscardCount(asyncEventRouter));
        }
    }

    /**
     * Actual writing occurs here.
     *
     * @param logEvent The LogEvent.
     */
    @Override
    public void append(LogEvent logEvent) {
        if (!isStarted()) {
            throw new IllegalStateException("AsyncAppender " + getName() + " is not active");
        }
        if (!(logEvent instanceof Log4jLogEvent)) {
            if (!(logEvent instanceof RingBufferLogEvent)) {
                return; // only know how to Serialize Log4jLogEvents and RingBufferLogEvents
            }
            logEvent = ((RingBufferLogEvent) logEvent).createMemento();
        }
        if (!Constants.FORMAT_MESSAGES_IN_BACKGROUND) { // LOG4J2-898: user may choose
            logEvent.getMessage().getFormattedMessage(); // LOG4J2-763: ask message to freeze parameters
        }
        final Log4jLogEvent coreEvent = (Log4jLogEvent) logEvent;
        logEvent(coreEvent);
    }

    private void logEvent(final Log4jLogEvent logEvent) {
        final Level logLevel = logEvent.getLevel();
        final int remainingCapacity = getQueueRemainingCapacity();
        final EventRoute route = asyncEventRouter.getRoute(thread.getId(), logLevel, queueSize, remainingCapacity);
        route.logMessage(this, logEvent);
    }

    /**
     * FOR INTERNAL USE ONLY.
     *
     * @param logEvent the event to log
     */
    public void logMessageInCurrentThread(final Log4jLogEvent logEvent) {
        logEvent.setEndOfBatch(queue.isEmpty());
        final boolean appendSuccessful = thread.callAppenders(logEvent);
        logToErrorAppenderIfNecessary(appendSuccessful, logEvent);
    }

    /**
     * FOR INTERNAL USE ONLY.
     *
     * @param logEvent the event to log
     */
    public void logMessageInBackgroundThread(final Log4jLogEvent logEvent) {
        final boolean success = blocking ? enqueueOrBlockIfQueueFull(logEvent) : enqueueOrDropIfQueueFull(logEvent);
        logToErrorAppenderIfNecessary(success, logEvent);
    }

    private boolean enqueueOrBlockIfQueueFull(final Log4jLogEvent logEvent) {
        boolean appendSuccessful;
        final Serializable serialized = Log4jLogEvent.serialize(logEvent, includeLocation);
        try {
            // wait for free slots in the queue
            queue.put(serialized);
            appendSuccessful = true;
        } catch (final InterruptedException e) {
            appendSuccessful = handleInterruptedException(serialized);
        }
        return appendSuccessful;
    }

    private boolean enqueueOrDropIfQueueFull(final Log4jLogEvent logEvent) {
        final boolean appendSuccessful = queue.offer(Log4jLogEvent.serialize(logEvent, includeLocation));
        if (!appendSuccessful) {
            error("Appender " + getName() + " is unable to write primary appenders. queue is full");
        }
        return appendSuccessful;
    }

    // LOG4J2-1049: Some applications use Thread.interrupt() to send
    // messages between application threads. This does not necessarily
    // mean that the queue is full. To prevent dropping a log message,
    // quickly try to offer the event to the queue again.
    // (Yes, this means there is a possibility the same event is logged twice.)
    //
    // Finally, catching the InterruptedException means the
    // interrupted flag has been cleared on the current thread.
    // This may interfere with the application's expectation of
    // being interrupted, so when we are done, we set the interrupted
    // flag again.
    private boolean handleInterruptedException(final Serializable serialized) {
        final boolean appendSuccessful = queue.offer(serialized);
        if (!appendSuccessful) {
            LOGGER.warn("Interrupted while waiting for a free slot in the AsyncAppender LogEvent-queue {}",
                    getName());
        }
        // set the interrupted flag again.
        Thread.currentThread().interrupt();
        return appendSuccessful;
    }

    private void logToErrorAppenderIfNecessary(final boolean appendSuccessful, final Log4jLogEvent logEvent) {
        if (!appendSuccessful && errorAppender != null) {
            errorAppender.callAppender(logEvent);
        }
    }
    /**
     * Create an AsyncAppender.
     *
     * @param appenderRefs The Appenders to reference.
     * @param errorRef An optional Appender to write to if the queue is full or other errors occur.
     * @param blocking True if the Appender should wait when the queue is full. The default is true.
     * @param shutdownTimeout How many milliseconds the Appender should wait to flush outstanding log events
     *                        in the queue on shutdown. The default is zero which means to wait forever.
     * @param size The size of the event queue. The default is 128.
     * @param name The name of the Appender.
     * @param includeLocation whether to include location information. The default is false.
     * @param filter The Filter or null.
     * @param config The Configuration.
     * @param ignoreExceptions If {@code "true"} (default) exceptions encountered when appending events are logged;
     *            otherwise they are propagated to the caller.
     * @return The AsyncAppender.
     */
    @PluginFactory
    public static AsyncAppender createAppender(
            // @formatter:off
            @PluginElement("AppenderRef") final AppenderRef[] appenderRefs,
            @PluginAttribute("errorRef") @PluginAliases("error-ref") final String errorRef,
            @PluginAttribute(value = "blocking", defaultBoolean = true) final boolean blocking,
            @PluginAttribute(value = "shutdownTimeout", defaultLong = 0L) final long shutdownTimeout,
            @PluginAttribute(value = "bufferSize", defaultInt = DEFAULT_QUEUE_SIZE) final int size,
            @PluginAttribute("name") final String name,
            @PluginAttribute(value = "includeLocation", defaultBoolean = false) final boolean includeLocation,
            @PluginElement("Filter") final Filter filter,
            @PluginConfiguration final Configuration config,
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final boolean ignoreExceptions) {
            // @formatter:on
        if (name == null) {
            LOGGER.error("No name provided for AsyncAppender");
            return null;
        }
        if (appenderRefs == null) {
            LOGGER.error("No appender references provided to AsyncAppender {}", name);
        }

        return new AsyncAppender(name, filter, appenderRefs, errorRef, size, blocking, ignoreExceptions,
                shutdownTimeout, config, includeLocation);
    }

    /**
     * Thread that calls the Appenders.
     */
    private class AsyncThread extends Thread {

        private volatile boolean shutdown = false;
        private final List<AppenderControl> appenders;
        private final BlockingQueue<Serializable> queue;

        public AsyncThread(final List<AppenderControl> appenders, final BlockingQueue<Serializable> queue) {
            this.appenders = appenders;
            this.queue = queue;
            setDaemon(true);
            setName("AsyncAppenderThread" + THREAD_SEQUENCE.getAndIncrement());
        }

        @Override
        public void run() {
            while (!shutdown) {
                Serializable s;
                try {
                    s = queue.take();
                    if (s != null && s instanceof String && SHUTDOWN.equals(s.toString())) {
                        shutdown = true;
                        continue;
                    }
                } catch (final InterruptedException ex) {
                    break; // LOG4J2-830
                }
                final Log4jLogEvent event = Log4jLogEvent.deserialize(s);
                event.setEndOfBatch(queue.isEmpty());
                final boolean success = callAppenders(event);
                if (!success && errorAppender != null) {
                    try {
                        errorAppender.callAppender(event);
                    } catch (final Exception ex) {
                        // Silently accept the error.
                    }
                }
            }
            // Process any remaining items in the queue.
            LOGGER.trace("AsyncAppender.AsyncThread shutting down. Processing remaining {} queue events.",
                    queue.size());
            int count = 0;
            int ignored = 0;
            while (!queue.isEmpty()) {
                try {
                    final Serializable s = queue.take();
                    if (Log4jLogEvent.canDeserialize(s)) {
                        final Log4jLogEvent event = Log4jLogEvent.deserialize(s);
                        event.setEndOfBatch(queue.isEmpty());
                        callAppenders(event);
                        count++;
                    } else {
                        ignored++;
                        LOGGER.trace("Ignoring event of class {}", s.getClass().getName());
                    }
                } catch (final InterruptedException ex) {
                    // May have been interrupted to shut down.
                    // Here we ignore interrupts and try to process all remaining events.
                }
            }
            LOGGER.trace("AsyncAppender.AsyncThread stopped. Queue has {} events remaining. "
                    + "Processed {} and ignored {} events since shutdown started.", queue.size(), count, ignored);
        }

        /**
         * Calls {@link AppenderControl#callAppender(LogEvent) callAppender} on all registered {@code AppenderControl}
         * objects, and returns {@code true} if at least one appender call was successful, {@code false} otherwise. Any
         * exceptions are silently ignored.
         *
         * @param event the event to forward to the registered appenders
         * @return {@code true} if at least one appender call succeeded, {@code false} otherwise
         */
        boolean callAppenders(final Log4jLogEvent event) {
            boolean success = false;
            for (final AppenderControl control : appenders) {
                try {
                    control.callAppender(event);
                    success = true;
                } catch (final Exception ex) {
                    // If no appender is successful the error appender will get it.
                }
            }
            return success;
        }

        public void shutdown() {
            shutdown = true;
            if (queue.isEmpty()) {
                queue.offer(SHUTDOWN);
            }
        }
    }

    /**
     * Returns the names of the appenders that this asyncAppender delegates to as an array of Strings.
     *
     * @return the names of the sink appenders
     */
    public String[] getAppenderRefStrings() {
        final String[] result = new String[appenderRefs.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = appenderRefs[i].getRef();
        }
        return result;
    }

    /**
     * Returns {@code true} if this AsyncAppender will take a snapshot of the stack with every log event to determine
     * the class and method where the logging call was made.
     *
     * @return {@code true} if location is included with every event, {@code false} otherwise
     */
    public boolean isIncludeLocation() {
        return includeLocation;
    }

    /**
     * Returns {@code true} if this AsyncAppender will block when the queue is full, or {@code false} if events are
     * dropped when the queue is full.
     *
     * @return whether this AsyncAppender will block or drop events when the queue is full.
     */
    public boolean isBlocking() {
        return blocking;
    }

    /**
     * Returns the name of the appender that any errors are logged to or {@code null}.
     *
     * @return the name of the appender that any errors are logged to or {@code null}
     */
    public String getErrorRef() {
        return errorRef;
    }

    public int getQueueCapacity() {
        return queueSize;
    }

    public int getQueueRemainingCapacity() {
        return queue.remainingCapacity();
    }
}
