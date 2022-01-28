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
package org.apache.log4j.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Able to handle the contents of the LogEvent's MDC and either
 * output the entire contents of the properties, or to output the value of a specific key
 * within the property bundle when this pattern converter has the option set.
 */
@Plugin(name = "Log4j1MdcPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "properties" })
public final class Log4j1MdcPatternConverter extends LogEventPatternConverter {
    /**
     * Name of property to output.
     */
    private final String key;

    /**
     * Private constructor.
     *
     * @param options options, may be null.
     */
    private Log4j1MdcPatternConverter(final String[] options) {
        super(options != null && options.length > 0 ? "Log4j1MDC{" + options[0] + '}' : "Log4j1MDC", "property");
        if (options != null && options.length > 0) {
            key = options[0];
        } else {
            key = null;
        }
    }

    /**
     * Obtains an instance of PropertiesPatternConverter.
     *
     * @param options options, may be null or first element contains name of property to format.
     * @return instance of PropertiesPatternConverter.
     */
    public static Log4j1MdcPatternConverter newInstance(final String[] options) {
        return new Log4j1MdcPatternConverter(options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final Map<String, String> contextMap = event.getContextMap();
        if (key == null) {
            // if there is no additional options, we output every single Key/Value pair for the MDC
            toAppendTo.append('{');
            if (contextMap != null && !contextMap.isEmpty()) {
                final Set<String> eventKeys = new TreeSet<>(contextMap.keySet());
                for (final String eventKey : eventKeys) {
                    toAppendTo.append('{').append(eventKey).append(',').append(contextMap.get(eventKey)).append('}');

                }
            }
            toAppendTo.append('}');
        } else {
            // otherwise they just want a single key output
            final Object val = contextMap.get(key);
            if (val != null) {
                toAppendTo.append(val);
            }
        }
    }
}
