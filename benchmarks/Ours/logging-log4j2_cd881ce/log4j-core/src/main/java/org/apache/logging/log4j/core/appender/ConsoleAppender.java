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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.core.util.CloseShieldOutputStream;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Appends log events to <code>System.out</code> or <code>System.err</code> using a layout specified by the user. The
 * default target is <code>System.out</code>.
 * <p>
 * TODO accessing System.out or .err as a byte stream instead of a writer bypasses the JVM's knowledge of the proper
 * encoding. (RG) Encoding is handled within the Layout. Typically, a Layout will generate a String and then call
 * getBytes which may use a configured encoding or the system default. OTOH, a Writer cannot print byte streams.
 */
@Plugin(name = "Console", category = "Core", elementType = "appender", printObject = true)
public final class ConsoleAppender extends AbstractOutputStreamAppender<OutputStreamManager> {

    private static final long serialVersionUID = 1L;
    private static final String JANSI_CLASS = "org.fusesource.jansi.WindowsAnsiOutputStream";
    private static ConsoleManagerFactory factory = new ConsoleManagerFactory();
    private static final Target DEFAULT_TARGET = Target.SYSTEM_OUT;
    private static final AtomicInteger COUNT = new AtomicInteger();

    private final Target target;
    
    /**
     * Enumeration of console destinations.
     */
    public static enum Target {
        /** Standard output. */
        SYSTEM_OUT,
        /** Standard error output. */
        SYSTEM_ERR
    }

    private ConsoleAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
            final OutputStreamManager manager, final boolean ignoreExceptions, Target target) {
        super(name, layout, filter, ignoreExceptions, true, manager);
        this.target = target;
    }

    /**
     * Creates a Console Appender.
     * 
     * @param layout The layout to use (required).
     * @param filter The Filter or null.
     * @param targetStr The target ("SYSTEM_OUT" or "SYSTEM_ERR"). The default is "SYSTEM_OUT".
     * @param follow If true will follow changes to the underlying output stream.
     * @param name The name of the Appender (required).
     * @param ignore If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise they
     *            are propagated to the caller.
     * @return The ConsoleAppender.
     * @deprecated Use {@link #createAppender(Layout, Filter, Target, String, String, boolean)}.
     */
    @Deprecated
    public static ConsoleAppender createAppender(Layout<? extends Serializable> layout,
            final Filter filter,
            final String targetStr,
            final String name,
            final String follow,
            final String ignore) {
        if (name == null) {
            LOGGER.error("No name provided for ConsoleAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        final boolean isFollow = Boolean.parseBoolean(follow);
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        final Target target = targetStr == null ? DEFAULT_TARGET : Target.valueOf(targetStr);
        return new ConsoleAppender(name, layout, filter, getManager(target, isFollow, layout), ignoreExceptions, target);
    }

    /**
     * Creates a Console Appender.
     * 
     * @param layout The layout to use (required).
     * @param filter The Filter or null.
     * @param target The target (SYSTEM_OUT or SYSTEM_ERR). The default is SYSTEM_OUT.
     * @param follow If true will follow changes to the underlying output stream.
     * @param name The name of the Appender (required).
     * @param ignoreExceptions If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise they
     *            are propagated to the caller.
     * @return The ConsoleAppender.
     */
    @PluginFactory
    public static ConsoleAppender createAppender(
            // @formatter:off
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute(value = "target") Target target,
            @PluginAttribute("name") final String name,
            @PluginAttribute(value = "follow", defaultBoolean = false) final boolean follow,
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final boolean ignoreExceptions) {
            // @formatter:on
        if (name == null) {
            LOGGER.error("No name provided for ConsoleAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        target = target == null ? Target.SYSTEM_OUT : target;
        return new ConsoleAppender(name, layout, filter, getManager(target, follow, layout), ignoreExceptions, target);
    }

    public static ConsoleAppender createDefaultAppenderForLayout(final Layout<? extends Serializable> layout) {
        // this method cannot use the builder class without introducing an infinite loop due to DefaultConfiguration
        return new ConsoleAppender("DefaultConsole-" + COUNT.incrementAndGet(), layout, null,
                getDefaultManager(DEFAULT_TARGET, false, layout), true, DEFAULT_TARGET);
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Builds ConsoleAppender instances.
     */
    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ConsoleAppender> {

        @PluginElement("Layout")
        @Required
        private Layout<? extends Serializable> layout = PatternLayout.createDefaultLayout();

        @PluginElement("Filter")
        private Filter filter;

        @PluginBuilderAttribute
        @Required
        private Target target = DEFAULT_TARGET;

        @PluginBuilderAttribute
        @Required
        private String name;

        @PluginBuilderAttribute
        private boolean follow = false;

        @PluginBuilderAttribute
        private boolean ignoreExceptions = true;

        public Builder setLayout(final Layout<? extends Serializable> aLayout) {
            this.layout = aLayout;
            return this;
        }

        public Builder setFilter(final Filter aFilter) {
            this.filter = aFilter;
            return this;
        }

        public Builder setTarget(final Target aTarget) {
            this.target = aTarget;
            return this;
        }

        public Builder setName(final String aName) {
            this.name = aName;
            return this;
        }

        public Builder setFollow(final boolean shouldFollow) {
            this.follow = shouldFollow;
            return this;
        }

        public Builder setIgnoreExceptions(final boolean shouldIgnoreExceptions) {
            this.ignoreExceptions = shouldIgnoreExceptions;
            return this;
        }

        @Override
        public ConsoleAppender build() {
            return new ConsoleAppender(name, layout, filter, getManager(target, follow, layout), ignoreExceptions, target);
        }
    }

    private static OutputStreamManager getDefaultManager(final Target target, final boolean follow,
            final Layout<? extends Serializable> layout) {
        final OutputStream os = getOutputStream(follow, target);

        // LOG4J2-1176 DefaultConfiguration should not share OutputStreamManager instances to avoid memory leaks.
        final String managerName = target.name() + '.' + follow + "-" + COUNT.get();
        return OutputStreamManager.getManager(managerName, new FactoryData(os, managerName, layout), factory);
    }

    private static OutputStreamManager getManager(final Target target, final boolean follow,
            final Layout<? extends Serializable> layout) {
        final OutputStream os = getOutputStream(follow, target);
        final String managerName = target.name() + '.' + follow;
        return OutputStreamManager.getManager(managerName, new FactoryData(os, managerName, layout), factory);
    }

    private static OutputStream getOutputStream(final boolean follow, final Target target) {
        final String enc = Charset.defaultCharset().name();
        OutputStream outputStream = null;
        try {
            // @formatter:off
            outputStream = target == Target.SYSTEM_OUT ?
                follow ? new PrintStream(new SystemOutStream(), true, enc) : System.out :
                follow ? new PrintStream(new SystemErrStream(), true, enc) : System.err;
            // @formatter:on
            outputStream = new CloseShieldOutputStream(outputStream);
        } catch (final UnsupportedEncodingException ex) { // should never happen
            throw new IllegalStateException("Unsupported default encoding " + enc, ex);
        }
        final PropertiesUtil propsUtil = PropertiesUtil.getProperties();
        if (!propsUtil.isOsWindows() || propsUtil.getBooleanProperty("log4j.skipJansi")) {
            return outputStream;
        }
        try {
            // We type the parameter as a wildcard to avoid a hard reference to Jansi.
            final Class<?> clazz = Loader.loadClass(JANSI_CLASS);
            final Constructor<?> constructor = clazz.getConstructor(OutputStream.class);
            return new CloseShieldOutputStream((OutputStream) constructor.newInstance(outputStream));
        } catch (final ClassNotFoundException cnfe) {
            LOGGER.debug("Jansi is not installed, cannot find {}", JANSI_CLASS);
        } catch (final NoSuchMethodException nsme) {
            LOGGER.warn("{} is missing the proper constructor", JANSI_CLASS);
        } catch (final Exception ex) {
            LOGGER.warn("Unable to instantiate {}", JANSI_CLASS);
        }
        return outputStream;
    }

    /**
     * An implementation of OutputStream that redirects to the current System.err.
     */
    private static class SystemErrStream extends OutputStream {
        public SystemErrStream() {
        }

        @Override
        public void close() {
            // do not close sys err!
        }

        @Override
        public void flush() {
            System.err.flush();
        }

        @Override
        public void write(final byte[] b) throws IOException {
            System.err.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            System.err.write(b, off, len);
        }

        @Override
        public void write(final int b) {
            System.err.write(b);
        }
    }

    /**
     * An implementation of OutputStream that redirects to the current System.out.
     */
    private static class SystemOutStream extends OutputStream {
        public SystemOutStream() {
        }

        @Override
        public void close() {
            // do not close sys out!
        }

        @Override
        public void flush() {
            System.out.flush();
        }

        @Override
        public void write(final byte[] b) throws IOException {
            System.out.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            System.out.write(b, off, len);
        }

        @Override
        public void write(final int b) throws IOException {
            System.out.write(b);
        }
    }

    /**
     * Data to pass to factory method.
     */
    private static class FactoryData {
        private final OutputStream os;
        private final String name;
        private final Layout<? extends Serializable> layout;

        /**
         * Constructor.
         * 
         * @param os The OutputStream.
         * @param type The name of the target.
         * @param layout A Serializable layout
         */
        public FactoryData(final OutputStream os, final String type, final Layout<? extends Serializable> layout) {
            this.os = os;
            this.name = type;
            this.layout = layout;
        }
    }

    /**
     * Factory to create the Appender.
     */
    private static class ConsoleManagerFactory implements ManagerFactory<OutputStreamManager, FactoryData> {

        /**
         * Create an OutputStreamManager.
         * 
         * @param name The name of the entity to manage.
         * @param data The data required to create the entity.
         * @return The OutputStreamManager
         */
        @Override
        public OutputStreamManager createManager(final String name, final FactoryData data) {
            return new OutputStreamManager(data.os, data.name, data.layout, true);
        }
    }

    public Target getTarget() {
        return target;
    }

}
