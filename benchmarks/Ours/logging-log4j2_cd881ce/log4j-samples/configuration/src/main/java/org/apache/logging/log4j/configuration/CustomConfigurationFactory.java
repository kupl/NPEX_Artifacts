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
package org.apache.logging.log4j.configuration;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;

import java.net.URI;

/**
 * Factory to construct a  CustomConfiguration.
 */
@Plugin(name = "CustomConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(50)
public class CustomConfigurationFactory extends ConfigurationFactory {

    /**
     * Valid file extensions for XML files.
     */
    public static final String[] SUFFIXES = new String[] {"*"};

    /**
     * Returns the Configuration.
     * @param source The InputSource.
     * @return The Configuration.
     */
    @Override
    public Configuration getConfiguration(final ConfigurationSource source) {
        return new CustomConfiguration(source);
    }

    @Override
    public Configuration getConfiguration(final String name, final URI configLocation) {
        return new CustomConfiguration();
    }

    /**
     * Returns the file suffixes for XML files.
     * @return An array of File extensions.
     */
    @Override
    public String[] getSupportedTypes() {
        return SUFFIXES;
    }
}
