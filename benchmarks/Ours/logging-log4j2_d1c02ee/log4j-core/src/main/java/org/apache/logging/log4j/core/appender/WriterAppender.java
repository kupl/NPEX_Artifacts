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

import java.io.Writer;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.CloseShieldWriter;

/**
 * Appends log events to a {@link Writer}.
 */
@Plugin(name = "Writer", category = "Core", elementType = "appender", printObject = true)
public final class WriterAppender extends AbstractWriterAppender<WriterManager> {

    /**
     * Builds WriterAppender instances.
     */
    public static class Builder implements org.apache.logging.log4j.core.util.Builder<WriterAppender> {

        private Filter filter;

        private boolean follow = false;

        private boolean ignoreExceptions = true;

        private StringLayout layout = PatternLayout.createDefaultLayout();

        private String name;

        private Writer target;

        @Override
        public WriterAppender build() {
            return new WriterAppender(name, layout, filter, getManager(target, follow, layout), ignoreExceptions);
        }

        public Builder setFilter(final Filter aFilter) {
            this.filter = aFilter;
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

        public Builder setLayout(final StringLayout aLayout) {
            this.layout = aLayout;
            return this;
        }

        public Builder setName(final String aName) {
            this.name = aName;
            return this;
        }

        public Builder setTarget(final Writer aTarget) {
            this.target = aTarget;
            return this;
        }
    }
    /**
     * Holds data to pass to factory method.
     */
    private static class FactoryData {
        private final StringLayout layout;
        private final String name;
        private final Writer writer;

        /**
         * Builds instances.
         * 
         * @param writer
         *            The OutputStream.
         * @param type
         *            The name of the target.
         * @param layout
         *            A String layout
         */
        public FactoryData(final Writer writer, final String type, final StringLayout layout) {
            this.writer = writer;
            this.name = type;
            this.layout = layout;
        }
    }

    private static class WriterManagerFactory implements ManagerFactory<WriterManager, FactoryData> {

        /**
         * Creates a WriterManager.
         * 
         * @param name
         *            The name of the entity to manage.
         * @param data
         *            The data required to create the entity.
         * @return The WriterManager
         */
        @Override
        public WriterManager createManager(final String name, final FactoryData data) {
            return new WriterManager(data.writer, data.name, data.layout, true);
        }
    }

    private static WriterManagerFactory factory = new WriterManagerFactory();

    /**
     * Creates a WriterAppender.
     * 
     * @param layout
     *            The layout to use or null to get the default layout.
     * @param filter
     *            The Filter or null.
     * @param target
     *            The target Writer
     * @param follow
     *            If true will follow changes to the underlying output stream.
     *            Use false as the default.
     * @param name
     *            The name of the Appender (required).
     * @param ignore
     *            If {@code "true"} (default) exceptions encountered when
     *            appending events are logged; otherwise they are propagated to
     *            the caller. Use true as the default.
     * @return The ConsoleAppender.
     */
    @PluginFactory
    public static WriterAppender createAppender(StringLayout layout, final Filter filter, final Writer target,
            final String name, final boolean follow, final boolean ignore) {
        if (name == null) {
            LOGGER.error("No name provided for WriterAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new WriterAppender(name, layout, filter, getManager(target, follow, layout), ignore);
    }

    private static WriterManager getManager(final Writer target, final boolean follow, final StringLayout layout) {
        final Writer writer = new CloseShieldWriter(target);
        final String managerName = target.getClass().getName() + "@" + Integer.toHexString(target.hashCode()) + '.'
                + follow;
        return WriterManager.getManager(managerName, new FactoryData(writer, managerName, layout), factory);
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    private WriterAppender(final String name, final StringLayout layout, final Filter filter,
            final WriterManager manager, final boolean ignoreExceptions) {
        super(name, layout, filter, ignoreExceptions, true, manager);
    }

}
