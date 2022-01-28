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
package org.apache.logging.log4j.web.appender;

import java.io.Serializable;
import javax.servlet.ServletContext;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.web.WebLoggerContextUtils;

/**
 * Logs using the ServletContext's log method
 */
@Plugin(name = "Servlet", category = "Core", elementType = "appender", printObject = true)
public class ServletAppender extends AbstractAppender {

    private static final long serialVersionUID = 1L;

    private final ServletContext servletContext;

    private ServletAppender(final String name, final AbstractStringLayout layout, final Filter filter,
                            final ServletContext servletContext, final boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
        this.servletContext = servletContext;
    }

    @Override
    public void append(final LogEvent event) {
        servletContext.log(((AbstractStringLayout) getLayout()).toSerializable(event));
    }

    /**
     * Create a Servlet Appender.
     * @param layout The layout to use (required). Must extend {@link AbstractStringLayout}.
     * @param filter The Filter or null.
     * @param name The name of the Appender (required).
     * @param ignoreExceptions If {@code true} (default) exceptions encountered when appending events are logged;
     *                         otherwise they are propagated to the caller.
     * @return The ServletAppender.
     */
    @PluginFactory
    public static ServletAppender createAppender(
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("name")
            @Required(message = "No name provided for ServletAppender")
            final String name,
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final boolean ignoreExceptions) {
        final ServletContext servletContext = WebLoggerContextUtils.getServletContext();
        if (servletContext == null) {
            LOGGER.error("No servlet context is available");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        } else if (!(layout instanceof AbstractStringLayout)) {
            LOGGER.error("Layout must be a StringLayout to log to ServletContext");
            return null;
        }
        return new ServletAppender(name, (AbstractStringLayout) layout, filter, servletContext, ignoreExceptions);
    }

}
