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
package org.apache.logging.log4j.core.config.builder.impl;

import java.lang.reflect.Constructor;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.AppenderRefComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.Component;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.CustomLevelComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ScriptComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ScriptFileComponentBuilder;

/**
 * @param <T> The BuiltConfiguration type.
 * @since 2.4
 */
public class DefaultConfigurationBuilder<T extends BuiltConfiguration> implements ConfigurationBuilder<T> {

    private final Component root = new Component();
    private Component loggers;
    private Component appenders;
    private Component filters;
    private Component properties;
    private Component customLevels;
    private Component scripts;
    private final Class<T> clazz;
    private ConfigurationSource source;
    private int monitorInterval = 0;
    private Level level = null;
    private String verbosity = null;
    private String destination = null;
    private String packages = null;
    private String shutdownFlag = null;
    private String advertiser = null;

    private String name = null;

    @SuppressWarnings("unchecked")
    public DefaultConfigurationBuilder() {
        this((Class<T>) BuiltConfiguration.class);
        root.addAttribute("name", "Built");
    }

    public DefaultConfigurationBuilder(final Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("A Configuration class must be provided");
        }
        this.clazz = clazz;
        final List<Component> components = root.getComponents();
        properties = new Component("Properties");
        components.add(properties);
        scripts = new Component("Scripts");
        components.add(scripts);
        customLevels = new Component("CustomLevels");
        components.add(customLevels);
        filters = new Component("Filters");
        components.add(filters);
        appenders = new Component("Appenders");
        components.add(appenders);
        loggers = new Component("Loggers");
        components.add(loggers);
    }

    protected ConfigurationBuilder<T> add(final Component parent, final ComponentBuilder<?> builder) {
        parent.getComponents().add(builder.build());
        return this;
    }

    @Override
    public ConfigurationBuilder<T> add(final AppenderComponentBuilder builder) {
        return add(appenders, builder);
    }

    @Override
    public ConfigurationBuilder<T> add(final CustomLevelComponentBuilder builder) {
        return add(customLevels, builder);
    }

    @Override
    public ConfigurationBuilder<T> add(final FilterComponentBuilder builder) {
        return add(filters, builder);
    }

    @Override
    public ConfigurationBuilder<T> add(final ScriptComponentBuilder builder) {
        return add(scripts, builder);
    }

    @Override
    public ConfigurationBuilder<T> add(final ScriptFileComponentBuilder builder) {
        return add(scripts, builder);
    }

    @Override
    public ConfigurationBuilder<T> add(final LoggerComponentBuilder builder) {
        return add(loggers, builder);
    }

    @Override
    public ConfigurationBuilder<T> add(final RootLoggerComponentBuilder builder) {
        for (final Component c : loggers.getComponents()) {
            if (c.getPluginType().equals(LoggerConfig.ROOT)) {
                throw new ConfigurationException("Root Logger was previously defined");
            }
        }
        return add(loggers, builder);
    }

    @Override
    public ConfigurationBuilder<T> addProperty(final String key, final String value) {
        properties.addComponent(newComponent(key, "Property", value).build());
        return this;
    }

    @Override
    public T build() {
        return build(true);
    }

    @Override
    public T build(final boolean initialize) {
        T configuration;
        try {
            if (source == null) {
                source = ConfigurationSource.NULL_SOURCE;
            }
            final Constructor<T> constructor = clazz.getConstructor(ConfigurationSource.class, Component.class);
            configuration = constructor.newInstance(source, root);
            configuration.setMonitorInterval(monitorInterval);
            configuration.getRootNode().getAttributes().putAll(root.getAttributes());
            if (name != null) {
                configuration.setName(name);
            }
            if (level != null) {
                configuration.getStatusConfiguration().withStatus(level);
            }
            if (verbosity != null) {
                configuration.getStatusConfiguration().withVerbosity(verbosity);
            }
            if (destination != null) {
                configuration.getStatusConfiguration().withDestination(destination);
            }
            if (packages != null) {
                configuration.setPluginPackages(packages);
            }
            if (shutdownFlag != null) {
                configuration.setShutdownHook(shutdownFlag);
            }
            if (advertiser != null) {
                configuration.createAdvertiser(advertiser, source);
            }
        } catch (final Exception ex) {
            throw new IllegalArgumentException("Invalid Configuration class specified", ex);
        }
        configuration.getStatusConfiguration().initialize();
        if (initialize) {
            configuration.initialize();
        }
        return configuration;
    }

    @Override
    public ScriptComponentBuilder newScript(final String name, final String language, final String text) {
        return new DefaultScriptComponentBuilder(this, name, language, text);
    }


    @Override
    public ScriptFileComponentBuilder newScriptFile(final String path) {
        return new DefaultScriptFileComponentBuilder(this, path, path);
    }

    @Override
    public ScriptFileComponentBuilder newScriptFile(final String name, final String path) {
        return new DefaultScriptFileComponentBuilder(this, name, path);
    }

    @Override
    public AppenderComponentBuilder newAppender(final String name, final String type) {
        return new DefaultAppenderComponentBuilder(this, name, type);
    }

    @Override
    public AppenderRefComponentBuilder newAppenderRef(final String ref) {
        return new DefaultAppenderRefComponentBuilder(this, ref);
    }

    @Override
    public LoggerComponentBuilder newAsyncLogger(final String name, final Level level) {
        return new DefaultLoggerComponentBuilder(this, name, level.toString(), "AsyncLogger", false);
    }

    @Override
    public LoggerComponentBuilder newAsyncLogger(final String name, final Level level, final boolean includeLocation) {
        return new DefaultLoggerComponentBuilder(this, name, level.toString(), "AsyncLogger", includeLocation);
    }

    @Override
    public LoggerComponentBuilder newAsyncLogger(final String name, final String level) {
        return new DefaultLoggerComponentBuilder(this, name, level, "AsyncLogger", false);
    }

    @Override
    public LoggerComponentBuilder newAsyncLogger(final String name, final String level, final boolean includeLocation) {
        return new DefaultLoggerComponentBuilder(this, name, level, "AsyncLogger");
    }

    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(final Level level) {
        return new DefaultRootLoggerComponentBuilder(this, level.toString(), "AsyncRoot", false);
    }

    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(final Level level, final boolean includeLocation) {
        return new DefaultRootLoggerComponentBuilder(this, level.toString(), "AsyncRoot", includeLocation);
    }

    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(final String level) {
        return new DefaultRootLoggerComponentBuilder(this, level, "AsyncRoot", false);
    }

    @Override
    public RootLoggerComponentBuilder newAsyncRootLogger(final String level, final boolean includeLocation) {
        return new DefaultRootLoggerComponentBuilder(this, level, "AsyncRoot", includeLocation);
    }


    @Override
    public <B extends ComponentBuilder<B>> ComponentBuilder<B> newComponent(final String type) {
        return new DefaultComponentBuilder<>(this, type);
    }

    @Override
    public <B extends ComponentBuilder<B>> ComponentBuilder<B> newComponent(final String name, final String type) {
        return new DefaultComponentBuilder<>(this, name, type);
    }

    @Override
    public <B extends ComponentBuilder<B>> ComponentBuilder<B> newComponent(final String name, final String type,
                                                                            final String value) {
        return new DefaultComponentBuilder<>(this, name, type, value);
    }


    @Override
    public CustomLevelComponentBuilder newCustomLevel(final String name, final int level) {
        return new DefaultCustomLevelComponentBuilder(this, name, level);
    }

    @Override
    public FilterComponentBuilder newFilter(final String type, final Filter.Result onMatch,
                                            final Filter.Result onMisMatch) {
        return new DefaultFilterComponentBuilder(this, type, onMatch.name(), onMisMatch.name());
    }

    @Override
    public FilterComponentBuilder newFilter(final String type, final String onMatch, final String onMisMatch) {
        return new DefaultFilterComponentBuilder(this, type, onMatch, onMisMatch);
    }

    @Override
    public LayoutComponentBuilder newLayout(final String type) {
        return new DefaultLayoutComponentBuilder(this, type);
    }


    @Override
    public LoggerComponentBuilder newLogger(final String name, final Level level) {
        return new DefaultLoggerComponentBuilder(this, name, level.toString(), true);
    }

    @Override
    public LoggerComponentBuilder newLogger(final String name, final Level level, final boolean includeLocation) {
        return new DefaultLoggerComponentBuilder(this, name, level.toString(), includeLocation);
    }

    @Override
    public LoggerComponentBuilder newLogger(final String name, final String level) {
        return new DefaultLoggerComponentBuilder(this, name, level, true);
    }

    @Override
    public LoggerComponentBuilder newLogger(final String name, final String level, final boolean includeLocation) {
        return new DefaultLoggerComponentBuilder(this, name, level, includeLocation);
    }

    @Override
    public RootLoggerComponentBuilder newRootLogger(final Level level) {
        return new DefaultRootLoggerComponentBuilder(this, level.toString(), true);
    }

    @Override
    public RootLoggerComponentBuilder newRootLogger(final Level level, final boolean includeLocation) {
        return new DefaultRootLoggerComponentBuilder(this, level.toString(), includeLocation);
    }

    @Override
    public RootLoggerComponentBuilder newRootLogger(final String level) {
        return new DefaultRootLoggerComponentBuilder(this, level, true);
    }

    @Override
    public RootLoggerComponentBuilder newRootLogger(final String level, final boolean includeLocation) {
        return new DefaultRootLoggerComponentBuilder(this, level, includeLocation);
    }

    @Override
    public ConfigurationBuilder<T> setAdvertiser(final String advertiser) {
        this.advertiser = advertiser;
        return this;
    }

    /**
     * Set the name of the configuration.
     *
     * @param name the name of the {@link Configuration}. By default is {@code "Assembled"}.
     * @return this builder instance
     */
    @Override
    public ConfigurationBuilder<T> setConfigurationName(final String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the ConfigurationSource.
     *
     * @param configurationSource the {@link ConfigurationSource}
     * @return this builder instance
     */
    @Override
    public ConfigurationBuilder<T> setConfigurationSource(final ConfigurationSource configurationSource) {
        source = configurationSource;
        return this;
    }

    @Override
    public ConfigurationBuilder<T> setMonitorInterval(final String intervalSeconds) {
        monitorInterval = Integer.parseInt(intervalSeconds);
        return this;
    }

    @Override
    public ConfigurationBuilder<T> setPackages(final String packages) {
        this.packages = packages;
        return this;
    }

    @Override
    public ConfigurationBuilder<T> setShutdownHook(final String flag) {
        this.shutdownFlag = flag;
        return this;
    }

    @Override
    public ConfigurationBuilder<T> setStatusLevel(final Level level) {
        this.level = level;
        return this;
    }

    @Override
    public ConfigurationBuilder<T> setVerbosity(final String verbosity) {
        this.verbosity = verbosity;
        return this;
    }

    @Override
    public ConfigurationBuilder<T> setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    @Override
    public ConfigurationBuilder<T> addRootProperty(final String key, final String value) {
        root.getAttributes().put(key, value);
        return this;
    }
}
