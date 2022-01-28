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
package org.apache.logging.log4j.core.pattern;

import java.util.List;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * VariablesNotEmpty pattern converter.
 */
@Plugin(name = "notEmpty", category = PatternConverter.CATEGORY)
@ConverterKeys({ "notEmpty", "varsNotEmpty", "variablesNotEmpty", })
public final class VariablesNotEmptyReplacementConverter extends LogEventPatternConverter {

    private final List<PatternFormatter> formatters;

    /**
     * Construct the converter.
     * 
     * @param formatters
     *            The PatternFormatters to generate the text to manipulate.
     */
    private VariablesNotEmptyReplacementConverter(final List<PatternFormatter> formatters) {
        super("notEmpty", "notEmpty");
        this.formatters = formatters;
    }

    /**
     * Gets an instance of the class.
     *
     * @param config
     *            The current Configuration.
     * @param options
     *            pattern options, may be null.
     * @return instance of class.
     */
    public static VariablesNotEmptyReplacementConverter newInstance(final Configuration config,
            final String[] options) {
        if (options.length != 1) {
            LOGGER.error("Incorrect number of options on varsNotEmpty. Expected 1 received " + options.length);
            return null;
        }
        if (options[0] == null) {
            LOGGER.error("No pattern supplied on varsNotEmpty");
            return null;
        }
        final PatternParser parser = PatternLayout.createPatternParser(config);
        final List<PatternFormatter> formatters = parser.parse(options[0]);
        return new VariablesNotEmptyReplacementConverter(formatters);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final StringBuilder buf = new StringBuilder();
        int emptyVars = 0;
        int vars = 0;
        for (final PatternFormatter formatter : formatters) {
            final int start = buf.length();
            formatter.format(event, buf);
            final boolean isVariable = formatter.getConverter().isVariable();
            vars += isVariable ? 1 : 0;
            if (isVariable && buf.length() - start == 0) {
                emptyVars++;
            }
        }
        if (vars > 0 && emptyVars != vars) {
            toAppendTo.append(buf.toString());
        }
    }
}
