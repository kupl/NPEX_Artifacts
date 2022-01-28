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

package org.apache.fop.render.bitmap;

import org.apache.avalon.framework.configuration.Configuration;

import org.apache.xmlgraphics.util.MimeConstants;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.fonts.DefaultFontConfig;
import org.apache.fop.fonts.DefaultFontConfig.DefaultFontConfigParser;
import org.apache.fop.fonts.FontEventAdapter;

/**
 * The PNG renderer configuration data object.
 */
public final class PNGRendererConfig extends BitmapRendererConfig {

    private PNGRendererConfig(DefaultFontConfig fontConfig) {
        super(fontConfig);
    }

    /**
     * The PNG renderer configuration parser.
     */
    public static class PNGRendererConfigParser implements RendererConfigParser {

        public PNGRendererConfig build(FOUserAgent userAgent, Configuration cfg)
                throws FOPException {
            return new PNGRendererConfig(new DefaultFontConfigParser().parse(cfg,
                    userAgent.validateStrictly(),
                    new FontEventAdapter(userAgent.getEventBroadcaster())));
        }

        /** {@inheritDoc} */
        public String getMimeType() {
            return MimeConstants.MIME_PNG;
        }
    }
}
