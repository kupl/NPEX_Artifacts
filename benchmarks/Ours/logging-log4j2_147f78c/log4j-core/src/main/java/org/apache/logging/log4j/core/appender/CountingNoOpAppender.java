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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * No-Operation Appender that counts events.
 */
@Plugin(name = "CountingNoOp", category = "Core", elementType = "appender", printObject = true)
public class CountingNoOpAppender extends AbstractAppender  {

    private final AtomicLong total = new AtomicLong();

    public CountingNoOpAppender(final String name, final Layout<?> layout) {
        super(name, null, layout);
    }

    public long getCount() {
        return total.get();
    }

    @Override
    public void append(final LogEvent event) {
        total.incrementAndGet();
    }

    /**
     * Creates a CountingNoOp Appender.
     */
    @PluginFactory
    public static CountingNoOpAppender createAppender(@PluginAttribute("name") final String name) {
        return new CountingNoOpAppender(Objects.requireNonNull(name), null);
    }
}
