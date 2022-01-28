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
package org.apache.logging.log4j.core.config.builder.api;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.util.Builder;

/**
 * Interface for building logging configurations.
 * @param <T> The Configuration type created by this builder.
 * @since 2.4
 */
public interface ConfigurationBuilder<T extends Configuration> extends Builder<T> {


    /**
     * Adds a ScriptComponent.
     * @param builder The ScriptComponentBuilder with all of its attributes and sub components set.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> add(ScriptComponentBuilder builder);

    /**
     * Adds a ScriptFileComponent.
     * @param builder The ScriptFileComponentBuilder with all of its attributes and sub components set.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> add(ScriptFileComponentBuilder builder);

    /**
     * Adds an AppenderComponent.
     * @param builder The AppenderComponentBuilder with all of its attributes and sub components set.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> add(AppenderComponentBuilder builder);

    /**
     * Adds a CustomLevel component.
     * @param builder The CustomLevelComponentBuilder with all of its attributes set.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> add(CustomLevelComponentBuilder builder);

    /**
     * Adds a Filter component.
     * @param builder the FilterComponentBuilder with all of its attributes and sub components set.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> add(FilterComponentBuilder builder);

    /**
     * Adds a Logger component.
     * @param builder The LoggerComponentBuilder with all of its attributes and sub components set.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> add(LoggerComponentBuilder builder);

    /**
     * Adds the root Logger component.
     * @param builder The RootLoggerComponentBuilder with all of its attributes and sub components set.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> add(RootLoggerComponentBuilder builder);

    /**
     * Adds a Property key and value.
     * @param key The property key.
     * @param value The property value.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> addProperty(String key, String value);


    /**
     * Returns a builder for creating Async Loggers.
     * @param name The name of the Logger.
     * @param language The script language
     * @param text The script to execute.
     * @return A new ScriptComponentBuilder.
     */
    ScriptComponentBuilder newScript(String name, String language, String text);

    /**
     * Returns a builder for creating Async Loggers.
     * @param path The location of the script file.
     * @return A new ScriptFileComponentBuilder.
     */
    ScriptFileComponentBuilder newScriptFile(String path);


    /**
     * Returns a builder for creating Async Loggers.
     * @param name The name of the script file.
     * @param path The location of the script file.
     * @return A new ScriptFileComponentBuilder.
     */
    ScriptFileComponentBuilder newScriptFile(String name, String path);


    /**
     * Returns a builder for creating Appenders.
     * @param name The name of the Appender.
     * @param pluginName The Plugin type of the Appender.
     * @return A new AppenderComponentBuilder.
     */
    AppenderComponentBuilder newAppender(String name, String pluginName);

    /**
     * Returns a builder for creating AppenderRefs.
     * @param ref The name of the Appender being referenced.
     * @return A new AppenderRefComponentBuilder.
     */
    AppenderRefComponentBuilder newAppenderRef(String ref);

    /**
     * Returns a builder for creating Async Loggers.
     * @param name The name of the Logger.
     * @param level The logging Level to be assigned to the Logger.
     * @return A new LoggerComponentBuilder.
     */
    LoggerComponentBuilder newAsyncLogger(String name, Level level);

    /**
     * Returns a builder for creating Async Loggers.
     * @param name The name of the Logger.
     * @param level The logging Level to be assigned to the Logger.
     * @param includeLocation If true include location information.
     * @return A new LoggerComponentBuilder.
     */
    LoggerComponentBuilder newAsyncLogger(String name, Level level, boolean includeLocation);

    /**
     * Returns a builder for creating Async Loggers.
     * @param name The name of the Logger.
     * @param level The logging Level to be assigned to the Logger.
     * @return A new LoggerComponentBuilder.
     */
    LoggerComponentBuilder newAsyncLogger(String name, String level);

    /**
     * Returns a builder for creating Async Loggers.
     * @param name The name of the Logger.
     * @param level The logging Level to be assigned to the Logger.
     * @param includeLocation If true include location information.
     * @return A new LoggerComponentBuilder.
     */
    LoggerComponentBuilder newAsyncLogger(String name, String level, boolean includeLocation);

    /**
     * Returns a builder for creating the async root Logger.
     * @param level The logging Level to be assigned to the root Logger.
     * @return A new RootLoggerComponentBuilder.
     */
    RootLoggerComponentBuilder newAsyncRootLogger(Level level);


    /**
     * Returns a builder for creating the async root Logger.
     * @param level The logging Level to be assigned to the root Logger.
     * @param includeLocation If true include location information.
     * @return A new RootLoggerComponentBuilder.
     */
    RootLoggerComponentBuilder newAsyncRootLogger(Level level, boolean includeLocation);

    /**
     * Returns a builder for creating the async root Logger.
     * @param level The logging Level to be assigned to the root Logger.
     * @return A new RootLoggerComponentBuilder.
     */
    RootLoggerComponentBuilder newAsyncRootLogger(String level);


    /**
     * Returns a builder for creating the async root Logger.
     * @param level The logging Level to be assigned to the root Logger.
     * @param includeLocation If true include location information.
     * @return A new RootLoggerComponentBuilder.
     */
    RootLoggerComponentBuilder newAsyncRootLogger(String level, boolean includeLocation);

    /**
     * Returns a builder for creating generic components.
     * @param <B> ComponentBuilder target type
     * @param pluginName The Plugin type of the component.
     * @return A new ComponentBuilder.
     */
    <B extends ComponentBuilder<B>> ComponentBuilder<B> newComponent(String pluginName);

    /**
     * Returns a builder for creating generic components.
     * @param <B> ComponentBuilder target type
     * @param name The name of the component (may be null).
     * @param pluginName The Plugin type of the component.
     * @return A new ComponentBuilder.
     */
    <B extends ComponentBuilder<B>> ComponentBuilder<B> newComponent(String name, String pluginName);

    /**
     * Returns a builder for creating generic components.
     * @param <B> ComponentBuilder target type
     * @param name The name of the component (may be null).
     * @param pluginName The Plugin type of the component.
     * @param value The value of the component.
     * @return A new ComponentBuilder.
     */
    <B extends ComponentBuilder<B>> ComponentBuilder<B> newComponent(String name, String pluginName, String value);


    /**
     * Returns a builder for creating CustomLevels
     * @param name The name of the custom level.
     * @param level The integer value to be assigned to the level.
     * @return A new CustomLevelComponentBuilder.
     */
    CustomLevelComponentBuilder newCustomLevel(String name, int level);

    /**
     * Returns a builder for creating Filters.
     * @param pluginName The Plugin type of the Filter.
     * @param onMatch "ACCEPT", "DENY", or "NEUTRAL"
     * @param onMisMatch "ACCEPT", "DENY", or "NEUTRAL"
     * @return A new FilterComponentBuilder.
     */
    FilterComponentBuilder newFilter(String pluginName, Filter.Result onMatch, Filter.Result onMisMatch);

    /**
     * Returns a builder for creating Filters.
     * @param pluginName The Plugin type of the Filter.
     * @param onMatch "ACCEPT", "DENY", or "NEUTRAL"
     * @param onMisMatch "ACCEPT", "DENY", or "NEUTRAL"
     * @return A new FilterComponentBuilder.
     */
    FilterComponentBuilder newFilter(String pluginName, String onMatch, String onMisMatch);

    /**
     * Returns a builder for creating Layouts.
     * @param pluginName The Plugin type of the Layout.
     * @return A new LayoutComponentBuilder.
     */
    LayoutComponentBuilder newLayout(String pluginName);

    /**
     * Returns a builder for creating Loggers.
     * @param name The name of the Logger.
     * @param level The logging Level to be assigned to the Logger.
     * @return A new LoggerComponentBuilder.
     */
    LoggerComponentBuilder newLogger(String name, Level level);

    /**
     * Returns a builder for creating Loggers.
     * @param name The name of the Logger.
     * @param level The logging Level to be assigned to the Logger.
     * @param includeLocation If true include location information.
     * @return A new LoggerComponentBuilder.
     */
    LoggerComponentBuilder newLogger(String name, Level level, boolean includeLocation);

    /**
     * Returns a builder for creating Loggers.
     * @param name The name of the Logger.
     * @param level The logging Level to be assigned to the Logger.
     * @return A new LoggerComponentBuilder.
     */
    LoggerComponentBuilder newLogger(String name, String level);

    /**
     * Returns a builder for creating Loggers.
     * @param name The name of the Logger.
     * @param level The logging Level to be assigned to the Logger.
     * @param includeLocation If true include location information.
     * @return A new LoggerComponentBuilder.
     */
    LoggerComponentBuilder newLogger(String name, String level, boolean includeLocation);

    /**
     * Returns a builder for creating the root Logger.
     * @param level The logging Level to be assigned to the root Logger.
     * @return A new RootLoggerComponentBuilder.
     */
    RootLoggerComponentBuilder newRootLogger(Level level);

    /**
     * Returns a builder for creating the root Logger.
     * @param level The logging Level to be assigned to the root Logger.
     * @param includeLocation If true include location information.
     * @return A new RootLoggerComponentBuilder.
     */
    RootLoggerComponentBuilder newRootLogger(Level level, boolean includeLocation);

    /**
     * Returns a builder for creating the root Logger.
     * @param level The logging Level to be assigned to the root Logger.
     *
     * @return A new RootLoggerComponentBuilder.
     */
    RootLoggerComponentBuilder newRootLogger(String level);

    /**
     * Returns a builder for creating the root Logger.
     * @param level The logging Level to be assigned to the root Logger.
     *
     * @return A new RootLoggerComponentBuilder.
     */
    RootLoggerComponentBuilder newRootLogger(String level, boolean includeLocation);

    /**
     * Set the Advertiser Plugin name.
     * @param advertiser The Advertiser Plugin name.
     * @param includeLocation If true include location information.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> setAdvertiser(String advertiser);

    /**
     * Sets the name of the configuration.
     * @param name the name of the {@link Configuration}. By default is {@code "Constructed"}.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> setConfigurationName(String name);

    /**
     * Sets the configuration source, if one exists.
     * @param configurationSource the ConfigurationSource.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> setConfigurationSource(ConfigurationSource configurationSource);

    /**
     * Sets the interval at which the configuration file should be checked for changes.
     * @param intervalSeconds The number of seconds that should pass between checks of the configuration file.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> setMonitorInterval(String intervalSeconds);

    /**
     * Sets the list of packages to search for plugins.
     * @param packages The comma separated list of packages.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> setPackages(String packages);

    /**
     * Sets whether the shutdown hook should be disabled.
     * @param flag "disable" will prevent the shutdown hook from being set.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> setShutdownHook(String flag);


    /**
     * Sets the level of the StatusLogger.
     * @param level The logging level.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> setStatusLevel(Level level);


    /**
     * Sets whether the logging should include constructing Plugins.
     * @param verbosity "disable" will hide messages from plugin construction.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> setVerbosity(String verbosity);

    /**
     * Add the properties for the root node.
     * @param key The property key.
     * @param value The property value.
     * @return this builder instance.
     */
    ConfigurationBuilder<T> addRootProperty(String key, String value);

    /**
     * Build the configuration and optionally initialize it.
     * @param initialize true if the configuration should be initialized, false otherwise. Generally, Configurations
     *                   should not be initialized when they are constructed.
     * @return The constructed Configuration.
     */
    T build(boolean initialize);
}
