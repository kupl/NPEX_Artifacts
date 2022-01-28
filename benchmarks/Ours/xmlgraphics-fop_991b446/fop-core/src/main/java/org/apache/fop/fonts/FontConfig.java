/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.fonts;

import org.apache.avalon.framework.configuration.Configuration;

import org.apache.fop.apps.FOPException;
import org.apache.fop.events.EventProducer;

/**
 * An interface for font configuration information.
 */
public interface FontConfig {

    /**
     * An interface for parsing font configuration information.
     */
    public interface FontConfigParser {

        /**
         * Parse the font configuration and return an object containing all the necessary data.
         *
         * @param cfg the configuration object
         * @param fontManager the font manager
         * @param strict whether or not to enforce strict validation
         * @param eventProducer the event producer for handling font events
         * @return the configuration object
         * @throws FOPException if an error occurs creating the font configuration object
         */
        FontConfig parse(Configuration cfg, FontManager fontManager, boolean strict,
                EventProducer eventProducer) throws FOPException;
    }
}
